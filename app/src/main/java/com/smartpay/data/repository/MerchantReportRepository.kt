package com.smartpay.repository

import com.smartpay.data.models.BasicResponse
import com.smartpay.data.network.ApiClient
import com.smartpay.models.MerchantReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class MerchantReportRepository {

    private val apiService = ApiClient.apiService

    suspend fun getMerchantReport(): Response<MerchantReport> = withContext(Dispatchers.IO) {
        apiService.getMerchantReport()
    }

    suspend fun exportReportToPdf(): Response<BasicResponse> = withContext(Dispatchers.IO) {
        apiService.exportReportToPdf()
    }

    suspend fun exportReportToExcel(): Response<BasicResponse> = withContext(Dispatchers.IO) {
        apiService.exportReportToExcel()
    }
}