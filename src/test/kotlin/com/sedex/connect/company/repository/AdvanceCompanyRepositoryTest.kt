package com.sedex.connect.company.repository

import com.fasterxml.jackson.databind.JsonNode
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.sedex.connect.common.Address
import com.sedex.connect.common.CountryCode
import com.sedex.connect.common.EmailAddress
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.TelephoneNumber
import com.sedex.connect.common.json.withStandardMappings
import com.sedex.connect.common.randomCode
import com.sedex.connect.common.randomOrganisationCode
import com.sedex.connect.company.api.CompanyResponse
import com.sedex.connect.company.fixtures.FakeAdvanceCompanyRepository
import com.sedex.connect.company.models.AddressType
import com.sedex.connect.company.models.AddressType.BILLING_ADDRESS
import com.sedex.connect.company.models.AddressType.COMPANY_ADDRESS
import com.sedex.connect.company.models.AdvanceCompany
import com.sedex.connect.company.models.AdvanceCompanyAddress
import org.http4k.format.ConfigurableJackson
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

interface AdvanceCompanyRepositoryContract {
    val repository: AdvanceCompanyRepository

    @Test
    fun `should get company with address`() {
        val orgCode = randomOrganisationCode()
        val insertedCompany = buildCompany(orgCode)
        val insertedAddress = buildAddress(orgCode, COMPANY_ADDRESS)
        repository.insert(insertedCompany)
        repository.insertAddress(insertedAddress)

        val company = repository.get(orgCode)

        val companyPayload = insertedCompany.data
        assertCompanyDetailsMatch(company, orgCode, companyPayload)

        val address = company?.address
        val addressPayload = insertedAddress.data
        assertAddressDetailsMatch(address, addressPayload)

        assertNull(company?.billingAddress)
    }

    @Test
    fun `should get company with address and billing address`() {
        val orgCode = randomOrganisationCode()
        val insertedCompany = buildCompany(orgCode)
        val insertedAddress = buildAddress(orgCode, COMPANY_ADDRESS)
        val insertedBillingAddress = buildAddress(orgCode, BILLING_ADDRESS)
        repository.insert(insertedCompany)
        repository.insertAddress(insertedAddress)
        repository.insertAddress(insertedBillingAddress)

        val company = repository.get(orgCode)

        val companyPayload = insertedCompany.data
        assertCompanyDetailsMatch(company, orgCode, companyPayload)

        val address = company?.address
        val addressPayload = insertedAddress.data
        assertAddressDetailsMatch(address, addressPayload)

        val billingAddress = company?.billingAddress
        val billingAddressPayload = insertedBillingAddress.data
        assertAddressDetailsMatch(billingAddress, billingAddressPayload)
    }

    @Test
    fun `get company with missing company returns null`() {
        val orgCode = randomOrganisationCode()
        val insertedCompany = buildCompany(orgCode)
        repository.insert(insertedCompany)

        val company = repository.get(orgCode)
        assertNull(company)
    }

    @Test
    fun `get company with missing address returns null`() {
        val orgCode = randomOrganisationCode()
        val insertedAddress = buildAddress(orgCode, COMPANY_ADDRESS)
        repository.insertAddress(insertedAddress)

        val company = repository.get(orgCode)
        assertNull(company)
    }

    @Test
    fun `existsByCode should return true if company exists`() {
        val orgCode = randomOrganisationCode()
        val insertedCompany = buildCompany(orgCode)
        repository.insert(insertedCompany)

        val exists = repository.existsByCode(orgCode)

        assertTrue(exists)
    }

    @Test
    fun `existsByCode should return false if company does not exist`() {
        val orgCode = randomOrganisationCode()
        val insertedCompany = buildCompany(orgCode)
        repository.insert(insertedCompany)

        val exists = repository.existsByCode(OrganisationCode("missing code"))

        assertFalse(exists)
    }

    @Test
    fun `existsByNameForOrg should return true if company exists outside given org`() {
        val orgCode = randomOrganisationCode()
        val insertedCompany = buildCompany(orgCode)
        repository.insert(insertedCompany)

        val exists = repository.existsByNameForOrg("Some Company", OrganisationCode("Another Org"))

        assertTrue(exists)
    }

    @Test
    fun `existsByNameForOrg should return false if company exists within given org`() {
        val orgCode = randomOrganisationCode()
        val insertedCompany = buildCompany(orgCode)
        repository.insert(insertedCompany)

        val exists = repository.existsByNameForOrg("Some Company", orgCode)

        assertFalse(exists)
    }

