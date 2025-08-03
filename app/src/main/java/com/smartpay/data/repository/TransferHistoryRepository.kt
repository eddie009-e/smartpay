package com.smartpay.repository

import com.smartpay.data.network.ApiService
import com.smartpay.models.AllTransfersResponse
import com.smartpay.models.TransferSummaryResponse
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferHistoryRepository @Inject constructor(
    private val apiService: ApiService
) {
    
    /**
     * Get all transfers with optional filtering and pagination
     */
    suspend fun getAllTransfers(
        userId: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        status: String? = null,
        type: String? = null,
        page: Int = 1,
        limit: Int = 20
    ): Response<AllTransfersResponse> {
        return apiService.getAllTransfers(
            userId = userId,
            startDate = startDate,
            endDate = endDate,
            status = status,
            type = type,
            page = page,
            limit = limit
        )
    }
    
    /**
     * Get transfer summary statistics
     */
    suspend fun getTransferSummary(
        startDate: String? = null,
        endDate: String? = null
    ): Response<TransferSummaryResponse> {
        return apiService.getTransferSummary(
            startDate = startDate,
            endDate = endDate
        )
    }
    
    /**
     * Get sent transfers only
     */
    suspend fun getSentTransfers(
        page: Int = 1,
        limit: Int = 20,
        startDate: String? = null,
        endDate: String? = null
    ): Response<AllTransfersResponse> {
        return getAllTransfers(
            type = "sent",
            page = page,
            limit = limit,
            startDate = startDate,
            endDate = endDate
        )
    }
    
    /**
     * Get received transfers only
     */
    suspend fun getReceivedTransfers(
        page: Int = 1,
        limit: Int = 20,
        startDate: String? = null,
        endDate: String? = null
    ): Response<AllTransfersResponse> {
        return getAllTransfers(
            type = "received",
            page = page,
            limit = limit,
            startDate = startDate,
            endDate = endDate
        )
    }
    
    /**
     * Get transfers by status
     */
    suspend fun getTransfersByStatus(
        status: String,
        page: Int = 1,
        limit: Int = 20
    ): Response<AllTransfersResponse> {
        return getAllTransfers(
            status = status,
            page = page,
            limit = limit
        )
    }
    
    /**
     * Search transfers by date range
     */
    suspend fun getTransfersByDateRange(
        startDate: String,
        endDate: String,
        page: Int = 1,
        limit: Int = 20
    ): Response<AllTransfersResponse> {
        return getAllTransfers(
            startDate = startDate,
            endDate = endDate,
            page = page,
            limit = limit
        )
    }
}