package com.sedex.connect.company.services

import com.sedex.connect.common.kafka.KafkaTopicProducer
import com.sedex.connect.company.api.CompanyResponse
import com.sedex.connect.company.api.CompanyTopic
import com.sedex.connect.company.repository.toMap

class CompanyKafkaService(
    private val kafkaTopicProducer: KafkaTopicProducer<String, CompanyTopic>,
) {
    fun companyChanged(company: CompanyResponse) {
        kafkaTopicProducer.send(company.code.value, company.toCompanyTopic())
    }

    private fun CompanyResponse.toCompanyTopic() = CompanyTopic(
        organisationCode = code,
        data = this.toMap(),
        lastModifiedAt = lastModifiedAt ?: throw IllegalStateException("Company is not created"),
    )
}
