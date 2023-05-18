package com.sedex.connect.company.endpoint

import com.natpryce.hamkrest.assertion.assertThat
import com.sedex.connect.company.CompanyTestCase
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.hamkrest.hasBody
import org.junit.jupiter.api.Test

class PingPongRouteTest : CompanyTestCase() {

    @Test
    fun `returns pong`() {
        val response = app(Request(GET, "/ping"))
        assertThat(response, hasBody("PONG"))
    }
}
