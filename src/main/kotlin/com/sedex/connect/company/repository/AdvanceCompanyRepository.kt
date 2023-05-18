package com.sedex.connect.company.repository

import com.sedex.connect.common.Address
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.company.api.CompanyResponse
import com.sedex.connect.company.models.AdvanceCompany
import com.sedex.connect.company.models.AdvanceCompanyAddress

interface AdvanceCompanyRepository {

    fun get(code: OrganisationCode): CompanyResponse?

    fun existsByCode(code: OrganisationCode): Boolean

    fun existsByNameForOrg(name: String, orgCode: OrganisationCode?): Boolean

    fun existsByAddress(address: Address): Boolean

    fun getByCodes(codes: Set<OrganisationCode>): Set<CompanyResponse>

    fun insert(company: AdvanceCompany)

    fun insertAddress(address: AdvanceCompanyAddress)

    fun getAdvanceCompanyBySubscriptionType(subscriptionType: String): Set<OrganisationCode>

    fun findByLocalNameStartingWith(name: String): Set<CompanyResponse>

    fun findByLocalName(name: String): Set<CompanyResponse>
}
