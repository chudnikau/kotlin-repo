package com.sedex.connect.company.models

import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.PaymentCode
import com.sedex.connect.company.api.AdvanceSubscriptionRequest
import com.sedex.connect.lang.nullIfBlank
import java.time.Instant
import java.util.UUID

data class AdvanceSubscription(
    val id: UUID? = null,
    val orgCode: OrganisationCode,
    val paymentCode: PaymentCode?,
    val nrOfSites: Int?,
    val requestedDurationInYears: Int?,
    val highTier: Boolean?,
    val endDate: Instant?,
    val supplierPlusAvailableDate: Instant?,
    val timestamp: Instant?,
) {
    constructor(request: AdvanceSubscriptionRequest) : this(
        orgCode = request.orgCode,
        paymentCode = request.paymentCode?.nullIfBlank()?.let { PaymentCode(it) },
        nrOfSites = request.nrOfSites,
        requestedDurationInYears = request.requestedDurationInYears,
        highTier = request.highTier,
        endDate = request.endDate?.let { Instant.ofEpochMilli(it) },
        supplierPlusAvailableDate = request.supplierPlusAvailableDate?.let { Instant.ofEpochMilli(it) },
        timestamp = request.timestamp?.let { Instant.ofEpochMilli(it) }
    )
}
