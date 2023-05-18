package com.sedex.connect.company.fixtures

import com.sedex.connect.common.Address
import com.sedex.connect.common.EmailAddress
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.TelephoneNumber
import com.sedex.connect.common.VatNumber
import com.sedex.connect.company.api.Company
import com.sedex.connect.company.api.CompanySize
import com.sedex.connect.company.api.MembershipStatus
import java.time.Instant
import java.time.LocalDate

data class TimestampedCompany(
    val code: OrganisationCode,
    val createdTime: Instant?,
    val updatedTime: Instant?,
    override val englishName: String,
    override val localName: String?,
    override val businessLicenseNumber: String?,
    override val businessLicenseExpiration: LocalDate?,
    override val address: Address,
    override val telephone: TelephoneNumber?,
    override val email: EmailAddress,
    override val billingAddress: Address?,
    override val vatNumber: VatNumber?,
    override val smdEnrollStatus: String?,
    override val membershipStatus: MembershipStatus?,
    override val subscriptionType: String?,
    override val primaryIndustryClassificationCode: String?,
    override val secondaryIndustryClassificationCode: String?,
    override val companySize: CompanySize?
) : Company {
    constructor(
        orgCode: OrganisationCode,
        company: Company,
        createdTime: Instant?,
        updatedTime: Instant?
    ) : this(
        code = orgCode,
        createdTime = createdTime,
        updatedTime = updatedTime,
        englishName = company.englishName,
        localName = company.localName,
        businessLicenseNumber = company.businessLicenseNumber,
        businessLicenseExpiration = company.businessLicenseExpiration,
        address = company.address,
        telephone = company.telephone,
        email = company.email,
        billingAddress = company.billingAddress,
        vatNumber = company.vatNumber,
        smdEnrollStatus = company.smdEnrollStatus,
        membershipStatus = company.membershipStatus,
        subscriptionType = company.subscriptionType,
        primaryIndustryClassificationCode = company.primaryIndustryClassificationCode,
        secondaryIndustryClassificationCode = company.secondaryIndustryClassificationCode,
        companySize = company.companySize
    )
}
