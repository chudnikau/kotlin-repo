package com.sedex.connect.company.repository

import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.company.models.CompanySubsidiaryRelation

interface AdvanceCompanySubsidiaryRepository {
    fun create(parentOrgCode: OrganisationCode, childOrgCode: OrganisationCode)
    fun delete(parentOrgCode: OrganisationCode, childOrgCode: OrganisationCode)
    fun exists(parentOrgCode: OrganisationCode, childOrgCode: OrganisationCode): Boolean
    fun getByParentOrgCode(parentOrgCode: OrganisationCode): Set<OrganisationCode>
    fun getSubsidiariesRecursive(parentOrgCode: OrganisationCode): Set<CompanySubsidiaryRelation>
}
