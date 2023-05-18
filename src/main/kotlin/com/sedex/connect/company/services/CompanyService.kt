package com.sedex.connect.company.services

import com.sedex.connect.advance.company.AdvanceAdapterApi
import com.sedex.connect.advance.company.AdvanceIndustryClassification
import com.sedex.connect.advance.company.AdvanceMembershipStatus
import com.sedex.connect.advance.company.AdvanceSmdEnrollStatus
import com.sedex.connect.advance.company.AdvanceSubscriptionType
import com.sedex.connect.advance.company.CompanyRequest
import com.sedex.connect.advancecache.advanceCode
import com.sedex.connect.advancecache.fromAdvanceCode
import com.sedex.connect.common.Address
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.SmdEnrollStatus.OPTED_OUT
import com.sedex.connect.common.SubscriptionType
import com.sedex.connect.common.SubscriptionType.Auditor
import com.sedex.connect.company.api.Company
import com.sedex.connect.company.api.CompanyResponse
import com.sedex.connect.company.api.CompanySearchResponse
import com.sedex.connect.company.api.CompanySize
import com.sedex.connect.company.api.MembershipStatus
import com.sedex.connect.company.api.toCompanySearchResponse
import com.sedex.connect.company.api.validateStatus
import com.sedex.connect.company.models.AdvanceSubscription
import com.sedex.connect.company.models.OrganisationSummary
import com.sedex.connect.company.models.PersistedCompany
import com.sedex.connect.company.models.ProfileStatus
import com.sedex.connect.company.models.ProfileStatus.COMPLETE
import com.sedex.connect.company.models.ProfileStatus.PENDING
import com.sedex.connect.company.repository.AdvanceCompanyRepository
import com.sedex.connect.company.repository.CompanyNotFoundException
import com.sedex.connect.company.repository.CompanyRepository
import com.sedex.connect.company.validateCompany
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

