package com.sedex.connect.company.services

import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.company.api.exampleSubscriptionRequest
import com.sedex.connect.company.models.AdvanceSubscription
import com.sedex.connect.company.repository.AdvanceCompanySubscriptionsRepository
import com.sedex.connect.company.repository.AdvanceSubscriptionRepository
import com.sedex.connect.user.api.Feature
import com.sedex.connect.user.api.FeatureResource
import com.sedex.connect.user.api.UserApi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

internal class AdvanceSubscriptionServiceTest {
    private val subscriptionRepository: AdvanceSubscriptionRepository = mockk()
    private val companySubscriptionsRepository: AdvanceCompanySubscriptionsRepository = mockk()
    private val userApi: UserApi = mockk {
        every { getFeatures() } returns listOf(
            FeatureResource(
                id = UUID.randomUUID(),
                name = Feature.UseNewCompanySubscription.name,
                description = "",
                default = false,
                createdTime = "",
                updatedTime = null
            )
        )
    }
    private val subscriptionService =
        AdvanceSubscriptionService(subscriptionRepository, companySubscriptionsRepository, userApi)
    private val testSubscription = AdvanceSubscription(exampleSubscriptionRequest)

    @Test
    fun `should createOrUpdate advance subscription`() {
        every { subscriptionRepository.createOrUpdate(any()) } returns Unit
        subscriptionService.createOrUpdate(testSubscription)

        verify(exactly = 1) {
            subscriptionRepository.createOrUpdate(any())
        }
    }

    @Test
    fun `should return subscription by org code`() {
        val orgCode = OrganisationCode("ZC123")
        val mockedSubscription = mockk<AdvanceSubscription>()

        every { subscriptionRepository.findByOrgCode(orgCode) } returns mockedSubscription

        val response = subscriptionService.findByOrgCode(orgCode)

        assertEquals(mockedSubscription, response)

        verify(exactly = 1) { subscriptionRepository.findByOrgCode(orgCode) }
    }

    @Test
    fun `should call createNew`() {
        every { companySubscriptionsRepository.createOrUpdate(any()) } returns Unit
        subscriptionService.createNew(testSubscription)

        verify(exactly = 1) {
            companySubscriptionsRepository.createOrUpdate(testSubscription)
        }
    }

    @Test
    fun `should return latest subscription from new table when UseNewCompanySubscription is true`() {
        val orgCode = OrganisationCode("ZC123")
        val mockedOldSubscription = AdvanceSubscription(exampleSubscriptionRequest)
        val mockedNewSubscription =
            AdvanceSubscription(exampleSubscriptionRequest).copy(timestamp = Instant.ofEpochMilli(1689034600000))

        every { subscriptionRepository.findByOrgCode(orgCode) } returns mockedOldSubscription
        every { companySubscriptionsRepository.findByOrgCode(orgCode) } returns listOf(
            mockedOldSubscription,
            mockedNewSubscription
        )
        every { userApi.getFeatures() } returns listOf(
            FeatureResource(
                id = UUID.randomUUID(),
                name = Feature.UseNewCompanySubscription.name,
                description = "",
                default = true,
                createdTime = "",
                updatedTime = null
            )
        )

        val response = subscriptionService.findByOrgCode(orgCode)
        assertEquals(mockedNewSubscription, response)
    }
}
