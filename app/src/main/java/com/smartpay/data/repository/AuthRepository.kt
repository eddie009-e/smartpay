package com.smartpay.data.repository

import com.smartpay.data.models.*
import com.smartpay.models.*
import com.smartpay.data.network.ApiClient
import com.smartpay.data.network.ApiService
import retrofit2.Response

class AuthRepository {

    private val apiService: ApiService = ApiClient.apiService

    // =======================
    //  Auth - Users
    // =======================
    suspend fun registerUser(request: RegisterRequest): Response<AuthResponse> {
        return apiService.registerUser(request)
    }

    suspend fun loginUser(request: LoginRequest): Response<AuthResponse> {
        val response = apiService.loginUser(request)
        // لو نجح الدخول، نخزن التوكن تلقائياً
        if (response.isSuccessful) {
            response.body()?.token?.let { ApiClient.setAuthToken(it) }
        }
        return response
    }

    suspend fun setPin(pinRequest: PinRequest): Response<BasicResponse> {
        return apiService.setPin(ApiClientToken(), pinRequest)
    }

    // =======================
    // Wallet - Users
    // =======================
    suspend fun getWalletBalance(): Response<WalletResponse> {
        return apiService.getWalletBalance()
    }

    suspend fun topUp(amount: Long): Response<BasicResponse> {
        return apiService.topUp(WalletActionRequest(amount))
    }

    suspend fun withdraw(amount: Long): Response<BasicResponse> {
        return apiService.withdraw(WalletActionRequest(amount))
    }

    // =======================
    // Invoices - Users
    // =======================
    suspend fun getInvoices(): Response<List<Invoice>> {
        return apiService.getInvoices()
    }

    suspend fun createInvoice(request: InvoiceRequest): Response<BasicResponse> {
        return apiService.createInvoice(request)
    }

    // =======================
    // Transfer Money - Users
    // =======================
    suspend fun transferMoney(request: TransferRequest): Response<BasicResponse> {
        return apiService.transferMoney(request)
    }
    
    // =======================
    // Transaction History - Users
    // =======================
    suspend fun getTransactionHistory(): Response<List<Transaction>> {
        return apiService.getTransactionHistory()
    }

    suspend fun getIncomingRequests(): Response<List<IncomingRequestResponse>> {
        return apiService.getIncomingRequests()
    }

    suspend fun acceptRequest(requestId: String): Response<BasicResponse> {
        return apiService.acceptRequest(requestId)
    }

    suspend fun rejectRequest(requestId: String): Response<BasicResponse> {
        return apiService.rejectRequest(requestId)
    }

    // =======================
    // Auth - Merchant
    // =======================
    suspend fun registerBusiness(request: RegisterBusinessRequest): Response<AuthResponse> {
        return apiService.registerBusiness(request)
    }

    suspend fun loginBusiness(request: LoginRequest): Response<AuthResponse> {
        val response = apiService.loginBusiness(request)
        if (response.isSuccessful) {
            response.body()?.token?.let { ApiClient.setAuthToken(it) }
        }
        return response
    }

    // =======================
    // Merchant APIs
    // =======================
    suspend fun getMerchantWallet(merchantId: String): Response<WalletResponse> {
        return apiService.getMerchantWallet(merchantId)
    }

    suspend fun getMerchantTransactions(merchantId: String): Response<List<MerchantTransaction>> {
        return apiService.getMerchantTransactions(merchantId)
    }

    suspend fun getMerchantInvoices(merchantId: String): Response<List<Invoice>> {
        return apiService.getMerchantInvoices(merchantId)
    }

    suspend fun merchantSendMoney(merchantId: String, request: SendMoneyRequest): Response<BasicResponse> {
        return apiService.merchantSendMoney(merchantId, request)
    }
    
    // =======================
    // Merchant Requests
    // =======================
    suspend fun createMerchantRequest(request: MerchantRequestDto): Response<BasicResponse> {
        return apiService.createMerchantRequest(request)
    }
    
    suspend fun getMerchantRequests(): Response<List<MerchantRequestDto>> {
        return apiService.getMerchantRequests()
    }

    // =======================
    // Token Helper
    // =======================
    private fun ApiClientToken(): String {
        return ApiClient.getAuthToken() ?: ""
    }
}
