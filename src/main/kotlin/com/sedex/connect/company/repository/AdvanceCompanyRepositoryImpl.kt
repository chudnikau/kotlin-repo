package com.sedex.connect.company.repository

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.sedex.connect.common.Address
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.database.ConnectedDatabase
import com.sedex.connect.common.database.JsonKey
import com.sedex.connect.common.database.defaultJsonMapper
import com.sedex.connect.common.database.jsonString
import com.sedex.connect.common.database.jsonb
import com.sedex.connect.company.api.CompanyJson
import com.sedex.connect.company.api.CompanyResponse
import com.sedex.connect.company.models.AddressType
import com.sedex.connect.company.models.AddressType.BILLING_ADDRESS
import com.sedex.connect.company.models.AddressType.COMPANY_ADDRESS
import com.sedex.connect.company.models.AdvanceCompany
import com.sedex.connect.company.models.AdvanceCompanyAddress
import com.sedex.connect.company.utils.toCompany
import com.sedex.connect.lang.mapToSet
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
import org.jetbrains.exposed.sql.select
import java.time.Instant

class AdvanceCompanyRepositoryImpl(private val db: ConnectedDatabase) : AdvanceCompanyRepository {

    private val maxCompanySearchResultCount = 1000

    override fun get(code: OrganisationCode): CompanyResponse? {
        return db.transaction("get advance company with addresses $code") {
            val advanceCompany = findCompany(code) ?: return@transaction null
            val advanceCompanyAddress = findAddress(code, COMPANY_ADDRESS) ?: return@transaction null
            val advanceCompanyBillingAddress = findAddress(code, BILLING_ADDRESS)
            toCompany(advanceCompany, advanceCompanyAddress, advanceCompanyBillingAddress)
        }
    }

    override fun existsByCode(code: OrganisationCode): Boolean {
        return db.transaction("Advance company exists by code: $code") {
            exists(AdvanceCompanyTable.code eq code.value)
        }
    }

    override fun existsByNameForOrg(name: String, orgCode: OrganisationCode?): Boolean {
        return db.transaction("Advance company exists by name: $name") {
            exists(
                AdvanceCompanyTable.name.lowerCase() eq name.lowercase()
                    and (AdvanceCompanyTable.code neq (orgCode?.value ?: ""))
            )
        }
    }

    override fun existsByAddress(address: Address): Boolean {
        return db.transaction("Advance company address exists by address: $address") {
            addressExists(
                AdvanceCompanyAddressTable.addressLine1.lowerCase() eq address.line1!!.lowercase()
                    and (AdvanceCompanyAddressTable.city.lowerCase() eq address.city!!.lowercase())
                    and (AdvanceCompanyAddressTable.country.lowerCase() eq address.country!!.value.lowercase())
                    and (AdvanceCompanyAddressTable.postCode.lowerCase() eq address.postCode!!.lowercase())
            )
        }
    }

    override fun getByCodes(codes: Set<OrganisationCode>): Set<CompanyResponse> {
        val companies = getCompaniesByCodes(codes)
        val addresses = getAddressesByCodes(codes, COMPANY_ADDRESS)
        val billingAddresses = getAddressesByCodes(codes, BILLING_ADDRESS)

        return companies.mapNotNull { c ->
            val address = findAddressByCode(addresses, c.code)
            val billingAddress = findAddressByCode(billingAddresses, c.code)
            address?.let { toCompany(c, it, billingAddress) }
        }.toSet()
    }

    private fun findAddressByCode(addresses: Set<AdvanceCompanyAddress>, code: OrganisationCode): AdvanceCompanyAddress? {
        return addresses.firstOrNull { it.code == code }
    }

    private fun getCompaniesByCodes(codes: Set<OrganisationCode>): Set<AdvanceCompany> {
        return db.transaction("get advance companies") {
            return@transaction AdvanceCompanyTable.select(AdvanceCompanyTable.code inList codes.map { it.value }).mapToSet { it.toAdvanceCompany() }
        }
    }

    private fun getAddressesByCodes(codes: Set<OrganisationCode>, addressType: AddressType): Set<AdvanceCompanyAddress> {
        return db.transaction("get advance company addresses") {
            return@transaction AdvanceCompanyAddressTable.select {
                AdvanceCompanyAddressTable.code inList codes.map { it.value } and
                    (AdvanceCompanyAddressTable.addressType eq addressType.toString())
            }
                .mapToSet { it.toAdvanceCompanyAddress() }
        }
    }

    private fun exists(condition: Op<Boolean>): Boolean {
        return !AdvanceCompanyTable.select(condition).empty()
    }

    private fun addressExists(condition: Op<Boolean>): Boolean {
        return !AdvanceCompanyAddressTable.select(condition).empty()
    }

