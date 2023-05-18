package com.sedex.connect.company.endpoint

import com.sedex.connect.common.Address
import com.sedex.connect.common.CountryCode
import com.sedex.connect.common.ExistsResponse
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.auth.OnBehalfOf
import com.sedex.connect.common.auth.User
import com.sedex.connect.common.auth.authorisedIdentity
import com.sedex.connect.common.auth.isAdmin
import com.sedex.connect.common.http.ContractRoutes
import com.sedex.connect.company.endpoint.CreateCompanyRoute.Companion.orgCodePath
import com.sedex.connect.company.services.CompanyService
import com.sedex.connect.company.services.UserService
import com.sedex.connect.security.SecurityContext
import com.sedex.connect.user.api.UserJson.auto
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.string

class CompanyExistsRoute(
    private val securityContext: SecurityContext,
    private val companyService: CompanyService,
    private val userService: UserService,
) : ContractRoutes {

    private val companyExistsByCodeSpec = "/companies/exists/" / orgCodePath meta {
        summary = "Checks if a company exists with given org code"
        description = summary
        security = securityContext.noSecurity()
        returning(Status.OK, companyExistsResponseLens to exampleCompanyExistsResponse)
    }

    private val companyExistsByNameSpec = "companies/exists/name" / companyName meta {
        summary = "Checks if a company exists with given name"
        description = summary
        security = securityContext.noSecurity()
        queries += listOf(companyCodeQuery)
        returning(Status.OK, companyExistsResponseLens to exampleCompanyExistsResponse)
    }

    private val companyExistsByAddressSpec = "companies/address/exists" meta {
        summary = "Checks if a company exists with given name"
        description = summary
        security = securityContext.noSecurity()
        queries += listOf(
            addressLineQueryLens,
            cityQueryLens,
            postcodeQueryLens,
            countryQueryLens
        )
        returning(Status.OK, companyExistsResponseLens to exampleCompanyExistsResponse)
    }

    override fun contractRoutes() = listOf(
        companyExistsByCodeSpec bindContract GET to { orgCode ->
            {
                companyService.existsByCode(orgCode)
                    .let { Response(Status.OK).with(companyExistsResponseLens of ExistsResponse(it)) }
            }
        },
        companyExistsByNameSpec bindContract GET to { name ->
            {
                val identity = it.authorisedIdentity()
                val user = (identity as? User) ?: (identity as? OnBehalfOf)?.onBehalfOf

                val orgCode = user?.organisationCode
                    ?: user?.code?.let { userCode ->
                        userService.getUserByCode(userCode)?.orgCode?.let { OrganisationCode(it) }
                    }
                    ?: if (identity.isAdmin()) OrganisationCode(companyCodeQuery(it) ?: "") else null
                companyService.existsByNameForOrg(name.trim(), orgCode)
                    .let { exist ->
                        Response(Status.OK).with(companyExistsResponseLens of ExistsResponse(exist))
                    }
            }
        },
        companyExistsByAddressSpec bindContract GET to { request ->
            val exists = companyService.existsByAddress(
                Address(
                    line1 = addressLineQueryLens(request).trim(),
                    line2 = null,
                    line3 = null,
                    line4 = null,
                    city = cityQueryLens(request).trim(),
                    postCode = postcodeQueryLens(request).trim(),
                    country = CountryCode(countryQueryLens(request))
                )
            )
            Response(Status.OK).with(companyExistsResponseLens of ExistsResponse(exists))
        }
    )

    companion object {
        val companyName = Path.string().of("companyName", "Company Name")
        val companyExistsResponseLens = Body.auto<ExistsResponse>().toLens()
        val exampleCompanyExistsResponse = ExistsResponse(true)
        val addressLineQueryLens = Query.string().required("line1", "Address line 1")
        val cityQueryLens = Query.string().required("city")
        val postcodeQueryLens = Query.string().required("postcode")
        val countryQueryLens = Query.string().required("country")
        val companyCodeQuery = Query.string().optional("orgCode")
    }
}
