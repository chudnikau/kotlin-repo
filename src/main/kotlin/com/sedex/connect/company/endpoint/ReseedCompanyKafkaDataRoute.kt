package com.sedex.connect.company.endpoint

import com.sedex.connect.common.http.ContractRoutes
import com.sedex.connect.company.services.CompanyService
import com.sedex.connect.security.SecurityContext
import org.http4k.contract.ContractRoute
import org.http4k.contract.meta
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status

class ReseedCompanyKafkaDataRoute(
    securityContext: SecurityContext,
    private val companyService: CompanyService,
) : ContractRoutes {

    private val companySpec = "/companies/reseed/all" meta {
        summary = "Sends all companies to kafka"
        description = summary
        security = securityContext.onlySedexAdmin()
        returning(Status.OK)
    }

    override fun contractRoutes(): List<ContractRoute> = listOf(
        companySpec bindContract POST to { req ->
            val streamed = companyService.streamCompaniesToKafka()
            Response(Status.OK).body(streamed.toString())
        }
    )
}
