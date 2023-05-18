package com.sedex.connect.company.pact

import com.sedex.connect.advance.company.AdvanceAdapterApi
import com.sedex.connect.advance.company.AdvanceIndustryClassification
import com.sedex.connect.advance.company.AdvanceMembershipStatus
import com.sedex.connect.advance.company.AdvanceSmdEnrollStatus
import com.sedex.connect.advance.company.AdvanceSubscriptionType
import com.sedex.connect.advance.company.CompanyRequest
import com.sedex.connect.common.Address
import com.sedex.connect.common.CountryCode
import com.sedex.connect.common.EmailAddress
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.TelephoneNumber
import com.sedex.connect.common.VatNumber
import com.sedex.connect.common.auth.UserCode
import com.sedex.connect.company.api.CompanyJson
import com.sedex.connect.service.PactIntegration
import com.sedex.connect.service.authenticatedAs
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class AdvanceAdapterConsumerTest {

    private val integration = PactIntegration(provider = "advance-adapter-backend", client = ::AdvanceAdapterApi)

    private val aUserCode = UserCode("ZU1234")
    private val anOrgCode = OrganisationCode("ZC6789")

    private val exampleUpdateCompanyRequest = CompanyRequest(
        requestId = UUID.fromString("a99230ec-f61d-41d3-a84d-ce472a0f241a"),
        englishName = "Some Org",
        localName = "Le Some Org",
        businessLicenseNumber = "0987654321",
        businessLicenseExpiration = LocalDate.parse("2025-12-25"),
        address = Address(
            line1 = "Address Line 1", line2 = "Address Line 2",
            line3 = null, line4 = null,
            city = "City", country = CountryCode("GB"),
            postCode = "AB12 C34",
        ),
        telephone = TelephoneNumber("0791234567890"),
        email = EmailAddress("someone@example.com"),
        billingAddress = Address(
            line1 = "Billing Address Line 1", line2 = "Billing Address Line 2",
            line3 = null, line4 = null,
            city = "City", country = CountryCode("GB"),
            postCode = "DE56 F78",
        ),
        vatNumber = VatNumber("1234567890"),
        smdEnrollStatus = AdvanceSmdEnrollStatus("OPTED_IN"),
        membershipStatus = AdvanceMembershipStatus("ACTIVE"),
        subscriptionType = AdvanceSubscriptionType("ST002"),

        primaryIndustryClassificationCode = AdvanceIndustryClassification("ZIC1000000"),
        secondaryIndustryClassificationCode = AdvanceIndustryClassification("ZIC1000100"),
    )

    @Test
    fun `test update company pact`() {
        integration.consumerTest(
            pactBuilder = {
                given("a company is updated")
                    .uponReceiving("a request to directly link the buyer and supplier organisations")
                    .path("/companies/$anOrgCode")
                    .method("PUT")
                    .authenticatedAs(aUserCode)
                    .body(CompanyJson.asFormatString(exampleUpdateCompanyRequest))
                    .willRespondWith()
                    .status(OK.code)
            },
            interaction = {
                val response = client(authenticatedAs = aUserCode).sendCompanyToAdvance(exampleUpdateCompanyRequest, anOrgCode)
                assertEquals(OK, response.status)
            },
        )
    }
}
