package com.sedex.connect.company.services

import com.sedex.connect.advance.company.AdvanceAdapterApi
import com.sedex.connect.advancecache.advanceCode
import com.sedex.connect.common.Address
import com.sedex.connect.common.CountryCode
import com.sedex.connect.common.EmailAddress
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.SubscriptionType.BuyerSupplier
import com.sedex.connect.common.SubscriptionType.Supplier
import com.sedex.connect.common.TelephoneNumber
import com.sedex.connect.company.api.CompanyResponse
import com.sedex.connect.company.api.CompanySearchResponse
import com.sedex.connect.company.api.CompanySize.SMALL
import com.sedex.connect.company.api.MembershipStatus
import com.sedex.connect.company.api.MembershipStatus.ACTIVE
import com.sedex.connect.company.api.MembershipStatus.LAPSED
import com.sedex.connect.company.api.exampleSubscriptionRequest
import com.sedex.connect.company.models.AdvanceSubscription
import com.sedex.connect.company.models.OrganisationSummary
import com.sedex.connect.company.models.PersistedCompany
import com.sedex.connect.company.models.ProfileStatus.COMPLETE
import com.sedex.connect.company.models.ProfileStatus.PENDING
import com.sedex.connect.company.persistedCompanyExample
import com.sedex.connect.company.repository.AdvanceCompanyRepository
import com.sedex.connect.company.repository.CompanyNotFoundException
import com.sedex.connect.company.repository.CompanyRepositoryImpl
import com.sedex.connect.user.api.FeatureToggleResultResponse
import com.sedex.connect.user.api.FeatureTypeApi.USER
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

internal class CompanyServiceTest {
    private val advanceAdapterApi = mockk<AdvanceAdapterApi>()
    private val companyRepository = mockk<CompanyRepositoryImpl>()
    private val advanceCompanyRepository = mockk<AdvanceCompanyRepository>()
    private val advanceSubscriptionService = mockk<AdvanceSubscriptionService>()
    private val companyKafkaService = mockk<CompanyKafkaService> {
        every { companyChanged(any()) } returns Unit
    }
    private val orgCode = OrganisationCode("ZC123")
    private val auditSubscriptionType = "ST004"
    private val companyLocalName = "Local company name"

    private val companyService = CompanyService(
        sendToAdvance = true,
        advanceAdapterApi = advanceAdapterApi,
        companyRepository = companyRepository,
        advanceRepository = advanceCompanyRepository,
        advanceSubscriptionService = advanceSubscriptionService,
        companyKafkaService = companyKafkaService,
    )

    @Test
    fun `update should save company to repository and send to advance if 'sendToAdvance' is true`() {
        val company = PersistedCompany(orgCode = orgCode, company = persistedCompanyExample)
        val companyResponse = CompanyResponse(orgCode, company)
        every { advanceAdapterApi.sendCompanyToAdvance(any(), any()) } returns mockk()
        every { companyRepository.createOrUpdate(any()) } returns companyResponse

        val result = companyService.update(company, orgCode)
        assertEquals(companyResponse, result)

        verify(exactly = 1) { companyRepository.createOrUpdate(any()) }
        verify(exactly = 1) { advanceAdapterApi.sendCompanyToAdvance(any(), any()) }
        verify(exactly = 1) { companyKafkaService.companyChanged(any()) }
    }

    @Test
    fun `update should not send company to advance if 'sendToAdvance' is false`() {
        val company = PersistedCompany(orgCode = orgCode, company = persistedCompanyExample)
        val companyResponse = CompanyResponse(orgCode, company)
        every { advanceAdapterApi.sendCompanyToAdvance(any(), any()) } returns mockk()
        every { companyRepository.createOrUpdate(any()) } returns companyResponse

        val companyService = CompanyService(
            sendToAdvance = false,
            advanceAdapterApi = advanceAdapterApi,
            companyRepository = companyRepository,
            advanceRepository = advanceCompanyRepository,
            advanceSubscriptionService = advanceSubscriptionService,
            companyKafkaService = companyKafkaService,
        )

        val result = companyService.update(company, orgCode)

        assertEquals(companyResponse, result)

        verify(exactly = 1) { companyRepository.createOrUpdate(any()) }
        verify(exactly = 1) { companyKafkaService.companyChanged(any()) }
        verify(exactly = 0) { advanceAdapterApi.sendCompanyToAdvance(any(), any()) }
    }

