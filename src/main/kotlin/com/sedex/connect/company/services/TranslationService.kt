package com.sedex.connect.company.services

import com.sedex.connect.common.Language
import com.sedex.connect.company.api.IndustryClassification
import com.sedex.connect.i18n.Translations
import com.sedex.connect.i18n.TranslationsModel

class TranslationService(private val translations: Translations) {

    private fun translateChildren(translations: TranslationsModel, classifications: List<IndustryClassification>) =
        classifications.map { it.translate(translations) }

    fun translate(industryClassifications: List<IndustryClassification>, language: Language): List<IndustryClassification> {
        val translations = translations.translationsFor(language)
        return industryClassifications.map { it.translate(translations) }
    }

    private fun IndustryClassification.translate(translations: TranslationsModel): IndustryClassification {
        val translatedName = translations[code]?.text() ?: name
        return copy(name = translatedName, children = translateChildren(translations, children))
    }

    private fun Any?.text() = (this as Map<*, *>)["text"] as String
}
