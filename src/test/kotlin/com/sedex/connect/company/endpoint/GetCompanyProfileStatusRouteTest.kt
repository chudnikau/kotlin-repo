package com.sedex.connect.company.endpoint

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.auth.OrganisationAdmin
import com.sedex.connect.common.auth.UserCode
import com.sedex.connect.common.logging.OperationalEventsJson.auto
import com.sedex.connect.company.CompanyTestCase
import com.sedex.connect.company.api.CompanySize.MEDIUM
import com.sedex.connect.company.models.PersistedCompany
import com.sedex.connect.company.models.ProfileStatus.COMPLETE
import com.sedex.connect.company.persistedCompanyExample
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetCompanyProfileStatusRouteTest : CompanyTestCase() {

    @BeforeEach
    override fun beforeEach() {
        super.beforeEach()
        userService.addUser(userCode = aUserCode, organisationCode = anOrgCode)
        userService.addRoles(aUserCode, OrganisationAdmin(anOrgCode))
    }

    @Test
    fun `returns Completed with OK status`() {
        companyRepository.create(aPersistedCompany.copy(companySize = MEDIUM))

        val response = authorizeUser(aUserCode)
            .handle(Request(GET, "/companies/$anOrgCode/profile-status"))

        val status = orgProfileStatusResponseLens(response)

        assertThat(response.status, equalTo(OK))
        assertThat(status, equalTo(CompanyProfileResponse(COMPLETE)))
    }

    @Test
    fun `returns NOT_FOUND when company doesn't exist`() {
        val response = authorizeUser(aUserCode)
            .handle(Request(GET, "/companies/$anOrgCode/profile-status"))

        assertThat(response.status, equalTo(NOT_FOUND))
    }

    companion object {
        private val orgProfileStatusResponseLens = Body.auto<CompanyProfileResponse>().toLens()
        private val anOrgCode = OrganisationCode("ZC1234")
        private val aUserCode = UserCode("ZU1234")
        private val aPersistedCompany = PersistedCompany(anOrgCode, persistedCompanyExample)
    }
}
