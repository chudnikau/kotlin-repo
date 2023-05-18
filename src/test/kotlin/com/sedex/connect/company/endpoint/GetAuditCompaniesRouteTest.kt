package com.sedex.connect.company.endpoint

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.sedex.connect.common.Address
import com.sedex.connect.common.CountryCode
import com.sedex.connect.common.EmailAddress
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.TelephoneNumber
import com.sedex.connect.common.randomOrganisationCode
import com.sedex.connect.company.CompanyTestCase
import com.sedex.connect.company.api.MembershipStatus.ACTIVE
import com.sedex.connect.company.endpoint.GetAuditCompaniesRoute.Companion.auditCompaniesResponseLens
import com.sedex.connect.company.models.PersistedCompany
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status
import org.junit.jupiter.api.Test
import java.time.Instant

class GetAuditCompaniesRouteTest : CompanyTestCase() {

    private val anOrgCode = randomOrganisationCode()
    private val aPersistedCompany = buildCompany(anOrgCode)
    private val anAdvanceCompany = buildAdvanceCompany(anOrgCode, Instant.now(), Instant.now())

    @Test
    fun `returns OK with audit companies`() {
        companyRepository.create(aPersistedCompany)
        advanceCompanyRepository.insert(anAdvanceCompany)

        val response = app(Request(GET, "/audit-companies"))

        val companies = auditCompaniesResponseLens(response)

        assertThat(response.status, equalTo(Status.OK))
        assertThat(companies, equalTo(setOf(anOrgCode)))
    }

    private fun buildCompany(orgCode: OrganisationCode): PersistedCompany {
        return PersistedCompany(
            code = orgCode,
            englishName = "My Company",
            localName = null,
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
            membershipStatus = ACTIVE,
            subscriptionType = "ST004",
            companySize = null,
            isUpdatedByConnect = null
        )
    }
}
