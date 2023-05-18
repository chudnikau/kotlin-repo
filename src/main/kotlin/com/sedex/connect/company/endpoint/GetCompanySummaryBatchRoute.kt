package com.sedex.connect.company.endpoint

import com.sedex.connect.advancecache.fromAdvanceCode
import com.sedex.connect.common.CountryCode
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.SubscriptionType
import com.sedex.connect.common.SubscriptionType.Supplier
import com.sedex.connect.common.http.ContractRoutes
import com.sedex.connect.common.http.ResponseException
import com.sedex.connect.common.http.toResponse
import com.sedex.connect.company.api.CompanyJson
import com.sedex.connect.company.api.CompanyResponse
import com.sedex.connect.company.services.CompanyService
import com.sedex.connect.company.validateCompanyRequest
import com.sedex.connect.company.validateResponseHasAllRequestedCompanies
import com.sedex.connect.security.SecurityContext
import org.http4k.contract.meta
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with

class GetCompanySummaryBatchRoute(
    private val securityContext: SecurityContext,
    private val companyService: CompanyService,
) : ContractRoutes {
    private val spec = "company-summaries" meta {
        summary = "Get summaries of multiple companies"
        description = summary
        security = securityContext.noSecurity()
        receiving(companySummarySetRequest to exampleCompanySummaryBatchRequest)
        returning(OK, companySummaryMapResponse to exampleCompanySummaryMapResponse)
    }

    override fun contractRoutes() =
        listOf(
            spec bindContract POST to { request ->
                try {
                    val requestedCompanyCodes = companySummarySetRequest(request)
                    validateCompanyRequest(requestedCompanyCodes)
                    val latestCompanies = companyService.getLatestCompaniesByCodes(requestedCompanyCodes)

                    if (request.query("validation")?.toBoolean() != false)
                        validateResponseHasAllRequestedCompanies(requestedCompanyCodes, latestCompanies)

                    val response = latestCompanies
                        .map { toCompanySummaryResponse(it) }
                        .associateBy { it.code }
                    Response(OK).with(companySummaryMapResponse of response)
                } catch (e: ResponseException) {
                    e.toResponse().body(e.localizedMessage)
                }
            },
        )

    companion object {
        val companySummaryMapResponse = CompanyJson.autoBody<Map<OrganisationCode, CompanySummaryResponse>>().toLens()

        val exampleCompanySummaryMapResponse = mapOf(
            OrganisationCode("ZC1") to CompanySummaryResponse(
                code = OrganisationCode("ZC1"),
                name = "Company 1",
                countryCode = CountryCode("GB"),
                membershipStatus = "ACTIVE",
                subscriptionType = "ST002",
                subscriberType = Supplier,
            )
        )

        val companySummarySetRequest = CompanyJson.autoBody<Set<OrganisationCode>>().toLens()

        val exampleCompanySummaryBatchRequest = setOf(
            OrganisationCode("ZC1"),
            OrganisationCode("ZC2")
        )
    }

    data class CompanySummaryResponse(
        val code: OrganisationCode,
        val name: String,
        val countryCode: CountryCode?,
        val membershipStatus: String?,
        val subscriptionType: String?,
        val subscriberType: SubscriptionType?,
    )

    private fun toCompanySummaryResponse(company: CompanyResponse): CompanySummaryResponse {
        return CompanySummaryResponse(
            company.code,
            company.englishName,
            company.address.country,
            company.membershipStatus?.name,
            company.subscriptionType,
            company.subscriptionType?.fromAdvanceCode()
        )
    }
}
