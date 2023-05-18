package com.sedex.connect.company.endpoint

import com.sedex.connect.common.http.ContractRoutes
import org.http4k.contract.meta
import org.http4k.contract.security.NoSecurity
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK

object PingPongRoute : ContractRoutes {

    private val spec = "/ping" meta {
        summary = "Temporary endpoint"
        description = summary
        security = NoSecurity
        returning(OK)
    }

    private val handler: HttpHandler = {
        Response(OK).body("PONG")
    }

    override fun contractRoutes() = listOf(
        spec bindContract GET to handler
    )
}
