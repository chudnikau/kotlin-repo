package com.sedex.connect.company.endpoint

import com.sedex.connect.common.Address
import com.sedex.connect.common.CountryCode
import com.sedex.connect.common.EmailAddress
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.TelephoneNumber
import com.sedex.connect.common.http.ContractRoutes
import com.sedex.connect.company.api.CompanyJson
import com.sedex.connect.company.api.CompanyRequest
import com.sedex.connect.company.api.ValidationErrorResponse
import com.sedex.connect.company.repository.CompanyAlreadyExistsException
import com.sedex.connect.company.services.CompanyService
import com.sedex.connect.company.utils.toCompany
import com.sedex.connect.security.SecurityContext
import org.http4k.contract.div
import org.http4k.contract.meta
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.CONFLICT
import org.http4k.core.Status.Companion.CREATED
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.lens.Path
import org.http4k.lens.string

class CreateCompanyRoute(
    private val securityContext: SecurityContext,
    private val companyService: CompanyService,
) : ContractRoutes {

    private val spec = "/companies" / orgCodePath meta {
        summary = "Creates a new company"
        description = summary
        security = securityContext.noSecurity() // TODO another API is calling this endpoint - is this correct? Does it need blocking at the Gravitee layer?
        receiving(companyRequestLens to exampleCompany)
        returning(
            CREATED to "Company created successfully",
            CONFLICT to "Company already exists",
        )
    }

    override fun contractRoutes() = listOf(
        spec bindContract POST to { orgCode ->
            handler@{ request ->
                val company = companyRequestLens(request).toCompany(orgCode)
                try {
                    companyService.create(company, orgCode)
                    Response(CREATED)
                } catch (ex: CompanyAlreadyExistsException) {
                    Response(CONFLICT)
                } catch (ex: Exception) {
                    Response(INTERNAL_SERVER_ERROR)
                }
            }
        }
    )

    companion object {

        val orgCodePath = Path.string().map(::OrganisationCode).of("orgCode", "Organisation code")
        val companyRequestLens = CompanyJson.autoBody<CompanyRequest>().toLens()
        val validationErrorResponseLens = CompanyJson.autoBody<ValidationErrorResponse>().toLens()

        val exampleCompany = CompanyRequest(
            englishName = "My Company",
            localName = "Local company name",
            businessLicenseNumber = null,
            businessLicenseExpiration = null,
            primaryIndustryClassificationCode = "ZIC1000000",
            secondaryIndustryClassificationCode = "ZIC1000100",
            address = Address(
                line1 = "Somewhere",
                line2 = null, line3 = null, line4 = null,
                postCode = "AB12 3CD",
                city = "London",
                country = CountryCode("GB")
            ),
            telephone = TelephoneNumber("07123456789"),
            email = EmailAddress("email@example.com"),
            billingAddress = null,
            vatNumber = null,
            smdEnrollStatus = "OPTED_IN",
        )
    }
}
