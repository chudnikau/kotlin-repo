package com.sedex.connect.company.endpoint

import com.sedex.connect.common.auth.OrganisationAdmin
import com.sedex.connect.common.auth.SedexAdmin
import com.sedex.connect.common.auth.UserCode
import com.sedex.connect.common.randomOrganisationCode
import com.sedex.connect.company.CompanyTestCase
import com.sedex.connect.company.models.PersistedCompany
import com.sedex.connect.company.persistedCompanyExample
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.testing.Approver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GetCompanySummaryRouteTest : CompanyTestCase() {
    private val userCode = UserCode("ZU1234")
    private val orgCode = randomOrganisationCode()
    private val persistedCompany = PersistedCompany(orgCode, persistedCompanyExample)

    @BeforeEach
    override fun beforeEach() {
        super.beforeEach()
        userService.addUser(userCode = userCode, organisationCode = orgCode)
        userService.addRoles(userCode, OrganisationAdmin(orgCode))
    }

    @Test
    fun `should get company self summary for user with orgCode with 200 response`(approver: Approver) {
        companyRepository.create(persistedCompany)
        createSubscription(orgCode)

        val request = Request(GET, "/companies/self/summary")
        val response = authorizeUser(userCode)
            .handle(request)

        assertEquals(Status.OK, response.status)

        approver.assertApproved(response)
    }

    @Test
    fun `should get company self summary for user without orgCode with 200 response`(approver: Approver) {
        val userWithoutOrgCode = UserCode("ZU0000")
        userService.addUser(userCode = userWithoutOrgCode, organisationCode = null)
        userService.addRoles(userWithoutOrgCode, SedexAdmin)

        val request = Request(GET, "/companies/self/summary")
        val response = authorizeUser(userWithoutOrgCode)
            .handle(request)

        assertEquals(Status.OK, response.status)

        approver.assertApproved(response)
    }

    @Test
    fun `should get company self summary with 401 response`() {
        companyRepository.create(persistedCompany)
        createSubscription(orgCode)

        val request = Request(GET, "/companies/self/summary")
        val response = app.invoke(request)

        assertEquals(Status.UNAUTHORIZED, response.status)
    }
}
