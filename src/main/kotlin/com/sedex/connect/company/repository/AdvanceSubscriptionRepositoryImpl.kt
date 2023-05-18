package com.sedex.connect.company.repository

import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.database.ConnectedDatabase
import com.sedex.connect.company.models.AdvanceSubscription
import com.sedex.connect.company.table.AdvanceSubscriptionTable
import com.sedex.connect.company.table.find
import com.sedex.connect.company.table.insert
import com.sedex.connect.company.table.update

class AdvanceSubscriptionRepositoryImpl(
    private val db: ConnectedDatabase,
) : AdvanceSubscriptionRepository {

    override fun createOrUpdate(subscription: AdvanceSubscription) =
        db.transaction("Create subscription for company: ${subscription.orgCode.value}") {
            if (exists(subscription.orgCode)) {
                AdvanceSubscriptionTable.update(subscription)
            } else {
                AdvanceSubscriptionTable.insert(subscription)
            }
        }

    override fun exists(code: OrganisationCode): Boolean {
        return db.transaction("Subscription for org code $code exists") {
            AdvanceSubscriptionTable.find(code) != null
        }
    }

    override fun findByOrgCode(code: OrganisationCode): AdvanceSubscription? {
        return db.transaction("Get subscription for company: ${code.value}") {
            AdvanceSubscriptionTable.find(code)
        }
    }
}
