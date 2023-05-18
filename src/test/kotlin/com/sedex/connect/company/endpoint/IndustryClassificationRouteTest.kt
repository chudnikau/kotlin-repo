package com.sedex.connect.company.endpoint

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.sedex.connect.company.CompanyTestCase
import com.sedex.connect.company.endpoint.IndustryClassificationRoute.Companion.industryClassificationLens
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IndustryClassificationRouteTest : CompanyTestCase() {

    @Test
    fun `returns industry classifications`() {
        val response = app.invoke(Request(GET, "/industry-classifications"))

        val industryClassifications = industryClassificationLens(response)

        assertThat(response.status, equalTo(OK))
        assertThat(
            industryClassifications.map { it.name },
            equalTo(
                listOf(
                    "A - Agriculture, forestry and fishing",
                    "B - Mining and quarrying",
                    "C - Manufacturing",
                    "D - Electricity, gas, steam and air conditioning supply",
                    "E - Water supply; sewerage, waste management and remediation activities",
                    "F - Construction",
                    "G - Wholesale and retail trade; repair of motor vehicles and motorcycles",
                    "H - Transportation and storage",
                    "I - Accommodation and food service activities",
                    "J - Information and communication",
                    "K - Financial and insurance activities",
                    "L - Real estate activities",
                    "M - Professional, scientific and technical activities",
                    "N - Administrative and support service activities",
                    "O - Public administration and defence; compulsory social security",
                    "P - Education",
                    "Q - Human health and social work activities",
                    "R - Arts, entertainment and recreation",
                    "S - Other service activities",
                    "T - Activities of households as employers; undifferentiated goods- and services-producing activities of households for own use",
                    "U - Activities of extraterritorial organizations and bodies",
                )
            )
        )
        assertTrue(industryClassifications.all { it.children.isNotEmpty() })
    }

    @Test
    fun `returns industry classifications filtered by depth`() {
        val response = app.invoke(Request(GET, "/industry-classifications").query("depth", "0"))

        val industryClassifications = industryClassificationLens(response)

        assertThat(response.status, equalTo(OK))
        assertTrue(industryClassifications.all { it.children.isEmpty() })
    }
}
