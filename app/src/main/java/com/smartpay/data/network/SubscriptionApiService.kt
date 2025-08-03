package com.smartpay.data.network

import retrofit2.Response
import retrofit2.http.*

/**
 * API Service Interface for SmartPay Subscription Management
 * 
 * Defines all endpoints for subscription operations:
 * - Get available subscription plans
 * - Get current merchant subscription
 * - Submit manual upgrade requests
 * - Activate Google IAP subscriptions
 */
interface SubscriptionApiService {
    
    // Subscription Plans
    @GET("subscription-plans")
    suspend fun getSubscriptionPlans(): Response<SubscriptionPlansResponse>
    
    // Current Subscription
    @GET("merchant-subscription/{merchantId}")
    suspend fun getCurrentSubscription(
        @Header("Authorization") token: String,
        @Path("merchantId") merchantId: String
    ): Response<CurrentSubscriptionResponse>
    
    // Manual Upgrade Request
    @POST("upgrade-request")
    suspend fun submitUpgradeRequest(
        @Header("Authorization") token: String,
        @Body request: Map<String, Any>
    ): Response<UpgradeRequestResponse>
    
    // Google IAP Activation
    @POST("plans/activate-iap")
    suspend fun activateIAPSubscription(
        @Header("Authorization") token: String,
        @Body request: Map<String, Any>
    ): Response<IAPActivationResponse>
    
    // Admin endpoints (for viewing upgrade requests)
    @GET("upgrade-requests")
    suspend fun getUpgradeRequests(
        @Header("Authorization") token: String
    ): Response<UpgradeRequestsResponse>
    
    @POST("upgrade-requests/{requestId}/confirm")
    suspend fun confirmUpgradeRequest(
        @Header("Authorization") token: String,
        @Path("requestId") requestId: String
    ): Response<ConfirmUpgradeResponse>
}

// Response data classes
data class SubscriptionPlansResponse(
    val success: Boolean,
    val plans: List<SubscriptionPlanDto>,
    val message: String
)

data class SubscriptionPlanDto(
    val id: String,
    val name: String,
    val plan_code: String,
    val monthly_price: Double,
    val features: List<String>,
    val is_recommended: Boolean
)

data class CurrentSubscriptionResponse(
    val success: Boolean,
    val subscription: CurrentSubscriptionDto?,
    val message: String
)

data class CurrentSubscriptionDto(
    val id: String?,
    val plan_name: String?,
    val start_date: String?,
    val end_date: String?,
    val is_active: Boolean?,
    val plan_display_name: String?,
    val monthly_price: Double?,
    val features: List<String>?,
    val is_recommended: Boolean?
)

data class UpgradeRequestResponse(
    val success: Boolean,
    val request_id: String?,
    val message: String,
    val admin_phone: String?,
    val amount: Double?,
    val target_plan: String?
)

data class IAPActivationResponse(
    val success: Boolean,
    val subscription_id: String?,
    val plan_name: String?,
    val plan_code: String?,
    val start_date: String?,
    val end_date: String?,
    val message: String
)

data class UpgradeRequestsResponse(
    val success: Boolean,
    val requests: List<UpgradeRequestDto>,
    val message: String
)

data class UpgradeRequestDto(
    val id: String,
    val from_merchant_id: String,
    val amount: Double,
    val target_plan: String,
    val status: String,
    val created_at: String,
    val business_name: String?,
    val phone: String?,
    val plan_name: String?
)

data class ConfirmUpgradeResponse(
    val success: Boolean,
    val subscription_id: String?,
    val message: String
)