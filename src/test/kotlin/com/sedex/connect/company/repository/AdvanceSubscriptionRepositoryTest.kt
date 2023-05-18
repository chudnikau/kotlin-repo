package com.sedex.connect.company.repository

import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.company.api.exampleSubscriptionRequest
import com.sedex.connect.company.fixtures.FakeAdvanceSubscriptionRepository
import com.sedex.connect.company.models.AdvanceSubscription
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

interface AdvanceSubscriptionRepositoryContract {
    val subscriptionRepository: AdvanceSubscriptionRepository

    @Test
    fun `should create advance subscription data`() {
        val testSubscription = testSubscription()
        subscriptionRepository.createOrUpdate(testSubscription)
        assertTrue(subscriptionRepository.exists(testSubscription.orgCode))
    }

    @Test
    fun `should return false for exists subscription data by org code`() {
        val getSubscription = subscriptionRepository.exists(OrganisationCode("XXXXXX"))
        assertFalse(getSubscription)
    }

    private fun testSubscription(): AdvanceSubscription {
        return AdvanceSubscription(exampleSubscriptionRequest)
    }
}

internal class AdvanceSubscriptionRepositoryImplTest : AdvanceSubscriptionRepositoryContract, RepositoryTestBase() {
    override val subscriptionRepository = AdvanceSubscriptionRepositoryImpl(db)
}

internal class FakeAdvanceSubscriptionRepositoryTest : AdvanceSubscriptionRepositoryContract {
    override val subscriptionRepository = FakeAdvanceSubscriptionRepository()
}
