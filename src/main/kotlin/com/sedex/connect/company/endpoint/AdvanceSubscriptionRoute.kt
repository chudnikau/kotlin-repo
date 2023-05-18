package com.sedex.connect.company.endpoint

import com.sedex.connect.common.http.ContractRoutes
import com.sedex.connect.common.http.withError
import com.sedex.connect.company.api.exampleSubscriptionRequest
import com.sedex.connect.company.api.subscriptionRequestDataLens
import com.sedex.connect.company.models.AdvanceSubscription
import com.sedex.connect.company.services.AdvanceSubscriptionService
import com.sedex.connect.security.SecurityContext
import org.http4k.contract.ContractRoute
import org.http4k.contract.meta
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status

internal const val subscriptionPath: String = "/subscription"

class AdvanceSubscriptionRoute(
    private val subscriptionService: AdvanceSubscriptionService,
    private val securityContext: SecurityContext,
) : ContractRoutes {

    private val createAdvanceSubscriptionSpec = subscriptionPath meta {
        summary = "Create or Update Advance Subscription data"
        description = summary
        security = securityContext.requiresServiceToken()
        receiving(subscriptionRequestDataLens to exampleSubscriptionRequest)
        returning(Status.OK)
    }
    private val createAdvanceCompanySubscriptionsSpec = "/company-subscriptions" meta {
        summary = "Create or Update Advance Subscription data"
        description = summary
        security = securityContext.requiresServiceToken()
        receiving(subscriptionRequestDataLens to exampleSubscriptionRequest)
        returning(Status.OK)
    }

    override fun contractRoutes(): List<ContractRoute> = listOf(
        createAdvanceSubscriptionSpec bindContract Method.PUT to { request ->
            val subscriptionData = AdvanceSubscription(subscriptionRequestDataLens(request))
            runCatching {
                subscriptionService.createOrUpdate(subscriptionData)
                Response(Status.OK)
            }.getOrElse { ex -> Response(Status.INTERNAL_SERVER_ERROR).withError(ex) }
        },

        createAdvanceCompanySubscriptionsSpec bindContract Method.PUT to { request ->
            val subscriptionData = AdvanceSubscription(subscriptionRequestDataLens(request))
            runCatching {
                subscriptionService.createNew(subscriptionData)
                Response(Status.OK)
            }.getOrElse { ex -> Response(Status.INTERNAL_SERVER_ERROR).withError(ex) }
        }
    )
}
