package com.sedex.connect.company.fixtures

import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.company.models.CompanySubsidiaryRelation
import com.sedex.connect.company.repository.AdvanceCompanySubsidiaryRepository
import com.sedex.connect.lang.mapToSet

class FakeAdvanceCompanySubsidiaryRepository : AdvanceCompanySubsidiaryRepository {

    private val subsidiaries = mutableSetOf<CompanySubsidiaryRelation>()

    override fun create(parentOrgCode: OrganisationCode, childOrgCode: OrganisationCode) {
        subsidiaries.add(CompanySubsidiaryRelation(parentOrgCode, childOrgCode))
    }

    override fun delete(parentOrgCode: OrganisationCode, childOrgCode: OrganisationCode) {
        subsidiaries.remove(CompanySubsidiaryRelation(parentOrgCode, childOrgCode))
    }

    override fun exists(parentOrgCode: OrganisationCode, childOrgCode: OrganisationCode): Boolean =
        subsidiaries.contains(CompanySubsidiaryRelation(parentOrgCode, childOrgCode))

    override fun getByParentOrgCode(parentOrgCode: OrganisationCode): Set<OrganisationCode> =
        subsidiaries.filter { it.parentOrgCode == parentOrgCode }
            .mapToSet { it.childOrgCode }

    fun clear() = subsidiaries.clear()

    override fun getSubsidiariesRecursive(parentOrgCode: OrganisationCode): Set<CompanySubsidiaryRelation> {
        return subsidiaries
            .filter { it.parentOrgCode == parentOrgCode }
            .flatMap { rel -> getSubsidiariesRecursive(rel.childOrgCode) + rel }
            .toSet()
    }
}
