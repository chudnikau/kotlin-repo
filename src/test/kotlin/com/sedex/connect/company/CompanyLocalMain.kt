package com.sedex.connect.company

import com.sedex.connect.company.CompanyConfiguration.Companion.companyConfigurationFrom
import com.sedex.connect.environment.local.LocalEnvironment.cockroachEnvironment
import com.sedex.connect.environment.local.LocalEnvironment.jwksEnvironment
import com.sedex.connect.environment.local.LocalEnvironment.kafkaEnvironment
import com.sedex.connect.service.infrastructure
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.Environment.Companion.ENV

fun main() {
    val environment = ENV overrides Environment.from(
        mapOf(
            "PORT" to "18089",
            "BACKEND_USER_URL" to "https://sedex-dev-apim-gateway.cloud.gravitee.io/user/v1",
        ) + cockroachEnvironment() + jwksEnvironment() + kafkaEnvironment()
    )

    infrastructure(companyConfigurationFrom(environment))
        .companyApp()
        .startServerAndStopOnShutdown()
}
