package com.sedex.connect.company

import com.sedex.connect.common.Address
import com.sedex.connect.common.CountryCode
import com.sedex.connect.common.EmailAddress.Companion.emailRegex
import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.SmdEnrollStatus
import com.sedex.connect.common.SubscriptionTypeCode
import com.sedex.connect.common.http.ResponseException
import com.sedex.connect.common.http.aClientError
import com.sedex.connect.common.nameRegex
import com.sedex.connect.common.telephoneNumberRegex
import com.sedex.connect.company.api.Company
import com.sedex.connect.company.api.CompanyResponse
import com.sedex.connect.company.api.ValidationError
import com.sedex.connect.company.api.ValidationErrorResponse
import com.sedex.connect.company.endpoint.CreateCompanyRoute
import com.sedex.connect.company.repository.CompanyNotFoundException
import com.sedex.connect.lang.StringValue
import io.konform.validation.Validation
import io.konform.validation.ValidationBuilder
import io.konform.validation.jsonschema.maxLength
import io.konform.validation.jsonschema.pattern
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import java.util.Locale

private const val maxStringLength = 1000

private val validateAddress = Validation<Address> {
    Address::line1 required {
        notBlank()
        maxLength(maxStringLength)
    }
    Address::line2 ifPresent {
        notBlank()
        maxLength(1000)
    }
    Address::city required {
        notBlank()
        maxLength(maxStringLength)
    }
    Address::country required { isCountryValid() }
    Address::postCode required {
        notBlank()
        maxLength(maxStringLength)
    }
}

val validateCompany = Validation<Company> {
    Company::telephone required {
        pattern(telephoneNumberRegex)
        maxLength(maxStringLength)
    }
    Company::email required {
        pattern(emailRegex)
        maxLength(maxStringLength)
    }
    Company::englishName required {
        pattern(nameRegex)
        maxLength(maxStringLength)
    }
    Company::address required { run(validateAddress) }
    Company::billingAddress ifPresent { run(validateAddress) }
    validateIndustryClassificationCodes()
    Company::primaryIndustryClassificationCode ifPresent {
        notBlank()
        maxLength(maxStringLength)
    }
    Company::secondaryIndustryClassificationCode ifPresent {
        notBlank()
        maxLength(maxStringLength)
    }
    Company::smdEnrollStatus ifPresent { isSmdEnrollStatusValid() }
    Company::localName ifPresent {
        notBlank()
        maxLength(maxStringLength)
    }
    Company::businessLicenseNumber ifPresent {
        notBlank()
        maxLength(maxStringLength)
    }
    validateVatNumber()
}

private fun ValidationBuilder<String>.isSubscriptionTypeValid() = addConstraint("must exist in the subscriptionType list: ${SubscriptionTypeCode.values().joinToString()}") { SubscriptionTypeCode.isValid(it) }
private fun ValidationBuilder<String>.isSmdEnrollStatusValid() = addConstraint("must exist in the smdEnrollStatus list: ${SmdEnrollStatus.values().joinToString()}") { SmdEnrollStatus.isValid(it) }
private fun ValidationBuilder<CountryCode>.isCountryValid() = addConstraint("must exist in the country code list: ${Locale.getISOCountries().joinToString()}") { CountryCode.isValid(it.value) }
private fun ValidationBuilder<String>.notBlank() = addConstraint("must not be blank") { it.isNotBlank() }
private fun ValidationBuilder<out StringValue>.pattern(pattern: Regex) = addConstraint(
    "must match the expected pattern: $pattern",
    pattern.pattern
) { it.value.matches(pattern) }

private fun ValidationBuilder<out StringValue>.maxLength(maxLength: Int) = addConstraint("must have at most {value} characters") { it.length <= maxLength }

private fun ValidationBuilder<Company>.validateIndustryClassificationCodes() = addConstraint("Primary and secondary industry classification codes must both exist or not exist") {
    (it.primaryIndustryClassificationCode == null && it.secondaryIndustryClassificationCode == null) ||
        !(it.primaryIndustryClassificationCode == null || it.secondaryIndustryClassificationCode == null)
}

private fun ValidationBuilder<Company>.validateVatNumber() = addConstraint("Vat number is not valid") {
    val vatNumberCountryCode = it.billingAddress?.country?.value ?: it.address.country?.value
    if (it.vatNumber == null) true
    else if (it.vatNumber!!.value.isBlank()) false
    else if (vatNumberCountryCode?.let { countryCode -> it.vatNumber!!.value.startsWith(countryCode) } == false) false
    else if (vatNumberCountryCode in euCountries) vatNumberRegex.matches(it.vatNumber!!.value)
    else if (vatNumberCountryCode !in euCountries) Regex("^[a-zA-Z0-9-./\\s]*\$").matches(it.vatNumber!!.value)
    else true
}

// completed version of https://www.oreilly.com/library/view/regular-expressions-cookbook/9781449327453/ch04s21.html
private val vatNumberRegex = Regex(
    "^(ATU[0-9]{8}|BE[01][0-9]{9}|BG[0-9]{9,10}|HR[0-9]{11}|CY[0-9]{8}[A-Z]|CZ[0-9]{8,10}|DK[0-9]{8}" +
        "|EE[0-9]{9}|FI[0-9]{8}|FR[0-9A-Z]{2}[0-9]{9}|DE[0-9]{9}|EL[0-9]{9}|HU[0-9]{8}|IE([0-9]{7}[A-Z]{1,2}" +
        "|[0-9][A-Z][0-9]{5}[A-Z])|IT[0-9]{11}|LV[0-9]{11}|LT([0-9]{9}|[0-9]{12})|LU[0-9]{8}|MT[0-9]{8}" +
        "|NL[0-9]{9}B[0-9]{2}|PL[0-9]{10}|PT[0-9]{9}|RO[0-9]{2,10}|SK[0-9]{10}|SI[0-9]{8}|ES([A-Z][0-9]{8}" +
        "|[0-9]{8}[A-Z]|[A-Z][0-9]{7}[A-Z])|SE[0-9]{12}|GB([0-9]{9}|[0-9]{12}|GD[0-4][0-9]{2}|HA[5-9][0-9]{2}))\$"
)

private val euCountries = arrayOf(
    "AT", "BE", "BG", "CY", "CZ", "DE", "HR", "DK", "EE", "EL", "ES", "FI", "FR", "HU",
    "IE", "IT", "LT", "LU", "LV", "MT", "NL", "PL", "PT", "RO", "SE", "SI", "SK",
)

fun validateCompanyRequest(companyCodes: Set<OrganisationCode>?) {
    if (companyCodes.isNullOrEmpty()) throw ValidationException("The body must not be blank")
}
fun validateResponseHasAllRequestedCompanies(companyCodes: Set<OrganisationCode>, latestCompanies: Set<CompanyResponse>) {
    val difference = companyCodes - latestCompanies.map { it.code }.toSet()
    if (difference.isNotEmpty()) {
        throw CompanyNotFoundException(difference)
    }
}
fun Response.withValidationErrors(errors: List<ValidationError>): Response {
    val validationErrorFields = errors.joinToString(transform = { e -> e.field })
    val validationErrorMessage = "Validation failed for $validationErrorFields"

    return this.with(aClientError of validationErrorMessage)
        .with(CreateCompanyRoute.validationErrorResponseLens of ValidationErrorResponse(errors))
}

class ValidationException(message: String) : ResponseException(Status.BAD_REQUEST.description(message), message)
