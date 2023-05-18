package com.sedex.connect.company.endpoint

import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.http.ContractRoutes
import com.sedex.connect.company.api.CompanyJson
import com.sedex.connect.company.services.CompanyService
import com.sedex.connect.security.SecurityContext
import org.http4k.contract.meta
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with

class GetAuditCompaniesRoute(
    private val securityContext: SecurityContext,
    private val companyService: CompanyService,
) : ContractRoutes {

    private val specAuditCompanies = "/audit-companies" meta {
        summary = "Get all audit companies"
        description = summary
        security = securityContext.noSecurity()
        returning(Status.OK, auditCompaniesResponseLens to auditCompaniesResponseExample)
    }

    override fun contractRoutes() = listOf(
        specAuditCompanies bindContract GET to { _ ->
            val companies = companyService.getAuditCompanies()
            Response(Status.OK).with(auditCompaniesResponseLens of companies)
        }
    )

    companion object {
        val auditCompaniesResponseLens = CompanyJson.autoBody<Set<OrganisationCode>>().toLens()

        val auditCompaniesResponseExample = setOf(
            OrganisationCode("ZC001"),
            OrganisationCode("ZC002")
        )
    }
}
