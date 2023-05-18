package com.sedex.connect.company.utils

import com.fasterxml.jackson.databind.JsonNode

fun JsonNode?.getValue(key: String): String? {
    return this?.get(key)?.asText()?.replace("null", "")?.ifBlank { null }
}
