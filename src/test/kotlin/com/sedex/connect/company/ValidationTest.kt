package com.sedex.connect.company

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import com.sedex.connect.common.Address
import com.sedex.connect.common.CountryCode
import com.sedex.connect.common.EmailAddress
import com.sedex.connect.common.TelephoneNumber
import com.sedex.connect.common.VatNumber
import com.sedex.connect.common.letters
import com.sedex.connect.common.numbers
import com.sedex.connect.common.randomString
import com.sedex.connect.company.api.Company
import com.sedex.connect.company.api.CompanySize
import com.sedex.connect.company.api.MembershipStatus
import com.sedex.connect.company.api.MembershipStatus.ACTIVE
import io.konform.validation.Invalid
import io.konform.validation.Valid
import io.konform.validation.ValidationResult
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

class ValidationTest {

    @ParameterizedTest
    @MethodSource("negativeResultTests")
    fun passesValidation(test: ValidationTest) {
        val result = validateCompany(test.company)

        result.expectInvalid()
        assertThat(result.errors, hasSize(equalTo(1)))
        val error = result.errors[0]
        assertThat(error.dataPath, containsSubstring(test.messageContains))
    }

    @ParameterizedTest
    @MethodSource("positiveResultTests")
    fun passesValidation(testCompany: TestCompany) {
        val result = validateCompany(testCompany)

        result.expectValid()
    }

    @ParameterizedTest
    @MethodSource("vatNumberTests")
    fun testVatNumbers(test: VatNumberValidationTest) {
        val company = fullCompany.copy(
            vatNumber = test.vatNumber?.let { VatNumber(it) },
            address = fullCompany.address.copy(country = CountryCode(test.countryCode)),
            billingAddress = test.billingCountryCode?.let { fullCompany.billingAddress!!.copy(country = CountryCode(test.billingCountryCode)) },
            businessLicenseNumber = null
        )
        val result = validateCompany(company)
        if (test.valid) {
            result.expectValid()
        } else {
            result.expectInvalid()
        }
    }

