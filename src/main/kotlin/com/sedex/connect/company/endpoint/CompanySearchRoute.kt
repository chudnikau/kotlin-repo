package com.sedex.connect.company.endpoint

import com.sedex.connect.common.http.ContractRoutes
import com.sedex.connect.company.api.companiesSearchResponseExample
import com.sedex.connect.company.api.companiesSearchResponseLens
import com.sedex.connect.company.services.CompanyService
import com.sedex.connect.security.SecurityContext
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Path
import org.http4k.lens.string

class CompanySearchRoute(
    private val securityContext: SecurityContext,
    private val companyService: CompanyService,
) : ContractRoutes {

    private val companySearchByNameSpec = "/companies/search" / searchTerm meta {
        summary = "Searches company by search term"
        description = summary
        security = securityContext.onlySedexAdmin()
        returning(OK, companiesSearchResponseLens to companiesSearchResponseExample)
    }

    override fun contractRoutes() = listOf(
        companySearchByNameSpec bindContract GET to { searchTerm ->
            {
                val companies = companyService.searchCompaniesByName(searchTerm)
                Response(OK).with(companiesSearchResponseLens of companies)
            }
        }
    )

    companion object {
        val searchTerm = Path.string().of("searchTerm", "Search term")
    }
}