class CompanyService(
    private val sendToAdvance: Boolean,
    private val advanceAdapterApi: AdvanceAdapterApi,
    private val companyRepository: CompanyRepository,
    private val advanceRepository: AdvanceCompanyRepository,
    private val advanceSubscriptionService: AdvanceSubscriptionService,
    private val companyKafkaService: CompanyKafkaService,
) {
    fun getLatestCompany(orgCode: OrganisationCode): PersistedCompany {
        val advanceCompany = advanceRepository.get(orgCode)
        val connectCompany = companyRepository.get(orgCode)
        return listOfNotNull(connectCompany, advanceCompany)
            .sortedByDescending { it.lastModifiedAt }
            .map { company ->
                PersistedCompany(
                    company.copy(companySize = connectCompany?.companySize),
                    advanceCompany?.isUpdatedByConnect
                )
            }
            .firstOrNull() ?: throw CompanyNotFoundException(setOf(orgCode))
    }

    fun getLatestCompaniesByCodes(codes: Set<OrganisationCode>): Set<CompanyResponse> {
        val connectCompanies = companyRepository.getByCodes(codes)
        val advanceCompanies = advanceRepository.getByCodes(codes)
        val allCompanies = connectCompanies + advanceCompanies
        val latestCompanies = allCompanies.groupingBy { it.code }
            .reduce { _, lastCo, thisCo ->
                takeLatestCompany(lastCo, thisCo)
            }.values
            .toSet()

        return latestCompanies
    }

    private fun takeLatestCompany(left: CompanyResponse, right: CompanyResponse): CompanyResponse {
        return when {
            left.lastModifiedAt == null -> right
            right.lastModifiedAt == null -> left
            left.lastModifiedAt!!.isAfter(right.lastModifiedAt) -> left
            else -> right
        }.copy(companySize = left.companySize ?: right.companySize)
    }

    fun existsByCode(code: OrganisationCode): Boolean {
        return companyRepository.existsByCode(code) || advanceRepository.existsByCode(code)
    }

    fun existsByNameForOrg(name: String, orgCode: OrganisationCode?): Boolean {
        return companyRepository.existsByNameForOrg(name, orgCode) || advanceRepository.existsByNameForOrg(name, orgCode)
    }

    fun existsByAddress(address: Address): Boolean {
        return companyRepository.existsByAddress(address) || advanceRepository.existsByAddress(address)
    }

    fun create(company: Company, orgCode: OrganisationCode): CompanyResponse {
        return companyRepository.create(PersistedCompany(orgCode, company)).also { created ->
            companyKafkaService.companyChanged(created)
        }
    }

    fun update(company: PersistedCompany, orgCode: OrganisationCode): CompanyResponse {
        val companyToUpdate = if (company.membershipStatus == null || company.subscriptionType == null) {
            val fromDb = getLatestCompany(orgCode)
            PersistedCompany(orgCode, company).copy(
                membershipStatus = fromDb.membershipStatus,
                subscriptionType = fromDb.subscriptionType
            )
        } else {
            company
        }
        sendToAdvance(companyToUpdate, orgCode)
        return companyRepository.createOrUpdate(companyToUpdate).also { updated ->
            companyKafkaService.companyChanged(updated)
        }
    }

    fun getCompanyWithSubscription(orgCode: OrganisationCode): Pair<PersistedCompany, AdvanceSubscription?> {
        val company = getLatestCompany(orgCode)
        val subscription = advanceSubscriptionService.findByOrgCode(orgCode)
        return verifyMembershipStatus(company, subscription) to subscription
    }

    fun getSelfOrganisationMembershipSummary(orgCode: OrganisationCode): OrganisationSummary {
        val company = getCompanyWithSubscription(orgCode).first
        return OrganisationSummary(
            membershipStatus = company.membershipStatus,
            membershipType = company.subscriptionType?.fromAdvanceCode(),
            membershipTypeCode = company.subscriptionType,
            countryCode = company.address.country
        )
    }

    fun getAuditCompanies(): Set<OrganisationCode> {
        val connectCompanies = companyRepository.getCompanyBySubscriptionType((Auditor.advanceCode()))
        val advanceCompanies = advanceRepository.getAdvanceCompanyBySubscriptionType(Auditor.advanceCode())
        return connectCompanies + advanceCompanies
    }

    fun updateCompanySubscription(orgCode: OrganisationCode, membershipStatus: MembershipStatus, subscriptionType: SubscriptionType): CompanyResponse {
        val updatedCompany = getLatestCompany(orgCode).copy(
            membershipStatus = membershipStatus,
            subscriptionType = subscriptionType.advanceCode()
        )
        return update(updatedCompany, orgCode)
    }

    fun updateCompanySize(orgCode: OrganisationCode, companySize: CompanySize): CompanyResponse {
        val updatedCompany = getLatestCompany(orgCode).copy(companySize = companySize)
        return update(updatedCompany, orgCode)
    }

    private fun verifyMembershipStatus(company: PersistedCompany, subscription: AdvanceSubscription?): PersistedCompany {
        if (company.membershipStatus == null) return company
        val membershipExpires = subscription?.endDate?.let { LocalDate.ofInstant(it, ZoneOffset.UTC) }
        val subscriptionType = company.subscriptionType?.fromAdvanceCode()
        val membership = if (subscriptionType != null) {
            company.membershipStatus.validateStatus(LocalDate.now(), membershipExpires, subscriptionType)
        } else {
            company.membershipStatus
        }
        return company.copy(membershipStatus = membership)
    }

    private fun sendToAdvance(company: Company, orgCode: OrganisationCode) {
        if (sendToAdvance) {
            advanceAdapterApi.sendCompanyToAdvance(company.toAdvanceRequest(), orgCode)
        }
    }

    private fun Company.toAdvanceRequest(): CompanyRequest {
        validateCompany(this)
        return CompanyRequest(
            requestId = UUID.randomUUID(),
            englishName = englishName,
            localName = localName,
            businessLicenseNumber = businessLicenseNumber,
            businessLicenseExpiration = businessLicenseExpiration,
            address = address,
            telephone = telephone,
            email = email,
            billingAddress = billingAddress,
            vatNumber = vatNumber,
            smdEnrollStatus = AdvanceSmdEnrollStatus(smdEnrollStatus ?: OPTED_OUT.name),
            membershipStatus = AdvanceMembershipStatus(membershipStatus!!.name),
            subscriptionType = AdvanceSubscriptionType(subscriptionType!!),
            primaryIndustryClassificationCode = primaryIndustryClassificationCode?.let { AdvanceIndustryClassification(primaryIndustryClassificationCode!!) },
            secondaryIndustryClassificationCode = secondaryIndustryClassificationCode?.let { AdvanceIndustryClassification(secondaryIndustryClassificationCode!!) },
        )
    }

    fun getCompanyProfileStatus(orgCode: OrganisationCode): ProfileStatus {

        val companyProfileStatus = companyRepository.getCompanyProfileStatus(orgCode)
        val advanceCompany = advanceRepository.get(orgCode)

        if ((companyProfileStatus == null) && (advanceCompany == null)) {
            throw CompanyNotFoundException(setOf(orgCode))
        }

        val advanceCompanyProfileStatus = getAdvanceCompanyProfileStatus(advanceCompany)

        if (((companyProfileStatus ?: PENDING) == COMPLETE) || (advanceCompanyProfileStatus == COMPLETE)) {
            return COMPLETE
        }
        return PENDING
    }

    private fun getAdvanceCompanyProfileStatus(advanceCompany: CompanyResponse?): ProfileStatus {
        if (advanceCompany == null) {
            return PENDING
        }

        if (anyAreEmpty(
                advanceCompany.englishName,
                advanceCompany.telephone?.value,
                advanceCompany.email.value,
                advanceCompany.address.city,
                advanceCompany.address.line1,
                advanceCompany.address.country?.value,
                advanceCompany.address.postCode
            )
        )
            return PENDING

        if (advanceCompany.telephone?.value.equals("N/A", true))
            return PENDING

        return COMPLETE
    }

    private fun anyAreEmpty(vararg values: String?): Boolean {
        return values.any { it.isNullOrEmpty() }
    }

    fun streamCompaniesToKafka(): Long {
        val count = AtomicLong()
        companyRepository.streamAll { company ->
            companyKafkaService.companyChanged(company)
            count.incrementAndGet()
        }
        return count.get()
    }

    private fun findByCode(code: String): List<CompanySearchResponse> {
        val connectCompanies = companyRepository.getByCodes(setOf(OrganisationCode(code)))
        val advanceCompanies = advanceRepository.getByCodes(setOf(OrganisationCode(code)))

        return (connectCompanies + advanceCompanies)
            .map { it.toCompanySearchResponse() }
            .toSet().sortedBy { it.englishName }
    }

    private fun findByNameStartingWith(name: String): List<CompanySearchResponse> {
        val exactConnectCompanies = companyRepository.findByLocalName(name)
        val exactAdvanceCompanies = advanceRepository.findByLocalName(name)
        val allExactCompanies = (exactConnectCompanies + exactAdvanceCompanies)
            .map { it.toCompanySearchResponse() }
            .toSet()
            .sortedBy { it.englishName }

        val matchConnectCompanies = companyRepository.findByLocalNameStartingWith(name)
        val matchAdvanceCompanies = advanceRepository.findByLocalNameStartingWith(name)
        val allMatchCompanies = (matchConnectCompanies + matchAdvanceCompanies)
            .map { it.toCompanySearchResponse() }
            .toSet()
            .sortedBy { it.englishName }

        val resultSearchResponse = (allExactCompanies + allMatchCompanies)
            .toSet()
            .map {
                CompanySearchResponse(
                    it.code,
                    it.englishName,
                    it.subscriptionType,
                    it.membershipStatus
                )
            }
        return resultSearchResponse
    }

    private fun String.isZCCode() =
        this.matches(Regex("^ZC\\d+$"))

    fun searchCompaniesByName(searchName: String): List<CompanySearchResponse> =
        if (searchName.isZCCode())
            findByCode(searchName)
        else
            findByNameStartingWith(searchName)
}