    @Test
    fun `update set membershipStatus and subscriptionType from db when they are null`() {
        val companyResponse = CompanyResponse(orgCode, persistedCompanyExample)
        val companyUpdateSlot = slot<PersistedCompany>()
        val companyToUpdate = persistedCompanyExample.copy(
            membershipStatus = null,
            subscriptionType = null
        )

        every { advanceAdapterApi.sendCompanyToAdvance(any(), any()) } returns mockk()
        every { companyRepository.createOrUpdate(capture(companyUpdateSlot)) } returns mockk()
        every { companyRepository.get(orgCode) } returns null
        every { advanceCompanyRepository.get(orgCode) } returns companyResponse

        val companyService = CompanyService(
            sendToAdvance = false,
            advanceAdapterApi = advanceAdapterApi,
            companyRepository = companyRepository,
            advanceRepository = advanceCompanyRepository,
            advanceSubscriptionService = advanceSubscriptionService,
            companyKafkaService = companyKafkaService,
        )

        companyService.update(companyToUpdate, orgCode)

        verify(exactly = 1) { advanceCompanyRepository.get(orgCode) }
        verify(exactly = 1) { companyRepository.get(orgCode) }

        val capturedCompanyToUpdate = companyUpdateSlot.captured
        assertEquals(persistedCompanyExample.membershipStatus, capturedCompanyToUpdate.membershipStatus)
        assertEquals(persistedCompanyExample.subscriptionType, capturedCompanyToUpdate.subscriptionType)
    }

    @Test
    fun `updateCompanySubscription should call update with membershipStatus and subscriptionType`() {
        val companyResponse = CompanyResponse(orgCode, persistedCompanyExample)
        val companyUpdateSlot = slot<PersistedCompany>()

        val companyService = spyk(
            CompanyService(
                sendToAdvance = false,
                advanceAdapterApi = advanceAdapterApi,
                companyRepository = companyRepository,
                advanceRepository = advanceCompanyRepository,
                advanceSubscriptionService = advanceSubscriptionService,
                companyKafkaService = companyKafkaService,
            )
        )

        every { companyService.update(capture(companyUpdateSlot), any()) } returns mockk()
        every { companyRepository.createOrUpdate(any()) } returns mockk()
        every { companyRepository.get(orgCode) } returns null
        every { advanceCompanyRepository.get(orgCode) } returns companyResponse

        companyService.updateCompanySubscription(orgCode, LAPSED, BuyerSupplier)

        val capturedCompanyToUpdate = companyUpdateSlot.captured
        assertEquals(LAPSED, capturedCompanyToUpdate.membershipStatus)
        assertEquals(BuyerSupplier.advanceCode(), capturedCompanyToUpdate.subscriptionType)
    }

    @Test
    fun `getCompanyProfileStatus should return status from company table if advance_company row not present`() {

        every { companyRepository.getCompanyProfileStatus(any()) } returns PENDING
        every { advanceCompanyRepository.get(any()) } returns null

        val profileStatus = companyService.getCompanyProfileStatus(orgCode,)

        assertEquals(profileStatus, PENDING)
    }

    @Test
    fun `getCompanyProfileStatus should return PENDING status if company table row not present and advance_company has missing mandatory fields`() {

        every { companyRepository.getCompanyProfileStatus(any()) } returns null
        every { advanceCompanyRepository.get(any()) } returns buildCompanyResponse(
            orgCode = orgCode,
            name = "My Name",
            email = "",
            phone = ""
        )

        val profileStatus = companyService.getCompanyProfileStatus(orgCode)

        assertEquals(profileStatus, PENDING)
    }

