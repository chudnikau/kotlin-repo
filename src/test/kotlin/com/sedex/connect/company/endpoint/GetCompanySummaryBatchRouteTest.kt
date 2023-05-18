package com.sedex.connect.company.endpoint

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.randomOrganisationCode
import com.sedex.connect.company.CompanyTestCase
import com.sedex.connect.company.api.CompanyJson
import com.sedex.connect.company.models.AdvanceCompany
import com.sedex.connect.company.models.PersistedCompany
import com.sedex.connect.company.persistedCompanyExample
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class GetCompanySummaryBatchRouteTest : CompanyTestCase() {
    private val persistedCompanyCodes = setOf(randomOrganisationCode(), randomOrganisationCode())
    private val persistedCompanies = persistedCompanyCodes.map { PersistedCompany(it, persistedCompanyExample.copy(englishName = "Some Company$it")) }
    private val request = Request(POST, "/company-summaries")
    private val pastDate = Instant.parse("2000-01-01T00:00:00.00Z")
    private val futureDate = Instant.parse("2050-01-01T00:00:00.00Z")

    @BeforeEach
    override fun beforeEach() {
        super.beforeEach()
    }

    @Test
    fun `returns OK with only Connect companies`() {
        persistedCompanies.forEach {
            companyRepository.create(it)
        }

        val requestBody = CompanyJson.mapper.writeValueAsString(persistedCompanyCodes)
        val response = app(request.body(requestBody))

        val companySummaryMapResponse = GetCompanySummaryBatchRoute.companySummaryMapResponse(response)
        val companyCodesResponse = companySummaryMapResponse.keys
        val companySummariesResponse = companySummaryMapResponse.values
        val persistedCompanySummaries = persistedCompanies.map { companySummaryMapResponse[it.code] }

        assertThat(response.status, equalTo(Status.OK))
        assertTrue(companyCodesResponse.containsAll(persistedCompanyCodes))
        assertTrue(companySummariesResponse.containsAll(persistedCompanySummaries))
    }

    @Test
    fun `returns OK with only Advance companies`() {
        persistedCompanyCodes.forEach { code ->
            val advanceCompany = buildAdvanceCompany(code, Instant.now(), null)
            storeAdvanceCompanyWithAddress(advanceCompany)
        }

        val requestBody = CompanyJson.mapper.writeValueAsString(persistedCompanyCodes)
        val response = app(request.body(requestBody))

        val companySummaryMapResponse = GetCompanySummaryBatchRoute.companySummaryMapResponse(response)
        val companyCodesResponse = companySummaryMapResponse.keys
        val companySummariesResponse = companySummaryMapResponse.values
        val persistedCompanySummaries = persistedCompanies.map { companySummaryMapResponse[it.code] }

        assertThat(response.status, equalTo(Status.OK))
        assertTrue(companyCodesResponse.containsAll(persistedCompanyCodes))
        assertTrue(companySummariesResponse.containsAll(persistedCompanySummaries))
    }

    @Test
    fun `returns OK and latest company for each code with Connect and Advance companies`() {
        val connectCompanies = listOf(
            buildConnectCompanyWithName("org1", "Connect Company 1"),
            buildConnectCompanyWithName("org2", "Connect Company 2"),
            buildConnectCompanyWithName("org3", "Connect Company 3")
        )
        connectCompanies.forEach {
            companyRepository.create(it)
        }

        val advanceCompanies = listOf(
            buildAdvanceCompanyWithName(OrganisationCode("org1"), "Advance Company 1", pastDate, pastDate),
            buildAdvanceCompanyWithName(OrganisationCode("org2"), "Advance Company 2", futureDate, null),
            buildAdvanceCompanyWithName(OrganisationCode("org4"), "Advance Company 4", Instant.now(), null)
        )
        advanceCompanies.forEach {
            storeAdvanceCompanyWithAddress(it)
        }

        val expectedOrgCodes = setOf(OrganisationCode("org1"), OrganisationCode("org2"), OrganisationCode("org3"), OrganisationCode("org4"))
        val requestBody = CompanyJson.mapper.writeValueAsString(expectedOrgCodes)
        val response = app(request.body(requestBody))

        val companySummaryMapResponse = GetCompanySummaryBatchRoute.companySummaryMapResponse(response)
        val companyCodesResponse = companySummaryMapResponse.keys
        val companySummariesResponse = companySummaryMapResponse.values
        val companyNamesResponse = companySummariesResponse.map { it.name }

        assertThat(response.status, equalTo(Status.OK))
        assertThat(companyCodesResponse.size, equalTo(4))
        assertTrue(companyCodesResponse.containsAll(expectedOrgCodes))
        val expectedNames = setOf("Connect Company 1", "Advance Company 2", "Connect Company 3", "Advance Company 4")
        assertTrue(companyNamesResponse.containsAll(expectedNames))
    }

    @Test
    fun `returns NOT_FOUND when one of the companies doesn't exist`() {
        persistedCompanies.forEach {
            companyRepository.create(it)
        }

        val nonExistentCodes = setOf(randomOrganisationCode())
        val companyCodesRequest = nonExistentCodes.plus(persistedCompanyCodes)
        val requestBody = CompanyJson.mapper.writeValueAsString(companyCodesRequest)
        val response = app(request.body(requestBody))

        assertThat(response.status, equalTo(Status.NOT_FOUND))
        assertThat(response.bodyString(), equalTo("Companies don't exist: $nonExistentCodes"))
    }

    @Test
    fun `does not return NOT_FOUND when one of the companies doesn't exist when requested`() {
        persistedCompanies.forEach {
            companyRepository.create(it)
        }

        val nonExistentCodes = setOf(randomOrganisationCode())
        val companyCodesRequest = nonExistentCodes.plus(persistedCompanyCodes)
        val requestBody = CompanyJson.mapper.writeValueAsString(companyCodesRequest)
        val response = app(request.query("validation", "false").body(requestBody))

        assertThat(response.status, equalTo(Status.OK))
    }

    @Test
    fun `returns BAD_REQUEST when the body is null`() {
        val requestBody = CompanyJson.mapper.writeValueAsString(null)
        val response = app(request.body(requestBody))

        assertThat(response.status, equalTo(Status.BAD_REQUEST))
        assertThat(response.bodyString(), equalTo("The body must not be blank"))
    }

    @Test
    fun `returns BAD_REQUEST when the batch is empty`() {
        val requestBody = CompanyJson.mapper.writeValueAsString(emptySet<OrganisationCode>())
        val response = app(request.body(requestBody))

        assertThat(response.status, equalTo(Status.BAD_REQUEST))
        assertThat(response.bodyString(), equalTo("The body must not be blank"))
    }

    private fun storeAdvanceCompanyWithAddress(advanceCompany: AdvanceCompany) {
        val advanceAddress = buildAdvanceAddress(advanceCompany.code)
        advanceCompanyRepository.insert(advanceCompany)
        advanceCompanyRepository.insertAddress(advanceAddress)
    }
}