    @Test
    fun `existsByNameForOrg should return false if company does not exist`() {
        val orgCode = randomOrganisationCode()
        val insertedCompany = buildCompany(orgCode)
        repository.insert(insertedCompany)

        val exists = repository.existsByNameForOrg("Another Company", OrganisationCode("Another Org"))

        assertFalse(exists)
    }

    @Test
    fun `existsByAddress should return true if address exists`() {
        val orgCode = randomOrganisationCode()
        val insertedAddress = buildAddress(orgCode, COMPANY_ADDRESS)
        repository.insertAddress(insertedAddress)

        val address = Address("Address Line 1", null, null, null, "AB12 3CD", "City", CountryCode("GB"))
        val exists = repository.existsByAddress(address)

        assertTrue(exists)
    }

    @Test
    fun `existsByAddress should return false if address does not exist`() {
        val orgCode = randomOrganisationCode()
        val insertedAddress = buildAddress(orgCode, COMPANY_ADDRESS)
        repository.insertAddress(insertedAddress)

        val address = Address("New Address Line 1", null, null, null, "AB12 3CD", "City", CountryCode("GB"))
        val exists = repository.existsByAddress(address)

        assertFalse(exists)
    }

    @Test
    fun `should get companies by codes`() {
        val companyCodes = setOf(randomOrganisationCode(), randomOrganisationCode())
        companyCodes.forEach { insertCompanyWithAddress(it) }

        val companies = repository.getByCodes(companyCodes)

        val persistedOrgCodes = companies.map { it.code }
        assertTrue(persistedOrgCodes.containsAll(companyCodes))
    }

    @Test
    fun `should get companies by subscription type`() {
        val orgCode = randomOrganisationCode()
        repository.insert(buildCompany(orgCode, "ST004"))

        val companies = repository.getAdvanceCompanyBySubscriptionType("ST004")

        assertThat(companies, equalTo(setOf(orgCode)))
    }

    @Test
    fun `should get companies by start local name`() {
        val orgCode = OrganisationCode(randomCode("ZC"))

        val insertedAddress = buildAddress(orgCode, COMPANY_ADDRESS)
        repository.insertAddress(insertedAddress)

        val insertedCompany = buildCompany(orgCode, "ST004")
        repository.insert(insertedCompany)

        val shortLocalName = "Quelque"
        val fullLocalName = "Quelque compagnie"

        val companies = repository.findByLocalNameStartingWith(shortLocalName)

        val persistedCompanies = companies
            .filter { it.code == orgCode && it.localName?.startsWith(fullLocalName) == true }

        assertTrue(persistedCompanies.isNotEmpty())
    }

    @Test
    fun `should get companies by local name`() {
        val orgCode = OrganisationCode(randomCode("ZC"))

        val insertedAddress = buildAddress(orgCode, COMPANY_ADDRESS)
        repository.insertAddress(insertedAddress)

        val insertedCompany = buildCompany(orgCode, "ST004")
        repository.insert(insertedCompany)

        val fullLocalName = "Quelque compagnie"
        val companies = repository.findByLocalName(fullLocalName)

        val persistedCompanies = companies
            .filter { it.code == orgCode && it.localName?.startsWith(fullLocalName) == true }

        assertTrue(persistedCompanies.isNotEmpty())
    }

    private fun assertCompanyDetailsMatch(company: CompanyResponse?, orgCode: OrganisationCode, companyPayload: JsonNode) {
        Assertions.assertNotNull(company)
        assertThat(company?.code, equalTo(orgCode))
        assertThat(company?.isUpdatedByConnect, equalTo(companyPayload["isUpdatedByConnect"].booleanValue()))
        assertThat(company?.englishName, equalTo(companyPayload["name"].textValue()))
        assertThat(company?.email, equalTo(EmailAddress(companyPayload["contact"].textValue())))
        assertThat(company?.telephone, equalTo(TelephoneNumber(companyPayload["telephone"].textValue())))
        assertThat(company?.localName, equalTo(companyPayload["smdNameInLocalLanguage"].textValue()))
        assertThat(company?.businessLicenseNumber, equalTo(companyPayload["smdBusinessLicenseNumber"].textValue()))
        assertThat(company?.subscriptionType, equalTo(companyPayload["subscriptionType"].textValue()))
        assertThat(company?.membershipStatus?.name, equalTo(companyPayload["membershipStatus"].textValue()))
        assertThat(company?.primaryIndustryClassificationCode, equalTo(companyPayload["primaryIndustryClassificationCode"].textValue()))
        assertThat(company?.secondaryIndustryClassificationCode, equalTo(companyPayload["secondaryIndustryClassificationCode"].textValue()))
    }

