package com.sedex.connect.company.endpoint

import com.sedex.connect.common.http.ContractRoutes
import com.sedex.connect.company.api.updateCompanySizeRequestExample
import com.sedex.connect.company.api.updateCompanySizeRequestLens
import com.sedex.connect.company.endpoint.CreateCompanyRoute.Companion.orgCodePath
import com.sedex.connect.company.services.CompanyService
import com.sedex.connect.security.SecurityContext
import com.sedex.connect.security.permissions.Action.Company.UpsertCompany
import org.http4k.contract.ContractRoute
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.core.Method.PATCH
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with

class UpdateCompanySizeRoute(
    private val securityContext: SecurityContext,
    private val companyService: CompanyService,
) : ContractRoutes {

    private val spec = "/companies" / orgCodePath / "size" meta {
        summary = "Update company size"
        description = summary
        security = securityContext.canPerform(UpsertCompany)
        receiving(updateCompanySizeRequestLens to updateCompanySizeRequestExample)
        returning(Status.OK)
    }

    override fun contractRoutes(): List<ContractRoute> = listOf(
        spec bindContract PATCH to { orgCode, _ ->
            {
                val updateSizeRequest = updateCompanySizeRequestLens(it)
                val result = companyService.updateCompanySize(orgCode, updateSizeRequest.companySize)
                Response(Status.OK).with(UpsertCompanyRoute.companyResponseLens of result)
            }
        }
    )
}
