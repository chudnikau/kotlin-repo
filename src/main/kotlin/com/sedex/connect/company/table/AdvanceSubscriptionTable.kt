package com.sedex.connect.company.table

import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.PaymentCode
import com.sedex.connect.company.models.AdvanceSubscription
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.`java-time`.timestamp
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.update
import java.time.Instant

object AdvanceSubscriptionTable : Table("advance_subscription") {
    val orgCode = text("org_code")
    val paymentCode = text("payment_code").nullable()
    val nrOfSites = integer("nr_of_sites").nullable()
    val requestedDurationInYears = integer("requested_duration_in_years").nullable()
    val highTier = bool("high_tier").nullable()
    val endDate = timestamp("end_date").nullable()
    val supplierPlusAvailableDate = timestamp("supplier_plus_available_date").nullable()
    val createdTime = timestamp("created_time")
    val updatedTime = timestamp("updated_time")
}

fun AdvanceSubscriptionTable.insert(subscriptionData: AdvanceSubscription) {
    val insertTime = Instant.now()
    insert {
        mapSubscriptionData(it, subscriptionData)
        it[createdTime] = insertTime
        it[updatedTime] = insertTime
    }
}

fun AdvanceSubscriptionTable.update(subscriptionData: AdvanceSubscription) {
    update({ orgCode eq subscriptionData.orgCode.value }) {
        mapSubscriptionData(it, subscriptionData)
        it[updatedTime] = Instant.now()
    }
}

private fun AdvanceSubscriptionTable.mapSubscriptionData(it: UpdateBuilder<Int>, subscriptionData: AdvanceSubscription) {
    it[orgCode] = subscriptionData.orgCode.value
    it[paymentCode] = subscriptionData.paymentCode?.value
    it[nrOfSites] = subscriptionData.nrOfSites
    it[highTier] = subscriptionData.highTier
    it[requestedDurationInYears] = subscriptionData.requestedDurationInYears
    it[supplierPlusAvailableDate] = subscriptionData.supplierPlusAvailableDate
    it[endDate] = subscriptionData.endDate
}

fun AdvanceSubscriptionTable.delete(code: OrganisationCode) {
    deleteWhere { orgCode.eq(code.value) }
}

fun AdvanceSubscriptionTable.find(code: OrganisationCode): AdvanceSubscription? {
    return select(orgCode eq code.value)
        .singleOrNull()
        ?.mapRow(this)
}

fun ResultRow.mapRow(table: AdvanceSubscriptionTable) = AdvanceSubscription(
    orgCode = OrganisationCode(this[table.orgCode]),
    paymentCode = this[table.paymentCode]?.let { PaymentCode(it) },
    nrOfSites = this[table.nrOfSites],
    highTier = this[table.highTier],
    requestedDurationInYears = this[table.requestedDurationInYears],
    supplierPlusAvailableDate = this[table.supplierPlusAvailableDate],
    endDate = this[table.endDate],
    timestamp = null
)
