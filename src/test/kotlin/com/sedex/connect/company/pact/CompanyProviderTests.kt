package com.sedex.connect.company.pact

import com.sedex.connect.advance.FakeAdvanceAdapter
import com.sedex.connect.common.FailOnUnexpectedErrors
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.TestOperationalEvents
import com.sedex.connect.common.auth.OrganisationAdmin
import com.sedex.connect.common.auth.SedexAdmin
import com.sedex.connect.common.auth.UserCode
import com.sedex.connect.common.auth.pactTestAuthorisationHeaders
import com.sedex.connect.common.database.randomDatabaseName
import com.sedex.connect.common.kafka.InMemoryTestKafka
import com.sedex.connect.common.kafka.KafkaConfig
import com.sedex.connect.common.kafka.toKafkaTopicConfig
import com.sedex.connect.common.service.DeploymentInfo
import com.sedex.connect.common.service.ServiceEnvironment.Local
import com.sedex.connect.common.service.ServiceName
import com.sedex.connect.common.service.ServiceVersion
import com.sedex.connect.company.CompanyConfiguration
import com.sedex.connect.company.api.CompanySize.SMALL
import com.sedex.connect.company.api.exampleSubscriptionRequest
import com.sedex.connect.company.companyApp
import com.sedex.connect.company.fixtures.FakeRepositories
import com.sedex.connect.company.models.AdvanceSubscription
import com.sedex.connect.company.models.PersistedCompany
import com.sedex.connect.company.persistedCompanyExample
import com.sedex.connect.environment.local.LocalEnvironment
import com.sedex.connect.environment.local.LocalEnvironment.localCockroach
import com.sedex.connect.i18n.I18NexusConfig
import com.sedex.connect.i18n.Namespace.IndustryClassifications
import com.sedex.connect.lang.TimeForTesting
import com.sedex.connect.security.SecurityConfig
import com.sedex.connect.service.pactProviderTests
import com.sedex.connect.service.testInfrastructure
import com.sedex.connect.user.FakeUserService
import com.sedex.connect.user.api.UserClient
import dev.minutest.Tests
import dev.minutest.junit.JUnit5Minutests
import org.http4k.cloudnative.env.Environment
import org.http4k.core.Uri

class CompanyProviderTests : JUnit5Minutests {

