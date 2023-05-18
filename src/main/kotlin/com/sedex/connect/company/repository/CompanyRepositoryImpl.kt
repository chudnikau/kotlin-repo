package com.sedex.connect.company.repository

import com.fasterxml.jackson.databind.JsonNode
import com.sedex.connect.common.Address
import com.sedex.connect.common.CountryCode
import com.sedex.connect.common.EmailAddress
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.TelephoneNumber
import com.sedex.connect.common.VatNumber
import com.sedex.connect.common.database.ConnectedDatabase
import com.sedex.connect.common.database.JsonKey
import com.sedex.connect.common.database.atMostOne
import com.sedex.connect.common.database.defaultJsonMapper
import com.sedex.connect.common.database.enumeration
import com.sedex.connect.common.database.jsonString
import com.sedex.connect.common.database.jsonb
import com.sedex.connect.common.http.ResponseException
import com.sedex.connect.company.api.Company
import com.sedex.connect.company.api.CompanyResponse
import com.sedex.connect.company.api.CompanySize
import com.sedex.connect.company.api.MembershipStatus
import com.sedex.connect.company.models.PersistedCompany
import com.sedex.connect.company.models.ProfileStatus
import com.sedex.connect.company.models.ProfileStatus.COMPLETE
import com.sedex.connect.company.models.ProfileStatus.PENDING
import com.sedex.connect.company.utils.getValue
import com.sedex.connect.lang.ReasonableClock
import com.sedex.connect.lang.instant
import com.sedex.connect.lang.mapToSet
import org.http4k.core.Status
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.`java-time`.timestamp
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.mapLazy
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class CompanyRepositoryImpl(private val db: ConnectedDatabase, private val clock: ReasonableClock) : CompanyRepository {

    private val maxCompanySearchResultCount = 1000

    override fun create(company: PersistedCompany): CompanyResponse {
        return db.transaction("create company ${company.code}") {
            if (find(company.code) != null) {
                throw CompanyAlreadyExistsException()
            }
            val createdTime = insert(company.code, company)
            CompanyResponse(company.code, createdTime, null, company)
        }
    }

    override fun createOrUpdate(company: PersistedCompany): CompanyResponse {

        return db.transaction("create or update company ${company.code}") {
            val existedCompany = find(company.code)
            var createdTime: Instant? = null
            var updatedTime: Instant? = null
            if (existedCompany == null) {
                createdTime = insert(company.code, company)
            } else {
                val mergedData = existedCompany.toMap() + company.toMap()
                updatedTime = update(company, company.englishName, mergedData)
            }
            return@transaction CompanyResponse(company.code, createdTime, updatedTime, company)
        }
    }

    override fun get(code: OrganisationCode): CompanyResponse? {
        return db.transaction("get company $code") {
            find(code)
        }
    }

    override fun existsByCode(code: OrganisationCode): Boolean {
        return db.transaction("company exists by code: $code") {
            exists(CompanyTable.code eq code.value)
        }
    }

    override fun existsByNameForOrg(name: String, orgCode: OrganisationCode?): Boolean {
        return db.transaction("company exists by name: $name") {
            exists(
                CompanyTable.name.lowerCase() eq name.lowercase()
                    and (CompanyTable.code neq (orgCode?.value ?: ""))
            )
        }
    }

    override fun existsByAddress(address: Address): Boolean {
        return db.transaction("company exists by address: $address") {
            exists(
                CompanyTable.addressLine1.lowerCase() eq address.line1!!.lowercase()
                    and (CompanyTable.city.lowerCase() eq address.city!!.lowercase())
                    and (CompanyTable.countryCode.lowerCase() eq address.country!!.value.lowercase())
                    and (CompanyTable.postCode.lowerCase() eq address.postCode!!.lowercase())
            )
        }
    }

    override fun getByCodes(codes: Set<OrganisationCode>): Set<CompanyResponse> {
        return db.transaction("get companies") {
            return@transaction CompanyTable.select(
                CompanyTable.code inList codes.map { it.value }
            ).limit(maxCompanySearchResultCount).mapToSet { it.toCompany() }
        }
    }

    override fun findByLocalNameStartingWith(name: String): Set<CompanyResponse> =
        db.transaction("get companies by local name starts with") {
            return@transaction CompanyTable.select(
                CompanyTable.data.jsonString(JsonKey("smdNameInLocalLanguage")) like "$name%"
            ).limit(maxCompanySearchResultCount).mapNotNull { it.toCompany() }.mapToSet { it }
        }

    override fun findByLocalName(name: String): Set<CompanyResponse> =
        db.transaction("get companies by local name") {
            return@transaction CompanyTable.select(
                CompanyTable.data.jsonString(JsonKey("smdNameInLocalLanguage")) eq name
            ).limit(maxCompanySearchResultCount).mapNotNull { it.toCompany() }.mapToSet { it }
        }

    override fun streamAll(action: (CompanyResponse) -> Unit) = db.transaction("streamAll") {
        CompanyTable.selectAll()
            .orderBy(CompanyTable.code)
            .mapLazy { it.toCompany() }
            .forEach(action)
    }

    private fun exists(condition: Op<Boolean>): Boolean {
        return !CompanyTable.select(condition).empty()
    }

    override fun getCompanyProfileStatus(code: OrganisationCode): ProfileStatus? {
        return db.transaction("get company profile status") {
            CompanyTable.select(CompanyTable.code eq code.value).atMostOne()?.let {
                if (isProfileCompleted(it.toCompany().toMap())) COMPLETE else PENDING
            }
        }
    }

    override fun getCompanyBySubscriptionType(subscriptionType: String): Set<OrganisationCode> {
        return db.transaction("get companies by subscriptionType") {
            return@transaction CompanyTable.slice(CompanyTable.code).select(
                (CompanyTable.data.jsonString(JsonKey("subscriptionType")) eq subscriptionType)
                    and (CompanyTable.data.jsonString(JsonKey("membershipStatus")) eq "ACTIVE")
            ).mapToSet {
                OrganisationCode(value = it[CompanyTable.code])
            }
        }
    }

    private fun find(code: OrganisationCode): CompanyResponse? {
        return CompanyTable.select(CompanyTable.code eq code.value).singleOrNull()?.toCompany()
    }

    private fun insert(orgCode: OrganisationCode, company: PersistedCompany): Instant {
        val createdTime = clock.instant()
        CompanyTable.insert {
            it[code] = orgCode.value
            it.setValues(company, company.englishName, company.toMap())
            it[this.createdTime] = createdTime
        }
        return createdTime
    }

    private fun update(company: PersistedCompany, companyName: String, companyData: Map<String, Any>): Instant {
        val updatedTime = clock.instant()
        CompanyTable.update({ CompanyTable.code eq company.code.value }) {
            it.setValues(company, companyName, companyData)
            it[this.updatedTime] = updatedTime
        }
        return updatedTime
    }

    private fun UpdateBuilder<*>.setValues(
        company: PersistedCompany,
        companyName: String,
        companyData: Map<String, Any>,
    ) {
        this[CompanyTable.name] = companyName
        this[CompanyTable.addressLine1] = company.address.line1 ?: ""
        this[CompanyTable.addressLine2] = company.address.line2
        this[CompanyTable.city] = company.address.city ?: ""
        this[CompanyTable.countryCode] = company.address.country?.value ?: ""
        this[CompanyTable.postCode] = company.address.postCode ?: ""
        this[CompanyTable.data] = companyData
        this[CompanyTable.companySize] = company.companySize
    }
}

