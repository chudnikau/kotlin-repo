package com.sedex.connect.company.services

import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.company.api.CompanyResponse
import com.sedex.connect.company.api.companyResponseExample
import com.sedex.connect.company.models.CompanySubsidiary
import com.sedex.connect.company.repository.AdvanceCompanySubsidiaryRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CompanySubsidiaryServiceTest {
    private val repository = mockk<AdvanceCompanySubsidiaryRepository>()
    private val companyService = mockk<CompanyService>()

    private val service = CompanySubsidiaryService(repository, companyService)

    private val parentOrgCode = OrganisationCode("ZC0001")
    private val childOrgCode = OrganisationCode("ZC0002")

    @Test
    fun `should not create subsidiary when exists in db`() {
        every { repository.exists(parentOrgCode, childOrgCode) } returns true

        service.create(parentOrgCode, childOrgCode)

        verify(exactly = 1) { repository.exists(parentOrgCode, childOrgCode) }
        verify(exactly = 0) { repository.create(parentOrgCode, childOrgCode) }
    }

    @Test
    fun `should create subsidiary when subsidiary does not exist in db`() {
        every { repository.exists(parentOrgCode, childOrgCode) } returns false
        every { repository.create(parentOrgCode, childOrgCode) } returns Unit

        service.create(parentOrgCode, childOrgCode)

        verify(exactly = 1) { repository.exists(parentOrgCode, childOrgCode) }
        verify(exactly = 1) { repository.create(parentOrgCode, childOrgCode) }
    }

    @Test
    fun `should delete`() {
        every { repository.delete(parentOrgCode, childOrgCode) } returns Unit

        service.delete(parentOrgCode, childOrgCode)

        verify(exactly = 1) { repository.delete(parentOrgCode, childOrgCode) }
    }

    @Test
    fun `should get company subsidiaries by parentOrgCode`() {
        val companyResponse = CompanyResponse(childOrgCode, companyResponseExample(childOrgCode.value))
        every { repository.getByParentOrgCode(parentOrgCode) } returns setOf(childOrgCode)
        every { companyService.getLatestCompaniesByCodes(setOf(childOrgCode)) } returns setOf(companyResponse)

        val sibsidiaries = service.getCompanySubsidiaries(parentOrgCode)

        assertEquals(sibsidiaries, setOf(CompanySubsidiary(childOrgCode, "My Company")))
        verify(exactly = 1) { repository.getByParentOrgCode(parentOrgCode) }
    }
}
