package com.smartpay.data.network

import com.smartpay.data.models.*
import com.smartpay.models.*
import com.smartpay.android.crash.CrashReportRequest
import com.smartpay.android.crash.CrashReportResponse
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // --- Auth (Users) ---
    @POST("auth/register")
    suspend fun registerUser(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun loginUser(@Body request: LoginRequest): Response<AuthResponse>

    // --- PIN ---
    @POST("auth/set-pin")
    suspend fun setPin(
        @Header("Authorization") token: String,
        @Body request: PinRequest
    ): Response<BasicResponse>

    // --- Wallet (Users) ---
    @GET("wallet/balance")
    suspend fun getWalletBalance(): Response<WalletResponse>

    @POST("auth/wallet/topup")
    suspend fun topUp(@Body request: WalletActionRequest): Response<BasicResponse>

    @POST("auth/wallet/withdraw")
    suspend fun withdraw(@Body request: WalletActionRequest): Response<BasicResponse>

    // --- Invoices (Users) ---
    @GET("auth/invoices")
    suspend fun getInvoices(): Response<List<Invoice>>

    @POST("auth/invoices")
    suspend fun createInvoice(@Body request: InvoiceRequest): Response<BasicResponse>


    // --- Auth (Business/Merchant) ---
    @POST("merchant/register")
    suspend fun registerBusiness(@Body request: RegisterBusinessRequest): Response<AuthResponse>

    @POST("merchant/login")
    suspend fun loginBusiness(@Body request: LoginRequest): Response<AuthResponse>


    // ========== Merchant APIs ==========
    @GET("merchant/{merchantId}/wallet")
    suspend fun getMerchantWallet(
        @Path("merchantId") merchantId: String
    ): Response<WalletResponse>

    @GET("merchant/{merchantId}/transactions")
    suspend fun getMerchantTransactions(
        @Path("merchantId") merchantId: String
    ): Response<List<MerchantTransaction>>

    @GET("merchant/{merchantId}/invoices")
    suspend fun getMerchantInvoices(
        @Path("merchantId") merchantId: String
    ): Response<List<Invoice>>

    @POST("merchant/{merchantId}/send-money")
    suspend fun merchantSendMoney(
        @Path("merchantId") merchantId: String,
        @Body request: SendMoneyRequest
    ): Response<BasicResponse>
    
    // --- Merchant Requests ---
    @POST("merchant/request")
    suspend fun createMerchantRequest(@Body request: MerchantRequestDto): Response<BasicResponse>
    
    @GET("merchant/requests")
    suspend fun getMerchantRequests(): Response<List<MerchantRequestDto>>
    
    // --- Salary Payments ---
    @GET("merchant/{businessId}/salary-payments")
    suspend fun getSalaryPayments(
        @Path("businessId") businessId: String
    ): Response<List<SalaryPayment>>
    
    @POST("merchant/salary-payments")
    suspend fun createSalaryPayment(@Body request: SalaryPaymentRequest): Response<BasicResponse>
    
    // --- Invoice Management ---
    @GET("invoices/merchant/{businessId}")
    suspend fun getMerchantInvoices(@Path("businessId") businessId: String): Response<List<InvoiceModel>>
    
    @GET("invoices/user")
    suspend fun getUserInvoices(): Response<List<InvoiceModel>>
    
    @POST("invoices")
    suspend fun createInvoice(@Body request: CreateInvoiceRequest): Response<BasicResponse>
    
    @PUT("invoices/{invoiceId}/status")
    suspend fun updateInvoiceStatus(
        @Path("invoiceId") invoiceId: String,
        @Body request: UpdateInvoiceStatusRequest
    ): Response<BasicResponse>
    
    @POST("invoices/{invoiceId}/pay")
    suspend fun payInvoice(@Path("invoiceId") invoiceId: String): Response<BasicResponse>


    // --- Transfer Money (Users) ---
    @POST("transfer")
    suspend fun transferMoney(@Body request: TransferRequest): Response<BasicResponse>
    
    // --- Transaction History (Users) ---
    @GET("transactions/history")
    suspend fun getTransactionHistory(): Response<List<Transaction>>

    @POST("requests")
    suspend fun sendMoneyRequest(@Body request: Map<String, Any>): Response<BasicResponse>

    @GET("requests")
    suspend fun getIncomingRequests(): Response<List<IncomingRequestResponse>>

    @POST("requests/{id}/accept")
    suspend fun acceptRequest(@Path("id") requestId: String): Response<BasicResponse>

    @POST("requests/{id}/reject")
    suspend fun rejectRequest(@Path("id") requestId: String): Response<BasicResponse>
    
    // --- Merchant Reports ---
    @GET("merchant/report")
    suspend fun getMerchantReport(): Response<MerchantReport>
    
    @GET("merchant/export/pdf")
    suspend fun exportReportToPdf(): Response<BasicResponse>
    
    @GET("merchant/export/excel")
    suspend fun exportReportToExcel(): Response<BasicResponse>
    
    // --- Merchant Subscriptions ---
    @GET("merchant/subscription")
    suspend fun getCurrentSubscription(): Response<SubscriptionResponse>
    
    @POST("merchant/subscription")
    suspend fun subscribeToPlan(@Body request: SubscriptionRequest): Response<SubscriptionResponse>
    
    @HTTP(method = "PATCH", path = "merchant/subscription")
    suspend fun cancelSubscription(): Response<SubscriptionResponse>
    
    // --- Merchant Audit Logs ---
    @GET("merchant/audit-log")
    suspend fun getMerchantAuditLogs(): Response<AuditLogResponse>
    
    // --- Recurring Invoices ---
    @GET("merchant/recurring-invoices")
    suspend fun getRecurringInvoices(): Response<RecurringInvoiceResponse>
    
    @POST("merchant/recurring-invoices")
    suspend fun createRecurringInvoice(@Body request: CreateRecurringInvoiceRequest): Response<RecurringInvoiceResponse>
    
    @PATCH("merchant/recurring-invoices/{id}/cancel")
    suspend fun cancelRecurringInvoice(@Path("id") id: String): Response<RecurringInvoiceResponse>
    
    // --- Transaction Categories ---
    @GET("merchant/transaction-categories")
    suspend fun getTransactionCategories(): Response<TransactionCategoryResponse>
    
    @POST("merchant/transaction-categories")
    suspend fun createTransactionCategory(@Body request: CreateTransactionCategoryRequest): Response<TransactionCategoryResponse>
    
    @PATCH("merchant/transaction-categories/{id}")
    suspend fun updateTransactionCategory(
        @Path("id") id: String,
        @Body request: UpdateTransactionCategoryRequest
    ): Response<TransactionCategoryResponse>
    
    @DELETE("merchant/transaction-categories/{id}")
    suspend fun deleteTransactionCategory(@Path("id") id: String): Response<TransactionCategoryResponse>
    
    // --- Sub-Accounts ---
    @POST("sub-account/login")
    suspend fun loginSubAccount(@Body request: SubAccountLoginRequest): Response<SubAccountLoginResponse>
    
    @GET("merchant/sub-accounts")
    suspend fun getSubAccounts(): Response<SubAccountResponse>
    
    @POST("merchant/sub-accounts")
    suspend fun createSubAccount(@Body request: CreateSubAccountRequest): Response<SubAccountResponse>
    
    @PATCH("merchant/sub-accounts/{id}")
    suspend fun updateSubAccount(
        @Path("id") id: String,
        @Body request: UpdateSubAccountRequest
    ): Response<SubAccountResponse>
    
    @DELETE("merchant/sub-accounts/{id}")
    suspend fun deleteSubAccount(@Path("id") id: String): Response<SubAccountResponse>
    
    // --- Reminders ---
    @GET("merchant/reminders")
    suspend fun getReminders(
        @Query("status") status: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): Response<ReminderResponse>
    
    @POST("merchant/reminders")
    suspend fun createReminder(@Body request: CreateReminderRequest): Response<ReminderResponse>
    
    @PATCH("merchant/reminders/{id}")
    suspend fun updateReminder(
        @Path("id") id: String,
        @Body request: UpdateReminderRequest
    ): Response<ReminderResponse>
    
    @DELETE("merchant/reminders/{id}")
    suspend fun deleteReminder(@Path("id") id: String): Response<BasicResponse>
    
    @GET("merchant/reminders/stats")
    suspend fun getReminderStats(): Response<ReminderStatsResponse>
    
    // --- Financial Reports ---
    @GET("merchant/reports")
    suspend fun getFinancialReports(
        @Query("report_type") reportType: String? = null,
        @Query("category_id") categoryId: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("min_amount") minAmount: String? = null,
        @Query("max_amount") maxAmount: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<FinancialReportResponse>
    
    @POST("merchant/reports")
    suspend fun createFinancialReport(@Body request: CreateFinancialReportRequest): Response<FinancialReportResponse>
    
    @GET("merchant/reports/summary")
    suspend fun getFinancialReportSummary(
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("report_type") reportType: String? = null
    ): Response<FinancialReportSummaryResponse>
    
    @GET("merchant/reports/graph")
    suspend fun getFinancialReportGraphData(
        @Query("group_by") groupBy: String = "type", // type, category, date
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("report_type") reportType: String? = null
    ): Response<FinancialReportGraphResponse>
    
    @GET("merchant/reports/export")
    suspend fun exportFinancialReports(
        @Query("format") format: String, // pdf, excel
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("report_type") reportType: String? = null
    ): Response<BasicResponse>
    
    // --- Unified Business Reports (Pro Plan Only) ---
    @GET("merchant/unified-reports/dashboard")
    suspend fun getUnifiedReportsDashboard(
        @Query("time_period") timePeriod: String = "monthly",
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): Response<UnifiedReportDashboardResponse>
    
    @POST("merchant/unified-reports/generate")
    suspend fun generateUnifiedReport(@Body request: GenerateReportRequest): Response<UnifiedReportGenerateResponse>
    
    @GET("merchant/unified-reports/settings")
    suspend fun getUnifiedReportSettings(): Response<UnifiedReportSettingsResponse>
    
    @POST("merchant/unified-reports/settings")
    suspend fun saveUnifiedReportSetting(@Body request: CreateReportSettingRequest): Response<UnifiedReportSettingsResponse>
    
    @DELETE("merchant/unified-reports/settings/{id}")
    suspend fun deleteUnifiedReportSetting(@Path("id") settingId: String): Response<BasicResponse>
    
    @GET("merchant/unified-reports/snapshots")
    suspend fun getUnifiedReportSnapshots(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<UnifiedReportSnapshotsResponse>
    
    // --- Merchant API Keys (Pro Plan Only) ---
    @GET("merchant/api-keys")
    suspend fun getMerchantApiKeys(): Response<MerchantApiKeyResponse>
    
    @POST("merchant/api-keys")
    suspend fun createMerchantApiKey(@Body request: CreateApiKeyRequest): Response<MerchantApiKeyResponse>
    
    @PATCH("merchant/api-keys/{id}")
    suspend fun updateMerchantApiKey(
        @Path("id") keyId: String,
        @Body request: UpdateApiKeyRequest
    ): Response<MerchantApiKeyResponse>
    
    @DELETE("merchant/api-keys/{id}")
    suspend fun deleteMerchantApiKey(@Path("id") keyId: String): Response<BasicResponse>
    
    @GET("merchant/api-keys/stats")
    suspend fun getMerchantApiKeyStats(): Response<MerchantApiKeyStatsResponse>
    
    // --- Scheduled Operations (Pro Plan Only) ---
    @GET("merchant/scheduled-operations")
    suspend fun getScheduledOperations(
        @Query("status") status: String? = null,
        @Query("operation_type") operationType: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<ScheduledOperationResponse>
    
    @POST("merchant/scheduled-operations")
    suspend fun createScheduledOperation(@Body request: CreateScheduledOperationRequest): Response<ScheduledOperationResponse>
    
    @PATCH("merchant/scheduled-operations/{id}")
    suspend fun updateScheduledOperation(
        @Path("id") operationId: String,
        @Body request: UpdateScheduledOperationRequest
    ): Response<ScheduledOperationResponse>
    
    @DELETE("merchant/scheduled-operations/{id}")
    suspend fun cancelScheduledOperation(@Path("id") operationId: String): Response<BasicResponse>
    
    @GET("merchant/scheduled-operations/stats")
    suspend fun getScheduledOperationStats(): Response<ScheduledOperationStatsResponse>
    
    @POST("merchant/scheduled-operations/{id}/execute")
    suspend fun executeScheduledOperation(@Path("id") operationId: String): Response<BasicResponse>
    
    // --- Merchant Tax Management (Pro Plan Only) ---
    @GET("merchant/taxes")
    suspend fun getMerchantTaxes(): Response<MerchantTaxResponse>
    
    @POST("merchant/taxes")
    suspend fun createMerchantTax(@Body request: CreateMerchantTaxRequest): Response<MerchantTaxResponse>
    
    @PATCH("merchant/taxes/{id}")
    suspend fun updateMerchantTax(
        @Path("id") taxId: String,
        @Body request: UpdateMerchantTaxRequest
    ): Response<MerchantTaxResponse>
    
    @DELETE("merchant/taxes/{id}")
    suspend fun deleteMerchantTax(@Path("id") taxId: String): Response<BasicResponse>
    
    @GET("merchant/taxes/report")
    suspend fun generateTaxReport(
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): Response<TaxReportResponse>
    
    @GET("merchant/taxes/stats")
    suspend fun getTaxStats(): Response<TaxStatsResponse>
    
    // --- Transfer History ---
    @GET("api/all-transfers")
    suspend fun getAllTransfers(
        @Query("user_id") userId: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("status") status: String? = null,
        @Query("type") type: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<AllTransfersResponse>
    
    @GET("api/all-transfers/summary")
    suspend fun getTransferSummary(
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): Response<TransferSummaryResponse>
    
    // --- Mobile Error Reporting ---
    @POST("api/log-mobile-error")
    fun logMobileError(@Body request: CrashReportRequest): retrofit2.Call<CrashReportResponse>
}
