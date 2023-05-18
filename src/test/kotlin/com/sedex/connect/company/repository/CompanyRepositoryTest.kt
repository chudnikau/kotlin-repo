package com.sedex.connect.company.repository

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import com.sedex.connect.common.Address
import com.sedex.connect.common.CountryCode
import com.sedex.connect.common.EmailAddress
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.TelephoneNumber
import com.sedex.connect.common.VatNumber
import com.sedex.connect.common.randomCode
import com.sedex.connect.common.randomOrganisationCode
import com.sedex.connect.company.api.CompanyResponse
import com.sedex.connect.company.api.CompanySize.MEDIUM
import com.sedex.connect.company.api.MembershipStatus.ACTIVE
import com.sedex.connect.company.fixtures.FakeCompanyRepository
import com.sedex.connect.company.models.PersistedCompany
import com.sedex.connect.company.models.ProfileStatus.COMPLETE
import com.sedex.connect.company.models.ProfileStatus.PENDING
import com.sedex.connect.lang.TimeForTesting
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

interface CompanyRepositoryContract {

    val repository: CompanyRepository

    @Test
    fun `should get`() {
        val orgCode = randomOrganisationCode()
        insert(buildCompany(orgCode))

        val company = repository.get(orgCode)

        assertThat(company!!.englishName, equalTo("Some Company"))
        assertThat(company.localName, equalTo("Some Company in some other language"))
        assertThat(company.businessLicenseNumber, equalTo("1234567890"))
        assertThat(company.businessLicenseExpiration, equalTo(LocalDate.parse("2022-05-12")))
        assertThat(company.address.line1, equalTo("Address Line 1"))
        assertThat(company.address.line2, equalTo("Address Line 2"))
        assertThat(company.address.city, equalTo("City"))
        assertThat(company.address.postCode, equalTo("AB12 3CD"))
        assertThat(company.address.country, equalTo(CountryCode("GB")))
        assertThat(company.telephone, equalTo(TelephoneNumber("0791234567890")))
        assertThat(company.email, equalTo(EmailAddress("someone@example.com")))
        assertThat(company.billingAddress!!.line1, equalTo("Billing Address Line 1"))
        assertThat(company.billingAddress!!.line2, equalTo("Billing Address Line 2"))
        assertThat(company.billingAddress!!.city, equalTo("Billing City"))
        assertThat(company.billingAddress!!.postCode, equalTo("EF45 6GH"))
        assertThat(company.billingAddress!!.country, equalTo(CountryCode("GB")))
        assertThat(company.vatNumber, equalTo(VatNumber("0987654321")))
        assertThat(company.smdEnrollStatus, equalTo("OPTED_OUT"))
        assertThat(company.membershipStatus?.name, equalTo("ACTIVE"))
        assertThat(company.subscriptionType, equalTo("ST002"))
        assertThat(company.primaryIndustryClassificationCode, equalTo("ZIC1000000"))
        assertThat(company.secondaryIndustryClassificationCode, equalTo("ZIC1000100"))
    }

    @Test
    fun `should get with blank telephone if telephone was N-A`() {
        val orgCode = randomOrganisationCode()
        insert(buildCompany(orgCode).copy(telephone = TelephoneNumber("N/A")))

        val company = repository.get(orgCode)

        assertThat(company!!.englishName, equalTo("Some Company"))
        assertThat(company.address.line1, equalTo("Address Line 1"))
        assertThat(company.telephone?.value, equalTo(""))
        assertThat(company.email, equalTo(EmailAddress("someone@example.com")))
    }

    @Test
    fun `should get null`() {
        val orgCode = randomOrganisationCode()
        val company = repository.get(orgCode)
        assertNull(company)
    }

    @Test
    fun `should remove values from db if they are null`() {
        val orgCode = randomOrganisationCode()
        insert(buildCompany(orgCode))
        var company = repository.get(orgCode)

        assertEquals("0791234567890", company?.telephone?.value)
        repository.createOrUpdate(PersistedCompany(orgCode, company!!).copy(telephone = null))
        company = repository.get(orgCode)
        assertNull(company?.telephone)
    }

    @Test
    fun `should get companies by codes`() {
        val companyCodes = setOf(randomOrganisationCode(), randomOrganisationCode())
        companyCodes.forEach { insert(buildCompany(it).copy(englishName = "Some Company$it")) }

        val companies = repository.getByCodes(companyCodes)

        val persistedOrgCodes = companies.map { it.code }
        assertTrue(persistedOrgCodes.containsAll(companyCodes))
    }

    @Test
    fun `should return empty collection if requested companies don't exist`() {
        val companyCodes = setOf(randomOrganisationCode(), randomOrganisationCode())
        companyCodes.forEach { insert(buildCompany(it).copy(englishName = "Some Company$it")) }
        val nonExistentCodes = setOf(randomOrganisationCode(), randomOrganisationCode())

        val companies = repository.getByCodes(nonExistentCodes)

        assertThat(companies, isEmpty)
    }

