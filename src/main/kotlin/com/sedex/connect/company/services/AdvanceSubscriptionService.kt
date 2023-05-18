package com.sedex.connect.company.services

import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.company.models.AdvanceSubscription
import com.sedex.connect.company.repository.AdvanceCompanySubscriptionsRepository
import com.sedex.connect.company.repository.AdvanceSubscriptionRepository
import com.sedex.connect.user.api.Feature
import com.sedex.connect.user.api.UserApi

class AdvanceSubscriptionService(
    private val advanceSubscriptionRepository: AdvanceSubscriptionRepository,
    private val advanceCompanySubscriptionsRepository: AdvanceCompanySubscriptionsRepository,
    private val userApi: UserApi,
) {

    fun createOrUpdate(subscriptionData: AdvanceSubscription) =
        advanceSubscriptionRepository.createOrUpdate(subscriptionData)

    fun findByOrgCode(orgCode: OrganisationCode): AdvanceSubscription? {
        val feature = userApi.getFeatures().firstOrNull { it.name == Feature.UseNewCompanySubscription.name }
        return if (feature != null && feature.default) {
            advanceCompanySubscriptionsRepository.findByOrgCode(orgCode)
                .filter { it.timestamp != null }
                .maxByOrNull { it.timestamp!! }
        } else {
            advanceSubscriptionRepository.findByOrgCode(orgCode)
        }
    }

    fun createNew(subscriptionData: AdvanceSubscription) =
        advanceCompanySubscriptionsRepository.createOrUpdate(subscriptionData)
}
