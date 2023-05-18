package com.sedex.connect.company

import com.sedex.connect.advance.company.AdvanceAdapterApi
import com.sedex.connect.advance.company.advanceAdapterServiceName
import com.sedex.connect.common.kafka.Kafka
import com.sedex.connect.common.kafka.kafkaFor
import com.sedex.connect.common.kafka.serializer
import com.sedex.connect.common.service.OpenApiConfiguration
import com.sedex.connect.company.CompanyConfiguration.Companion.companyConfigurationFrom
import com.sedex.connect.company.api.CompanyJson
import com.sedex.connect.company.api.companyObjectMapper
import com.sedex.connect.company.endpoint.AdvanceSubscriptionRoute
import com.sedex.connect.company.endpoint.CompanyExistsRoute
import com.sedex.connect.company.endpoint.CompanySearchRoute
import com.sedex.connect.company.endpoint.CompanySubsidiaryRoute
import com.sedex.connect.company.endpoint.CreateCompanyRoute
import com.sedex.connect.company.endpoint.GetAuditCompaniesRoute
import com.sedex.connect.company.endpoint.GetCompanyProfileStatus
import com.sedex.connect.company.endpoint.GetCompanyRoute
import com.sedex.connect.company.endpoint.GetCompanySummaryBatchRoute
import com.sedex.connect.company.endpoint.GetCompanySummaryRoute
import com.sedex.connect.company.endpoint.IndustryClassificationRoute
import com.sedex.connect.company.endpoint.PingPongRoute
import com.sedex.connect.company.endpoint.ReseedCompanyKafkaDataRoute
import com.sedex.connect.company.endpoint.UpdateCompanySizeRoute
import com.sedex.connect.company.endpoint.UpdateCompanySubscriptionRoute
import com.sedex.connect.company.endpoint.UpsertCompanyRoute
import com.sedex.connect.company.services.AdvanceSubscriptionService
import com.sedex.connect.company.services.CompanyKafkaService
import com.sedex.connect.company.services.CompanyService
import com.sedex.connect.company.services.CompanySubsidiaryService
import com.sedex.connect.company.services.TranslationService
import com.sedex.connect.company.services.UserService
import com.sedex.connect.i18n.I18NexusTranslations
import com.sedex.connect.i18n.Translations
import com.sedex.connect.i18n.cached
import com.sedex.connect.service.RestServer
import com.sedex.connect.service.RestService
import com.sedex.connect.service.ServiceInfrastructure
import com.sedex.connect.service.infrastructure
import com.sedex.connect.user.api.UserApi
import com.sedex.connect.user.api.UserClient
import com.sedex.connect.user.api.userServiceName
import okhttp3.OkHttpClient
import org.apache.kafka.common.serialization.StringSerializer
import org.http4k.client.OkHttp
import org.http4k.cloudnative.env.Environment.Companion.ENV
import java.util.concurrent.TimeUnit.SECONDS

private const val HTTP_TIMEOUT_SECONDS: Long = 60

fun ServiceInfrastructure<CompanyConfiguration>.companyApp(
    repositories: Repositories = DatabaseRepositories(config, databaseFor(config.appDatabase), clock),
    userApiOverride: UserApi? = null,
    userServiceOverride: UserService? = null,
    companyServiceOverride: CompanyService? = null,
    advanceSubscriptionServiceOverride: AdvanceSubscriptionService? = null,
    translationsOverride: Translations? = null,
    kafka: Kafka = kafkaFor(config.kafkaConfig),
): RestServer {
    val handlerWithTimeout = OkHttp(
        client = OkHttpClient.Builder()
            .readTimeout(HTTP_TIMEOUT_SECONDS, SECONDS).build()
    )
    val advanceHttp = internalHttpClientFor(advanceAdapterServiceName, handlerWithTimeout, baseUri = config.advanceAdapterBackend)
    val advanceAdapterApi = AdvanceAdapterApi(advanceHttp)

    val userApi = userApiOverride ?: UserClient(internalHttpClientFor(userServiceName, baseUri = config.userBackend))
    val userService = userServiceOverride ?: UserService(userApi)
    val advanceSubscriptionService = advanceSubscriptionServiceOverride ?: AdvanceSubscriptionService(
        repositories.advanceSubscriptionRepository,
        repositories.advanceCompanySubscriptionsRepository,
        userApi
    )
    val companyKafkaService = CompanyKafkaService(
        kafka.topicProducer(
            config.companyTopicName,
            StringSerializer(),
            companyObjectMapper.serializer(),
        )
    )
    val companyService = companyServiceOverride ?: CompanyService(
        sendToAdvance = config.sendCompanyUpdatesToAdvance,
        advanceAdapterApi = advanceAdapterApi,
        companyRepository = repositories.companyRepository,
        advanceRepository = repositories.advanceCompanyRepository,
        advanceSubscriptionService = advanceSubscriptionService,
        companyKafkaService = companyKafkaService,
    )

    val companySubsidiaryService = CompanySubsidiaryService(repositories.advanceCompanySubsidiaryRepository, companyService)

    val securityContext = CompanySecurityContext(
        parent = securityContextFor(
            config = config.security,
            getUserRoles = userApi::roles,
        ),
        userApi = userApi,
        companyService = companyService,
        companySubsidiaryService = companySubsidiaryService,
    )

    val translations = translationsOverride ?: I18NexusTranslations(config.i18NexusConfig, OkHttp()).cached()
    val translationService = TranslationService(translations)

    kafka.provisionTopics(config)

    return restServer {
        RestService(
            configuration = config,
            openApiConfiguration = OpenApiConfiguration(json = CompanyJson),
            routes = listOf(
                PingPongRoute,
                GetCompanyRoute(events, securityContext, companyService),
                GetCompanySummaryRoute(securityContext, companyService, userService),
                CreateCompanyRoute(securityContext, companyService),
                UpsertCompanyRoute(events, securityContext, userService, companyService),
                IndustryClassificationRoute(repositories.industryClassificationRepository, translationService),
                GetCompanySummaryBatchRoute(securityContext, companyService),
                CompanyExistsRoute(securityContext, companyService, userService),
                GetCompanyProfileStatus(securityContext, companyService, userApi),
                AdvanceSubscriptionRoute(advanceSubscriptionService, securityContext),
                GetAuditCompaniesRoute(securityContext, companyService),
                ReseedCompanyKafkaDataRoute(securityContext, companyService),
                UpdateCompanySubscriptionRoute(securityContext, companyService),
                UpdateCompanySizeRoute(securityContext, companyService),
                CompanySubsidiaryRoute(companySubsidiaryService, securityContext),
                CompanySearchRoute(securityContext, companyService)
            ),
        )
    }
}

fun main() {
    infrastructure(companyConfigurationFrom(ENV))
        .companyApp()
        .startServerAndStopOnShutdown()
}

fun Kafka.provisionTopics(config: CompanyConfiguration) {
    this.admin()
        .setupTopics(mapOf(config.companyTopicName to config.companyTopicConfig))
}
