package com.sedex.connect.company.endpoint

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.auth.SedexAdmin
import com.sedex.connect.common.randomOrganisationCode
import com.sedex.connect.company.CompanyTestCase
import com.sedex.connect.company.api.companiesSearchResponseLens
import com.sedex.connect.company.models.PersistedCompany
import com.sedex.connect.company.persistedCompanyExample
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class CompanySearchRouteTest : CompanyTestCase() {

    private val orgCode = randomOrganisationCode()
    private val aPersistedCompany = PersistedCompany(orgCode, persistedCompanyExample)

    @Test
    fun `check search company by ZC code as admin`() {
        companyRepository.create(aPersistedCompany)

        val advanceAddress = buildAdvanceAddress(orgCode)
        advanceCompanyRepository.insertAddress(advanceAddress)

        val company = buildAdvanceCompany(orgCode, Instant.now(), Instant.now())
        advanceCompanyRepository.insert(company)

        val response = authorizeUser(roles = setOf(SedexAdmin))
            .handle(Request(GET, "/companies/search/$orgCode"))

        val expectedBody = "[{\"code\":\"$orgCode\",\"englishName\":\"My Company\",\"subscriptionType\":\"Supplier\",\"membershipStatus\":\"ACTIVE\"}]"

        assertEquals(Status.OK, response.status)
        assertThat(response.bodyString(), equalTo(expectedBody))
    }

    private fun populateCompanies(
        orgCode: OrganisationCode,
        localCompanyName: String = "Local company name",
        englishCompanyName: String = "My Company",
    ) {
        val advanceAddress = buildAdvanceAddress(orgCode)
        advanceCompanyRepository.insertAddress(advanceAddress)

        val customJsonContent = { code: OrganisationCode ->
            advanceJson(code, localCompanyName, englishCompanyName)
        }
        val advanceCompany = buildAdvanceCompany(orgCode, Instant.now(), Instant.now(), customJsonContent)

        advanceCompanyRepository.insert(advanceCompany)

        val aPersistedCompany = PersistedCompany(orgCode, persistedCompanyExample(localCompanyName, englishCompanyName))

        companyRepository.create(aPersistedCompany)
    }

    @Test
    fun `check search company by name as admin`() {
        val orgCode = randomOrganisationCode()

        populateCompanies(randomOrganisationCode(), "First company name")
        populateCompanies(orgCode, "Second company name")
        populateCompanies(randomOrganisationCode(), "Third company name")

        val fullLocalName = "Second company name"
        var response = authorizeUser(roles = setOf(SedexAdmin))
            .handle(Request(GET, "/companies/search/$fullLocalName"))

        val expectedBody = "[{\"code\":\"$orgCode\",\"englishName\":\"My Company\",\"subscriptionType\":\"Supplier\",\"membershipStatus\":\"ACTIVE\"}]"

        assertEquals(Status.OK, response.status)
        assertThat(response.bodyString(), equalTo(expectedBody))
    }

    @Test
    fun `check search company by short name as admin`() {
        val orgCode = randomOrganisationCode()

        populateCompanies(randomOrganisationCode(), "First company name")
        populateCompanies(orgCode, "Second company name")
        populateCompanies(randomOrganisationCode(), "Third company name")

        val shortLocalName = "Second company"
        var response = authorizeUser(roles = setOf(SedexAdmin))
            .handle(Request(GET, "/companies/search/$shortLocalName"))

        val expectedBody = "[{\"code\":\"$orgCode\",\"englishName\":\"My Company\",\"subscriptionType\":\"Supplier\",\"membershipStatus\":\"ACTIVE\"}]"

        assertEquals(Status.OK, response.status)
        assertThat(response.bodyString(), equalTo(expectedBody))
    }

    @Test
    fun `check sort companies result where exact match start on top`() {
        populateCompanies(randomOrganisationCode(), "Company name (First)", "Company")
        populateCompanies(randomOrganisationCode(), "Company name (Second)", "Company 1")
        populateCompanies(randomOrganisationCode(), "Company name (Third)", "Company 11")
        populateCompanies(randomOrganisationCode(), "Company name (Fourth)", "Company A")
        populateCompanies(randomOrganisationCode(), "Company name (Fifth)", "Company B")
        populateCompanies(randomOrganisationCode(), "Company name (Sixth)", "Company C")
        populateCompanies(randomOrganisationCode(), "Company name", "Company Top")

        val shortLocalName = "Company name"
        var response = authorizeUser(roles = setOf(SedexAdmin))
            .handle(Request(GET, "/companies/search/$shortLocalName"))

        val expectedBody = companiesSearchResponseLens(response)
        val companyNamesResponse = expectedBody.map { it.englishName }
        val expectedNames = listOf("Company Top", "Company", "Company 1", "Company 11", "Company A", "Company B", "Company C")

        assertEquals(Status.OK, response.status)
        Assertions.assertTrue(companyNamesResponse == expectedNames)
    }
}