    private fun findCompany(code: OrganisationCode): AdvanceCompany? {
        return AdvanceCompanyTable.select(AdvanceCompanyTable.code eq code.value).singleOrNull()?.toAdvanceCompany()
    }

    private fun findAddress(code: OrganisationCode, addressType: AddressType): AdvanceCompanyAddress? {
        return AdvanceCompanyAddressTable.select {
            (AdvanceCompanyAddressTable.code eq code.value) and
                (AdvanceCompanyAddressTable.addressType eq addressType.toString())
        }
            .singleOrNull()?.toAdvanceCompanyAddress()
    }

    override fun insert(company: AdvanceCompany) {
        return db.transaction("insert advance company") {
            AdvanceCompanyTable.insert {
                it[code] = company.code.value
                it[name] = company.data.get("name").textValue()
                it[data] = defaultJsonMapper.convertValue(company.data, object : TypeReference<Map<String, Any>>() {})
                it[createdTime] = company.createdTime
                it[updatedTime] = company.updatedTime
            }
        }
    }

    override fun insertAddress(address: AdvanceCompanyAddress) {
        return db.transaction("insert advance company address") {
            AdvanceCompanyAddressTable.insert {
                it[code] = address.code.value
                it[addressType] = address.addressType.toString()
                it[addressLine1] = address.data.get("addressLine1")?.textValue()
                it[addressLine2] = address.data.get("addressLine2")?.textValue()
                it[city] = address.data.get("city")?.textValue()
                it[country] = address.data.get("countryCode")?.textValue()
                it[postCode] = address.data.get("postCode")?.textValue()
                it[data] = address.data
                it[createdTime] = Instant.now()
            }
        }
    }

    override fun getAdvanceCompanyBySubscriptionType(subscriptionType: String): Set<OrganisationCode> {
        return db.transaction("get advance companies by subscriptionType") {
            return@transaction AdvanceCompanyTable.slice(AdvanceCompanyTable.code).select(
                (AdvanceCompanyTable.data.jsonString(JsonKey("subscriptionType")) eq subscriptionType)
                    and (AdvanceCompanyTable.data.jsonString(JsonKey("membershipStatus")) eq "ACTIVE")
            ).mapToSet {
                OrganisationCode(value = it[AdvanceCompanyTable.code])
            }
        }
    }

    override fun findByLocalNameStartingWith(name: String): Set<CompanyResponse> =
        db.transaction("get advance companies by local name starts with") {
            return@transaction AdvanceCompanyTable.select(
                AdvanceCompanyTable.data.jsonString(JsonKey("smdNameInLocalLanguage")) like "$name%"
            ).limit(maxCompanySearchResultCount).mapNotNull { get(OrganisationCode(it[AdvanceCompanyTable.code])) }.mapToSet { it }
        }

    override fun findByLocalName(name: String): Set<CompanyResponse> =
        db.transaction("get advance companies by local name") {
            return@transaction AdvanceCompanyTable.select(
                AdvanceCompanyTable.data.jsonString(JsonKey("smdNameInLocalLanguage")) eq name
            ).limit(maxCompanySearchResultCount).mapNotNull { get(OrganisationCode(it[AdvanceCompanyTable.code])) }.mapToSet { it }
        }
}

object AdvanceCompanyTable : Table("advance_company") {
    val code = text("code")
    val name = text("name").nullable()
    val data = jsonb<Map<String, Any>>("data")
    val updatedTime = timestamp("updated_time").nullable()
    val createdTime = timestamp("created_time")
}

object AdvanceCompanyAddressTable : Table("advance_company_address") {
    val code = text("code")
    val addressType = text("address_type")
    val addressLine1 = text("address_line_1").nullable()
    val addressLine2 = text("address_line_2").nullable()
    val city = text("city").nullable()
    val country = text("country").nullable()
    val postCode = text("post_code").nullable()
    val data = jsonb("data", JsonNode::class.java, CompanyJson.mapper)
    val updatedTime = timestamp("updated_time").nullable()
    val createdTime = timestamp("created_time")
}

private fun ResultRow.toAdvanceCompany(): AdvanceCompany {
    return AdvanceCompany(
        code = OrganisationCode(this[AdvanceCompanyTable.code]),
        createdTime = this[AdvanceCompanyTable.createdTime],
        updatedTime = this[AdvanceCompanyTable.updatedTime],
        data = defaultJsonMapper.convertValue(this[AdvanceCompanyTable.data], JsonNode::class.java)
    )
}

private fun ResultRow.toAdvanceCompanyAddress(): AdvanceCompanyAddress {
    return AdvanceCompanyAddress(
        code = OrganisationCode(this[AdvanceCompanyAddressTable.code]),
        addressType = AddressType.valueOf(this[AdvanceCompanyAddressTable.addressType]),
        data = this[AdvanceCompanyAddressTable.data]
    )
}
