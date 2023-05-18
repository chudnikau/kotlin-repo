package com.sedex.connect.company.pact

import com.sedex.connect.common.EmailAddress
import com.sedex.connect.common.Language.en
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.company.api.CompanyJson
import com.sedex.connect.service.PactIntegration
import com.sedex.connect.user.api.EmailContact
import com.sedex.connect.user.api.OrgContactListResponse
import com.sedex.connect.user.api.UserClient
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UserConsumerTest {

    private val integration = PactIntegration(provider = "user-backend", client = ::UserClient)

    @Test
    fun getOrganisationContacts() {
        val exampleResponse = OrgContactListResponse(
            contacts = listOf(
                EmailContact(
                    userCode = "ZU12345",
                    firstName = "Bob",
                    lastName = "Jones",
                    emailAddress = EmailAddress("bob@jones@email.com"),
                    preferredLanguage = en,
                )
            )
        )
        integration.consumerTest(
            pactBuilder = {
                given("STATE: user with sedex code ZU12345 exists")
                    .uponReceiving("a request to look up organisation contacts from company service")
                    .path("/organisation/contacts/ZC14567")
                    .method("GET")
                    .willRespondWith()
                    .status(OK.code)
                    .body(CompanyJson.asFormatString(exampleResponse))
            },
            interaction = {
                assertEquals(
                    client().getOrganisationContacts(OrganisationCode("ZC14567")),
                    exampleResponse,
                )
            },
        )
    }
}
