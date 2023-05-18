package com.sedex.connect.company.endpoint

import com.natpryce.hamkrest.assertion.assertThat
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.auth.UserCode
import com.sedex.connect.company.CompanyTestCase
import com.sedex.connect.company.api.CompanySubsidiaryRequest
import com.sedex.connect.company.api.companySubsidiaryRequestLens
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.hamkrest.hasStatus
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class CompanySubsidiaryRouteTest : CompanyTestCase() {

    private val parentOrgCode = OrganisationCode("ZC0001")
    private val childOrgCode = OrganisationCode("ZC0002")

    @Test
    fun `should create company subsidiary`() {
        val request = Request(POST, "/subsidiary")
            .with(companySubsidiaryRequestLens of CompanySubsidiaryRequest(parentOrgCode, childOrgCode))

        val response = app(request)

        assertEquals(Status.CREATED, response.status)

        assertTrue(advanceCompanySubsidiaryRepository.exists(parentOrgCode, childOrgCode))
    }

    @Test
    fun `should delete company subsidiary`() {
        advanceCompanySubsidiaryRepository.create(parentOrgCode, childOrgCode)

        val request = Request(DELETE, "/subsidiary")
            .with(companySubsidiaryRequestLens of CompanySubsidiaryRequest(parentOrgCode, childOrgCode))

        val response = app(request)

        assertEquals(Status.OK, response.status)

        assertFalse(advanceCompanySubsidiaryRepository.exists(parentOrgCode, childOrgCode))
    }

    @Test
    fun `should get company subsidiary by parentOrgCode`(approver: Approver) {
        val company = buildAdvanceCompanyWithName(childOrgCode, "Test Company $childOrgCode", Instant.now(), null)
        val address = buildAdvanceAddress(childOrgCode)
        advanceCompanyRepository.insert(company)
        advanceCompanyRepository.insertAddress(address)
        advanceCompanySubsidiaryRepository.create(parentOrgCode, childOrgCode)

        val aUserCode = UserCode("ZU1234")
        val response = authorizeUser(aUserCode, parentOrgCode)
            .handle(Request(GET, "/companies/$parentOrgCode/subsidiaries"))

        assertEquals(Status.OK, response.status)

        approver.assertApproved(response)
    }

    @Test
    fun `should get company subsidiaries tree recursively`(approver: Approver) {
        advanceCompanySubsidiaryRepository.create(parentOrgCode, childOrgCode)
        advanceCompanySubsidiaryRepository.create(childOrgCode, OrganisationCode("ZC0003"))
        advanceCompanySubsidiaryRepository.create(childOrgCode, OrganisationCode("ZC0004"))

        val response = authorizeUser(UserCode("ZU1234"), parentOrgCode)
            .handle(Request(GET, "/companies/$parentOrgCode/subsidiaries/tree"))

        approver.assertApproved(response, Status.OK)
    }

    @Test
    fun `should get company subsidiaries tree recursively - no relations`() {
        val response = authorizeUser(UserCode("ZU1234"), parentOrgCode)
            .handle(Request(GET, "/companies/$parentOrgCode/subsidiaries/tree"))
        assertThat(response, hasStatus(Status.NOT_FOUND))
    }
}
