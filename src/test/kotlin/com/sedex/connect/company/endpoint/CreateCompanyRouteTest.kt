package com.sedex.connect.company.endpoint

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.company.CompanyTestCase
import com.sedex.connect.company.api.CompanyJson
import com.sedex.connect.company.endpoint.CreateCompanyRoute.Companion.exampleCompany
import com.sedex.connect.company.models.PersistedCompany
import com.sedex.connect.company.persistedCompanyExample
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CONFLICT
import org.http4k.core.Status.Companion.CREATED
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class CreateCompanyRouteTest : CompanyTestCase() {

    private val anOrgCode = OrganisationCode("ZC1234")

    private val aPersistedCompany = PersistedCompany(anOrgCode, persistedCompanyExample)

    @Test
    fun `returns CREATED`() {
        val expectedCompany = aPersistedCompany.copy(
            membershipStatus = null,
            subscriptionType = null
        )
        val response = app.invoke(Request(POST, "/companies/$anOrgCode").body(exampleCompany.json()))

        assertThat(response.status, equalTo(CREATED))
        assertThat(get(anOrgCode), equalTo(expectedCompany))

        assertThat(companyMessages.size, equalTo(1))
    }

    @Test
    fun `returns CONFLICT if company already exists`() {
        companyRepository.create(aPersistedCompany)

        val response = app.invoke(Request(POST, "/companies/$anOrgCode").body(exampleCompany.json()))

        assertThat(response.status, equalTo(CONFLICT))
        assertThat(get(anOrgCode), equalTo(aPersistedCompany))
    }

    @Test
    fun `returns BAD_REQUEST if payload fails validation`() {
        val invalidCompany = InvalidCompany(code = anOrgCode)

        val response = app.invoke(Request(POST, "/companies/$anOrgCode").body(invalidCompany.json()))

        assertThat(response.status, equalTo(BAD_REQUEST))
        assertFalse(exists(anOrgCode))
    }
}

internal fun Any.json() = CompanyJson.mapper.writeValueAsString(this)
internal data class InvalidCompany(val code: OrganisationCode) {
    fun json(): String = CompanyJson.mapper.writeValueAsString(this)
}
