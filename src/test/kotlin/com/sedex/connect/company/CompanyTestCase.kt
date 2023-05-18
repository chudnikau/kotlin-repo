package com.sedex.connect.company

import com.fasterxml.jackson.databind.node.ObjectNode
import com.sedex.connect.advance.FakeAdvanceAdapter
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.TestOperationalEvents
import com.sedex.connect.common.auth.SedexAdmin
import com.sedex.connect.common.auth.UserAccessToken
import com.sedex.connect.common.auth.UserCode
import com.sedex.connect.common.auth.UserRole
import com.sedex.connect.common.auth.UserTokenData
import com.sedex.connect.common.auth.toUserRoleResponse
import com.sedex.connect.common.json.withStandardMappings
import com.sedex.connect.common.kafka.InMemoryTestKafka
import com.sedex.connect.common.service.ServiceName
import com.sedex.connect.company.CompanyConfiguration.Companion.companyConfigurationFrom
import com.sedex.connect.company.api.CompanyTopic
import com.sedex.connect.company.api.exampleSubscriptionRequest
import com.sedex.connect.company.fixtures.FakeRepositories
import com.sedex.connect.company.models.AddressType.COMPANY_ADDRESS
import com.sedex.connect.company.models.AdvanceCompany
import com.sedex.connect.company.models.AdvanceCompanyAddress
import com.sedex.connect.company.models.AdvanceSubscription
import com.sedex.connect.company.models.PersistedCompany
import com.sedex.connect.company.pact.testTranslations
import com.sedex.connect.company.repository.AdvanceCompanyRepository
import com.sedex.connect.company.repository.CompanyRepository
import com.sedex.connect.environment.local.LocalEnvironment
import com.sedex.connect.lang.TimeForTesting
import com.sedex.connect.lang.mapToSet
import com.sedex.connect.security.testJwtIssuer
import com.sedex.connect.security.token.withAuthorisationToken
import com.sedex.connect.service.RestServer
import com.sedex.connect.service.TestServiceInfrastructure
import com.sedex.connect.service.testInfrastructure
import com.sedex.connect.user.FakeUserService
import org.http4k.cloudnative.env.Environment.Companion.from
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.then
import org.http4k.format.ConfigurableJackson
import org.http4k.testing.JsonApprovalTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.util.UUID.randomUUID

@ExtendWith(JsonApprovalTest::class)
@TestInstance(PER_CLASS)
open class CompanyTestCase {

    private val objectMapper = ConfigurableJackson(withStandardMappings()).mapper

    @BeforeEach
    open fun beforeEach() {
        userService.clear()
        advanceAdapter.clear()
        kafka.reset()
        repositories.clear()
    }

