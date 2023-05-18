package com.sedex.connect.company.table

import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.PaymentCode
import com.sedex.connect.company.models.AdvanceSubscription
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.`java-time`.timestamp
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.update
import java.time.Instant

object AdvanceCompanySubscriptionsTable : Table("advance_company_subscriptions") {
    val id = uuid("id")
    val orgCode = text("org_code")
    val paymentCode = text("payment_code").nullable()
    val nrOfSites = integer("nr_of_sites").nullable()
    val requestedDurationInYears = integer("requested_duration_in_years").nullable()
    val highTier = bool("high_tier").nullable()
    val endDate = timestamp("end_date").nullable()
    val supplierPlusAvailableDate = timestamp("supplier_plus_available_date").nullable()
    val createdTime = timestamp("created_time")
    val updatedTime = timestamp("updated_time")
    val timestamp = timestamp("timestamp").nullable()
}

fun AdvanceCompanySubscriptionsTable.insert(subscriptionData: AdvanceSubscription) {
    val insertTime = Instant.now()
    insert {
        mapSubscriptionData(it, subscriptionData)
        it[createdTime] = insertTime
        it[updatedTime] = insertTime
    }
}

fun AdvanceCompanySubscriptionsTable.update(subscriptionData: AdvanceSubscription) {
    update({ id eq subscriptionData.id!! }) {
        mapSubscriptionData(it, subscriptionData)
        it[updatedTime] = Instant.now()
    }
}

private fun AdvanceCompanySubscriptionsTable.mapSubscriptionData(
    it: UpdateBuilder<Int>,
    subscriptionData: AdvanceSubscription,
) {
    it[orgCode] = subscriptionData.orgCode.value
    it[paymentCode] = subscriptionData.paymentCode?.value
    it[nrOfSites] = subscriptionData.nrOfSites
    it[highTier] = subscriptionData.highTier
    it[requestedDurationInYears] = subscriptionData.requestedDurationInYears
    it[supplierPlusAvailableDate] = subscriptionData.supplierPlusAvailableDate
    it[endDate] = subscriptionData.endDate
    it[timestamp] = subscriptionData.timestamp
}

fun AdvanceCompanySubscriptionsTable.find(code: OrganisationCode): List<AdvanceSubscription> {
    return select(orgCode eq code.value)
        .map { it.mapRow() }
}

fun AdvanceCompanySubscriptionsTable.findByCodeAndEndDate(
    code: OrganisationCode,
    endDateInstant: Instant,
): AdvanceSubscription? {
    return select(orgCode eq code.value and (endDate eq endDateInstant))
        .map { it.mapRow() }
        .firstOrNull()
}

private fun ResultRow.mapRow() = AdvanceSubscription(
    id = this[AdvanceCompanySubscriptionsTable.id],
    orgCode = OrganisationCode(this[AdvanceCompanySubscriptionsTable.orgCode]),
    paymentCode = this[AdvanceCompanySubscriptionsTable.paymentCode]?.let { PaymentCode(it) },
    nrOfSites = this[AdvanceCompanySubscriptionsTable.nrOfSites],
    highTier = this[AdvanceCompanySubscriptionsTable.highTier],
    requestedDurationInYears = this[AdvanceCompanySubscriptionsTable.requestedDurationInYears],
    supplierPlusAvailableDate = this[AdvanceCompanySubscriptionsTable.supplierPlusAvailableDate],
    endDate = this[AdvanceCompanySubscriptionsTable.endDate],
    timestamp = this[AdvanceCompanySubscriptionsTable.timestamp]
)
