package com.sedex.connect.company.endpoint

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.sedex.connect.company.CompanyTestCase
import com.sedex.connect.company.api.exampleSubscriptionRequest
import com.sedex.connect.company.api.subscriptionRequestDataLens
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class AdvanceSubscriptionRouteTest : CompanyTestCase() {

    @Test
    fun `successfully creates an advance subscription`() {
        assertThat(
            app.invoke(
                Request(PUT, "/subscription")
                    .with(subscriptionRequestDataLens of exampleSubscriptionRequest)
            ),
            has(Response::status, equalTo(Status.OK))
        )
    }

    @Test
    fun `successfully creates an advance company subscription`() {
        assertThat(
            app.invoke(
                Request(PUT, "/company-subscriptions")
                    .with(subscriptionRequestDataLens of exampleSubscriptionRequest)
            ),
            has(Response::status, equalTo(Status.OK))
        )

        assertTrue(advanceCompanySubscriptionsRepository.findByOrgCode(exampleSubscriptionRequest.orgCode).isNotEmpty())
    }
}
