package com.sedex.connect.company.fixtures

import com.sedex.connect.common.Address
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.TelephoneNumber
import com.sedex.connect.company.api.CompanyResponse
import com.sedex.connect.company.api.MembershipStatus.ACTIVE
import com.sedex.connect.company.models.PersistedCompany
import com.sedex.connect.company.models.ProfileStatus
import com.sedex.connect.company.models.ProfileStatus.COMPLETE
import com.sedex.connect.company.models.ProfileStatus.PENDING
import com.sedex.connect.company.repository.CompanyAlreadyExistsException
import com.sedex.connect.company.repository.CompanyRepository
import com.sedex.connect.lang.ReasonableClock
import com.sedex.connect.lang.instant
import com.sedex.connect.lang.mapToSet
import java.time.Instant

class FakeCompanyRepository(private val clock: ReasonableClock) : CompanyRepository {
    private val storage: MutableMap<String, TimestampedCompany> = mutableMapOf()

    override fun create(company: PersistedCompany): CompanyResponse {
        if (storage.containsKey(company.code.value)) {
            throw CompanyAlreadyExistsException()
        }
        val createdTime = clock.instant()
        storage[company.code.value] = TimestampedCompany(company.code, company, createdTime, null)
        return CompanyResponse(company.code, createdTime, null, company)
    }

    override fun createOrUpdate(company: PersistedCompany): CompanyResponse {
        val existing = storage[company.code.value]

        var createdTime: Instant? = null
        var updatedTime: Instant? = null
        if (existing != null) {
            updatedTime = clock.instant()
            storage[company.code.value] = TimestampedCompany(company.code, company, existing.createdTime, updatedTime)
        } else {
            createdTime = clock.instant()
            storage[company.code.value] = TimestampedCompany(company.code, company, createdTime, null)
        }

        return CompanyResponse(company.code, createdTime, updatedTime, company)
    }

    override fun get(code: OrganisationCode): CompanyResponse? {
        val company = storage[code.value]
        return overrideTelephone(company)?.let { CompanyResponse(code, it.createdTime, it.updatedTime, it) }
    }

    private fun overrideTelephone(company: TimestampedCompany?): TimestampedCompany? {
        if (company == null) {
            return null
        }
        val telephone = company.telephone?.let { if (it.value == "N/A") TelephoneNumber("") else it }
        return company.copy(telephone = telephone)
    }

    override fun existsByCode(code: OrganisationCode): Boolean {
        return storage.containsKey(code.value)
    }

    override fun existsByNameForOrg(name: String, orgCode: OrganisationCode?): Boolean {
        return storage.values.any { it.englishName.equals(name, ignoreCase = true) && it.code != orgCode }
    }

    override fun existsByAddress(address: Address): Boolean {
        return storage.values.any { sameAddress(it.address, address) }
    }

    private fun sameAddress(storedAddress: Address, matchAddress: Address): Boolean {
        return (
            storedAddress.line1.equals(matchAddress.line1, ignoreCase = true) &&
                storedAddress.city.equals(matchAddress.city, ignoreCase = true) &&
                storedAddress.country?.value.equals(matchAddress.country?.value, ignoreCase = true) &&
                storedAddress.postCode.equals(matchAddress.postCode, ignoreCase = true)
            )
    }

    override fun getByCodes(codes: Set<OrganisationCode>): Set<CompanyResponse> {
        val codeValues = codes.map { it.value }
        val matchingCompanies = storage.values.filter { codeValues.contains(it.code.value) }
            .map { CompanyResponse(it.code, it.createdTime, it.updatedTime, it) }
        return HashSet(matchingCompanies)
    }

    override fun getCompanyProfileStatus(code: OrganisationCode): ProfileStatus? {
        val company = storage[code.value]
        return company?.let { toProfileStatus(company) }
    }

    private fun toProfileStatus(company: TimestampedCompany): ProfileStatus {
        return if (isProfileCompleted(company)) COMPLETE else PENDING
    }

    private fun isProfileCompleted(company: TimestampedCompany): Boolean {
        return (
            company.englishName.isNotEmpty() &&
                company.telephone?.value?.isNotEmpty() == true &&
                company.telephone?.value?.equals("N/A") == false &&
                company.email.value?.isNotEmpty() == true &&
                company.address.city?.isNotEmpty() == true &&
                company.address.line1?.isNotEmpty() == true &&
                company.address.country?.value?.isNotEmpty() == true &&
                company.address.postCode?.isNotEmpty() == true &&
                company.companySize != null
            )
    }

    override fun getCompanyBySubscriptionType(subscriptionType: String): Set<OrganisationCode> {
        val matchingCompanies = storage.values
            .filter { it.subscriptionType == subscriptionType }
            .filter { it.membershipStatus == ACTIVE }
            .map { it.code }
        return HashSet(matchingCompanies)
    }

    override fun streamAll(action: (CompanyResponse) -> Unit) {
        storage.values
            .sortedBy { it.code }
            .map { CompanyResponse(it.code, it.createdTime, it.updatedTime, it) }
            .forEach(action)
    }

    fun clear() {
        storage.clear()
    }

    override fun findByLocalNameStartingWith(name: String): Set<CompanyResponse> =
        storage.values
            .filter { it.localName?.startsWith(name) ?: false }
            .mapToSet { CompanyResponse(it.code, it.createdTime, it.updatedTime, it) }

    override fun findByLocalName(name: String): Set<CompanyResponse> =
        storage.values
            .filter { it.localName?.equals(name) ?: false }
            .mapToSet { CompanyResponse(it.code, it.createdTime, it.updatedTime, it) }
}
