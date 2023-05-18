package com.sedex.connect.company.repository

import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.database.ConnectedDatabase
import com.sedex.connect.company.models.AdvanceSubscription
import com.sedex.connect.company.table.AdvanceCompanySubscriptionsTable
import com.sedex.connect.company.table.find
import com.sedex.connect.company.table.findByCodeAndEndDate
import com.sedex.connect.company.table.insert
import com.sedex.connect.company.table.update

class AdvanceCompanySubscriptionsRepositoryImpl(
    private val db: ConnectedDatabase,
) : AdvanceCompanySubscriptionsRepository {
    override fun createOrUpdate(subscription: AdvanceSubscription) {
        db.transaction("Create or update subscription for company: ${subscription.orgCode.value}") {
            val current = subscription.endDate?.let {
                AdvanceCompanySubscriptionsTable.findByCodeAndEndDate(subscription.orgCode, subscription.endDate)
            }
            if (current == null) {
                AdvanceCompanySubscriptionsTable.insert(subscription)
            } else if (needToUpdate(current, subscription)) {
                AdvanceCompanySubscriptionsTable.update(subscription.copy(id = current.id))
            }
        }
    }

    override fun findByOrgCode(code: OrganisationCode): List<AdvanceSubscription> {
        return db.transaction("Find subscriptions for company: ${code.value}") {
            AdvanceCompanySubscriptionsTable.find(code)
        }
    }
}
