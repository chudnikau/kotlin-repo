package com.sedex.connect.company.endpoint

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.auth.OrganisationAdmin
import com.sedex.connect.common.auth.UserCode
import com.sedex.connect.company.CompanyTestCase
import com.sedex.connect.company.endpoint.CreateCompanyRoute.Companion.companyRequestLens
import com.sedex.connect.company.endpoint.CreateCompanyRoute.Companion.exampleCompany
import com.sedex.connect.company.models.PersistedCompany
import com.sedex.connect.company.persistedCompanyExample
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.testing.Approver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class GetCompanyRouteTest : CompanyTestCase() {

    private val anOrgCode = OrganisationCode("ZC1234")

    private val aUserCode = UserCode("ZU1234")

    private val aPersistedCompany = { orgCode: OrganisationCode -> PersistedCompany(orgCode, persistedCompanyExample) }

    private val pastDate = Instant.parse("2000-01-01T00:00:00.00Z")

    private val futureDate = Instant.parse("2050-01-01T00:00:00.00Z")

    @BeforeEach
    override fun beforeEach() {
        super.beforeEach()
        userService.addUser(userCode = aUserCode, organisationCode = anOrgCode)
        userService.addRoles(aUserCode, OrganisationAdmin(anOrgCode))
    }

    @Test
    fun `returns OK - company exists in Connect`() {
        companyRepository.create(aPersistedCompany(anOrgCode))

        val response = authorizeUser(aUserCode)
            .handle(Request(GET, "/companies/$anOrgCode"))

        val company = companyRequestLens(response)

        assertThat(response.status, equalTo(OK))
        assertThat(company, equalTo(exampleCompany))
    }

    @Test
    fun `returns OK - company exists in Advance`() {
        val advanceCompany = buildAdvanceCompany(anOrgCode, Instant.now(), null)
        advanceCompanyRepository.insert(advanceCompany)
        val advanceAddress = buildAdvanceAddress(anOrgCode)
        advanceCompanyRepository.insertAddress(advanceAddress)

        val response = authorizeUser(aUserCode)
            .handle(Request(GET, "/companies/$anOrgCode"))

        val company = companyRequestLens(response)

        assertThat(response.status, equalTo(OK))
        assertThat(company, equalTo(exampleCompany))
    }

    @Test
    fun `returns OK - companies exists in Connect and Advance, with Connect newer`() {
        companyRepository.create(aPersistedCompany(anOrgCode))

        val advanceCompany = buildAdvanceCompanyWithName(
            anOrgCode,
            "Advance Company",
            pastDate,
            pastDate
        ) // to ensure this one won't match the assertion
        advanceCompanyRepository.insert(advanceCompany)
        val advanceAddress = buildAdvanceAddress(anOrgCode)
        advanceCompanyRepository.insertAddress(advanceAddress)

        val response = authorizeUser(aUserCode)
            .handle(Request(GET, "/companies/$anOrgCode"))

        val company = companyRequestLens(response)

        assertThat(response.status, equalTo(OK))
        assertThat(company, equalTo(exampleCompany))
    }

    @Test
    fun `returns OK - companies exists in Connect and Advance, with Advance newer`() {
        val connectCompany = PersistedCompany(orgCode = anOrgCode, persistedCompanyExample).copy(englishName = "Connect Company") // to ensure this one won't match the assertion
        companyRepository.create(connectCompany)

        val advanceCompany = buildAdvanceCompany(anOrgCode, pastDate, futureDate)
        advanceCompanyRepository.insert(advanceCompany)
        val advanceAddress = buildAdvanceAddress(anOrgCode)
        advanceCompanyRepository.insertAddress(advanceAddress)

        val response = authorizeUser(aUserCode)
            .handle(Request(GET, "/companies/$anOrgCode"))

        val company = companyRequestLens(response)

        assertThat(response.status, equalTo(OK))
        assertThat(company, equalTo(exampleCompany))
    }

    @Test
    fun `returns OK - companies exists in Connect and Advance, with Advance newer but only with created time`() {
        val connectCompany = PersistedCompany(orgCode = anOrgCode, persistedCompanyExample).copy(englishName = "Connect Company") // to ensure this one won't match the assertion
        companyRepository.create(connectCompany)

        val advanceCompany = buildAdvanceCompany(anOrgCode, futureDate, null)
        advanceCompanyRepository.insert(advanceCompany)
        val advanceAddress = buildAdvanceAddress(anOrgCode)
        advanceCompanyRepository.insertAddress(advanceAddress)

        val response = authorizeUser(aUserCode)
            .handle(Request(GET, "/companies/$anOrgCode"))

        val company = companyRequestLens(response)

        assertThat(response.status, equalTo(OK))
        assertThat(company, equalTo(exampleCompany))
    }

    @Test
    fun `returns NOT_FOUND when company doesn't exist`() {
        val response = authorizeUser(aUserCode)
            .handle(Request(GET, "/companies/$anOrgCode"))

        assertThat(response.status, equalTo(NOT_FOUND))
    }

    @Test
    fun `returns NOT_FOUND if user is not associated with the company`() {
        companyRepository.create(aPersistedCompany(anOrgCode))
        val anotherUserCode = anotherUser()

        val response = authorizeUser(anotherUserCode)
            .handle(Request(GET, "/companies/$anOrgCode"))

        assertThat(response.status, equalTo(FORBIDDEN))
    }

    @Test
    fun `returns company with subscription by org code`(approver: Approver) {
        companyRepository.create(aPersistedCompany(anOrgCode))
        createSubscription(anOrgCode)

        val request = Request(GET, "/companies/$anOrgCode/with-subscription")
        val response = authorizeUser(aUserCode)
            .handle(request)

        assertEquals(OK, response.status)

        approver.assertApproved(response)
    }

    private fun anotherUser(): UserCode {
        val anotherUserCode = UserCode(aUserCode.value + "1")
        val anotherOrganisationCode = OrganisationCode(anOrgCode.value + "1")
        userService.addUser(userCode = anotherUserCode, organisationCode = anotherOrganisationCode)
        userService.addRoles(anotherUserCode, OrganisationAdmin(anotherOrganisationCode))
        return anotherUserCode
    }
}
