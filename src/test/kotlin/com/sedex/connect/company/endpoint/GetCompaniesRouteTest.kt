package com.sedex.connect.company.endpoint

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.auth.SuperSedexAdmin
import com.sedex.connect.common.randomOrganisationCode
import com.sedex.connect.company.CompanyTestCase
import com.sedex.connect.company.api.CompanyJson
import com.sedex.connect.company.api.companiesResponseLens
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

class GetCompaniesRouteTest : CompanyTestCase() {
    private val persistedCompanyCodes = setOf(randomOrganisationCode(), randomOrganisationCode())
    private val persistedCompanies = persistedCompanyCodes.map { PersistedCompany(it, persistedCompanyExample.copy(englishName = "Some Company$it")) }
    private val request = Request(POST, "/all-companies")
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
        val response = authorizeUser(roles = setOf(SuperSedexAdmin))
            .handle(request.body(requestBody))
        val companiesResponse = companiesResponseLens(response)
        val companyCodes = companiesResponse.keys
        val companies = companiesResponse.values
        val persistedCompanies = persistedCompanies.map { companiesResponse[it.code] }

        assertThat(response.status, equalTo(Status.OK))
        assertTrue(companyCodes.containsAll(persistedCompanyCodes))
        assertTrue(companies.containsAll(persistedCompanies))
    }

    @Test
    fun `returns OK with only Advance companies`() {
        persistedCompanyCodes.forEach { code ->
            val advanceCompany = buildAdvanceCompany(code, Instant.now(), null)
            storeAdvanceCompanyWithAddress(advanceCompany)
        }

        val requestBody = CompanyJson.mapper.writeValueAsString(persistedCompanyCodes)
        val response = authorizeUser(roles = setOf(SuperSedexAdmin))
            .handle(request.body(requestBody))

        val companiesResponse = companiesResponseLens(response)
        val companyCodes = companiesResponse.keys
        val companies = companiesResponse.values
        val persistedCompanies = persistedCompanies.map { companiesResponse[it.code] }

        assertThat(response.status, equalTo(Status.OK))
        assertTrue(companyCodes.containsAll(persistedCompanyCodes))
        assertTrue(companies.containsAll(persistedCompanies))
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
        val response = authorizeUser(roles = setOf(SuperSedexAdmin))
            .handle(request.body(requestBody))
        val companiesResponse = companiesResponseLens(response)
        val companyCodesResponse = companiesResponse.keys
        val companies = companiesResponse.values
        val companyNamesResponse = companies.map { it.englishName }

        assertThat(response.status, equalTo(Status.OK))
        assertThat(companyCodesResponse.size, equalTo(4))
        assertTrue(companyCodesResponse.containsAll(expectedOrgCodes))
        val expectedNames = setOf("Connect Company 1", "Advance Company 2", "Connect Company 3", "Advance Company 4")
        assertTrue(companyNamesResponse.containsAll(expectedNames))
    }

    private fun storeAdvanceCompanyWithAddress(advanceCompany: AdvanceCompany) {
        val advanceAddress = buildAdvanceAddress(advanceCompany.code)
        advanceCompanyRepository.insert(advanceCompany)
        advanceCompanyRepository.insertAddress(advanceAddress)
    }
}
