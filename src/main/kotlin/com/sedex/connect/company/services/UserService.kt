package com.sedex.connect.company.services

import com.sedex.connect.common.OrganisationCode
import com.sedex.connect.common.User
import com.sedex.connect.common.auth.UserCode
import com.sedex.connect.user.api.UserApi

class UserService(private val userApi: UserApi) {
    fun organisationContainsUserWithCode(orgCode: OrganisationCode, userCode: UserCode): Boolean {
        return userApi.getOrganisationContacts(orgCode).contacts
            .map { uc -> UserCode(uc.userCode) }
            .any { it == userCode }
    }

    fun getUserByCode(userCode: UserCode): User? = userApi.getUserBySedexCode(userCode)

    fun getNotNullUserByCode(userCode: UserCode): User = userApi.getNotNullUserBySedexCode(userCode)
}