    @Test
    fun `getCompanyProfileStatus should return COMPLETE status if company table row not present and advance_company has complete mandatory fields`() {

        every { companyRepository.getCompanyProfileStatus(any()) } returns null
        every { advanceCompanyRepository.get(any()) } returns buildCompanyResponse(
            orgCode = orgCode,
            name = "My Name",
            email = "me@company.com",
            phone = "0555 555555"
        )

        val profileStatus = companyService.getCompanyProfileStatus(orgCode)

        assertEquals(profileStatus, COMPLETE)
    }

    @Test
    fun `getCompanyProfileStatus should return PENDING status if company table row not present and advance_company has telephone number of NA`() {

        every { companyRepository.getCompanyProfileStatus(any()) } returns null
        every { advanceCompanyRepository.get(any()) } returns buildCompanyResponse(
            orgCode = orgCode,
            name = "My Name",
            email = "me@company.com",
            phone = "N/A"
        )

        val profileStatus = companyService.getCompanyProfileStatus(orgCode)

        assertEquals(profileStatus, PENDING)
    }

    @Test
    fun `getCompanyProfileStatus should return COMPLETE status if company table row is COMPLETE and advance_company has missing mandatory fields`() {

        every { companyRepository.getCompanyProfileStatus(any()) } returns COMPLETE
        every { advanceCompanyRepository.get(any()) } returns buildCompanyResponse(
            orgCode = orgCode,
            name = "My Name",
            email = "",
            phone = ""
        )

        val profileStatus = companyService.getCompanyProfileStatus(orgCode)

        assertEquals(profileStatus, COMPLETE)
    }

    @Test
    fun `getCompanyProfileStatus should return COMPLETE status if company table row is PENDING and advance_company has completed mandatory fields`() {

        every { companyRepository.getCompanyProfileStatus(any()) } returns PENDING
        every { advanceCompanyRepository.get(any()) } returns buildCompanyResponse(
            orgCode = orgCode,
            name = "My Name",
            email = "me@company.com",
            phone = "0555 555555"
        )

        val profileStatus = companyService.getCompanyProfileStatus(orgCode)

        assertEquals(profileStatus, COMPLETE)
    }

    @Test
    fun `getCompanyProfileStatus should return PENDING status if company table row is PENDING and advance_company has missing mandatory fields`() {

        every { companyRepository.getCompanyProfileStatus(any()) } returns PENDING
        every { advanceCompanyRepository.get(any()) } returns buildCompanyResponse(
            orgCode = orgCode,
            name = "",
            email = "me@company.com",
            phone = "0555 555555"
        )

        val profileStatus = companyService.getCompanyProfileStatus(orgCode)

        assertEquals(profileStatus, PENDING)
    }

    @Test
    fun `getCompanyProfileStatus should throw an exception if neither of company table or advance_company rows are present`() {

        every { companyRepository.getCompanyProfileStatus(any()) } returns null
        every { advanceCompanyRepository.get(any()) } returns null

        assertThrows<CompanyNotFoundException> { companyService.getCompanyProfileStatus(orgCode) }
    }

    @Test
    fun `getCompanyWithSubscription should return company and subscription`() {
        val company = PersistedCompany(orgCode = orgCode, company = persistedCompanyExample)
        val companyResponse = CompanyResponse(orgCode, company)
        val advanceSubscription = AdvanceSubscription(exampleSubscriptionRequest)

        every { companyRepository.get(orgCode) } returns companyResponse
        every { advanceCompanyRepository.get(orgCode) } returns null
        every { advanceSubscriptionService.findByOrgCode(orgCode) } returns advanceSubscription

        val result = companyService.getCompanyWithSubscription(orgCode)

        verify(exactly = 1) { advanceSubscriptionService.findByOrgCode(orgCode) }
        assertEquals(company, result.first)
        assertEquals(advanceSubscription, result.second)
    }

