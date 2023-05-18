package com.sedex.connect.company.fixtures

import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.company.models.AdvanceSubscription
import com.sedex.connect.company.repository.AdvanceSubscriptionRepository

class FakeAdvanceSubscriptionRepository : AdvanceSubscriptionRepository {
    private val subscription = mutableMapOf<OrganisationCode, AdvanceSubscription>()

    override fun createOrUpdate(subscription: AdvanceSubscription) {
        this.subscription[subscription.orgCode] = subscription
    }

    override fun exists(code: OrganisationCode): Boolean {
        return subscription[code] != null
    }

    override fun findByOrgCode(code: OrganisationCode): AdvanceSubscription? {
        return subscription[code]
    }

    fun clear() {
        subscription.clear()
    }
}
