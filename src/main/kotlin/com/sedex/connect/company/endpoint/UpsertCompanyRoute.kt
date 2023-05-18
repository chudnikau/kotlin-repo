package com.sedex.connect.company.endpoint

import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.auth.authorisedIdentity
import com.sedex.connect.common.auth.isAdmin
import com.sedex.connect.common.http.ContractRoutes
import com.sedex.connect.common.http.withError
import com.sedex.connect.common.logging.OperationalEvents
import com.sedex.connect.company.api.CompanyJson
import com.sedex.connect.company.api.CompanyResponse
import com.sedex.connect.company.api.ValidationError
import com.sedex.connect.company.api.trimData
import com.sedex.connect.company.endpoint.CreateCompanyRoute.Companion.companyRequestLens
import com.sedex.connect.company.endpoint.CreateCompanyRoute.Companion.exampleCompany
import com.sedex.connect.company.endpoint.CreateCompanyRoute.Companion.orgCodePath
import com.sedex.connect.company.models.CompanyUpdatingFailed
import com.sedex.connect.company.services.CompanyService
import com.sedex.connect.company.services.UserService
import com.sedex.connect.company.utils.toCompany
import com.sedex.connect.company.validateCompany
import com.sedex.connect.company.withValidationErrors
import com.sedex.connect.security.SecurityContext
import com.sedex.connect.security.permissions.Action.Company.UpsertCompany
import io.konform.validation.Invalid
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with

class UpsertCompanyRoute(
    private val events: OperationalEvents,
    private val securityContext: SecurityContext,
    private val userService: UserService,
    private val companyService: CompanyService,
) : ContractRoutes {

    private val spec = "/companies" / orgCodePath meta {
        summary = "Creates or updates a company"
        description = summary
        security = securityContext.canPerform(UpsertCompany)
        receiving(companyRequestLens to exampleCompany)
        returning(
            OK to "Company upserted successfully",
        )
    }

    override fun contractRoutes() = listOf(
        spec bindContract PUT to { orgCode ->
            handler@{ request ->
                update(orgCode, request)
            }
        }
    )

    private fun update(orgCode: OrganisationCode, request: Request): Response {
        try {
            val company = companyRequestLens(request).trimData().toCompany(orgCode)
            val identity = request.authorisedIdentity()
            val userCode = identity.requireUserCode()
            if (!userService.organisationContainsUserWithCode(orgCode, userCode) && !identity.isAdmin()) {
                return Response(NOT_FOUND)
            }
            val validationResult = validateCompany(company)

            if (validationResult is Invalid) {
                val errors = validationResult.errors
                    .map { ValidationError(field = it.dataPath, reason = it.message) }
                return Response(BAD_REQUEST).withValidationErrors(errors)
            }

            val result = companyService.update(company, orgCode)
            return Response(OK).with(companyResponseLens of result)
        } catch (ex: Exception) {
            events(CompanyUpdatingFailed(orgCode, ex))
            return Response(Status.INTERNAL_SERVER_ERROR).withError(ex)
        }
    }

    companion object {
        val companyResponseLens = CompanyJson.autoBody<CompanyResponse>().toLens()
    }
}
