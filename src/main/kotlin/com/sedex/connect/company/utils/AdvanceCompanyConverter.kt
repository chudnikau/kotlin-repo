package com.sedex.connect.company.utils

import com.sedex.connect.common.Address
import com.sedex.connect.common.CountryCode
import com.sedex.connect.common.EmailAddress
import com.sedex.connect.common.TelephoneNumber
import com.sedex.connect.common.VatNumber
import com.sedex.connect.company.api.CompanyResponse
import com.sedex.connect.company.api.CompanySize
import com.sedex.connect.company.api.MembershipStatus
import com.sedex.connect.company.models.AdvanceCompany
import com.sedex.connect.company.models.AdvanceCompanyAddress
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

fun toCompany(
    advanceCompany: AdvanceCompany,
    advanceAddress: AdvanceCompanyAddress,
    advanceBillingAddress: AdvanceCompanyAddress?,
): CompanyResponse {
    val payload = advanceCompany.data
    return CompanyResponse(
        code = advanceCompany.code,
        isUpdatedByConnect = payload.getValue("isUpdatedByConnect")?.toBoolean(),
        createdTime = advanceCompany.createdTime,
        updatedTime = advanceCompany.updatedTime,
        englishName = payload.getValue("name") ?: "",
        localName = payload.getValue("smdNameInLocalLanguage"),
        businessLicenseNumber = payload.getValue("smdBusinessLicenseNumber"),
        primaryIndustryClassificationCode = payload.getValue("primaryIndustryClassificationCode"),
        secondaryIndustryClassificationCode = payload.getValue("secondaryIndustryClassificationCode"),
        address = advanceAddress.toAddress(),
        billingAddress = advanceBillingAddress?.toAddress(),
        telephone = payload.getValue("telephone")?.let { if (it == "N/A") TelephoneNumber("") else TelephoneNumber(it) },
        email = payload.getValue("contact")?.let { EmailAddress(it) } ?: EmailAddress(""),
        vatNumber = payload.getValue("vatNumber")?.let { VatNumber(it) },
        businessLicenseExpiration = payload.getValue("smdBusinessLicenseExpDate")?.let { it.toLongOrNull()?.toLocalDate() },
        smdEnrollStatus = payload.getValue("smdEnrollStatus"),
        subscriptionType = payload.getValue("subscriptionType"),
        membershipStatus = payload.getValue("membershipStatus")?.let { MembershipStatus.valueOf(it) },
        companySize = payload.getValue("companySize")?.let { CompanySize.valueOf(it) }
    )
}

fun AdvanceCompanyAddress.toAddress(): Address = Address(
    data.getValue("addressLine1"),
    data.getValue("addressLine2"),
    data.getValue("addressLine3"),
    data.getValue("addressLine4"),
    data.getValue("postCode"),
    data.getValue("city"),
    data.getValue("countryCode")?.let { if (it.isNotBlank()) CountryCode(it) else null }
)

fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