    @Test
    fun `shouldn't exist by code`() {
        val orgCode = randomOrganisationCode()
        assertFalse(repository.existsByCode(orgCode), "Company $orgCode doesn't exist ")
    }

    @Test
    fun `should exist by code`() {
        val orgCode = randomOrganisationCode()
        insert(buildCompany(orgCode))
        assertTrue(repository.existsByCode(orgCode), "Company $orgCode exists")
    }

    @Test
    fun `shouldn't exist by name`() {
        val orgCode = randomOrganisationCode()
        insert(buildCompany(orgCode))
        val name = "Test Name"
        assertFalse(repository.existsByNameForOrg(name, orgCode), "Company $name doesn't exists")
    }

    @Test
    fun `should exist by name`() {
        val orgCode = randomOrganisationCode()
        val name = "Some Company"
        insert(buildCompany(orgCode))
        assertTrue(repository.existsByNameForOrg(name, randomOrganisationCode()), "Company $name exists")
    }

    @Test
    fun `should exist by name with different case`() {
        val orgCode = randomOrganisationCode()
        val name = "Some Company"
        insert(buildCompany(orgCode))
        assertTrue(repository.existsByNameForOrg("some company", randomOrganisationCode()), "Company $name exists")
    }

    @Test
    fun `should get pending company profile status when feature company size enabled`() {
        val orgCode = randomOrganisationCode()
        insert(buildCompany(orgCode))

        val status = repository.getCompanyProfileStatus(orgCode)

        assertThat(status, equalTo(PENDING))
    }

    @Test
    fun `should get completed company profile status when feature company size enabled`() {
        val orgCode = randomOrganisationCode()
        insert(buildCompany(orgCode).copy(companySize = MEDIUM))

        val status = repository.getCompanyProfileStatus(orgCode)

        assertThat(status, equalTo(COMPLETE))
    }

    @Test
    fun `should return true if company exists by address`() {
        val orgCode = randomOrganisationCode()
        val company = insert(buildCompany(orgCode))

        val exists = repository.existsByAddress(company.address)

        assertTrue(exists)
    }

    @Test
    fun `should get companies by subscription type`() {
        val orgCode = randomOrganisationCode()
        insert(buildCompany(orgCode, "ST004"))

        val companies = repository.getCompanyBySubscriptionType("ST004")

        assertThat(companies, equalTo(setOf(orgCode)))
    }

    @Test
    fun `should get companies by start local name`() {
        val orgCode = OrganisationCode(randomCode("ZC"))

        val company = insert(buildCompany(orgCode, "ST004"))
        val expected = setOf(
            company
        )
        val result = repository.findByLocalNameStartingWith("Some Company")

        assertEquals(expected, result)
    }

    @Test
    fun `should get companies by local name`() {
        val orgCode = OrganisationCode(randomCode("ZC"))

        val company = insert(buildCompany(orgCode, "ST004"))
        val expected = setOf(
            company
        )
        val result = repository.findByLocalName("Some Company in some other language")

        assertEquals(expected, result)
    }

    private fun insert(company: PersistedCompany): CompanyResponse = repository.createOrUpdate(company)

    private fun buildCompany(orgCode: OrganisationCode, subscriptionType: String? = "ST002"): PersistedCompany {
        return PersistedCompany(
            code = orgCode,
            isUpdatedByConnect = null,
            englishName = "Some Company",
            localName = "Some Company in some other language",
            businessLicenseNumber = "1234567890",
            businessLicenseExpiration = LocalDate.of(2022, 5, 12),
            primaryIndustryClassificationCode = "ZIC1000000",
            secondaryIndustryClassificationCode = "ZIC1000100",
            address = Address(
                line1 = "Address Line 1",
                line2 = "Address Line 2",
                line3 = null,
                line4 = null,
                postCode = "AB12 3CD",
                city = "City",
                country = CountryCode("GB")
            ),
            telephone = TelephoneNumber("0791234567890"),
            email = EmailAddress("someone@example.com"),
            billingAddress = Address(
                line1 = "Billing Address Line 1",
                line2 = "Billing Address Line 2",
                line3 = null,
                line4 = null,
                postCode = "EF45 6GH",
                city = "Billing City",
                country = CountryCode("GB")
            ),
            vatNumber = VatNumber("0987654321"),
            smdEnrollStatus = "OPTED_OUT",
            subscriptionType = subscriptionType,
            membershipStatus = ACTIVE,
            companySize = null
        )
    }
}

class CompanyRepositoryTest : CompanyRepositoryContract, RepositoryTestBase() {
    override val repository = CompanyRepositoryImpl(db, time)
}

class FakeCompanyRepositoryTest : CompanyRepositoryContract {
    val time = TimeForTesting("2022-08-09T09:00:00.00Z")
    override val repository = FakeCompanyRepository(time)
}
