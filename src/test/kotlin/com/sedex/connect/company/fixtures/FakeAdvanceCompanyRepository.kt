package com.sedex.connect.company.fixtures

import com.sedex.connect.common.Address
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.company.api.CompanyResponse
import com.sedex.connect.company.models.AddressType.BILLING_ADDRESS
import com.sedex.connect.company.models.AdvanceCompany
import com.sedex.connect.company.models.AdvanceCompanyAddress
import com.sedex.connect.company.repository.AdvanceCompanyRepository
import com.sedex.connect.company.utils.toCompany
import com.sedex.connect.lang.mapToSet

class FakeAdvanceCompanyRepository : AdvanceCompanyRepository {
    private val companies = mutableMapOf<OrganisationCode, AdvanceCompany>()
    private val companiesAddresses = mutableMapOf<OrganisationCode, AdvanceCompanyAddress>()
    private val companiesBillingAddresses = mutableMapOf<OrganisationCode, AdvanceCompanyAddress>()

    override fun get(code: OrganisationCode): CompanyResponse? {
        val company = companies[code] ?: return null
        val companyAddress = companiesAddresses[code] ?: return null
        val companyBillingAddress = companiesBillingAddresses[code]

        return toCompany(company, companyAddress, companyBillingAddress)
    }

    override fun existsByCode(code: OrganisationCode): Boolean {
        return companies[code] != null
    }

    override fun existsByNameForOrg(name: String, orgCode: OrganisationCode?): Boolean {
        return companies.any {
            it.value.data["name"].textValue().lowercase().equals(name.lowercase()) &&
                !it.value.code.equals(orgCode ?: "")
        }
    }

    override fun existsByAddress(address: Address): Boolean {
        return companiesAddresses.any {
            it.value.data["addressLine1"].textValue().equals(address.line1) &&
                it.value.data["city"].textValue().equals(address.city) &&
                it.value.data["countryCode"].textValue().equals(address.country?.value) &&
                it.value.data["postCode"].textValue().equals(address.postCode)
        }
    }

    override fun getByCodes(codes: Set<OrganisationCode>): Set<CompanyResponse> {
        val companiesAfterFiltering = companies.filter { codes.contains(it.key) }.map { it.value }.toSet()
        val companiesAddressAfterFiltering = companiesAddresses.filter { codes.contains(it.key) }.map { it.value }.toSet()
        val companiesBillingAddressAfterFiltering = companiesBillingAddresses.filter { codes.contains(it.key) }.map { it.value }.toSet()

        return companiesAfterFiltering.mapNotNull { c ->
            val address = companiesAddressAfterFiltering.firstOrNull { it.code === c.code }
            val billingAddress = companiesBillingAddressAfterFiltering.firstOrNull { it.code === c.code }
            address?.let { toCompany(c, it, billingAddress) }
        }.toSet()
    }

    override fun insert(company: AdvanceCompany) {
        companies[company.code] = company
    }

    override fun insertAddress(address: AdvanceCompanyAddress) {
        if (address.addressType == BILLING_ADDRESS) {
            companiesBillingAddresses[address.code] = address
        } else {
            companiesAddresses[address.code] = address
        }
    }

    override fun getAdvanceCompanyBySubscriptionType(subscriptionType: String): Set<OrganisationCode> {
        return companies.filter {
            it.value.data["subscriptionType"].textValue().equals(subscriptionType) &&
                it.value.data["membershipStatus"].textValue().equals("ACTIVE")
        }.map { it.key }.toSet()
    }

    override fun findByLocalNameStartingWith(name: String): Set<CompanyResponse> =
        companies
            .filter { it.value.data["smdNameInLocalLanguage"].textValue().startsWith(name) }
            .mapNotNull { get(it.value.code) }
            .mapToSet { it }

    override fun findByLocalName(name: String): Set<CompanyResponse> =
        companies
            .filter { it.value.data["smdNameInLocalLanguage"].textValue().equals(name) }
            .mapNotNull { get(it.value.code) }
            .mapToSet { it }

    fun clear() {
        companies.clear()
        companiesAddresses.clear()
        companiesBillingAddresses.clear()
    }
}
