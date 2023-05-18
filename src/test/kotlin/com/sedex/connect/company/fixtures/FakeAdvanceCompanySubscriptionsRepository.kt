package com.sedex.connect.company.fixtures

import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.company.models.AdvanceSubscription
import com.sedex.connect.company.repository.AdvanceCompanySubscriptionsRepository
import java.time.Instant
import java.util.UUID

class FakeAdvanceCompanySubscriptionsRepository : AdvanceCompanySubscriptionsRepository {
    private val storage: MutableMap<UUID, AdvanceSubscription> = mutableMapOf()

    override fun createOrUpdate(subscription: AdvanceSubscription) {
        val current = subscription.endDate?.let { findByOrgCodeAndEndDate(subscription.orgCode, it) }
        if (current == null) {
            val id = UUID.randomUUID()
            storage[id] = subscription.copy(id = id)
        } else if (needToUpdate(current, subscription)) {
            storage[subscription.id!!] = subscription
        }
    }

    override fun findByOrgCode(code: OrganisationCode): List<AdvanceSubscription> =
        storage.values.filter { it.orgCode == code }

    private fun findByOrgCodeAndEndDate(code: OrganisationCode, endDate: Instant): AdvanceSubscription? {
        return storage.values.find { it.orgCode == code && it.endDate == endDate }
    }

    fun clear() = storage.clear()
}
