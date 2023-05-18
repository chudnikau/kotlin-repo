package com.sedex.connect.company.models

data class IndustryClassificationFlat(
    val code: String,
    val name: String,
    val parentCode: String?,
)
