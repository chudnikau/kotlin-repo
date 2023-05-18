package com.sedex.connect.company.models

import com.sedex.connect.common.Address
import com.sedex.connect.common.EmailAddress
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.TelephoneNumber
import com.sedex.connect.common.VatNumber
import com.sedex.connect.company.api.Company
import com.sedex.connect.company.api.CompanyResponse
import com.sedex.connect.company.api.CompanySize
import com.sedex.connect.company.api.MembershipStatus
import java.time.LocalDate

data class PersistedCompany(
    val code: OrganisationCode,
    val isUpdatedByConnect: Boolean?,
    override val englishName: String,
    override val localName: String?,
    override val businessLicenseNumber: String?,
    override val businessLicenseExpiration: LocalDate?,
    override val primaryIndustryClassificationCode: String?,
    override val secondaryIndustryClassificationCode: String?,
    override val address: Address,
    override val telephone: TelephoneNumber?,
    override val email: EmailAddress,
    override val billingAddress: Address?,
    override val vatNumber: VatNumber?,
    override val smdEnrollStatus: String?,
    override val subscriptionType: String?,
    override val membershipStatus: MembershipStatus?,
    override val companySize: CompanySize?
) : Company {
    constructor(orgCode: OrganisationCode, company: Company, isUpdatedByConnect: Boolean?) : this(
        code = orgCode,
        isUpdatedByConnect = isUpdatedByConnect,
        englishName = company.englishName,
        localName = company.localName,
        businessLicenseNumber = company.businessLicenseNumber,
        businessLicenseExpiration = company.businessLicenseExpiration,
        primaryIndustryClassificationCode = company.primaryIndustryClassificationCode,
        secondaryIndustryClassificationCode = company.secondaryIndustryClassificationCode,
        address = company.address,
        telephone = company.telephone,
        email = company.email,
        billingAddress = company.billingAddress,
        vatNumber = company.vatNumber,
        smdEnrollStatus = company.smdEnrollStatus,
        subscriptionType = company.subscriptionType,
        membershipStatus = company.membershipStatus,
        companySize = company.companySize,
    )

    constructor(orgCode: OrganisationCode, company: Company) : this(
        orgCode,
        company,
        null
    )

    constructor(companyResponse: CompanyResponse, isUpdatedByConnect: Boolean?) : this(
        companyResponse.code,
        companyResponse,
        isUpdatedByConnect
    )

    constructor(companyResponse: CompanyResponse) : this(
        companyResponse,
        null
    )
}

fun PersistedCompany.toResponse(): CompanyResponse = CompanyResponse(code, this, isUpdatedByConnect)
