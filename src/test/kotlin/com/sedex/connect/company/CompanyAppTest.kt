package com.sedex.connect.company

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.contains
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test

class CompanyAppTest : CompanyTestCase() {

    @Test
    fun `returns open API spec`(approver: Approver) {
        val response = app(Request(GET, "/api"))

        approver.assertApproved(response, OK)
    }

    @Test
    fun `returns health`() {
        val response = app(Request(GET, "/health"))

        assertThat(
            response,
            hasStatus(OK)
                .and(hasBody(contains(Regex("Health Status: OK"))))
        )
    }

    @Test
    fun `returns metrics`() {
        val response = app(Request(GET, "/metrics"))

        assertThat(response, hasStatus(OK))
    }
}
