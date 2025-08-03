package com.smartpay.data.repository

import com.smartpay.data.models.*
import com.smartpay.models.*
import com.smartpay.data.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class UserRepository {

    private val apiService = ApiClient.apiService

    // --- الرصيد (Wallet) ---
    suspend fun getWalletBalance(): Response<WalletResponse> = withContext(Dispatchers.IO) {
        apiService.getWalletBalance()
    }

    suspend fun topUp(amount: Long): Response<BasicResponse> = withContext(Dispatchers.IO) {
        apiService.topUp(WalletActionRequest(amount))
    }

    suspend fun withdraw(amount: Long): Response<BasicResponse> = withContext(Dispatchers.IO) {
        apiService.withdraw(WalletActionRequest(amount))
    }

    // --- PIN ---
    suspend fun setPin(pin: String): Response<BasicResponse> = withContext(Dispatchers.IO) {
        val token = ApiClient.getAuthToken() ?: ""
        apiService.setPin(token, PinRequest(pin))
    }

    // --- Transfer Money ---
    suspend fun transferMoney(request: TransferRequest): Response<BasicResponse> = withContext(Dispatchers.IO) {
        apiService.transferMoney(request)
    }
    
    // --- Transaction History ---
    suspend fun getTransactionHistory(): Response<List<Transaction>> = withContext(Dispatchers.IO) {
        apiService.getTransactionHistory()
    }

    // --- طلب أموال ---
    suspend fun sendMoneyRequest(toPhone: String, amount: Long): Response<BasicResponse> = withContext(Dispatchers.IO) {
        val request = mapOf("toPhone" to toPhone, "amount" to amount)
        apiService.sendMoneyRequest(request)
    }

    // --- الفواتير ---
    suspend fun getInvoices(): Response<List<Invoice>> = withContext(Dispatchers.IO) {
        apiService.getInvoices()
    }

    suspend fun createInvoice(request: InvoiceRequest): Response<BasicResponse> = withContext(Dispatchers.IO) {
        apiService.createInvoice(request)
    }

    // --- الطلبات الواردة ---
    suspend fun getIncomingRequests(): Response<List<IncomingRequestResponse>> = withContext(Dispatchers.IO) {
        apiService.getIncomingRequests()
    }

    suspend fun acceptRequest(requestId: String): Response<BasicResponse> = withContext(Dispatchers.IO) {
        apiService.acceptRequest(requestId)
    }

    suspend fun rejectRequest(requestId: String): Response<BasicResponse> = withContext(Dispatchers.IO) {
        apiService.rejectRequest(requestId)
    }
}
