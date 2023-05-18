package com.sedex.connect.company.endpoint

import com.fasterxml.jackson.module.kotlin.readValue
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.auth.OrganisationAdmin
import com.sedex.connect.common.auth.UserCode
import com.sedex.connect.company.CompanyTestCase
import com.sedex.connect.company.api.CompanyJson
import com.sedex.connect.company.api.CompanyResponse
import com.sedex.connect.company.api.CompanySize.SMALL
import com.sedex.connect.company.api.UpdateCompanySizeRequest
import com.sedex.connect.company.models.PersistedCompany
import com.sedex.connect.company.persistedCompanyExample
import org.http4k.core.Method.PATCH
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class UpdateCompanySizeRouteTest : CompanyTestCase() {

    private val orgCode = OrganisationCode("ZC12345")
    private val company = PersistedCompany(orgCode, persistedCompanyExample)
    private val userCode = UserCode("ZU1234")

    @BeforeEach
    override fun beforeEach() {
        super.beforeEach()
        userService.addUser(userCode = userCode, organisationCode = orgCode)
        userService.addRoles(userCode, OrganisationAdmin(orgCode))
        advanceAdapter.willRespond(orgCode, Response(Status.OK))
    }

    @Test
    fun `should return 404 for not found company`() {
        val updateCompanySizeRequest = UpdateCompanySizeRequest(companySize = SMALL)
        val response = authorizeUser(userCode)
            .handle(
                Request(PATCH, "/companies/$orgCode/size")
                    .body(updateCompanySizeRequest.json())
            )

        assertEquals(response.status, Status.NOT_FOUND)
    }

    @Test
    fun `should return OK for update company size request`() {
        companyRepository.create(company)
        val updateCompanySizeRequest = UpdateCompanySizeRequest(companySize = SMALL)
        val response = authorizeUser(userCode)
            .handle(
                Request(PATCH, "/companies/$orgCode/size")
                    .body(updateCompanySizeRequest.json())
            )

        val companyResponse = CompanyJson.mapper.readValue<CompanyResponse>(response.body.payload.array())
        assertEquals(response.status, Status.OK)
        assertEquals(companyResponse.companySize, SMALL)
    }
}
