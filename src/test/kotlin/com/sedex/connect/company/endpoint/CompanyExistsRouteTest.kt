package com.sedex.connect.company.endpoint

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.sedex.connect.common.CountryCode
import com.sedex.connect.common.auth.OrganisationAdmin
import com.sedex.connect.common.auth.SedexAdmin
import com.sedex.connect.common.randomOrganisationCode
import com.sedex.connect.company.CompanyTestCase
import com.sedex.connect.company.models.PersistedCompany
import com.sedex.connect.company.persistedCompanyExample
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class CompanyExistsRouteTest : CompanyTestCase() {

    private val orgCode = randomOrganisationCode()
    private val aPersistedCompany = PersistedCompany(orgCode, persistedCompanyExample)

    @Test
    fun `check for company by code where code exists`() {
        companyRepository.create(aPersistedCompany)
        val response = app(Request(GET, "/companies/exists/$orgCode"))

        assertEquals(Status.OK, response.status)
        assertThat(response.bodyString(), equalTo("{\"exists\":true}"))
    }

    @Test
    fun `check for company by code where code exists in Advance`() {
        val company = buildAdvanceCompany(orgCode, Instant.now(), Instant.now())
        advanceCompanyRepository.insert(company)
        val response = app(Request(GET, "/companies/exists/$orgCode"))

        assertEquals(Status.OK, response.status)
        assertThat(response.bodyString(), equalTo("{\"exists\":true}"))
    }

    @Test
    fun `check for company by code where code does not exist`() {
        val response = app(Request(GET, "/companies/exists/$orgCode"))

        assertEquals(Status.OK, response.status)
        assertThat(response.bodyString(), equalTo("{\"exists\":false}"))
    }

    @Test
    fun `check for company by impersonated user code where name exists`() {
        companyRepository.create(aPersistedCompany)
        val response = impersonateUser(orgCode = orgCode, roles = setOf(OrganisationAdmin(orgCode)))
            .handle(Request(GET, "/companies/exists/name/${aPersistedCompany.englishName}"))

        assertEquals(Status.OK, response.status)
        assertThat(response.bodyString(), equalTo("{\"exists\":false}"))
    }

    @Test
    fun `check for company by name with whitespace where name exists`() {
        companyRepository.create(aPersistedCompany)
        val response = app(Request(GET, "/companies/exists/name/%20${aPersistedCompany.englishName}%20"))

        assertEquals(Status.OK, response.status)
        assertThat(response.bodyString(), equalTo("{\"exists\":true}"))
    }

    @Test
    fun `check for company by name where name exists`() {
        companyRepository.create(aPersistedCompany)
        val response = app(Request(GET, "/companies/exists/name/${aPersistedCompany.englishName}"))

        assertEquals(Status.OK, response.status)
        assertThat(response.bodyString(), equalTo("{\"exists\":true}"))
    }

    @Test
    fun `check for company by name where name exists in Advance`() {
        val company = buildAdvanceCompany(orgCode, Instant.now(), Instant.now())
        advanceCompanyRepository.insert(company)
        val response = app(Request(GET, "/companies/exists/name/My%20Company"))

        assertEquals(Status.OK, response.status)
        assertThat(response.bodyString(), equalTo("{\"exists\":true}"))
    }

    @Test
    fun `check for company by name where name does not exist`() {
        val response = app(Request(GET, "/companies/exists/name/${aPersistedCompany.englishName}"))

        assertEquals(Status.OK, response.status)
        assertThat(response.bodyString(), equalTo("{\"exists\":false}"))
    }

    @Test
    fun `check for company by name where user is logged in`() {
        val company = companyRepository.create(aPersistedCompany)

        var response = authorizeUser(orgCode = orgCode)
            .handle(Request(GET, "/companies/exists/name/${company.englishName}"))

        assertEquals(Status.OK, response.status)
        assertThat(response.bodyString(), equalTo("{\"exists\":false}"))

        response = authorizeUser(orgCode = randomOrganisationCode())
            .handle(Request(GET, "/companies/exists/name/${company.englishName}"))

        assertEquals(Status.OK, response.status)
        assertThat(response.bodyString(), equalTo("{\"exists\":true}"))
    }

    @Test
    fun `check for company by address`() {
        companyRepository.create(aPersistedCompany)
        val uri = "/companies/address/exists?" +
            "line1=${aPersistedCompany.address.line1}" +
            "&city=${aPersistedCompany.address.city}" +
            "&postcode=${aPersistedCompany.address.postCode}" +
            "&country=${aPersistedCompany.address.country?.value}"
        val response = app(Request(GET, uri))

        assertEquals(Status.OK, response.status)
        assertThat(response.bodyString(), equalTo("{\"exists\":true}"))
    }

    @Test
    fun `check for company by address where address exists in Advance`() {
        val company = buildAdvanceCompany(orgCode, Instant.now(), Instant.now())
        advanceCompanyRepository.insert(company)
        val address = buildAdvanceAddress(orgCode)
        advanceCompanyRepository.insertAddress(address)
        val uri = "/companies/address/exists?" +
            "line1=Somewhere" +
            "&city=London" +
            "&postcode=AB12 3CD" +
            "&country=GB"
        val response = app(Request(GET, uri))

        assertEquals(Status.OK, response.status)
        assertThat(response.bodyString(), equalTo("{\"exists\":true}"))
    }

    @Test
    fun `should return BAD REQUEST if any of parameters is empty`() {
        companyRepository.create(aPersistedCompany)
        var uri = "/companies/address/exists?" +
            "&city=${aPersistedCompany.address.city}" +
            "&postcode=${aPersistedCompany.address.postCode}" +
            "&country=${aPersistedCompany.address.country?.value}"
        var response = app(Request(GET, uri))

        assertEquals(Status.BAD_REQUEST, response.status)

        uri = "/companies/address/exists?" +
            "line1=${aPersistedCompany.address.line1}" +
            "&postcode=${aPersistedCompany.address.postCode}" +
            "&country=${aPersistedCompany.address.country?.value}"
        response = app(Request(GET, uri))

        assertEquals(Status.BAD_REQUEST, response.status)

        uri = "/companies/address/exists?" +
            "line1=${aPersistedCompany.address.line1}" +
            "&city=${aPersistedCompany.address.city}" +
            "&country=${aPersistedCompany.address.country?.value}"
        response = app(Request(GET, uri))

        assertEquals(Status.BAD_REQUEST, response.status)

        uri = "/companies/address/exists?" +
            "line1=${aPersistedCompany.address.line1}" +
            "&city=${aPersistedCompany.address.city}" +
            "&postcode=${aPersistedCompany.address.postCode}"
        response = app(Request(GET, uri))

        assertEquals(Status.BAD_REQUEST, response.status)
    }

    @Test
    fun `should return false if company does not exist in db`() {
        companyRepository.create(aPersistedCompany)
        val uri = "/companies/address/exists?" +
            "line1=line" +
            "&city=city" +
            "&postcode=11111" +
            "&country=AA"
        val response = app(Request(GET, uri))
        assertThat(response.bodyString(), equalTo("{\"exists\":false}"))
    }

    @Test
    fun `should return true when input address data with spaces`() {
        companyRepository.create(
            aPersistedCompany.copy(
                address = aPersistedCompany.address.copy(
                    line1 = "Laisvės pr. 75",
                    city = "Vilniaus m. sav.",
                    postCode = "06144",
                    country = CountryCode("LT")
                )
            )
        )
        val uri = "/companies/address/exists?line1=Laisv%C4%97s%20pr.%2075&city=Vilniaus%20m.%20sav.&postcode=06144&country=LT"
        val response = app(Request(GET, uri))
        assertThat(response.bodyString(), equalTo("{\"exists\":true}"))
    }

    @Test
    fun `check company name as admin`() {
        val company = buildAdvanceCompany(orgCode, Instant.now(), Instant.now())
        advanceCompanyRepository.insert(company)
        var response = authorizeUser(roles = setOf(SedexAdmin))
            .handle(Request(GET, "/companies/exists/name/My%20Company?orgCode=$orgCode"))

        assertEquals(Status.OK, response.status)
        assertThat(response.bodyString(), equalTo("{\"exists\":false}"))

        response = authorizeUser(roles = setOf(SedexAdmin))
            .handle(Request(GET, "/companies/exists/name/My%20Company"))

        assertEquals(Status.OK, response.status)
        assertThat(response.bodyString(), equalTo("{\"exists\":true}"))
    }
}
