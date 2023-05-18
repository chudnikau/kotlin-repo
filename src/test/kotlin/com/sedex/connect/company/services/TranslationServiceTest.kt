package com.sedex.connect.company.services

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.sedex.connect.common.Language
import com.sedex.connect.common.Language.en
import com.sedex.connect.common.Language.es
import com.sedex.connect.common.Language.ja
import com.sedex.connect.common.Language.tr
import com.sedex.connect.common.Language.vi
import com.sedex.connect.common.Language.zh
import com.sedex.connect.company.api.IndustryClassification
import com.sedex.connect.company.pact.testTranslations
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class TranslationServiceTest {

    private val service = TranslationService(testTranslations)

    @ParameterizedTest
    @MethodSource("translationTest")
    fun translates(test: TranslationTest) {
        val translated = service.translate(someIndustryClassifications, test.language)

        assertThat(translated, equalTo(test.expected))
    }

    companion object {

        private val someIndustryClassifications = listOf(
            IndustryClassification(
                "ZIC1000000", "A - Agriculture, forestry and fishing", 0,
                listOf(
                    IndustryClassification("ZIC1000100", "Crop and animal production, hunting and related service activities", 1, emptyList()),
                )
            ),
        )

        @JvmStatic
        fun translationTest() = listOf(
            TranslationTest(
                language = en,
                expected = listOf(
                    IndustryClassification(
                        "ZIC1000000", "A - Agriculture, forestry and fishing", 0,
                        listOf(
                            IndustryClassification("ZIC1000100", "Crop and animal production, hunting and related service activities", 1, emptyList()),
                        )
                    ),
                )
            ),

            TranslationTest(
                language = es,
                expected = listOf(
                    IndustryClassification(
                        "ZIC1000000", "A - Agricultura, silvicultura y pesca", 0,
                        listOf(
                            IndustryClassification("ZIC1000100", "Producción agrícola y animal, caza y actividades de servicios conexas", 1, emptyList()),
                        )
                    )
                )
            ),

            TranslationTest(
                language = zh,
                expected = listOf(
                    IndustryClassification(
                        "ZIC1000000", "A - 农业、林业和渔业", 0,
                        listOf(
                            IndustryClassification("ZIC1000100", "作物和动物生产、狩猎和相关服务活动", 1, emptyList()),
                        )
                    )
                )
            ),

            TranslationTest(
                language = ja,
                expected = listOf(
                    IndustryClassification(
                        "ZIC1000000", "A-農業、林業、漁業", 0,
                        listOf(
                            IndustryClassification("ZIC1000100", "作物と動物の生産、狩猟および関連するサービス活動", 1, emptyList()),
                        )
                    )
                )
            ),
            TranslationTest(
                language = tr,
                expected = listOf(
                    IndustryClassification(
                        "ZIC1000000", "A - Tarım, ormancılık ve balıkçılık", 0,
                        listOf(
                            IndustryClassification("ZIC1000100", "Tahıl ve hayvansal üretim, avcılık ve ilgili hizmet faaliyetleri", 1, emptyList()),
                        )
                    ),
                )
            ),
            TranslationTest(
                language = vi,
                expected = listOf(
                    IndustryClassification(
                        "ZIC1000000", "A-Nông nghiệp, lâm nghiệp, thủy sản", 0,
                        listOf(
                            IndustryClassification("ZIC1000100", "Trồng trọt và chăn nuôi, săn bắn và các hoạt động dịch vụ có liên quan", 1, emptyList()),
                        )
                    ),
                )
            ),
        )
    }

    data class TranslationTest(
        val language: Language,
        val expected: List<IndustryClassification>,
    )
}