    companion object {
        val app: RestServer
        val userService = FakeUserService()
        val advanceAdapter = FakeAdvanceAdapter()
        val time = TimeForTesting("2022-08-09T09:00:00.00Z")
        private val repositories = FakeRepositories(time)

        @JvmStatic
        protected val events = TestOperationalEvents()
        private val config: CompanyConfiguration
        private val infra: TestServiceInfrastructure<CompanyConfiguration>
        private val kafka = InMemoryTestKafka(events = events)

        val companyRepository: CompanyRepository
            get() = repositories.companyRepository
        val advanceCompanyRepository: AdvanceCompanyRepository
            get() = repositories.advanceCompanyRepository
        val advanceCompanySubsidiaryRepository = repositories.advanceCompanySubsidiaryRepository
        val advanceCompanySubscriptionsRepository = repositories.advanceCompanySubscriptionsRepository

        val companyMessages
            get() = kafka.producedMessages<String, CompanyTopic>(config.companyTopicName)

        init {
            val env = from(LocalEnvironment.kafkaEnvironment())
            config = companyConfigurationFrom(env)
            infra = testInfrastructure(configuration = config, clock = time, timer = time, events = events)
                .withInternalHttpClient(ServiceName("user"), userService.handler)
                .withInternalHttpClient(ServiceName("advance-adapter"), advanceAdapter.handler)
            infra.run {
                app = companyApp(
                    repositories = repositories,
                    translationsOverride = testTranslations,
                    kafka = kafka,
                )
            }
        }

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            app.startServiceOnly()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            app.stopServiceOnly()
        }
    }

    fun authorizeUser(
        userCode: UserCode = UserCode("ZU0000000"),
        orgCode: OrganisationCode? = null,
        roles: Set<UserRole> = emptySet(),
    ) = object : AuthorizedUserApp {
        override val handler = Filter { next ->
            {
                next(
                    it.withAuthorisationToken(
                        testJwtIssuer()
                            .jwtFor(UserAccessToken(randomUUID(), userCode, orgCode, roles.mapToSet { it.toUserRoleResponse() }))
                    )
                )
            }
        }
            .then(app)
    }

    protected fun impersonateUser(
        userCode: UserCode = UserCode("ZU0000000"),
        adminUserCode: UserCode = UserCode("ZU0000001"),
        orgCode: OrganisationCode? = null,
        roles: Set<UserRole> = emptySet(),
    ) = object : AuthorizedUserApp {
        override val handler = Filter { next ->
            {
                val userTokenData = UserTokenData(
                    userCode = userCode.value,
                    organisationCode = orgCode?.value,
                    roles = roles.map { role -> role.toUserRoleResponse() }.toSet()
                )
                val adminTokenData = UserTokenData(
                    userCode = adminUserCode.value,
                    roles = setOf(SedexAdmin.toUserRoleResponse())
                )
                next(
                    it.withAuthorisationToken(
                        testJwtIssuer()
                            .jwtFor(
                                UserAccessToken(
                                    userId = randomUUID().toString(),
                                    userData = userTokenData,
                                    impersonator = adminTokenData
                                )
                            )
                    )
                )
            }
        }
            .then(app)
    }

    fun get(orgCode: OrganisationCode): PersistedCompany? {
        return repositories.companyRepository.get(orgCode)?.let { PersistedCompany(it) }
    }

    fun exists(orgCode: OrganisationCode): Boolean {
        return repositories.companyRepository.existsByCode(orgCode)
    }

    protected fun buildAdvanceCompany(orgCode: OrganisationCode, createdTime: Instant, updatedTime: Instant?): AdvanceCompany {
        val json = objectMapper.readTree(advanceJson(orgCode))
        return AdvanceCompany(
            orgCode,
            createdTime,
            updatedTime,
            json
        )
    }

    protected fun buildAdvanceCompany(orgCode: OrganisationCode, createdTime: Instant, updatedTime: Instant?, customJsonContent: (orgCode: OrganisationCode) -> String): AdvanceCompany {
        val json = objectMapper.readTree(customJsonContent(orgCode))
        return AdvanceCompany(
            orgCode,
            createdTime,
            updatedTime,
            json
        )
    }

    protected fun buildAdvanceCompanyWithName(orgCode: OrganisationCode, name: String, createdTime: Instant, updatedTime: Instant?): AdvanceCompany {
        val json = objectMapper.readTree(advanceJson(orgCode))
        val jsonObject = json as ObjectNode
        jsonObject.put("name", name)
        return AdvanceCompany(
            orgCode,
            createdTime,
            updatedTime,
            jsonObject
        )
    }

    protected fun buildAdvanceAddress(orgCode: OrganisationCode): AdvanceCompanyAddress {
        return AdvanceCompanyAddress(
            orgCode,
            COMPANY_ADDRESS,
            objectMapper.readTree(advanceAddressJson(orgCode))
        )
    }

    protected fun buildConnectCompanyWithName(code: String, name: String): PersistedCompany {
        return PersistedCompany(OrganisationCode(code), persistedCompanyExample.copy(englishName = name))
    }

    protected fun createSubscription(orgCode: OrganisationCode) {
        val advanceSubscription = AdvanceSubscription(exampleSubscriptionRequest).copy(
            orgCode = orgCode
        )
        repositories.advanceSubscriptionRepository.createOrUpdate(advanceSubscription)
    }

    public fun advanceJson(orgCode: OrganisationCode, localName: String = "Local company name", englishName: String = "My Company"): String = """
            {
              "createdOn": 1645703361920,
              "modifiedOn": 1645703361920,
              "isUpdatedByConnect": false,
              "code": "$orgCode",
              "subscriptionType": "ST002",
              "membershipStatus": "ACTIVE",
              "name": "$englishName",
              "smdNameInLocalLanguage": "$localName",
              "contact": "email@example.com",
              "telephone": "07123456789",
              "smdBusinessLicenseNumber": null,
              "businessLicenseExpiredDateNotApplicable": false,
              "smdEnrollStatus": "OPTED_IN",
              "smdMainContacts": [],
              "primaryIndustryClassificationCode": "ZIC1000000",
              "secondaryIndustryClassificationCode": "ZIC1000100"
            }
        """

    private fun advanceAddressJson(orgCode: OrganisationCode): String = """
            {
              "createdOn": 1645703361920,
              "modifiedOn": 1645703361920,
              "isUpdatedByConnect": false,
              "code": "$orgCode",
              "addressLine1": "Somewhere",
              "addressLine2": null,
              "addressLine3": null,
              "addressLine4": null,
              "city": "London",
              "countryCode": "GB",
              "postCode": "AB12 3CD"
            }
        """
}

interface AuthorizedUserApp {
    val handler: HttpHandler
    fun handle(request: Request): Response = handler(request)
}
