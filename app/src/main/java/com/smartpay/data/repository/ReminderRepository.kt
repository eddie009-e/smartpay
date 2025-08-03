package com.smartpay.repository

import com.smartpay.data.network.ApiClient
import com.smartpay.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class ReminderRepository {

    private val apiService = ApiClient.apiService

    suspend fun getReminders(
        status: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Response<ReminderResponse> = withContext(Dispatchers.IO) {
        apiService.getReminders(
            status = status,
            limit = limit,
            offset = offset
        )
    }

    suspend fun createReminder(
        title: String,
        description: String?,
        scheduledAt: String,
        isRecurring: Boolean,
        recurrenceInterval: String?
    ): Response<ReminderResponse> = withContext(Dispatchers.IO) {
        val request = CreateReminderRequest(
            title = title,
            description = description,
            scheduledAt = scheduledAt,
            isRecurring = isRecurring,
            recurrenceInterval = recurrenceInterval
        )
        apiService.createReminder(request)
    }

    suspend fun updateReminder(
        id: String,
        title: String?,
        description: String?,
        scheduledAt: String?,
        isRecurring: Boolean?,
        recurrenceInterval: String?
    ): Response<ReminderResponse> = withContext(Dispatchers.IO) {
        val request = UpdateReminderRequest(
            title = title,
            description = description,
            scheduledAt = scheduledAt,
            isRecurring = isRecurring,
            recurrenceInterval = recurrenceInterval
        )
        apiService.updateReminder(id, request)
    }

    suspend fun deleteReminder(id: String): Response<BasicResponse> = withContext(Dispatchers.IO) {
        apiService.deleteReminder(id)
    }

    suspend fun getReminderStats(): Response<ReminderStatsResponse> = withContext(Dispatchers.IO) {
        apiService.getReminderStats()
    }
}