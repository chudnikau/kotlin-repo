package com.sedex.connect.company.repository

import com.sedex.connect.advance.toEpochMilli
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.PaymentCode
import com.sedex.connect.company.fixtures.FakeAdvanceCompanySubscriptionsRepository
import com.sedex.connect.company.models.AdvanceSubscription
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

interface AdvanceCompanySubscriptionsRepositoryContract {
    val repository: AdvanceCompanySubscriptionsRepository

    @Test
    fun `should create and find subscription`() {
        repository.createOrUpdate(subscription)

        val result = repository.findByOrgCode(orgCode)

        assertEquals(1, result.size)
        assertEquals(subscription.orgCode, result[0].orgCode)
    }

    @Test
    fun `should return all subscriptions for company`() {
        repository.createOrUpdate(subscription)
        repository.createOrUpdate(
            subscription.copy(
                timestamp = Instant.ofEpochMilli(LocalDate.of(2023, 11, 1).toEpochMilli()),
                endDate = Instant.ofEpochMilli(LocalDate.of(2023, 11, 1).toEpochMilli())
            )
        )

        val result = repository.findByOrgCode(orgCode)

        assertEquals(2, result.size)
    }

    @Test
    fun `should update subscription with the same end date`() {
        repository.createOrUpdate(subscription)

        var dbSubscription = repository.findByOrgCode(orgCode)[0]

        val newTimestamp = Instant.ofEpochMilli(LocalDate.of(2024, 11, 1).toEpochMilli())

        repository.createOrUpdate(dbSubscription.copy(timestamp = newTimestamp))

        dbSubscription = repository.findByOrgCode(orgCode)[0]

        assertEquals(newTimestamp, dbSubscription.timestamp)
    }

    @Test
    fun `should not update subscription with the same end date`() {
        repository.createOrUpdate(subscription)

        var dbSubscription = repository.findByOrgCode(orgCode)[0]

        val newTimestamp = Instant.ofEpochMilli(LocalDate.of(2021, 11, 1).toEpochMilli())

        repository.createOrUpdate(dbSubscription.copy(timestamp = newTimestamp))

        dbSubscription = repository.findByOrgCode(orgCode)[0]

        assertEquals(subscription.timestamp, dbSubscription.timestamp)
    }
}

val orgCode = OrganisationCode("ZC123")
val paymentCode = PaymentCode("ZP123")

val subscription = AdvanceSubscription(
    orgCode = orgCode,
    paymentCode = paymentCode,
    nrOfSites = 1,
    requestedDurationInYears = 1,
    highTier = false,
    supplierPlusAvailableDate = Instant.ofEpochMilli(LocalDate.of(2023, 1, 1).toEpochMilli()),
    endDate = Instant.ofEpochMilli(LocalDate.of(2023, 1, 1).toEpochMilli()),
    timestamp = Instant.ofEpochMilli(LocalDate.of(2023, 1, 1).toEpochMilli())
)

class AdvanceCompanySubscriptionsRepositoryTest : AdvanceCompanySubscriptionsRepositoryContract, RepositoryTestBase() {
    override val repository: AdvanceCompanySubscriptionsRepository = AdvanceCompanySubscriptionsRepositoryImpl(db)
}

class FakeAdvanceCompanySubscriptionsRepositoryTest : AdvanceCompanySubscriptionsRepositoryContract,
    RepositoryTestBase() {
    override val repository: AdvanceCompanySubscriptionsRepository = FakeAdvanceCompanySubscriptionsRepository()
}
