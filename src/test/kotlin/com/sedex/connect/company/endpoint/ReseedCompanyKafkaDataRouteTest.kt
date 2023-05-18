package com.sedex.connect.company.endpoint

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.auth.OrganisationAdmin
import com.sedex.connect.common.auth.SedexAdmin
import com.sedex.connect.company.CompanyTestCase
import com.sedex.connect.company.models.PersistedCompany
import com.sedex.connect.company.persistedCompanyExample
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

class ReseedCompanyKafkaDataRouteTest : CompanyTestCase() {

    @Test
    fun `reseed company data`() {
        (1..5).forEach {
            companyRepository.create(PersistedCompany(OrganisationCode("ZC$it"), persistedCompanyExample))
        }

        val request = Request(POST, "/companies/reseed/all")
        val response = authorizeUser(roles = setOf(SedexAdmin)).handle(request)

        assertThat(response, hasStatus(Status.OK))
        assertThat(response, hasBody("5"))

        assertThat(companyMessages.map { it.key() }, equalTo(listOf("ZC1", "ZC2", "ZC3", "ZC4", "ZC5")))
    }

    @Test
    fun `reseed company data - restricted with admin auth`() {
        val request = Request(POST, "/companies/reseed/all")
        val response = authorizeUser(roles = setOf(OrganisationAdmin(OrganisationCode("ZC1")))).handle(request)

        assertThat(response, hasStatus(Status.FORBIDDEN))
    }
}