    companion object {

        @JvmStatic
        private fun negativeResultTests() = listOf(
            ValidationTest(fullCompany.copy(address = fullCompany.address.copy(line1 = null)), "address.line1"),
            ValidationTest(fullCompany.copy(address = fullCompany.address.copy(line1 = "  ")), "address.line1"),
            ValidationTest(fullCompany.copy(address = fullCompany.address.copy(line1 = randomString(minLength = 1001, maxLength = 1005))), "address.line1"),
            ValidationTest(fullCompany.copy(address = fullCompany.address.copy(line2 = randomString(minLength = 1001, maxLength = 1005))), "address.line2"),
            ValidationTest(fullCompany.copy(address = fullCompany.address.copy(line2 = "   ")), "address.line2"),
            ValidationTest(fullCompany.copy(address = fullCompany.address.copy(city = null)), "address.city"),
            ValidationTest(fullCompany.copy(address = fullCompany.address.copy(city = "  ")), "address.city"),
            ValidationTest(fullCompany.copy(address = fullCompany.address.copy(city = randomString(minLength = 1001, maxLength = 1005))), "address.city"),
            ValidationTest(fullCompany.copy(address = fullCompany.address.copy(postCode = null)), "address.postCode"),
            ValidationTest(fullCompany.copy(address = fullCompany.address.copy(postCode = "  ")), "address.postCode"),
            ValidationTest(fullCompany.copy(address = fullCompany.address.copy(postCode = randomString(minLength = 1001, maxLength = 1005))), "address.postCode"),
            ValidationTest(fullCompany.copy(address = fullCompany.address.copy(country = null)), "address.country"),
            ValidationTest(fullCompany.copy(email = EmailAddress("not-an-email")), "email"),
            ValidationTest(fullCompany.copy(englishName = "Jupiter Mining Corporation !£$%~#"), "englishName"),
            ValidationTest(fullCompany.copy(localName = "   "), "localName"),
            ValidationTest(fullCompany.copy(localName = randomString(minLength = 1001, maxLength = 1005)), "localName"),
            ValidationTest(fullCompany.copy(businessLicenseNumber = "   "), "businessLicenseNumber"),
            ValidationTest(fullCompany.copy(businessLicenseNumber = randomString(minLength = 1001, maxLength = 1005)), "businessLicenseNumber"),
            ValidationTest(fullCompany.copy(billingAddress = fullCompany.billingAddress!!.copy(line1 = " ")), "illingAddress.line1"),
            ValidationTest(fullCompany.copy(billingAddress = fullCompany.billingAddress.copy(line1 = randomString(minLength = 1001, maxLength = 1005))), "billingAddress.line1"),
            ValidationTest(fullCompany.copy(billingAddress = fullCompany.billingAddress.copy(line1 = null)), "billingAddress.line1"),
            ValidationTest(fullCompany.copy(billingAddress = fullCompany.billingAddress.copy(line2 = randomString(minLength = 1001, maxLength = 1005))), "billingAddress.line2"),
            ValidationTest(fullCompany.copy(billingAddress = fullCompany.billingAddress.copy(city = " ")), "billingAddress.city"),
            ValidationTest(fullCompany.copy(billingAddress = fullCompany.billingAddress.copy(city = randomString(minLength = 1001, maxLength = 1005))), "billingAddress.city"),
            ValidationTest(fullCompany.copy(billingAddress = fullCompany.billingAddress.copy(city = null)), "billingAddress.city"),
            ValidationTest(fullCompany.copy(billingAddress = fullCompany.billingAddress.copy(postCode = " ")), "billingAddress.postCode"),
            ValidationTest(fullCompany.copy(billingAddress = fullCompany.billingAddress.copy(postCode = randomString(minLength = 1001, maxLength = 1005))), "billingAddress.postCode"),
            ValidationTest(fullCompany.copy(billingAddress = fullCompany.billingAddress.copy(postCode = null)), "billingAddress.postCode"),
            ValidationTest(fullCompany.copy(billingAddress = fullCompany.billingAddress.copy(country = null)), "billingAddress.country"),
            ValidationTest(fullCompany.copy(primaryIndustryClassificationCode = null), ""),
            ValidationTest(fullCompany.copy(primaryIndustryClassificationCode = " "), "primaryIndustryClassificationCode"),
            ValidationTest(fullCompany.copy(secondaryIndustryClassificationCode = null), ""),
            ValidationTest(fullCompany.copy(secondaryIndustryClassificationCode = " "), "secondaryIndustryClassificationCode"),
            ValidationTest(fullCompany.copy(smdEnrollStatus = " "), "smdEnrollStatus"),
            ValidationTest(fullCompany.copy(englishName = randomString(minLength = 1001, maxLength = 1005, symbolSet = letters)), "englishName"),
            ValidationTest(fullCompany.copy(telephone = TelephoneNumber(randomString(minLength = 1001, maxLength = 1005, symbolSet = numbers))), "telephone"),
            ValidationTest(fullCompany.copy(email = EmailAddress("${randomString(minLength = 1001, maxLength = 1005, symbolSet = letters)}@gmail.com")), "email"),
            ValidationTest(fullCompany.copy(primaryIndustryClassificationCode = randomString(minLength = 1001, maxLength = 1005)), "primaryIndustryClassificationCode"),
            ValidationTest(fullCompany.copy(secondaryIndustryClassificationCode = randomString(minLength = 1001, maxLength = 1005)), "secondaryIndustryClassificationCode"),
        )

        @JvmStatic
        private fun positiveResultTests() = listOf(
            fullCompany,
            fullCompany.copy(businessLicenseExpiration = LocalDate.now()),
            fullCompany.copy(businessLicenseExpiration = null),
            fullCompany.copy(businessLicenseExpiration = LocalDate.now().minusDays(1)),
            fullCompany.copy(englishName = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789()'&-."),
        )

        @JvmStatic
        private fun vatNumberTests() = listOf(
            VatNumberValidationTest(countryCode = "BE", billingCountryCode = "AT", vatNumber = "ATU12345678", valid = true),
            VatNumberValidationTest(countryCode = "AT", billingCountryCode = "AT", vatNumber = "AT123456789", valid = false),
            VatNumberValidationTest(countryCode = "AT", billingCountryCode = "AT", vatNumber = "AT1234567890", valid = false),
            VatNumberValidationTest(countryCode = "AT", billingCountryCode = null, vatNumber = "AT1234567890", valid = false),
            VatNumberValidationTest(countryCode = "BE", billingCountryCode = "BE", vatNumber = "BE1234567890", valid = true),
            VatNumberValidationTest(countryCode = "AT", billingCountryCode = "BE", vatNumber = "BE0123456890", valid = true),
            VatNumberValidationTest(countryCode = "BE", billingCountryCode = null, vatNumber = "BE0123456890", valid = true),
            VatNumberValidationTest(countryCode = "BE", billingCountryCode = "BE", vatNumber = "BE123456789", valid = false),
            VatNumberValidationTest(countryCode = "BE", billingCountryCode = "BE", vatNumber = "BE123456789022", valid = false),
            VatNumberValidationTest(countryCode = "BE", billingCountryCode = "BG", vatNumber = "BG123456789", valid = true),
            VatNumberValidationTest(countryCode = "AT", billingCountryCode = "BG", vatNumber = "BG1234567890", valid = true),
            VatNumberValidationTest(countryCode = "BG", billingCountryCode = "BG", vatNumber = "BG12345678", valid = false),
            VatNumberValidationTest(countryCode = "BE", billingCountryCode = "BG", vatNumber = "BG12345678901", valid = false),
            VatNumberValidationTest(countryCode = "BE", billingCountryCode = "CY", vatNumber = "CY12345678C", valid = true),
            VatNumberValidationTest(countryCode = "CY", billingCountryCode = "CY", vatNumber = "CY12345678X", valid = true),
            VatNumberValidationTest(countryCode = "CY", billingCountryCode = null, vatNumber = "CY123456789", valid = false),
            VatNumberValidationTest(countryCode = "BE", billingCountryCode = "CY", vatNumber = "CY2123456789X", valid = false),
            VatNumberValidationTest(countryCode = "BE", billingCountryCode = "CZ", vatNumber = "CZ12345678", valid = true),
            VatNumberValidationTest(countryCode = "CZ", billingCountryCode = "CZ", vatNumber = "CZ123456789", valid = true),
            VatNumberValidationTest(countryCode = "CZ", billingCountryCode = null, vatNumber = "CZ1234567890", valid = true),
            VatNumberValidationTest(countryCode = "CZ", billingCountryCode = "CZ", vatNumber = "CZ1234567", valid = false),
            VatNumberValidationTest(countryCode = "CZ", billingCountryCode = null, vatNumber = "CZ12345678901", valid = false),
            VatNumberValidationTest(countryCode = "DE", billingCountryCode = "DE", vatNumber = "DE123456789", valid = true),
            VatNumberValidationTest(countryCode = "EL", billingCountryCode = "DE", vatNumber = "DE12345678", valid = false),
            VatNumberValidationTest(countryCode = "DE", billingCountryCode = null, vatNumber = "DE1234567890", valid = false),
            VatNumberValidationTest(countryCode = "DE", billingCountryCode = "HR", vatNumber = "HR12345678901", valid = true),
            VatNumberValidationTest(countryCode = "DE", billingCountryCode = "HR", vatNumber = "HR1234567890", valid = false),
            VatNumberValidationTest(countryCode = "HR", billingCountryCode = "HR", vatNumber = "HR123456789012", valid = false),
            VatNumberValidationTest(countryCode = "DK", billingCountryCode = "DK", vatNumber = "DK12345678", valid = true),
            VatNumberValidationTest(countryCode = "DE", billingCountryCode = "DK", vatNumber = "DK1234567", valid = false),
            VatNumberValidationTest(countryCode = "DK", billingCountryCode = "DK", vatNumber = "DK123456789", valid = false),
            VatNumberValidationTest(countryCode = "EE", billingCountryCode = "EE", vatNumber = "EE123456789", valid = true),
            VatNumberValidationTest(countryCode = "EE", billingCountryCode = "EE", vatNumber = "EE12345678", valid = false),
            VatNumberValidationTest(countryCode = "ES", billingCountryCode = "EE", vatNumber = "EE1234567890", valid = false),
            VatNumberValidationTest(countryCode = "EL", billingCountryCode = "EL", vatNumber = "EL123456789", valid = true),
            VatNumberValidationTest(countryCode = "EL", billingCountryCode = null, vatNumber = "EL12345678", valid = false),
            VatNumberValidationTest(countryCode = "EE", billingCountryCode = "EL", vatNumber = "EL1234567890", valid = false),
            VatNumberValidationTest(countryCode = "ES", billingCountryCode = "ES", vatNumber = "ESX12345678", valid = true),
            VatNumberValidationTest(countryCode = "ES", billingCountryCode = null, vatNumber = "ES12345678X", valid = true),
            VatNumberValidationTest(countryCode = "EL", billingCountryCode = "ES", vatNumber = "ESX1234567X", valid = true),
            VatNumberValidationTest(countryCode = "ES", billingCountryCode = "ES", vatNumber = "ES123456789", valid = false),
            VatNumberValidationTest(countryCode = "DK", billingCountryCode = "ES", vatNumber = "ESX1234567", valid = false),
            VatNumberValidationTest(countryCode = "ES", billingCountryCode = "ES", vatNumber = "ESX123456789", valid = false),
            VatNumberValidationTest(countryCode = "ES", billingCountryCode = "FI", vatNumber = "FI12345678", valid = true),
            VatNumberValidationTest(countryCode = "ES", billingCountryCode = "FI", vatNumber = "FI1234567", valid = false),
            VatNumberValidationTest(countryCode = "FI", billingCountryCode = "FI", vatNumber = "FI123456789", valid = false),
            VatNumberValidationTest(countryCode = "FR", billingCountryCode = "FR", vatNumber = "FR12345678901", valid = true),
            VatNumberValidationTest(countryCode = "ES", billingCountryCode = "FR", vatNumber = "FRX1234567890", valid = true),
            VatNumberValidationTest(countryCode = "FR", billingCountryCode = null, vatNumber = "FR1X123456789", valid = true),
            VatNumberValidationTest(countryCode = "ES", billingCountryCode = "FR", vatNumber = "FRXX123456789", valid = true),
            VatNumberValidationTest(countryCode = "FR", billingCountryCode = "FR", vatNumber = "FR1234567890", valid = false),
            VatNumberValidationTest(countryCode = "ES", billingCountryCode = "FR", vatNumber = "FR123456789012", valid = false),
            VatNumberValidationTest(countryCode = "FR", billingCountryCode = "FR", vatNumber = "FR1XX123456789", valid = false),
            VatNumberValidationTest(countryCode = "ES", billingCountryCode = "FR", vatNumber = "FRXXX23456789", valid = false),
            VatNumberValidationTest(countryCode = "ES", billingCountryCode = "HU", vatNumber = "HU12345678", valid = true),
            VatNumberValidationTest(countryCode = "HU", billingCountryCode = "HU", vatNumber = "HU1234567", valid = false),
            VatNumberValidationTest(countryCode = "FR", billingCountryCode = "HU", vatNumber = "HU123456789", valid = false),
            VatNumberValidationTest(countryCode = "IE", billingCountryCode = "IE", vatNumber = "IE1234567X", valid = true),
            VatNumberValidationTest(countryCode = "IE", billingCountryCode = null, vatNumber = "IE1X23456X", valid = true),
            VatNumberValidationTest(countryCode = "FR", billingCountryCode = "IE", vatNumber = "IE1234567XX", valid = true),
            VatNumberValidationTest(countryCode = "IE", billingCountryCode = "IE", vatNumber = "IE123456789", valid = false),
            VatNumberValidationTest(countryCode = "IE", billingCountryCode = "IT", vatNumber = "IT12345678901", valid = true),
            VatNumberValidationTest(countryCode = "IE", billingCountryCode = "IT", vatNumber = "IT1234567890", valid = false),
            VatNumberValidationTest(countryCode = "IT", billingCountryCode = "IT", vatNumber = "IT123456789012", valid = false),
            VatNumberValidationTest(countryCode = "LT", billingCountryCode = null, vatNumber = "LT123456789", valid = true),
            VatNumberValidationTest(countryCode = "LT", billingCountryCode = "LT", vatNumber = "LT123456789012", valid = true),
            VatNumberValidationTest(countryCode = "LT", billingCountryCode = "LT", vatNumber = "LT12345678", valid = false),
            VatNumberValidationTest(countryCode = "IE", billingCountryCode = "LT", vatNumber = "LT1234567890", valid = false),
            VatNumberValidationTest(countryCode = "LT", billingCountryCode = "LT", vatNumber = "LT12345678901", valid = false),
            VatNumberValidationTest(countryCode = "LT", billingCountryCode = "LU", vatNumber = "LU12345678", valid = true),
            VatNumberValidationTest(countryCode = "LU", billingCountryCode = "LU", vatNumber = "LU1234567", valid = false),
            VatNumberValidationTest(countryCode = "LU", billingCountryCode = null, vatNumber = "LU123456789", valid = false),
            VatNumberValidationTest(countryCode = "LU", billingCountryCode = "LV", vatNumber = "LV12345678901", valid = true),
            VatNumberValidationTest(countryCode = "LV", billingCountryCode = "LV", vatNumber = "LV1234567890", valid = false),
            VatNumberValidationTest(countryCode = "LV", billingCountryCode = "LV", vatNumber = "LV123456789012", valid = false),
            VatNumberValidationTest(countryCode = "MT", billingCountryCode = "MT", vatNumber = "MT12345678", valid = true),
            VatNumberValidationTest(countryCode = "MT", billingCountryCode = "MT", vatNumber = "MT1234567", valid = false),
            VatNumberValidationTest(countryCode = "MT", billingCountryCode = "MT", vatNumber = "MT123456789", valid = false),
            VatNumberValidationTest(countryCode = "MT", billingCountryCode = "NL", vatNumber = "NL123456789B01", valid = true),
            VatNumberValidationTest(countryCode = "NL", billingCountryCode = "NL", vatNumber = "NL123456789B02", valid = true),
            VatNumberValidationTest(countryCode = "NL", billingCountryCode = "NL", vatNumber = "NL123456789A01", valid = false),
            VatNumberValidationTest(countryCode = "NL", billingCountryCode = null, vatNumber = "NL123456789101", valid = false),
            VatNumberValidationTest(countryCode = "NL", billingCountryCode = "NL", vatNumber = "NL123456789B0", valid = false),
            VatNumberValidationTest(countryCode = "LT", billingCountryCode = "NL", vatNumber = "NL123456789B012", valid = false),
            VatNumberValidationTest(countryCode = "PL", billingCountryCode = "PL", vatNumber = "PL1234567890", valid = true),
            VatNumberValidationTest(countryCode = "PL", billingCountryCode = "PL", vatNumber = "PL123456789", valid = false),
            VatNumberValidationTest(countryCode = "LT", billingCountryCode = "PL", vatNumber = "PL12345678901", valid = false),
            VatNumberValidationTest(countryCode = "PL", billingCountryCode = "PT", vatNumber = "PT123456789", valid = true),
            VatNumberValidationTest(countryCode = "PT", billingCountryCode = "PT", vatNumber = "PT12345678", valid = false),
            VatNumberValidationTest(countryCode = "PT", billingCountryCode = null, vatNumber = "PT1234567890", valid = false),
            VatNumberValidationTest(countryCode = "RO", billingCountryCode = "RO", vatNumber = "RO1234567890", valid = true),
            VatNumberValidationTest(countryCode = "NL", billingCountryCode = "RO", vatNumber = "RO12", valid = true),
            VatNumberValidationTest(countryCode = "RO", billingCountryCode = "RO", vatNumber = "RO123", valid = true),
            VatNumberValidationTest(countryCode = "RO", billingCountryCode = "RO", vatNumber = "RO123456", valid = true),
            VatNumberValidationTest(countryCode = "MT", billingCountryCode = "RO", vatNumber = "RO1", valid = false),
            VatNumberValidationTest(countryCode = "RO", billingCountryCode = null, vatNumber = "RO123456789012", valid = false),
            VatNumberValidationTest(countryCode = "RO", billingCountryCode = "SE", vatNumber = "SE123456789012", valid = true),
            VatNumberValidationTest(countryCode = "SE", billingCountryCode = null, vatNumber = "SE12345678901", valid = false),
            VatNumberValidationTest(countryCode = "SE", billingCountryCode = "SE", vatNumber = "SE1234567890123", valid = false),
            VatNumberValidationTest(countryCode = "SE", billingCountryCode = "SI", vatNumber = "SI12345678", valid = true),
            VatNumberValidationTest(countryCode = "SI", billingCountryCode = "SI", vatNumber = "SI1234567", valid = false),
            VatNumberValidationTest(countryCode = "SI", billingCountryCode = "SI", vatNumber = "SI123456789", valid = false),
            VatNumberValidationTest(countryCode = "SK", billingCountryCode = "SK", vatNumber = "SK1234567890", valid = true),
            VatNumberValidationTest(countryCode = "SK", billingCountryCode = null, vatNumber = "SK123456789", valid = false),
            VatNumberValidationTest(countryCode = "SI", billingCountryCode = "SK", vatNumber = "SK12345678901", valid = false),
            VatNumberValidationTest(countryCode = "SK", billingCountryCode = null, vatNumber = "", valid = false),
            VatNumberValidationTest(countryCode = "SI", billingCountryCode = "IN", vatNumber = "ID111111111", valid = false),
            VatNumberValidationTest(countryCode = "IN", billingCountryCode = "IN", vatNumber = "ID111111111", valid = false),
            VatNumberValidationTest(countryCode = "SI", billingCountryCode = "IN", vatNumber = null, valid = true),
            VatNumberValidationTest(countryCode = "IN", billingCountryCode = null, vatNumber = null, valid = true),
            VatNumberValidationTest(countryCode = "MK", billingCountryCode = null, vatNumber = "MK12345622", valid = true),
            VatNumberValidationTest(countryCode = "MK", billingCountryCode = null, vatNumber = "MK11-33.eee234/5", valid = true),
            VatNumberValidationTest(countryCode = "MK", billingCountryCode = null, vatNumber = "MK11-33 eee234/5", valid = true),
            VatNumberValidationTest(countryCode = "MK", billingCountryCode = null, vatNumber = "MK11-33 eee234/5 **", valid = false),
            VatNumberValidationTest(countryCode = "MK", billingCountryCode = null, vatNumber = "MK11-33 eee234/5 %\$", valid = false),
            VatNumberValidationTest(countryCode = "MK", billingCountryCode = null, vatNumber = "MK11-33 eee234/5 ?\\^)", valid = false),
        )

        private val fullCompany = TestCompany(
            englishName = "The Jupiter Mining Corporation",
            localName = "Le Jupiter Mining Corporation",
            businessLicenseNumber = "12345678910",
            businessLicenseExpiration = LocalDate.parse("2099-01-01"),
            primaryIndustryClassificationCode = "ICRD001",
            secondaryIndustryClassificationCode = "ICRD002",
            address = Address(
                line1 = "JMC",
                line2 = "Some place",
                line3 = "Somewhere",
                line4 = "Line 4",
                postCode = "AB12 3CD",
                city = "London",
                country = CountryCode("GB")
            ),
            telephone = TelephoneNumber("+449876543210"),
            email = EmailAddress("frank.hollister@example.com"),
            billingAddress = Address(
                line1 = "Line 1",
                line2 = "Some other place",
                line3 = "Somewhere",
                line4 = "Line 4",
                postCode = "EF45 6GH",
                city = "London",
                country = CountryCode("GB")
            ),
            vatNumber = VatNumber("GB123456789"),
            smdEnrollStatus = "OPTED_IN",
            membershipStatus = ACTIVE,
            subscriptionType = "ST002",
            companySize = null
        )
    }

    data class ValidationTest(
        val company: Company,
        val messageContains: String,
    )

    data class VatNumberValidationTest(
        val countryCode: String,
        val billingCountryCode: String?,
        val vatNumber: String?,
        val valid: Boolean,
    )
}

private fun <T> ValidationResult<T>.expectValid() {
    if (this !is Valid) {
        fail("Expected Valid but got $this")
    }
}

private fun <T> ValidationResult<T>.expectInvalid() {
    if (this !is Invalid) {
        fail("Expected Invalid but got $this")
    }
}

data class TestCompany(
    override val englishName: String,
    override val localName: String? = null,
    override val businessLicenseNumber: String? = null,
    override val businessLicenseExpiration: LocalDate? = null,
    override val address: Address,
    override val telephone: TelephoneNumber?,
    override val email: EmailAddress,
    override val billingAddress: Address? = null,
    override val vatNumber: VatNumber? = null,
    override val smdEnrollStatus: String?,
    override val membershipStatus: MembershipStatus?,
    override val subscriptionType: String?,
    override val primaryIndustryClassificationCode: String?,
    override val secondaryIndustryClassificationCode: String?,
    override val companySize: CompanySize?,
) : Company
