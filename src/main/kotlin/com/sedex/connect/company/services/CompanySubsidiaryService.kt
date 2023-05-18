package com.sedex.connect.company.services

import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.company.models.CompanySubsidiary
import com.sedex.connect.company.models.CompanySubsidiaryRelation
import com.sedex.connect.company.repository.AdvanceCompanySubsidiaryRepository
import com.sedex.connect.lang.mapToSet

class CompanySubsidiaryService(
    private val companySubsidiaryRepository: AdvanceCompanySubsidiaryRepository,
    private val companyService: CompanyService
) {
    fun create(parentOrgCode: OrganisationCode, childOrgCode: OrganisationCode) {
        if (!companySubsidiaryRepository.exists(parentOrgCode, childOrgCode)) {
            companySubsidiaryRepository.create(parentOrgCode, childOrgCode)
        }
    }

    fun delete(parentOrgCode: OrganisationCode, childOrgCode: OrganisationCode) {
        companySubsidiaryRepository.delete(parentOrgCode, childOrgCode)
    }

    fun getCompanySubsidiaries(parentOrgCode: OrganisationCode): Set<CompanySubsidiary> {
        val subsidiaryCodes = companySubsidiaryRepository.getByParentOrgCode(parentOrgCode)
        if (subsidiaryCodes.isEmpty()) return emptySet()
        return companyService.getLatestCompaniesByCodes(subsidiaryCodes)
            .mapToSet { CompanySubsidiary(it.code, it.englishName) }
    }

    fun getCompanySubsidiariesTree(parentOrgCode: OrganisationCode): List<CompanySubsidiaryRelation> {
        return companySubsidiaryRepository.getSubsidiariesRecursive(parentOrgCode).toList()
    }
}
