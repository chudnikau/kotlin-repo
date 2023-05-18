package com.sedex.connect.company.repository

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.sedex.connect.company.api.IndustryClassification
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class InMemoryIndustryClassificationRepositoryTest {

    @Test
    fun `gets all classifications`() {
        val repo = InMemoryIndustryClassificationRepository(
            """
            [
              {
                "code": "ZIC1000200",
                "name": "Forestry and logging",
                "parentCode": "ZIC1000000"
              },
              {
                "code": "ZIC1000000",
                "name": "A - Agriculture, forestry and fishing"
              },
              {
                "code": "ZIC1000100",
                "name": "Crop and animal production, hunting and related service activities",
                "parentCode": "ZIC1000000"
              },
              {
                "code": "ZIC1000300",
                "name": "Fishing and aquaculture",
                "parentCode": "ZIC1000000"
              },
              {
                "code": "ZIC1010000",
                "name": "B - Mining and quarrying"
              }
            ]
            """.trimIndent()
        )

        val classifications = repo.getClassifications(null)

        assertThat(
            classifications,
            equalTo(
                listOf(
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
        )
    }

    @ParameterizedTest
    @MethodSource("filterByDepthTestData")
    fun `filters by depth`(test: FilterTest) {
        val repo = InMemoryIndustryClassificationRepository(test.json)

        val classifications = repo.getClassifications(test.depth)

        assertThat(classifications, equalTo(test.expected))
    }

    companion object {

        @JvmStatic
        fun filterByDepthTestData() = listOf(
            FilterTest(
                1,
                listOf(
                    IndustryClassification(
                        "100", "Parent:100", 0,
                        listOf(
                            IndustryClassification("100-01", "Parent:100 -> Child:01", 1, emptyList()),
                            IndustryClassification("100-02", "Parent:100 -> Child:02", 1, emptyList()),
                        )
                    ),
                    IndustryClassification("200", "Parent:200", 0, emptyList()),
                )
            ),

            FilterTest(
                2,
                listOf(
                    IndustryClassification(
                        "100", "Parent:100", 0,
                        listOf(
                            IndustryClassification(
                                "100-01", "Parent:100 -> Child:01", 1,
                                listOf(
                                    IndustryClassification("100-01-A", "Parent:100 -> Child:01 -> Child:A", 2, emptyList()),
                                    IndustryClassification("100-01-B", "Parent:100 -> Child:01 -> Child:B", 2, emptyList()),
                                )
                            ),
                            IndustryClassification(
                                "100-02", "Parent:100 -> Child:02", 1,
                                listOf(
                                    IndustryClassification("100-02-A", "Parent:100 -> Child:02 -> Child:A", 2, emptyList()),
                                )
                            ),
                        )
                    ),
                    IndustryClassification("200", "Parent:200", 0, emptyList()),
                )
            ),

            FilterTest(
                3,
                listOf(
                    IndustryClassification(
                        "100", "Parent:100", 0,
                        listOf(
                            IndustryClassification(
                                "100-01", "Parent:100 -> Child:01", 1,
                                listOf(
                                    IndustryClassification(
                                        "100-01-A", "Parent:100 -> Child:01 -> Child:A", 2,
                                        listOf(
                                            IndustryClassification("100-01-A-001", "Parent:100 -> Child:01 -> Child:A -> Child:001", 3, emptyList())
                                        )
                                    ),
                                    IndustryClassification("100-01-B", "Parent:100 -> Child:01 -> Child:B", 2, emptyList()),
                                )
                            ),
                            IndustryClassification(
                                "100-02", "Parent:100 -> Child:02", 1,
                                listOf(
                                    IndustryClassification("100-02-A", "Parent:100 -> Child:02 -> Child:A", 2, emptyList()),
                                )
                            ),
                        )
                    ),
                    IndustryClassification("200", "Parent:200", 0, emptyList()),
                )
            ),
        )
    }

    data class FilterTest(
        val depth: Int? = Int.MAX_VALUE,
        val expected: List<IndustryClassification>,
        val json: String = Companion.json,
    ) {
        companion object {
            val json = """
                [
                  {
                    "code": "100",
                    "name": "Parent:100"
                  },
                  {
                    "code": "200",
                    "name": "Parent:200"
                  },
                  {
                    "code": "100-01",
                    "name": "Parent:100 -> Child:01",
                    "parentCode": "100"
                  },
                  {
                    "code": "100-02",
                    "name": "Parent:100 -> Child:02",
                    "parentCode": "100"
                  },
                  {
                    "code": "100-01-A",
                    "name": "Parent:100 -> Child:01 -> Child:A",
                    "parentCode": "100-01"
                  },
                  {
                    "code": "100-01-B",
                    "name": "Parent:100 -> Child:01 -> Child:B",
                    "parentCode": "100-01"
                  },
                  {
                    "code": "100-02-A",
                    "name": "Parent:100 -> Child:02 -> Child:A",
                    "parentCode": "100-02"
                  },
                  {
                    "code": "100-01-A-001",
                    "name": "Parent:100 -> Child:01 -> Child:A -> Child:001",
                    "parentCode": "100-01-A"
                  }
                ]
            """.trimIndent()
        }
    }
}
