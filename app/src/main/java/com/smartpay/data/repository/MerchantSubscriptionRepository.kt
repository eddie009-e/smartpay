package com.smartpay.repository

import com.smartpay.data.network.ApiClient
import com.smartpay.models.SubscriptionRequest
import com.smartpay.models.SubscriptionResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class MerchantSubscriptionRepository {

    private val apiService = ApiClient.apiService

    suspend fun getCurrentSubscription(): Response<SubscriptionResponse> = withContext(Dispatchers.IO) {
        apiService.getCurrentSubscription()
    }

    suspend fun subscribeToPlan(planName: String): Response<SubscriptionResponse> = withContext(Dispatchers.IO) {
        val request = SubscriptionRequest(planName = planName)
        apiService.subscribeToPlan(request)
    }

    suspend fun cancelSubscription(): Response<SubscriptionResponse> = withContext(Dispatchers.IO) {
        apiService.cancelSubscription()
    }
}