package com.sedex.connect.company.utils

import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.company.api.CompanyRequest
import com.sedex.connect.company.models.PersistedCompany

fun CompanyRequest.toCompany(orgCode: OrganisationCode, isUpdatedByConnect: Boolean? = null): PersistedCompany = PersistedCompany(
    code = orgCode,
    isUpdatedByConnect = isUpdatedByConnect,
    englishName = englishName,
    localName = localName,
    businessLicenseNumber = businessLicenseNumber,
    businessLicenseExpiration = businessLicenseExpiration,
    primaryIndustryClassificationCode = primaryIndustryClassificationCode,
    secondaryIndustryClassificationCode = secondaryIndustryClassificationCode,
    address = address,
    telephone = telephone,
    email = email,
    billingAddress = billingAddress,
    vatNumber = vatNumber,
    smdEnrollStatus = smdEnrollStatus,
    subscriptionType = null,
    membershipStatus = null,
    companySize = companySize,
)
