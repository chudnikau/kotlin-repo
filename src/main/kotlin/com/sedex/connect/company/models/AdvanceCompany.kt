package com.sedex.connect.company.models

import com.fasterxml.jackson.databind.JsonNode
import com.sedex.connect.common.OrganisationCode
import java.time.Instant

data class AdvanceCompany(
    val code: OrganisationCode,
    val createdTime: Instant,
    val updatedTime: Instant?,
    val data: JsonNode,
)
