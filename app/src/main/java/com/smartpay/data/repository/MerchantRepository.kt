package com.smartpay.data.repository

import com.smartpay.data.models.*
import com.smartpay.models.*
import com.smartpay.data.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class MerchantRepository {

    private val apiService = ApiClient.apiService

    // جلب الرصيد (Wallet)
    suspend fun getWallet(merchantId: String): Response<WalletResponse> = withContext(Dispatchers.IO) {
        apiService.getMerchantWallet(merchantId)
    }

    // جلب المعاملات (Transactions)
    suspend fun getTransactions(merchantId: String): Response<List<MerchantTransaction>> = withContext(Dispatchers.IO) {
        apiService.getMerchantTransactions(merchantId)
    }

    // جلب الفواتير (Invoices)
    suspend fun getInvoices(merchantId: String): Response<List<Invoice>> = withContext(Dispatchers.IO) {
        apiService.getMerchantInvoices(merchantId)
    }

    // إرسال أموال
    suspend fun sendMoney(merchantId: String, request: SendMoneyRequest): Response<BasicResponse> = withContext(Dispatchers.IO) {
        apiService.merchantSendMoney(merchantId, request)
    }
    
    // طلبات التاجر
    suspend fun createRequest(request: MerchantRequestDto): Response<BasicResponse> = withContext(Dispatchers.IO) {
        apiService.createMerchantRequest(request)
    }
    
    suspend fun getRequests(): Response<List<MerchantRequestDto>> = withContext(Dispatchers.IO) {
        apiService.getMerchantRequests()
    }
    
    // طلبات الرواتب
    suspend fun getSalaryPayments(businessId: String): Response<List<SalaryPayment>> = withContext(Dispatchers.IO) {
        apiService.getSalaryPayments(businessId)
    }
    
    suspend fun createSalaryPayment(request: SalaryPaymentRequest): Response<BasicResponse> = withContext(Dispatchers.IO) {
        apiService.createSalaryPayment(request)
    }
}
