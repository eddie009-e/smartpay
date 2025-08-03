package com.smartpay.repository

import com.smartpay.data.models.BasicResponse
import com.smartpay.data.network.ApiClient
import com.smartpay.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class InvoiceRepository {

    private val apiService = ApiClient.apiService

    // جلب الفواتير للتاجر
    suspend fun getMerchantInvoices(businessId: String): Response<List<InvoiceModel>> = withContext(Dispatchers.IO) {
        apiService.getMerchantInvoices(businessId)
    }

    // جلب الفواتير للمستخدم
    suspend fun getUserInvoices(): Response<List<InvoiceModel>> = withContext(Dispatchers.IO) {
        apiService.getUserInvoices()
    }

    // إنشاء فاتورة جديدة
    suspend fun createInvoice(request: CreateInvoiceRequest): Response<BasicResponse> = withContext(Dispatchers.IO) {
        apiService.createInvoice(request)
    }

    // تحديث حالة الفاتورة
    suspend fun updateInvoiceStatus(invoiceId: String, request: UpdateInvoiceStatusRequest): Response<BasicResponse> = withContext(Dispatchers.IO) {
        apiService.updateInvoiceStatus(invoiceId, request)
    }

    // دفع فاتورة
    suspend fun payInvoice(invoiceId: String): Response<BasicResponse> = withContext(Dispatchers.IO) {
        apiService.payInvoice(invoiceId)
    }
}