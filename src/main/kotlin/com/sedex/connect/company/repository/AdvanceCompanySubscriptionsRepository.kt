package com.sedex.connect.company.repository

import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.company.models.AdvanceSubscription

interface AdvanceCompanySubscriptionsRepository {
    fun createOrUpdate(subscription: AdvanceSubscription)

    fun findByOrgCode(code: OrganisationCode): List<AdvanceSubscription>

    fun needToUpdate(current: AdvanceSubscription, update: AdvanceSubscription): Boolean {
        return update.timestamp != null && current.timestamp != null && update.timestamp > current.timestamp
    }
}
