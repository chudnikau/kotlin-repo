package com.sedex.connect.company.endpoint

import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.http.ContractRoutes
import com.sedex.connect.common.logging.OperationalEvents
import com.sedex.connect.company.CompanySecurityContext
import com.sedex.connect.company.api.AdvanceSubscriptionResponse
import com.sedex.connect.company.api.CompanySubscriptionResponse
import com.sedex.connect.company.api.companiesRequest
import com.sedex.connect.company.api.companiesRequestExample
import com.sedex.connect.company.api.companiesResponseExample
import com.sedex.connect.company.api.companiesResponseLens
import com.sedex.connect.company.api.companyResponseExample
import com.sedex.connect.company.api.companyResponseLens
import com.sedex.connect.company.api.companySubscriptionResponseExample
import com.sedex.connect.company.api.companySubscriptionResponseLens
import com.sedex.connect.company.endpoint.CreateCompanyRoute.Companion.orgCodePath
import com.sedex.connect.company.models.AdvanceSubscription
import com.sedex.connect.company.models.FailedToGetCompanies
import com.sedex.connect.company.models.toResponse
import com.sedex.connect.company.repository.CompanyNotFoundException
import com.sedex.connect.company.services.CompanyService
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.get
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.contract.security.and
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with

class GetCompanyRoute(
    private val events: OperationalEvents,
    private val securityContext: CompanySecurityContext,
    private val companyService: CompanyService,
) : ContractRoutes {

    private val companyByOrgCode = "/companies" / orgCodePath meta {
        summary = "Gets a company"
        description = summary
        security = securityContext.anyLoggedInUser()
            .and(securityContext.canAccessByOrgCode { orgCodePath(it).value })
        returning(OK, companyResponseLens to companyResponseExample("ZC12345"))
    }

    private val companiesByOrgCodes = "/all-companies" meta {
        summary = "Gets companies"
        description = summary
        security = securityContext.anyLoggedInUser()
            .and(securityContext.canAccessByOrgCodes { companiesRequest(it) })
        receiving(companiesRequest to companiesRequestExample)
        returning(OK, companiesResponseLens to companiesResponseExample)
    }

    private val companyWithSubscriptionSpec = "/companies" / orgCodePath / "with-subscription" meta {
        summary = "Gets a company with subscription"
        description = summary
        security = securityContext.anyLoggedInUser()
            .and(securityContext.canAccessByOrgCode { orgCodePath(it).value })
        returning(OK, companySubscriptionResponseLens to companySubscriptionResponseExample)
    }

    override fun contractRoutes() = listOf(
        companiesByOrgCodes bindContract POST to { request ->
            getAll(companiesRequest(request)).get()
        },
        companyByOrgCode bindContract GET to { orgCode ->
            handler@{
                get(orgCode).get()
            }
        },
        companyWithSubscriptionSpec bindContract GET to { orgCode, _ ->
            {
                getWithSubscription(orgCode).get()
            }
        }
    )

    private fun getAll(orgCodes: Set<OrganisationCode>): Result<Response, Response> {
        return withExceptionHandling(orgCodes) {
            val companies = companyService.getLatestCompaniesByCodes(orgCodes)
                .associateBy { it.code }
            Success(Response(OK).with(companiesResponseLens of companies))
        }
    }

    private fun get(orgCode: OrganisationCode): Result<Response, Response> {
        return withExceptionHandling(setOf(orgCode)) {
            val company = companyService.getCompanyWithSubscription(orgCode)
            Success(Response(OK).with(companyResponseLens of company.first.toResponse()))
        }
    }

    private fun getWithSubscription(orgCode: OrganisationCode): Result<Response, Response> {
        return withExceptionHandling(setOf(orgCode)) {
            val (company, subscription) = companyService.getCompanyWithSubscription(orgCode)
            val response = CompanySubscriptionResponse(
                company = company.toResponse(),
                subscription = subscription?.toResponse()
            )
            Success(Response(OK).with(companySubscriptionResponseLens of response))
        }
    }

    private fun withExceptionHandling(orgCodes: Set<OrganisationCode>, block: () -> Success<Response>): Result<Response, Response> {
        return try {
            block()
        } catch (ex: CompanyNotFoundException) {
            Failure(Response(NOT_FOUND))
        } catch (ex: Exception) {
            events(FailedToGetCompanies(orgCodes, ex.message))
            Failure(Response(BAD_REQUEST, ex.message ?: ""))
        }
    }

    private fun AdvanceSubscription.toResponse(): AdvanceSubscriptionResponse {
        return AdvanceSubscriptionResponse(
            orgCode = orgCode,
            paymentCode = paymentCode,
            nrOfSites = nrOfSites,
            requestedDurationInYears = requestedDurationInYears,
            highTier = highTier,
            endDate = endDate,
            supplierPlusAvailableDate = supplierPlusAvailableDate
        )
    }
}