object CompanyTable : Table("company") {
    val code = text("code")
    val name = text("name").nullable()
    val addressLine1 = text("address_line_1").nullable()
    val addressLine2 = text("address_line_2").nullable()
    val city = text("city").nullable()
    val countryCode = text("country").nullable()
    val postCode = text("post_code").nullable()
    val data = jsonb<Map<String, Any>>("data")
    val updatedTime = timestamp("updated_time").nullable()
    val createdTime = timestamp("created_time")
    val companySize = enumeration<CompanySize>("company_size").nullable()
}

class CompanyAlreadyExistsException : Exception()
class CompanyNotFoundException(codes: Set<OrganisationCode>, message: String = "Companies don't exist: $codes") : ResponseException(Status.NOT_FOUND.description(message), message)

private fun ResultRow.toCompany(): CompanyResponse {
    val payload = defaultJsonMapper.convertValue(this[CompanyTable.data], JsonNode::class.java)
    return CompanyResponse(
        code = OrganisationCode(this[CompanyTable.code]),
        isUpdatedByConnect = null,
        createdTime = this[CompanyTable.createdTime],
        updatedTime = this[CompanyTable.updatedTime],
        englishName = this[CompanyTable.name] ?: "",
        localName = payload.getValue("smdNameInLocalLanguage"),
        businessLicenseNumber = payload.getValue("smdBusinessLicenseNumber"),
        primaryIndustryClassificationCode = payload.getValue("primaryIndustryClassificationCode"),
        secondaryIndustryClassificationCode = payload.getValue("secondaryIndustryClassificationCode"),
        address = Address(
            line1 = this[CompanyTable.addressLine1],
            line2 = this[CompanyTable.addressLine2],
            line3 = null,
            line4 = null,
            postCode = this[CompanyTable.postCode],
            city = this[CompanyTable.city],
            country = this[CompanyTable.countryCode]?.let {
                if (it.isNotBlank()) CountryCode(it) else null
            },
        ),
        billingAddress = if (payload.containsAnyBillingAddressValue()) {
            Address(
                line1 = payload.getValue("billing_addressLine1"),
                line2 = payload.getValue("billing_addressLine2"),
                line3 = null,
                line4 = null,
                postCode = payload.getValue("billing_postCode"),
                city = payload.getValue("billing_city"),
                country = payload.getValue("billing_countryCode")?.let {
                    if (it.isNotBlank()) CountryCode(it) else null
                }
            )
        } else null,
        telephone = payload.getValue("telephone")?.let { if (it == "N/A") TelephoneNumber("") else TelephoneNumber(it) },
        email = payload.getValue("contact")?.let { EmailAddress(it) } ?: EmailAddress(""),
        vatNumber = payload.getValue("vatNumber")?.let { VatNumber(it) },
        businessLicenseExpiration = payload.getValue("smdBusinessLicenseExpDate")?.let { it.toLongOrNull()?.toLocalDate() },
        smdEnrollStatus = payload.getValue("smdEnrollStatus"),
        subscriptionType = payload.getValue("subscriptionType"),
        membershipStatus = payload.getValue("membershipStatus")?.let { MembershipStatus.valueOf(it) },
        companySize = this[CompanyTable.companySize],
    )
}

