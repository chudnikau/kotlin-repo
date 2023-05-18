package com.sedex.connect.company.endpoint

import com.fasterxml.jackson.module.kotlin.readValue
import com.natpryce.hamkrest.anyElement
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.isA
import com.sedex.connect.common.Address
import com.sedex.connect.common.CountryCode
import com.sedex.connect.common.EmailAddress
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.TelephoneNumber
import com.sedex.connect.common.assertApproved
import com.sedex.connect.common.auth.OrganisationAdmin
import com.sedex.connect.common.auth.SedexAdmin
import com.sedex.connect.common.auth.UserCode
import com.sedex.connect.common.http.HttpIn
import com.sedex.connect.common.letters
import com.sedex.connect.common.randomString
import com.sedex.connect.company.CompanyTestCase
import com.sedex.connect.company.api.CompanyJson
import com.sedex.connect.company.api.CompanyRequest
import com.sedex.connect.company.api.CompanyResponse
import com.sedex.connect.company.api.ValidationError
import com.sedex.connect.company.api.ValidationErrorResponse
import com.sedex.connect.company.endpoint.CreateCompanyRoute.Companion.exampleCompany
import com.sedex.connect.company.endpoint.CreateCompanyRoute.Companion.validationErrorResponseLens
import com.sedex.connect.company.models.PersistedCompany
import com.sedex.connect.company.persistedCompanyExample
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.testing.Approver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class UpsertCompanyRouteTest : CompanyTestCase() {

    private val anOrgCode = OrganisationCode("ZC1234")

    private val aUserCode = UserCode("ZU1234")

    private val aPersistedCompany = PersistedCompany(anOrgCode, persistedCompanyExample)

    @BeforeEach
    override fun beforeEach() {
        super.beforeEach()
        userService.addUser(userCode = aUserCode, organisationCode = anOrgCode)
        userService.addRoles(aUserCode, OrganisationAdmin(anOrgCode))
        advanceAdapter.willRespond(anOrgCode, Response(OK))
    }

    @ParameterizedTest
    @MethodSource("getValidCompanyRequests")
    fun `returns OK`(companyRequest: CompanyRequest, expectedResponse: CompanyRequest) {
        companyRepository.create(aPersistedCompany)
        val response = authorizeUser(aUserCode)
            .handle(Request(PUT, "/companies/$anOrgCode").body(companyRequest.json()))
        val companyResponse = CompanyJson.mapper.readValue<CompanyResponse>(response.body.payload.array())

        assertThat(response.status, equalTo(OK))

        assertEquals(expectedResponse.address, companyResponse.address)
        assertEquals(expectedResponse.englishName, companyResponse.englishName)
        assertEquals(expectedResponse.localName, companyResponse.localName)
        assertEquals(expectedResponse.businessLicenseNumber, companyResponse.businessLicenseNumber)
        assertEquals(expectedResponse.businessLicenseExpiration, companyResponse.businessLicenseExpiration)
        assertEquals(expectedResponse.primaryIndustryClassificationCode, companyResponse.primaryIndustryClassificationCode)
        assertEquals(expectedResponse.secondaryIndustryClassificationCode, companyResponse.secondaryIndustryClassificationCode)
        assertEquals(expectedResponse.telephone, companyResponse.telephone)
        assertEquals(expectedResponse.billingAddress, companyResponse.billingAddress)
        assertEquals(expectedResponse.vatNumber, companyResponse.vatNumber)
        assertEquals(expectedResponse.smdEnrollStatus, companyResponse.smdEnrollStatus)

        assertThat(companyMessages.size, equalTo(1))
    }

    @Test
    fun `returns OK when company already exists`(approver: Approver) {
        companyRepository.create(aPersistedCompany)
        val updatedCompany = exampleCompany.copy(englishName = "Updated")

        val response = authorizeUser(aUserCode)
            .handle(Request(PUT, "/companies/$anOrgCode").body(updatedCompany.json()))

        assertThat(response.status, equalTo(OK))

        approver.assertApproved(response)

        assertThat(companyMessages.size, equalTo(1))
    }

    @Test
    fun `produces message to kafka when company is created`(approver: Approver) {
        companyRepository.create(aPersistedCompany)
        val response = authorizeUser(aUserCode)
            .handle(Request(PUT, "/companies/$anOrgCode").body(exampleCompany.json()))

        assertThat(response.status, equalTo(OK))
        assertThat(companyMessages.size, equalTo(1))

        assertThat(companyMessages.size, equalTo(1))
        approver.assertApproved(companyMessages[0].value().json())
    }

    @Test
    fun `returns NOT_FOUND if user is not associated with the company`() {
        val anotherUserCode = anotherUser()

        val response = authorizeUser(anotherUserCode)
            .handle(Request(PUT, "/companies/$anOrgCode").body(exampleCompany.json()))

        assertThat(response.status, equalTo(NOT_FOUND))
    }

    private fun anotherUser(): UserCode {
        val anotherUserCode = UserCode(aUserCode.value + "1")
        val anotherOrganisationCode = OrganisationCode(anOrgCode.value + "1")
        userService.addUser(userCode = anotherUserCode, organisationCode = anotherOrganisationCode)
        userService.addRoles(anotherUserCode, OrganisationAdmin(anotherOrganisationCode))
        return anotherUserCode
    }

    @Test
    fun `returns BAD_REQUEST if payload fails validation`() {
        val invalidCompany = InvalidCompany(code = anOrgCode)

        val response = authorizeUser(aUserCode)
            .handle(Request(PUT, "/companies/$anOrgCode").body(invalidCompany.json()))

        assertThat(response.status, equalTo(BAD_REQUEST))
        assertFalse(exists(anOrgCode))
    }

    @Test
    fun `returns BAD_REQUEST if payload fails (additional) validation`() {
        companyRepository.create(aPersistedCompany)
        val updatedCompany = exampleCompany.copy(email = EmailAddress("not-a-valid-email"))

        val response = authorizeUser(aUserCode)
            .handle(Request(PUT, "/companies/$anOrgCode").body(updatedCompany.json()))

        assertThat(response.status, equalTo(BAD_REQUEST))
        assertThat(get(anOrgCode), equalTo(aPersistedCompany))
        val validationError = validationErrorResponseLens(response)
        assertThat(validationError, equalTo(ValidationErrorResponse(listOf(ValidationError(".email", "must match the expected pattern: ${EmailAddress.emailRegex}")))))
        assertThat(
            events.log,
            anyElement(
                isA<HttpIn>(
                    has(HttpIn::clientError, equalTo("Validation failed for .email"))
                )
            )
        )
    }

    @Test
    @Disabled
    fun `returns BAD_REQUEST if advance adapter returns BAD_REQUEST`() {
        advanceAdapter.willRespond(anOrgCode, Response(BAD_REQUEST))
        val response = authorizeUser(aUserCode)
            .handle(Request(PUT, "/companies/$anOrgCode").body(exampleCompany.json()))

        assertThat(response.status, equalTo(BAD_REQUEST))
        assertFalse(exists(anOrgCode))
    }

    @Test
    fun `returns BAD_REQUEST if payload of any input property more than 1000 characters`() {
        companyRepository.create(aPersistedCompany)
        val updatedCompany = exampleCompany.copy(localName = randomString(minLength = 1001, maxLength = 1002, symbolSet = letters))

        val response = authorizeUser(aUserCode)
            .handle(Request(PUT, "/companies/$anOrgCode").body(updatedCompany.json()))

        assertEquals(BAD_REQUEST, response.status)
        val validationError = validationErrorResponseLens(response)
        assertEquals(validationError.errors[0].field, ".localName")
        assertEquals(validationError.errors[0].reason, "must have at most 1000 characters")
    }

    @Test
    fun `should update as admin`(approver: Approver) {
        userService.addRoles(aUserCode, SedexAdmin)
        companyRepository.create(aPersistedCompany)
        val updatedCompany = exampleCompany.copy(englishName = "Updated")

        val response = authorizeUser(userCode = aUserCode, roles = setOf(SedexAdmin))
            .handle(Request(PUT, "/companies/$anOrgCode").body(updatedCompany.json()))

        assertThat(response.status, equalTo(OK))

        approver.assertApproved(response)
    }

    @ParameterizedTest
    @MethodSource("failUpdateCompanyRequest")
    fun `returns BAD_REQUEST if payload has null value for primary or secondary industry classification codes`(companyRequest: CompanyRequest) {
        companyRepository.create(aPersistedCompany)

        val response = authorizeUser(aUserCode)
            .handle(Request(PUT, "/companies/$anOrgCode").body(companyRequest.json()))

        assertEquals(BAD_REQUEST, response.status)
        val validationError = validationErrorResponseLens(response)
        assertEquals(validationError.errors[0].reason, "Primary and secondary industry classification codes must both exist or not exist")
    }

    companion object {
        @JvmStatic
        private fun getValidCompanyRequests(): List<Arguments> {
            val secondTestCompany = exampleCompany.copy(
                englishName = "  My Company  ",
                address = Address(
                    line1 = "  Somewhere  ",
                    line2 = null, line3 = null, line4 = null,
                    postCode = "  AB12 3CD  ",
                    city = "  London  ",
                    country = CountryCode("GB")
                ),
                telephone = TelephoneNumber("  07123456789 "),
                email = EmailAddress("  email@example.com  ")
            )
            val thirdTestCompany = exampleCompany.copy(primaryIndustryClassificationCode = null, secondaryIndustryClassificationCode = null)
            return listOf(
                Arguments.of(exampleCompany, exampleCompany),
                Arguments.of(secondTestCompany, exampleCompany),
                Arguments.of(thirdTestCompany, thirdTestCompany)
            )
        }

        @JvmStatic
        private fun failUpdateCompanyRequest() = listOf(
            exampleCompany.copy(primaryIndustryClassificationCode = null),
            exampleCompany.copy(secondaryIndustryClassificationCode = null)
        )
    }
}
