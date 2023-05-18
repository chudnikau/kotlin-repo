package com.sedex.connect.company

import com.sedex.connect.advancecache.fromAdvanceCode
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.SubscriptionType
import com.sedex.connect.common.asSuccess
import com.sedex.connect.common.auth.UserCode
import com.sedex.connect.company.models.CompanySubsidiaryRelation
import com.sedex.connect.company.services.CompanyService
import com.sedex.connect.company.services.CompanySubsidiaryService
import com.sedex.connect.lang.mapToSet
import com.sedex.connect.security.SecurityContext
import com.sedex.connect.security.http4k.UserCanViewCompanyByOrgCodesSecurity
import com.sedex.connect.security.http4k.UserCanViewCompanySecurity
import com.sedex.connect.user.api.UserApi
import org.http4k.contract.security.Security
import org.http4k.core.Request

class CompanySecurityContext(
    private val parent: SecurityContext,
    private val userApi: UserApi,
    private val companyService: CompanyService,
    private val companySubsidiaryService: CompanySubsidiaryService,
) : SecurityContext by parent {
    fun canAccessByOrgCode(getOrgCode: (Request) -> String?): Security =
        UserCanViewCompanySecurity(
            getOrgCode = getOrgCode,
            getOrgCodeByUserCode = ::getOrgCodeByUserCode,
            getSubsidiariesEnabled = true,
            getCompanySubsidiaries = ::getCompanySubsidiaries,
        )

    private fun getCompanySubsidiaries(orgCode: OrganisationCode) =
        companySubsidiaryService.getCompanySubsidiariesTree(orgCode)
            .mapToSet(CompanySubsidiaryRelation::childOrgCode)
            .asSuccess()

    fun canAccessByOrgCodes(getOrgCodes: (Request) -> Set<OrganisationCode>): Security =
        UserCanViewCompanyByOrgCodesSecurity(
            getOrgCodes = getOrgCodes,
            getOrgCodeByUserCode = ::getOrgCodeByUserCode,
            getSubscriptionTypeByOrgCode = ::getSubscriptionTypeByOrgCode,
        )

    private fun getOrgCodeByUserCode(userCode: UserCode?): OrganisationCode? {
        return userCode?.let {
            userApi.getUserBySedexCode(it)?.orgCode?.let { orgCode -> OrganisationCode(orgCode) }
        }
    }

    private fun getSubscriptionTypeByOrgCode(orgCode: OrganisationCode): SubscriptionType? {
        return companyService.getLatestCompany(orgCode).subscriptionType?.fromAdvanceCode()
    }
}
