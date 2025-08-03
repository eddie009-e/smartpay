package com.smartpay.repository

import com.smartpay.data.network.ApiClient
import com.smartpay.models.AuditLog
import com.smartpay.models.AuditLogResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class MerchantAuditRepository {

    private val apiService = ApiClient.apiService

    suspend fun getAuditLogs(): Response<AuditLogResponse> = withContext(Dispatchers.IO) {
        apiService.getMerchantAuditLogs()
    }
}