private fun JsonNode.containsAnyBillingAddressValue(): Boolean {
    return this.getValue("billing_addressLine1") != null || this.getValue("billing_addressLine2") != null || this.getValue("billing_postCode") != null || this.getValue("billing_city") != null || this.getValue("billing_countryCode") != null || this.getValue(
        "contactName"
    ) != null
}

private val profileRequiredFields = listOf("name", "telephone", "contact", "city", "addressLine1", "countryCode", "postCode", "companySize")

private fun isProfileCompleted(map: Map<String, Any>): Boolean = profileRequiredFields.none {
    map[it].toString().isEmpty()
} && !map["telephone"].toString().equals("N/A", true)

fun Company.toMap(): Map<String, Any> {
    val result = mutableMapOf<String, Any>()
    result["name"] = englishName
    result["contact"] = email.value

    result["smdNameInLocalLanguage"] = localName.orEmpty()
    result["smdBusinessLicenseNumber"] = businessLicenseNumber.orEmpty()
    result["primaryIndustryClassificationCode"] = primaryIndustryClassificationCode.orEmpty()
    result["secondaryIndustryClassificationCode"] = secondaryIndustryClassificationCode.orEmpty()

    result["addressLine1"] = address.line1.orEmpty()
    result["addressLine2"] = address.line2.orEmpty()
    result["postCode"] = address.postCode.orEmpty()
    result["city"] = address.city.orEmpty()
    result["countryCode"] = address.country?.value.orEmpty()

    result["billing_addressLine1"] = billingAddress?.line1.orEmpty()
    result["billing_addressLine2"] = billingAddress?.line2.orEmpty()
    result["billing_postCode"] = billingAddress?.postCode.orEmpty()
    result["billing_city"] = billingAddress?.city.orEmpty()
    result["billing_countryCode"] = billingAddress?.country?.value.orEmpty()

    result["telephone"] = telephone?.value.orEmpty()
    result["vatNumber"] = vatNumber?.value.orEmpty()
    result["smdBusinessLicenseExpDate"] = businessLicenseExpiration?.toEpochMilli().toString()
    result["smdEnrollStatus"] = smdEnrollStatus.orEmpty()
    result["subscriptionType"] = subscriptionType.orEmpty()
    result["membershipStatus"] = membershipStatus?.name.orEmpty()
    result["companySize"] = companySize?.name.orEmpty()

    return result
}

private fun LocalDate.toEpochMilli(): Long = atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