    @Tests
    fun tests() =
        pactProviderTests {
            val env = Environment.from(LocalEnvironment.kafkaEnvironment())
            val time = TimeForTesting("2022-08-09T09:00:00.00Z")
            val events = TestOperationalEvents(FailOnUnexpectedErrors())
            val dbConfig = localCockroach().copy(database = randomDatabaseName("company-backend"))
            val configuration =
                CompanyConfiguration(
                    port = 0,
                    security = SecurityConfig(Uri.of("/test-jwks")),
                    deployment = DeploymentInfo(ServiceVersion("TEST"), Local),
                    userBackend = Uri.of("http://example.com/not-used"),
                    advanceAdapterBackend = Uri.of("http://example.com/not-used"),
                    appDatabase = dbConfig,
                    i18NexusConfig = I18NexusConfig(
                        baseUri = Uri.of("http://example.com/not-used"),
                        apiKey = "ignored",
                        namespace = IndustryClassifications,
                    ),
                    sendCompanyUpdatesToAdvance = false,
                    kafkaConfig = KafkaConfig.fromEnvironment(env),
                    companyTopicName = "connect.company",
                    companyTopicConfig = env.toKafkaTopicConfig("COMPANY")
                )

            val user = FakeUserService()
            val advanceAdapter = FakeAdvanceAdapter()
            val kafka = InMemoryTestKafka(events = events)
            val repositories = FakeRepositories(time, industryClassificationsFlatJson)
            val infrastructure =
                testInfrastructure(
                    configuration = configuration,
                    events = events,
                    clock = time,
                    timer = time,
                    additionalFilter = pactTestAuthorisationHeaders(),
                )
                    .withInternalHttpClient(ServiceName("user"), user.handler)
                    .withInternalHttpClient(ServiceName("advance-adapter"), advanceAdapter.handler)

            provider(
                infrastructure
                    .companyApp(
                        userApiOverride = UserClient(user.handler),
                        repositories = repositories,
                        translationsOverride = testTranslations,
                        kafka = kafka,
                    )
            )

            beforeEach {
                user.clear()
                advanceAdapter.clear()
                repositories.clear()
                kafka.reset()
            }

            state("industry classifications are available") {}
            state("company ZC111 is present for user ZU123") {
                val orgCode = OrganisationCode("ZC111")
                val company = PersistedCompany(orgCode, persistedCompanyExample)
                repositories.companyRepository.create(company)

                user.addUser(userCode = UserCode("ZU123"), organisationCode = orgCode)
                user.addRoles(UserCode("ZU123"), OrganisationAdmin(orgCode))
            }

            state("company ZC111 is present for user ZU123 and sedex admin ZU1234567890") {
                val orgCode = OrganisationCode("ZC111")
                val company = PersistedCompany(orgCode, persistedCompanyExample)
                repositories.companyRepository.create(company)

                user.addUser(userCode = UserCode("ZU123"), organisationCode = orgCode)
                user.addUser(userCode = UserCode("ZU1234567890"))
                user.addRoles(UserCode("ZU123"), OrganisationAdmin(orgCode))
                user.addRoles(UserCode("ZU1234567890"), SedexAdmin)
            }

            state("company summaries for companies ZC1234, ZC1235") {
                val companyCodes = setOf(OrganisationCode("ZC1234"), OrganisationCode("ZC1235"))
                val persistedCompanies = companyCodes.map { PersistedCompany(it, persistedCompanyExample) }
                persistedCompanies.forEach {
                    repositories.companyRepository.create(it)
                }
            }

            state("company ZC1234 profile COMPLETE") {
                val persistedCompany = PersistedCompany(OrganisationCode("ZC1234"), persistedCompanyExample.copy(companySize = SMALL))
                repositories.companyRepository.create(persistedCompany)
            }

            state("a company name exists") {
                val persistedCompany = PersistedCompany(OrganisationCode("ZC1"), persistedCompanyExample.copy(englishName = "Tomato farm"))
                repositories.companyRepository.create(persistedCompany)
            }

            state("a company name exists with all valid characters") {
                val persistedCompany = PersistedCompany(OrganisationCode("ZC1"), persistedCompanyExample.copy(englishName = "Name123///&(')-"))
                repositories.companyRepository.create(persistedCompany)
            }

            state("company ZC123 with subscription") {
                val orgCode = OrganisationCode("ZC111")
                val company = PersistedCompany(orgCode, persistedCompanyExample)
                repositories.companyRepository.create(company)
                user.addUser(userCode = UserCode("ZU123"), organisationCode = orgCode)
                user.addRoles(UserCode("ZU123"), OrganisationAdmin(orgCode))
                val subscription = AdvanceSubscription(exampleSubscriptionRequest).copy(orgCode = orgCode)
                repositories.advanceSubscriptionRepository.createOrUpdate(subscription)
            }

            state("company ZC555 self summary") {
                val orgCode = OrganisationCode("ZC555")
                val company = PersistedCompany(orgCode, persistedCompanyExample)
                repositories.companyRepository.create(company)
                user.addUser(userCode = UserCode("ZU123"), organisationCode = orgCode)
                user.addRoles(UserCode("ZU123"), OrganisationAdmin(orgCode))
                val subscription = AdvanceSubscription(exampleSubscriptionRequest).copy(orgCode = orgCode)
                repositories.advanceSubscriptionRepository.createOrUpdate(subscription)
            }

            state("company ZC666 subsidiaries") {
                val parentOrgCode = OrganisationCode("ZC666")
                val parentCompany = PersistedCompany(parentOrgCode, persistedCompanyExample)
                repositories.companyRepository.create(parentCompany)
                user.addUser(userCode = UserCode("ZU123"), organisationCode = parentOrgCode)
                user.addRoles(UserCode("ZU123"), OrganisationAdmin(parentOrgCode))

                val subsidiaryOrgCode = OrganisationCode("ZC001")
                val subsidiaryCompany = PersistedCompany(subsidiaryOrgCode, persistedCompanyExample.copy(englishName = "Test org ZC001"))
                repositories.companyRepository.create(subsidiaryCompany)
                repositories.advanceCompanySubsidiaryRepository.create(parentOrgCode, subsidiaryOrgCode)
            }
        }
}