    private fun assertAddressDetailsMatch(address: Address?, addressPayload: JsonNode) {
        Assertions.assertNotNull(address)
        assertThat(address?.line1, equalTo(addressPayload["addressLine1"].textValue()))
        assertThat(address?.line2, equalTo(addressPayload["addressLine2"].textValue()))
        assertThat(address?.line3, equalTo(addressPayload["addressLine3"].textValue()))
        assertThat(address?.line4, equalTo(addressPayload["addressLine4"].textValue()))
        assertThat(address?.postCode, equalTo(addressPayload["postCode"].textValue()))
        assertThat(address?.city, equalTo(addressPayload["city"].textValue()))
        assertThat(address?.country, equalTo(CountryCode(addressPayload["countryCode"].textValue())))
    }

    private fun buildCompany(orgCode: OrganisationCode, subscriptionType: String? = "ST002"): AdvanceCompany {
        val json = objectMapper.readTree(json(orgCode, subscriptionType))
        return AdvanceCompany(
            orgCode,
            Instant.ofEpochMilli(json["createdOn"].longValue()),
            json["modifiedOn"]?.let { Instant.ofEpochMilli(it.longValue()) },
            json
        )
    }

    private fun buildAddress(orgCode: OrganisationCode, addressType: AddressType): AdvanceCompanyAddress {
        return AdvanceCompanyAddress(
            orgCode,
            addressType,
            if (addressType == COMPANY_ADDRESS) {
                objectMapper.readTree(addressJson(orgCode))
            } else {
                objectMapper.readTree(billingAddressJson(orgCode))
            }
        )
    }

    private fun insertCompanyWithAddress(orgCode: OrganisationCode) {
        val insertedCompany = buildCompany(orgCode)
        val insertedAddress = buildAddress(orgCode, COMPANY_ADDRESS)
        repository.insert(insertedCompany)
        repository.insertAddress(insertedAddress)
    }

    private fun json(orgCode: OrganisationCode, subscriptionType: String? = "ST002"): String = """
            {
              "createdOn": 1645703361920,
              "modifiedOn": 1645703361920,
              "isUpdatedByConnect": false,
              "code": "$orgCode",
              "subscriptionType": "$subscriptionType",
              "membershipStatus": "ACTIVE",
              "name": "Some Company",
              "smdNameInLocalLanguage": "Quelque compagnie",
              "contact": "name@company.com",
              "telephone": "05555 555555",
              "smdBusinessLicenseNumber": "12345",
              "businessLicenseExpiredDateNotApplicable": false,
              "smdEnrollStatus": "OPTED_OUT",
              "smdMainContacts": [],
              "primaryIndustryClassificationCode": "primary",
              "secondaryIndustryClassificationCode": "secondary"
            }
        """

    private fun addressJson(orgCode: OrganisationCode): String = """
            {
              "createdOn": 1645703361920,
              "modifiedOn": 1645703361920,
              "isUpdatedByConnect": false,
              "code": "$orgCode",
              "addressLine1": "Address Line 1",
              "addressLine2": "Address Line 2",
              "addressLine3": "Address Line 3",
              "addressLine4": "Address Line 4",
              "city": "City",
              "countryCode": "GB",
              "postCode": "AB12 3CD"
            }
        """

    private fun billingAddressJson(orgCode: OrganisationCode): String = """
            {
              "createdOn": 1645703361920,
              "modifiedOn": 1645703361920,
              "isUpdatedByConnect": false,
              "code": "$orgCode",
              "addressLine1": "Billing Address Line 1",
              "addressLine2": "Billing Address Line 2",
              "addressLine3": "Billing Address Line 3",
              "addressLine4": "Billing Address Line 4",
              "city": "Billing City",
              "countryCode": "GB",
              "postCode": "BC12 3EF"
            }
        """
    companion object {
        val objectMapper = ConfigurableJackson(withStandardMappings()).mapper
    }
}
class AdvanceCompanyRepositoryTest : AdvanceCompanyRepositoryContract, RepositoryTestBase() {
    override val repository = AdvanceCompanyRepositoryImpl(db)
}

class FakeAdvanceCompanyRepositoryTest : AdvanceCompanyRepositoryContract {
    override val repository = FakeAdvanceCompanyRepository()
}
