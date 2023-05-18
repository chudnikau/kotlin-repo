package com.sedex.connect.company.endpoint

import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.OrganisationMembershipStatus.Active
import com.sedex.connect.common.auth.authorisedIdentity
import com.sedex.connect.common.http.ContractRoutes
import com.sedex.connect.common.http.toResponse
import com.sedex.connect.company.api.OrganisationSummaryResponse
import com.sedex.connect.company.api.orgSummaryResponse
import com.sedex.connect.company.api.organisationSummaryResponseLens
import com.sedex.connect.company.api.toOrgStatus
import com.sedex.connect.company.models.OrganisationSummary
import com.sedex.connect.company.services.CompanyService
import com.sedex.connect.company.services.UserService
import com.sedex.connect.security.SecurityContext
import org.http4k.contract.ContractRoute
import org.http4k.contract.meta
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with

class GetCompanySummaryRoute(
    private val securityContext: SecurityContext,
    private val companyService: CompanyService,
    private val userService: UserService,
) : ContractRoutes {

    private val companySummary = "/companies/self/summary" meta {
        summary = "Gets a company summary"
        description = "Org summary details for use Google and Intercom analytics/tracking"
        security = securityContext.anyLoggedInUser()
        returning(Status.OK, organisationSummaryResponseLens to orgSummaryResponse)
    }

    override fun contractRoutes(): List<ContractRoute> = listOf(
        companySummary bindContract GET to { request ->
            runCatching {
                val userCode = request.authorisedIdentity().requireUserCode()
                val user = userService.getNotNullUserByCode(userCode)
                val orgCode = user.orgCode?.let { OrganisationCode(it) }
                val orgSummary =
                    // Sedex users in Advance has no org-code
                    if (orgCode == null) {
                        OrganisationSummaryResponse(Active)
                    } else {
                        companyService.getSelfOrganisationMembershipSummary(orgCode).toResponse()
                    }
                Response(Status.OK).with(organisationSummaryResponseLens of orgSummary)
            }.getOrElse { it.toResponse() }
        }
    )

    private fun OrganisationSummary.toResponse(): OrganisationSummaryResponse = OrganisationSummaryResponse(
        membershipStatus = membershipStatus?.toOrgStatus(),
        membershipType = membershipType,
        membershipTypeCode = membershipTypeCode,
        countryCode = countryCode
    )
}
