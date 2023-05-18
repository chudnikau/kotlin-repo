package com.sedex.connect.company.repository

import com.sedex.connect.common.DatabaseCleaner
import com.sedex.connect.common.TestOperationalEvents
import com.sedex.connect.common.database.DatabaseMigrations
import com.sedex.connect.common.database.DisconnectedDatabase
import com.sedex.connect.common.database.PooledDisconnectedDatabase
import com.sedex.connect.common.database.cockroachConfigFrom
import com.sedex.connect.company.table.AdvanceCompanySubscriptionsTable
import com.sedex.connect.company.table.AdvanceCompanySubsidiaryTable
import com.sedex.connect.company.table.AdvanceSubscriptionTable
import com.sedex.connect.docker.CockroachContainer
import com.sedex.connect.environment.local.LocalEnvironment
import com.sedex.connect.lang.TimeForTesting
import org.http4k.cloudnative.env.Environment
import org.junit.jupiter.api.BeforeEach

@CockroachContainer
abstract class RepositoryTestBase {

    @BeforeEach
    fun beforeEach() {
        dbCleaner.clean()
    }

    companion object {
        private val disconnectedDb: DisconnectedDatabase by lazy { createDb() }
        private val dbCleaner: DatabaseCleaner by lazy { dbCleaner(disconnectedDb) }

        @JvmStatic
        protected val db by lazy { disconnectedDb.connect() }
        private val tablesToClean = setOf(
            CompanyTable,
            AdvanceCompanyTable,
            AdvanceCompanyAddressTable,
            AdvanceSubscriptionTable,
            AdvanceCompanySubsidiaryTable,
            AdvanceCompanySubscriptionsTable
        )
        val events = TestOperationalEvents(print = true)
        val time = TimeForTesting("2022-08-09T09:00:00.00Z")

        private fun createDb(): DisconnectedDatabase {
            val databaseConfig = cockroachConfigFrom(Environment.from(LocalEnvironment.cockroachEnvironment()))
            val disconnectedDatabase = PooledDisconnectedDatabase(databaseConfig, events)
            disconnectedDatabase.createIfNotExists()
            DatabaseMigrations(databaseConfig).run("db/company-backend/migration")

            return disconnectedDatabase
        }

        private fun dbCleaner(db: DisconnectedDatabase): DatabaseCleaner {
            return DatabaseCleaner(db, tablesToClean = tablesToClean, tablesToKeep = emptySet())
        }
    }
}
