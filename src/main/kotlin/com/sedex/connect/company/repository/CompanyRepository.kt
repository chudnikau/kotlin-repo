package com.sedex.connect.company.repository

import com.sedex.connect.common.Address
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.company.api.CompanyResponse
import com.sedex.connect.company.models.PersistedCompany
import com.sedex.connect.company.models.ProfileStatus

interface CompanyRepository {
    fun create(company: PersistedCompany): CompanyResponse
    fun createOrUpdate(company: PersistedCompany): CompanyResponse
    fun get(code: OrganisationCode): CompanyResponse?
    fun existsByCode(code: OrganisationCode): Boolean
    fun existsByNameForOrg(name: String, orgCode: OrganisationCode?): Boolean
    fun existsByAddress(address: Address): Boolean
    fun getByCodes(codes: Set<OrganisationCode>): Set<CompanyResponse>
    fun getCompanyProfileStatus(code: OrganisationCode): ProfileStatus?
    fun getCompanyBySubscriptionType(subscriptionType: String): Set<OrganisationCode>
    fun streamAll(action: (CompanyResponse) -> Unit)
    fun findByLocalNameStartingWith(name: String): Set<CompanyResponse>
    fun findByLocalName(name: String): Set<CompanyResponse>
}
