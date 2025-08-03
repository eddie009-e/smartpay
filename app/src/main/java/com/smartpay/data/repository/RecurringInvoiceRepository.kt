package com.smartpay.repository

import com.smartpay.data.network.ApiClient
import com.smartpay.models.CreateRecurringInvoiceRequest
import com.smartpay.models.RecurringInvoiceResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class RecurringInvoiceRepository {

    private val apiService = ApiClient.apiService

    suspend fun getRecurringInvoices(): Response<RecurringInvoiceResponse> = withContext(Dispatchers.IO) {
        apiService.getRecurringInvoices()
    }

    suspend fun createRecurringInvoice(request: CreateRecurringInvoiceRequest): Response<RecurringInvoiceResponse> = withContext(Dispatchers.IO) {
        apiService.createRecurringInvoice(request)
    }

    suspend fun cancelRecurringInvoice(id: String): Response<RecurringInvoiceResponse> = withContext(Dispatchers.IO) {
        apiService.cancelRecurringInvoice(id)
    }
}