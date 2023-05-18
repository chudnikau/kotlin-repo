package com.sedex.connect.company.repository

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.randomOrganisationCode
import com.sedex.connect.company.fixtures.FakeAdvanceCompanySubsidiaryRepository
import com.sedex.connect.company.models.CompanySubsidiaryRelation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

interface AdvanceCompanySubsidiaryRepositoryContract {
    val repository: AdvanceCompanySubsidiaryRepository

    @Test
    fun `should create`() {
        val parentOrgCode = randomOrganisationCode()
        val childOrgCode = randomOrganisationCode()

        repository.create(parentOrgCode, childOrgCode)
        assertTrue(repository.exists(parentOrgCode, childOrgCode))
    }

    @Test
    fun `should delete`() {
        val parentOrgCode = randomOrganisationCode()
        val childOrgCode = randomOrganisationCode()

        repository.create(parentOrgCode, childOrgCode)
        assertTrue(repository.exists(parentOrgCode, childOrgCode))

        repository.delete(parentOrgCode, childOrgCode)
        assertFalse(repository.exists(parentOrgCode, childOrgCode))
    }

    @Test
    fun `should exists`() {
        val parentOrgCode = randomOrganisationCode()
        val childOrgCode = randomOrganisationCode()

        repository.create(parentOrgCode, childOrgCode)
        assertTrue(repository.exists(parentOrgCode, childOrgCode))
    }

    @Test
    fun `should get company Subsidiaries`() {
        val parentOrgCode = randomOrganisationCode()
        val childOrgCode = randomOrganisationCode()
        repository.create(parentOrgCode, childOrgCode)

        val actual = repository.getByParentOrgCode(parentOrgCode)

        assertEquals(setOf(childOrgCode), actual)
    }

    @Test
    fun `should get company subsidiaries recursive`() {
        repository.create(OrganisationCode("ZC1"), OrganisationCode("ZC10"))
        repository.create(OrganisationCode("ZC10"), OrganisationCode("ZC100"))
        repository.create(OrganisationCode("ZC10"), OrganisationCode("ZC101"))
        repository.create(OrganisationCode("ZC10"), OrganisationCode("ZC102"))
        repository.create(OrganisationCode("ZC102"), OrganisationCode("ZC1020"))
        repository.create(OrganisationCode("ZC200"), OrganisationCode("ZC11"))

        val actual = repository.getSubsidiariesRecursive(OrganisationCode("ZC1"))

        val expected = setOf(
            CompanySubsidiaryRelation(OrganisationCode("ZC1"), OrganisationCode("ZC10")),
            CompanySubsidiaryRelation(OrganisationCode("ZC10"), OrganisationCode("ZC100")),
            CompanySubsidiaryRelation(OrganisationCode("ZC10"), OrganisationCode("ZC101")),
            CompanySubsidiaryRelation(OrganisationCode("ZC10"), OrganisationCode("ZC102")),
            CompanySubsidiaryRelation(OrganisationCode("ZC102"), OrganisationCode("ZC1020")),
        )
        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `should get company subsidiaries recursive - not found`() {
        repository.create(OrganisationCode("ZC2"), OrganisationCode("ZC11"))

        val actual = repository.getSubsidiariesRecursive(OrganisationCode("ZC1"))
        assertThat(actual, equalTo(emptySet()))
    }
}

class AdvanceCompanySubsidiaryRepositoryTest : AdvanceCompanySubsidiaryRepositoryContract, RepositoryTestBase() {
    override val repository = AdvanceCompanySubsidiaryRepositoryImpl(db)
}

class FakeAdvanceCompanySubsidiaryRepositoryTest : AdvanceCompanySubsidiaryRepositoryContract {
    override val repository = FakeAdvanceCompanySubsidiaryRepository()
}
