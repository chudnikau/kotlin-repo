package com.sedex.connect.company

import com.sedex.connect.common.database.ConnectedDatabase
import com.sedex.connect.common.database.DatabaseMigrations
import com.sedex.connect.common.database.DisconnectedDatabase
import com.sedex.connect.company.repository.AdvanceCompanyRepository
import com.sedex.connect.company.repository.AdvanceCompanyRepositoryImpl
import com.sedex.connect.company.repository.AdvanceCompanySubscriptionsRepository
import com.sedex.connect.company.repository.AdvanceCompanySubscriptionsRepositoryImpl
import com.sedex.connect.company.repository.AdvanceCompanySubsidiaryRepository
import com.sedex.connect.company.repository.AdvanceCompanySubsidiaryRepositoryImpl
import com.sedex.connect.company.repository.AdvanceSubscriptionRepository
import com.sedex.connect.company.repository.AdvanceSubscriptionRepositoryImpl
import com.sedex.connect.company.repository.CompanyRepository
import com.sedex.connect.company.repository.CompanyRepositoryImpl
import com.sedex.connect.company.repository.InMemoryIndustryClassificationRepository
import com.sedex.connect.lang.ReasonableClock

interface Repositories {
    val companyRepository: CompanyRepository
    val advanceCompanyRepository: AdvanceCompanyRepository
    val advanceSubscriptionRepository: AdvanceSubscriptionRepository
    val industryClassificationRepository: InMemoryIndustryClassificationRepository
    val advanceCompanySubsidiaryRepository: AdvanceCompanySubsidiaryRepository
    val advanceCompanySubscriptionsRepository: AdvanceCompanySubscriptionsRepository
}

class DatabaseRepositories(
    config: CompanyConfiguration,
    disconnectedDatabase: DisconnectedDatabase,
    clock: ReasonableClock,
) : Repositories {
    private val connectedDatabase: ConnectedDatabase

    init {
        disconnectedDatabase.createIfNotExists()
        DatabaseMigrations(config.appDatabase).run("db/company-backend/migration")
        connectedDatabase = disconnectedDatabase.connect()
    }

    override val companyRepository: CompanyRepository by lazy { CompanyRepositoryImpl(connectedDatabase, clock) }
    override val advanceCompanyRepository: AdvanceCompanyRepository by lazy { AdvanceCompanyRepositoryImpl(connectedDatabase) }
    override val advanceSubscriptionRepository: AdvanceSubscriptionRepository by lazy { AdvanceSubscriptionRepositoryImpl(connectedDatabase) }
    override val industryClassificationRepository = InMemoryIndustryClassificationRepository()
    override val advanceCompanySubsidiaryRepository: AdvanceCompanySubsidiaryRepository by lazy { AdvanceCompanySubsidiaryRepositoryImpl(connectedDatabase) }
    override val advanceCompanySubscriptionsRepository: AdvanceCompanySubscriptionsRepository by lazy { AdvanceCompanySubscriptionsRepositoryImpl(connectedDatabase) }
}
