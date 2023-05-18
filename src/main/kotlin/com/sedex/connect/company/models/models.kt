package com.sedex.connect.company.models

import com.sedex.connect.common.CountryCode
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.SubscriptionType
import com.sedex.connect.common.logging.OperationalEvent
import com.sedex.connect.common.logging.OperationalEventCategory.Error
import com.sedex.connect.company.api.MembershipStatus

data class FailedToGetCompanies(val orgCodes: Set<OrganisationCode>, val message: String?) : OperationalEvent(Error)
data class CompanyUpdatingFailed(val orgCode: OrganisationCode, val ex: Exception) : OperationalEvent(Error)

data class OrganisationSummary(
    val membershipStatus: MembershipStatus?,
    val membershipType: SubscriptionType?,
    val membershipTypeCode: String?,
    val countryCode: CountryCode?
)

data class CompanySubsidiary(
    val code: OrganisationCode,
    val name: String?,
)

data class CompanySubsidiaryRelation(
    val parentOrgCode: OrganisationCode,
    val childOrgCode: OrganisationCode
)
