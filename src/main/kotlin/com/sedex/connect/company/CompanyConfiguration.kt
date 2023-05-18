package com.sedex.connect.company

import com.sedex.connect.common.database.DatabaseConfig
import com.sedex.connect.common.database.DatabaseName
import com.sedex.connect.common.database.cockroachConfigFrom
import com.sedex.connect.common.kafka.KafkaConfig
import com.sedex.connect.common.kafka.KafkaTopicConfig
import com.sedex.connect.common.kafka.toKafkaTopicConfig
import com.sedex.connect.common.service.DeploymentInfo
import com.sedex.connect.common.service.DeploymentInfo.Companion.deploymentInfoFrom
import com.sedex.connect.common.service.ServiceConfiguration
import com.sedex.connect.common.service.ServiceConfiguration.Companion.HTTP_PORT
import com.sedex.connect.common.service.ServiceName
import com.sedex.connect.common.service.ServiceOwner
import com.sedex.connect.common.service.ServiceOwner.Companion.ac12
import com.sedex.connect.i18n.I18NexusConfig
import com.sedex.connect.i18n.I18NexusConfig.Companion.i18NexusConfigFrom
import com.sedex.connect.i18n.Namespace.IndustryClassifications
import com.sedex.connect.security.SecurityConfig
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.core.Uri
import org.http4k.lens.boolean
import org.http4k.lens.string
import org.http4k.lens.uri

data class CompanyConfiguration(
    override val deployment: DeploymentInfo,
    override val port: Int,
    val security: SecurityConfig,
    val appDatabase: DatabaseConfig,
    val userBackend: Uri,
    val advanceAdapterBackend: Uri,
    val i18NexusConfig: I18NexusConfig,
    val sendCompanyUpdatesToAdvance: Boolean,
    val kafkaConfig: KafkaConfig,
    val companyTopicName: String,
    val companyTopicConfig: KafkaTopicConfig,
) : ServiceConfiguration {
    override val serviceName: ServiceName = ServiceName("company")
    override val serviceOwner: ServiceOwner = ac12

    companion object {

        private val userBackend = EnvironmentKey
            .uri()
            .defaulted("BACKEND_USER_URL", Uri.of("http://user:443"))

        private val advanceAdapterBackend = EnvironmentKey
            .uri()
            .defaulted("BACKEND_ADVANCE_ADAPTER_URL", Uri.of("http://advance-adapter:443"))

        private val KAFKA_APPLICATION_VERSION = EnvironmentKey.string().defaulted("KAFKA_APPLICATION_VERSION", "0.0.1")

        fun companyConfigurationFrom(env: Environment): CompanyConfiguration {
            val kafkaApplicationVersion = KAFKA_APPLICATION_VERSION(env)
            return CompanyConfiguration(
                deployment = deploymentInfoFrom(env),
                port = HTTP_PORT(env),
                security = SecurityConfig.securityConfigFrom(env),
                appDatabase = cockroachConfigFrom(env).copy(database = DatabaseName("company-backend")),
                userBackend = userBackend.extract(env),
                advanceAdapterBackend = advanceAdapterBackend.extract(env),
                i18NexusConfig = i18NexusConfigFrom(env overrides Environment.from(mapOf("I18NEXUS_NAMESPACE" to IndustryClassifications.code))),
                sendCompanyUpdatesToAdvance = EnvironmentKey.boolean().defaulted("SEND_COMPANY_INFO_UPDATES_TO_ADVANCE", false).extract(env),
                kafkaConfig = KafkaConfig.fromEnvironment(env),
                companyTopicName = "connect.company-$kafkaApplicationVersion",
                companyTopicConfig = env.toKafkaTopicConfig("COMPANY")
            )
        }
    }
}
