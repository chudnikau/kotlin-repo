package com.sedex.connect.company.repository

import com.fasterxml.jackson.module.kotlin.readValue
import com.sedex.connect.company.api.CompanyJson
import com.sedex.connect.company.api.IndustryClassification
import com.sedex.connect.company.models.IndustryClassificationFlat
import okhttp3.internal.toImmutableList

class InMemoryIndustryClassificationRepository(private val src: String? = null) {
    private val classificationsFlat: List<IndustryClassificationFlat>

    init {
        classificationsFlat = getClassificationsFlat(src)
    }

    fun getClassifications(depth: Int?): List<IndustryClassification> {
        val classificationsByParentCode = classificationsFlat.filter { it.parentCode != null }.groupBy { it.parentCode }

        return classificationsFlat
            .filter { it.parentCode == null }
            .map { IndustryClassification(it.code, it.name, 0, childrenOf(it.code, classificationsByParentCode)) }
            .sortedBy { it.name }.toImmutableList()
            .map { it.copy(children = childrenAtDepth(it, depth ?: Int.MAX_VALUE)) }
    }

    private fun getClassificationsFlat(src: String?): List<IndustryClassificationFlat> =
        if (src != null) {
            CompanyJson.mapper.readValue(src)
        } else {
            CompanyJson.mapper.readValue(ClassLoader.getSystemResource("industry_classifications.json"))
        }

    private fun childrenOf(
        code: String,
        classificationsByParentCode: Map<String?, List<IndustryClassificationFlat>>,
        depth: Int = 0,
    ): List<IndustryClassification> {
        val classifications = classificationsByParentCode[code] ?: emptyList()
        val nextDepth = depth + 1
        return classifications
            .map { IndustryClassification(it.code, it.name, nextDepth, childrenOf(it.code, classificationsByParentCode, nextDepth)) }
            .sortedBy { it.name }.toImmutableList()
    }

    private fun childrenAtDepth(classification: IndustryClassification, depth: Int): List<IndustryClassification> =
        if (classification.depth >= depth) {
            emptyList()
        } else {
            classification.children.map { it.copy(children = childrenAtDepth(it, depth)) }
        }
}