    @Test
    fun `getCompanyWithSubscription should return company and subscription with Lapsed status`() {
        val company = PersistedCompany(orgCode = orgCode, company = persistedCompanyExample)
        val companyResponse = CompanyResponse(orgCode, company)
        val expectedCompanyResponse = company.copy(membershipStatus = LAPSED)

        val expiredSubscription = exampleSubscriptionRequest.copy(endDate = 1577945440000)
        val advanceSubscription = AdvanceSubscription(expiredSubscription)

        every { companyRepository.get(orgCode) } returns companyResponse
        every { advanceCompanyRepository.get(orgCode) } returns null
        every { advanceSubscriptionService.findByOrgCode(orgCode) } returns advanceSubscription

        val result = companyService.getCompanyWithSubscription(orgCode)

        verify(exactly = 1) { advanceSubscriptionService.findByOrgCode(orgCode) }
        assertEquals(advanceSubscription, result.second)
        assertEquals(expectedCompanyResponse, result.first)
    }

    @Test
    fun `getSelfOrganisationMembershipSummary should return company summary`() {
        val company = PersistedCompany(orgCode = orgCode, company = persistedCompanyExample)

        every { companyRepository.get(orgCode) } returns CompanyResponse(orgCode, company)
        every { advanceCompanyRepository.get(orgCode) } returns null
        every { advanceSubscriptionService.findByOrgCode(orgCode) } returns AdvanceSubscription(exampleSubscriptionRequest)

        val result = companyService.getSelfOrganisationMembershipSummary(orgCode)

        val expectedResponse = OrganisationSummary(ACTIVE, Supplier, "ST002", CountryCode("GB"))

        verify(exactly = 1) { companyRepository.get(orgCode) }
        verify(exactly = 1) { advanceCompanyRepository.get(orgCode) }
        verify(exactly = 1) { advanceSubscriptionService.findByOrgCode(orgCode) }

        assertEquals(expectedResponse, result)
    }

    @Test
    fun `getAuditCompanies should return all audit companies`() {
        val companies = setOf(orgCode)

        every { companyRepository.getCompanyBySubscriptionType(auditSubscriptionType) } returns companies
        every { advanceCompanyRepository.getAdvanceCompanyBySubscriptionType(auditSubscriptionType) } returns companies

        val result = companyService.getAuditCompanies()

        assertEquals(companies, result)
    }

    @Test
    fun `getLatestCompanies should return latest company with isCreatedByConnect flag`() {
        val company = PersistedCompany(orgCode = orgCode, company = persistedCompanyExample, true)
        val now = Instant.now()
        val companyResponse = CompanyResponse(orgCode, now.plus(3, ChronoUnit.MINUTES), null, company)
        val advanceCompanyResponse = CompanyResponse(orgCode, true, now, null, company.copy(email = EmailAddress("test")))

        every { companyRepository.get(orgCode) } returns companyResponse
        every { advanceCompanyRepository.get(orgCode) } returns advanceCompanyResponse

        val result = companyService.getLatestCompany(orgCode)

        assertEquals(company, result)
    }

    @Test
    fun `getLatestCompanies should return latest advance company with actual connect company size`() {
        val company = persistedCompanyExample.copy(code = orgCode, companySize = SMALL)
        val now = Instant.now()
        val companyResponse = CompanyResponse(orgCode, now, null, company)
        val advanceCompanyName = "AdvanceCompany"
        val advanceCompanyResponse = CompanyResponse(
            orgCode,
            true,
            now.plus(3, ChronoUnit.MINUTES),
            null,
            persistedCompanyExample.copy(englishName = advanceCompanyName)
        )

        every { companyRepository.get(orgCode) } returns companyResponse
        every { advanceCompanyRepository.get(orgCode) } returns advanceCompanyResponse

        val result = companyService.getLatestCompany(orgCode)

        assertEquals(result.companySize, SMALL)
        assertEquals(result.englishName, advanceCompanyName)
    }

    @Test
    fun `update set company size fom existed company with size null`() {
        val companyResponse = CompanyResponse(orgCode, persistedCompanyExample)
        val companyUpdateSlot = slot<PersistedCompany>()

        val companyService = spyk(companyService)

        every { companyService.update(capture(companyUpdateSlot), any()) } returns mockk()
        every { companyRepository.createOrUpdate(any()) } returns mockk()
        every { companyRepository.get(orgCode) } returns null
        every { advanceCompanyRepository.get(orgCode) } returns companyResponse

        companyService.updateCompanySize(orgCode, SMALL)

        assertEquals(SMALL, companyUpdateSlot.captured.companySize)
    }

