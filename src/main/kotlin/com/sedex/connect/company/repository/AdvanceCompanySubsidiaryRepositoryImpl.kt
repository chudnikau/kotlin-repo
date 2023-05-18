package com.sedex.connect.company.repository

import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.database.ConnectedDatabase
import com.sedex.connect.company.models.CompanySubsidiaryRelation
import com.sedex.connect.company.table.AdvanceCompanySubsidiaryTable
import com.sedex.connect.company.table.delete
import com.sedex.connect.company.table.exists
import com.sedex.connect.company.table.getByParentOrgCode
import com.sedex.connect.company.table.getByParentOrgCodeRecursive
import com.sedex.connect.company.table.insert

class AdvanceCompanySubsidiaryRepositoryImpl(private val db: ConnectedDatabase) : AdvanceCompanySubsidiaryRepository {
    override fun create(parentOrgCode: OrganisationCode, childOrgCode: OrganisationCode) {
        db.transaction("Create subsidiary rel between $parentOrgCode and $childOrgCode") {
            if (!exists(parentOrgCode, childOrgCode))
                AdvanceCompanySubsidiaryTable.insert(parentOrgCode, childOrgCode)
        }
    }

    override fun delete(parentOrgCode: OrganisationCode, childOrgCode: OrganisationCode) {
        db.transaction("Delete subsidiary rel between $parentOrgCode and $childOrgCode") {
            AdvanceCompanySubsidiaryTable.delete(parentOrgCode, childOrgCode)
        }
    }

    override fun exists(parentOrgCode: OrganisationCode, childOrgCode: OrganisationCode): Boolean =
        db.transaction("Subsidiary rel exists between $parentOrgCode and $childOrgCode") {
            AdvanceCompanySubsidiaryTable.exists(parentOrgCode, childOrgCode)
        }

    override fun getByParentOrgCode(parentOrgCode: OrganisationCode): Set<OrganisationCode> =
        db.transaction("Get all Subsidiaries by $parentOrgCode") {
            AdvanceCompanySubsidiaryTable.getByParentOrgCode(parentOrgCode)
        }

    override fun getSubsidiariesRecursive(parentOrgCode: OrganisationCode): Set<CompanySubsidiaryRelation> =
        db.transaction("Get subsidiaries recursive") {
            AdvanceCompanySubsidiaryTable.getByParentOrgCodeRecursive(parentOrgCode)
        }
}
