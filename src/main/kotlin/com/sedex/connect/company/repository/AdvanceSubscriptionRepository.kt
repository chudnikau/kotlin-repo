package com.sedex.connect.company.repository

import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.company.models.AdvanceSubscription

interface AdvanceSubscriptionRepository {

    fun createOrUpdate(subscription: AdvanceSubscription)

    fun exists(code: OrganisationCode): Boolean

    fun findByOrgCode(code: OrganisationCode): AdvanceSubscription?
}
