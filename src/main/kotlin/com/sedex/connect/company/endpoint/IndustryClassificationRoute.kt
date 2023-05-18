package com.sedex.connect.company.endpoint

import com.sedex.connect.common.Language
import com.sedex.connect.common.Language.en
import com.sedex.connect.common.http.ContractRoutes
import com.sedex.connect.company.api.CompanyJson
import com.sedex.connect.company.api.IndustryClassification
import com.sedex.connect.company.repository.InMemoryIndustryClassificationRepository
import com.sedex.connect.company.services.TranslationService
import org.http4k.contract.meta
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Query
import org.http4k.lens.enum
import org.http4k.lens.int

class IndustryClassificationRoute(
    private val inMemoryIndustryClassificationRepository: InMemoryIndustryClassificationRepository,
    private val translationService: TranslationService,
) : ContractRoutes {

    private val spec = "/industry-classifications" meta {
        summary = "Returns a list of all industry classifications"
        queries += depth
        queries += lang
        description = summary
        returning(
            OK,
            industryClassificationLens to listOf(
                IndustryClassification(
                    "ZIC1000000", "A - Agriculture, forestry and fishing", 0,
                    listOf(
                        IndustryClassification("ZIC1000100", "Crop and animal production, hunting and related service activities", 1, emptyList()),
                        IndustryClassification("ZIC1000300", "Fishing and aquaculture", 1, emptyList()),
                        IndustryClassification("ZIC1000200", "Forestry and logging", 1, emptyList()),
                    )
                ),
                IndustryClassification("ZIC1010000", "B - Mining and quarrying", 0, emptyList()),
            )
        )
    }

    override fun contractRoutes() = listOf(
        spec bindContract GET to { req ->
            val depth = req.query("depth")?.toInt()
            val lang = req.query("lang")?.toLanguage()

            val classifications = inMemoryIndustryClassificationRepository.getClassifications(depth).translate(lang ?: en)

            Response(OK).with(industryClassificationLens of classifications)
        }
    )

    companion object {
        val industryClassificationLens = CompanyJson.autoBody<List<IndustryClassification>>().toLens()
        private val depth = Query.int().optional("depth", "How many levels deep to return (0-based)")
        private val lang = Query.enum<Language>().optional("lang", "The language to return (defaulted to English)")
    }

    private fun List<IndustryClassification>.translate(lang: Language) = translationService.translate(this, lang)
    private fun String.toLanguage() = Language.valueOf(this)
}
