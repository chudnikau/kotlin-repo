package com.sedex.connect.company

import com.sedex.connect.common.Address
import com.sedex.connect.common.CountryCode
import com.sedex.connect.common.EmailAddress
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.TelephoneNumber
import com.sedex.connect.company.api.MembershipStatus.ACTIVE
import com.sedex.connect.company.models.PersistedCompany

val persistedCompanyExample = persistedCompanyExample("Local company name", "My Company")

fun persistedCompanyExample(localName: String, englishName: String): PersistedCompany = PersistedCompany(
    englishName = englishName,
    localName = localName,
    businessLicenseNumber = null,
    businessLicenseExpiration = null,
    primaryIndustryClassificationCode = "ZIC1000000",
    secondaryIndustryClassificationCode = "ZIC1000100",
    address = Address(
        line1 = "Somewhere",
        line2 = null, line3 = null, line4 = null,
        postCode = "AB12 3CD",
        city = "London",
        country = CountryCode("GB")
    ),
    telephone = TelephoneNumber("07123456789"),
    email = EmailAddress("email@example.com"),
    billingAddress = null,
    vatNumber = null,
    smdEnrollStatus = "OPTED_IN",
    membershipStatus = ACTIVE,
    subscriptionType = "ST002",
    isUpdatedByConnect = null,
    code = OrganisationCode("ZC12345"),
    companySize = null
)