    @Test
    fun `search company by valid ZC code on connect and on advance`() {
        val companyResponse = buildCompanyResponse(
            orgCode = orgCode,
            name = "My Name",
            email = "",
            phone = "",
            localName = null,
            subscriptionType = "ST002",
            membershipStatus = ACTIVE
        )

        val expectedResult = arrayListOf(
            CompanySearchResponse(
                code = orgCode,
                englishName = "My Name",
                subscriptionType = "Supplier",
                membershipStatus = ACTIVE
            )
        )

        every { companyRepository.getByCodes(setOf(orgCode)) } returns setOf(companyResponse)
        every { advanceCompanyRepository.getByCodes(setOf(orgCode)) } returns setOf(companyResponse)

        val result = companyService.searchCompaniesByName(orgCode.value)
        assertEquals(expectedResult, result)
    }

    @Test
    fun `search company by valid ZC code on connect`() {
        val companyResponse = buildCompanyResponse(
            orgCode = orgCode,
            name = "My Name",
            email = "",
            phone = "",
            localName = null,
            subscriptionType = "ST002",
            membershipStatus = ACTIVE
        )

        val expectedResult = arrayListOf(
            CompanySearchResponse(
                code = orgCode,
                englishName = "My Name",
                subscriptionType = "Supplier",
                membershipStatus = ACTIVE
            )
        )

        every { companyRepository.getByCodes(setOf(orgCode)) } returns setOf(companyResponse)
        every { advanceCompanyRepository.getByCodes(setOf(orgCode)) } returns emptySet()

        val result = companyService.searchCompaniesByName(orgCode.value)
        assertEquals(expectedResult, result)
    }

    @Test
    fun `search company by valid ZC code on advance`() {
        val companyResponse = buildCompanyResponse(
            orgCode = orgCode,
            name = "My Name",
            email = "",
            phone = "",
            localName = null,
            subscriptionType = "ST002",
            membershipStatus = ACTIVE
        )

        val expectedResult = arrayListOf(
            CompanySearchResponse(
                code = orgCode,
                englishName = "My Name",
                subscriptionType = "Supplier",
                membershipStatus = ACTIVE
            )
        )

        every { companyRepository.getByCodes(setOf(orgCode)) } returns emptySet()
        every { advanceCompanyRepository.getByCodes(setOf(orgCode)) } returns setOf(companyResponse)

        val result = companyService.searchCompaniesByName(orgCode.value)
        assertEquals(expectedResult, result)
    }

    @Test
    fun `search company non-existing ZC code`() {
        val expectedResult = emptyList<CompanySearchResponse>()

        every { companyRepository.getByCodes(setOf(orgCode)) } returns emptySet()
        every { advanceCompanyRepository.getByCodes(setOf(orgCode)) } returns emptySet()

        val result = companyService.searchCompaniesByName(orgCode.value)

        assertEquals(expectedResult, result)
    }

    @Test
    fun `search company by local name on advance and on connect`() {
        val companyResponse = buildCompanyResponse(
            orgCode = orgCode,
            name = "My Name",
            email = "",
            phone = "",
            localName = null,
            subscriptionType = "ST002",
            membershipStatus = ACTIVE
        )

        val expectedResult = arrayListOf(
            CompanySearchResponse(
                code = orgCode,
                englishName = "My Name",
                subscriptionType = "Supplier",
                membershipStatus = ACTIVE
            )
        )

        every { companyRepository.findByLocalName(companyLocalName) } returns setOf(companyResponse)
        every { companyRepository.findByLocalNameStartingWith(companyLocalName) } returns setOf(companyResponse)

        every { advanceCompanyRepository.findByLocalName(companyLocalName) } returns setOf(companyResponse)
        every { advanceCompanyRepository.findByLocalNameStartingWith(companyLocalName) } returns setOf(companyResponse)

        val result = companyService.searchCompaniesByName(companyLocalName)

        assertEquals(expectedResult, result)
    }

