package com.sedex.connect.company.endpoint

import com.sedex.connect.advancecache.fromAdvanceCode
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.http.ContractRoutes
import com.sedex.connect.company.api.updateCompanySubscriptionRequestExample
import com.sedex.connect.company.api.updateCompanySubscriptionRequestLens
import com.sedex.connect.company.endpoint.CreateCompanyRoute.Companion.orgCodePath
import com.sedex.connect.company.services.CompanyService
import com.sedex.connect.security.SecurityContext
import org.http4k.contract.ContractRoute
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion
import org.http4k.core.with

class UpdateCompanySubscriptionRoute(
    private val securityContext: SecurityContext,
    private val companyService: CompanyService,
) : ContractRoutes {

    private val spec = "/companies" / orgCodePath / "subscription" meta {
        summary = "Update company subscription as admin impersonating supplier"
        description = summary
        security = securityContext.onlySedexAdmin()
        receiving(updateCompanySubscriptionRequestLens to updateCompanySubscriptionRequestExample)
        returning(
            Status.OK to "Company upserted successfully",
        )
    }

    override fun contractRoutes(): List<ContractRoute> = listOf(
        spec bindContract PUT to { orgCode, _ ->
            {
                updateSubscription(orgCode, it)
            }
        }
    )

    private fun updateSubscription(orgCode: OrganisationCode, request: Request): Response {
        val updateSubscriptionRequest = updateCompanySubscriptionRequestLens(request)
        val subscriptionType = updateSubscriptionRequest.subscriptionType.fromAdvanceCode()
            ?: return Response(Companion.BAD_REQUEST).body("Wrong subscription type")
        val membershipStatus = updateSubscriptionRequest.membershipStatus
        val result = companyService.updateCompanySubscription(orgCode, membershipStatus, subscriptionType)
        return Response(Status.OK).with(UpsertCompanyRoute.companyResponseLens of result)
    }
}
