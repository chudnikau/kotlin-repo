package com.sedex.connect.company.endpoint

import com.natpryce.hamkrest.assertion.assertThat
import com.sedex.connect.advancecache.advanceCode
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.SubscriptionType
import com.sedex.connect.common.auth.OrganisationAdmin
import com.sedex.connect.common.auth.UserCode
import com.sedex.connect.company.CompanyTestCase
import com.sedex.connect.company.api.MembershipStatus.NEW
import com.sedex.connect.company.api.UpdateCompanySubscriptionRequest
import com.sedex.connect.company.models.PersistedCompany
import com.sedex.connect.company.persistedCompanyExample
import com.sedex.connect.security.impersonate
import com.sedex.connect.security.orgAdminAuth
import com.sedex.connect.security.sedexAdminAuth
import com.sedex.connect.security.withAuthorisationToken
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.hamkrest.hasStatus
import org.http4k.testing.Approver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class UpdateCompanySubscriptionRouteTest : CompanyTestCase() {

    private val orgCode = OrganisationCode("ZC12345")
    private val company = PersistedCompany(orgCode, persistedCompanyExample)
    private val userCode = UserCode("ZU123")

    @BeforeEach
    override fun beforeEach() {
        super.beforeEach()
        userService.addUser(userCode = userCode, organisationCode = orgCode)
        userService.addRoles(userCode, OrganisationAdmin(orgCode))
        advanceAdapter.willRespond(orgCode, Response(Status.OK))
    }

    @Test
    fun `should return 403 for supplier role`() {
        companyRepository.create(company)

        val updateCompanySubscriptionRequest = UpdateCompanySubscriptionRequest(
            subscriptionType = SubscriptionType.BuyerSupplier.advanceCode(),
            membershipStatus = NEW
        )

        val request = Request(PUT, "/companies/$orgCode/subscription")
            .body(updateCompanySubscriptionRequest.json())

        val response = authorizeUser(userCode)
            .handle(request)

        assertEquals(Status.FORBIDDEN, response.status)
    }

    @Test
    fun `should return FORBIDDEN for impersonated supplier`(approver: Approver) {
        companyRepository.create(company)
        val updateCompanySubscriptionRequest = UpdateCompanySubscriptionRequest(
            subscriptionType = SubscriptionType.BuyerSupplier.advanceCode(),
            membershipStatus = NEW
        )
        val user = sedexAdminAuth(UserCode("ZU999"))
            .impersonate(orgAdminAuth(userCode, orgCode))
        val request = Request(PUT, "/companies/$orgCode/subscription")
            .withAuthorisationToken(user)
            .body(updateCompanySubscriptionRequest.json())

        val response = app(request)

        assertThat(response, hasStatus(Status.FORBIDDEN))
    }
}