    @Test
    fun `search company by local name on connect`() {
        val companyResponse = buildCompanyResponse(
            orgCode = orgCode,
            name = "My Name",
            email = "",
            phone = "",
            localName = null,
            subscriptionType = "ST002",
            membershipStatus = ACTIVE
        )

        val expectedResult = arrayListOf(
            CompanySearchResponse(
                code = orgCode,
                englishName = "My Name",
                subscriptionType = "Supplier",
                membershipStatus = ACTIVE
            )
        )

        every { companyRepository.findByLocalName(companyLocalName) } returns setOf(companyResponse)
        every { companyRepository.findByLocalNameStartingWith(companyLocalName) } returns setOf(companyResponse)

        every { advanceCompanyRepository.findByLocalName(companyLocalName) } returns emptySet()
        every { advanceCompanyRepository.findByLocalNameStartingWith(companyLocalName) } returns emptySet()

        val result = companyService.searchCompaniesByName(companyLocalName)

        assertEquals(expectedResult, result)
    }

    @Test
    fun `search company by local name on advance`() {
        val companyResponse = buildCompanyResponse(
            orgCode = orgCode,
            name = "My Name",
            email = "",
            phone = "",
            localName = null,
            subscriptionType = "ST002",
            membershipStatus = ACTIVE
        )

        val expectedResult = arrayListOf(
            CompanySearchResponse(
                code = orgCode,
                englishName = "My Name",
                subscriptionType = "Supplier",
                membershipStatus = ACTIVE
            )
        )

        every { companyRepository.findByLocalName(companyLocalName) } returns emptySet()
        every { companyRepository.findByLocalNameStartingWith(companyLocalName) } returns emptySet()

        every { advanceCompanyRepository.findByLocalName(companyLocalName) } returns setOf(companyResponse)
        every { advanceCompanyRepository.findByLocalNameStartingWith(companyLocalName) } returns setOf(companyResponse)

        val result = companyService.searchCompaniesByName(companyLocalName)

        assertEquals(expectedResult, result)
    }

    @Test
    fun `search company by local name doesn't match any companies`() {
        val searchName = "Non-exist local company name"
        val company = PersistedCompany(orgCode = orgCode, company = persistedCompanyExample)
        val companyResponse = CompanyResponse(orgCode, company)

        val expectedResult = arrayListOf(
            CompanySearchResponse(
                code = orgCode,
                englishName = "My Company",
                subscriptionType = "Supplier",
                membershipStatus = ACTIVE
            )
        )

        every { companyRepository.findByLocalName(searchName) } returns setOf(companyResponse)
        every { companyRepository.findByLocalNameStartingWith(searchName) } returns setOf(companyResponse)

        every { advanceCompanyRepository.findByLocalName(searchName) } returns setOf(companyResponse)
        every { advanceCompanyRepository.findByLocalNameStartingWith(searchName) } returns setOf(companyResponse)

        val result = companyService.searchCompaniesByName(searchName)

        assertEquals(expectedResult, result)
    }

    private fun buildCompanyResponse(
        orgCode: OrganisationCode,
        name: String,
        email: String,
        phone: String,
        localName: String? = null,
        subscriptionType: String? = null,
        membershipStatus: MembershipStatus? = null
    ): CompanyResponse {
        return CompanyResponse(
            code = orgCode,
            isUpdatedByConnect = null,
            createdTime = null,
            updatedTime = null,
            englishName = name,
            localName = localName,
            businessLicenseNumber = null,
            businessLicenseExpiration = null,
            address = Address(
                line1 = "line 1",
                line2 = "line 2",
                line3 = null,
                line4 = null,
                postCode = "ZZ1 1ZZ",
                city = "London",
                country = CountryCode("GB")
            ),
            telephone = TelephoneNumber(phone),
            email = EmailAddress(email),
            billingAddress = null,
            vatNumber = null,
            smdEnrollStatus = null,
            membershipStatus = membershipStatus,
            subscriptionType = subscriptionType,
            primaryIndustryClassificationCode = null,
            secondaryIndustryClassificationCode = null,
            companySize = null
        )
    }

    private val userFeature = FeatureToggleResultResponse(
        UUID.randomUUID(),
        "Feature name",
        true,
        USER,
        null
    )
}
