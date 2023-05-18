package com.sedex.connect.company.fixtures

import com.sedex.connect.company.Repositories
import com.sedex.connect.company.repository.InMemoryIndustryClassificationRepository
import com.sedex.connect.lang.TimeForTesting

class FakeRepositories(time: TimeForTesting, industryClassificationSource: String? = null) : Repositories {
    override val companyRepository = FakeCompanyRepository(time)
    override val advanceCompanyRepository = FakeAdvanceCompanyRepository()
    override val advanceSubscriptionRepository = FakeAdvanceSubscriptionRepository()
    override val industryClassificationRepository = InMemoryIndustryClassificationRepository(industryClassificationSource)
    override val advanceCompanySubsidiaryRepository = FakeAdvanceCompanySubsidiaryRepository()
    override val advanceCompanySubscriptionsRepository = FakeAdvanceCompanySubscriptionsRepository()

    fun clear() {
        companyRepository.clear()
        advanceCompanyRepository.clear()
        advanceSubscriptionRepository.clear()
        advanceCompanySubsidiaryRepository.clear()
        advanceCompanySubscriptionsRepository.clear()
    }
}
