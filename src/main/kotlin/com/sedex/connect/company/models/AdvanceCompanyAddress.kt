package com.sedex.connect.company.models

import com.fasterxml.jackson.databind.JsonNode
import com.sedex.connect.common.OrganisationCode

data class AdvanceCompanyAddress(
    val code: OrganisationCode,
    val addressType: AddressType,
    val data: JsonNode,
)
