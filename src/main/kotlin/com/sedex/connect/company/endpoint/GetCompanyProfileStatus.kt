package com.sedex.connect.company.endpoint

import com.sedex.connect.common.http.ContractRoutes
import com.sedex.connect.common.logging.OperationalEventsJson.auto
import com.sedex.connect.company.endpoint.CreateCompanyRoute.Companion.orgCodePath
import com.sedex.connect.company.models.ProfileStatus
import com.sedex.connect.company.models.ProfileStatus.COMPLETE
import com.sedex.connect.company.services.CompanyService
import com.sedex.connect.security.SecurityContext
import com.sedex.connect.user.api.UserApi
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with

data class CompanyProfileResponse(
    val status: ProfileStatus
)

private val exampleCompanyProfileResponse = CompanyProfileResponse(COMPLETE)

class GetCompanyProfileStatus(
    private val securityContext: SecurityContext,
    private val companyService: CompanyService,
    private val users: UserApi,
) : ContractRoutes {

    private val spec = "/companies" / orgCodePath / "profile-status" meta {
        summary = "Get organisation profile status"
        description = summary
        security = securityContext.anyLoggedInUser()
        returning(OK, orgProfileStatusResponseLens to exampleCompanyProfileResponse)
    }

    override fun contractRoutes() =
        listOf(
            spec bindContract GET to { orgCode, _ ->
                {
                    val companyProfileStatus = CompanyProfileResponse(companyService.getCompanyProfileStatus(orgCode))
                    Response(OK).with(orgProfileStatusResponseLens of companyProfileStatus)
                }
            },
        )

    companion object {
        private val orgProfileStatusResponseLens = Body.auto<CompanyProfileResponse>().toLens()
    }
}
