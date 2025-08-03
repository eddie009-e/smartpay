package com.smartpay.repository

import com.smartpay.data.network.ApiClient
import com.smartpay.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class SubAccountRepository {

    private val apiService = ApiClient.apiService

    suspend fun getAllSubAccounts(): Response<SubAccountResponse> = withContext(Dispatchers.IO) {
        apiService.getSubAccounts()
    }

    suspend fun createSubAccount(
        fullName: String,
        phone: String,
        password: String,
        permissions: SubAccountPermissions
    ): Response<SubAccountResponse> = withContext(Dispatchers.IO) {
        val request = CreateSubAccountRequest(
            fullName = fullName,
            phone = phone,
            password = password,
            permissions = permissions
        )
        apiService.createSubAccount(request)
    }

    suspend fun updateSubAccount(
        id: String,
        fullName: String? = null,
        permissions: SubAccountPermissions? = null,
        isActive: Boolean? = null
    ): Response<SubAccountResponse> = withContext(Dispatchers.IO) {
        val request = UpdateSubAccountRequest(
            fullName = fullName,
            permissions = permissions,
            isActive = isActive
        )
        apiService.updateSubAccount(id, request)
    }

    suspend fun deleteSubAccount(id: String): Response<SubAccountResponse> = withContext(Dispatchers.IO) {
        apiService.deleteSubAccount(id)
    }

    suspend fun loginSubAccount(phone: String, password: String): Response<SubAccountLoginResponse> = withContext(Dispatchers.IO) {
        val request = SubAccountLoginRequest(phone = phone, password = password)
        apiService.loginSubAccount(request)
    }
}