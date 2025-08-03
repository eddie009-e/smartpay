package com.smartpay.repositories

import com.smartpay.data.network.ApiService
import com.smartpay.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Repository for Scheduled Operations - Pro Plan Only Feature
 * 
 * Handles all API calls for scheduled operations management including:
 * - Creating and managing automated recurring operations
 * - Operation scheduling (daily, weekly, monthly)
 * - Status management (active, paused, cancelled)
 * - Statistics and monitoring
 * 
 * This repository implements proper error handling and coroutine support
 * for all scheduled operations.
 */
class ScheduledOperationRepository(private val apiService: ApiService) {
    
    /**
     * Get all scheduled operations for the merchant
     * @param status Optional status filter
     * @param operationType Optional operation type filter
     * @param limit Number of operations to retrieve
     * @param offset Offset for pagination
     * @return List of scheduled operations or error message
     */
    suspend fun getScheduledOperations(
        status: String? = null,
        operationType: String? = null,
        limit: Int = 20,
        offset: Int = 0
    ): Result<ScheduledOperationResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getScheduledOperations(status, operationType, limit, offset)
            handleResponse(response) { it }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تحميل العمليات المجدولة: ${e.message}"))
        }
    }
    
    /**
     * Create new scheduled operation
     * @param request Create scheduled operation request
     * @return Created scheduled operation or error message
     */
    suspend fun createScheduledOperation(request: CreateScheduledOperationRequest): Result<ScheduledOperation> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.createScheduledOperation(request)
            handleResponse(response) { it.scheduledOperation }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في إنشاء العملية المجدولة: ${e.message}"))
        }
    }
    
    /**
     * Update scheduled operation
     * @param operationId ID of the operation to update
     * @param request Update request with new values
     * @return Updated scheduled operation or error message
     */
    suspend fun updateScheduledOperation(operationId: String, request: UpdateScheduledOperationRequest): Result<ScheduledOperation> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateScheduledOperation(operationId, request)
            handleResponse(response) { it.scheduledOperation }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تحديث العملية المجدولة: ${e.message}"))
        }
    }
    
    /**
     * Cancel/Delete scheduled operation
     * @param operationId ID of the operation to cancel
     * @return Success result or error message
     */
    suspend fun cancelScheduledOperation(operationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.cancelScheduledOperation(operationId)
            handleResponse(response) { Unit }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في إلغاء العملية المجدولة: ${e.message}"))
        }
    }
    
    /**
     * Get scheduled operations statistics
     * @return Statistics about scheduled operations or error message
     */
    suspend fun getScheduledOperationStats(): Result<ScheduledOperationStats> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getScheduledOperationStats()
            handleResponse(response) { it.stats }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تحميل إحصائيات العمليات المجدولة: ${e.message}"))
        }
    }
    
    /**
     * Execute scheduled operation manually
     * @param operationId ID of the operation to execute
     * @return Execution result or error message
     */
    suspend fun executeScheduledOperation(operationId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.executeScheduledOperation(operationId)
            handleResponse(response) { it.message ?: "تم تنفيذ العملية بنجاح" }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تنفيذ العملية المجدولة: ${e.message}"))
        }
    }
    
    /**
     * Toggle scheduled operation status (pause/resume)
     * @param operationId ID of the operation to toggle
     * @param isActive New status (true for active, false for paused)
     * @return Updated scheduled operation or error message
     */
    suspend fun toggleOperationStatus(operationId: String, isActive: Boolean): Result<ScheduledOperation> = withContext(Dispatchers.IO) {
        try {
            val status = if (isActive) "active" else "paused"
            val request = UpdateScheduledOperationRequest(status = status)
            val response = apiService.updateScheduledOperation(operationId, request)
            handleResponse(response) { it.scheduledOperation }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تغيير حالة العملية المجدولة: ${e.message}"))
        }
    }
    
    /**
     * Update operation description only
     * @param operationId ID of the operation to update
     * @param description New description
     * @return Updated scheduled operation or error message
     */
    suspend fun updateOperationDescription(operationId: String, description: String): Result<ScheduledOperation> = withContext(Dispatchers.IO) {
        try {
            val request = UpdateScheduledOperationRequest(description = description)
            val response = apiService.updateScheduledOperation(operationId, request)
            handleResponse(response) { it.scheduledOperation }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تحديث وصف العملية المجدولة: ${e.message}"))
        }
    }
    
    /**
     * Get operations by status
     * @param status Status to filter by
     * @return Filtered list of operations or error message
     */
    suspend fun getOperationsByStatus(status: String): Result<List<ScheduledOperation>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getScheduledOperations(status = status, limit = 100)
            handleResponse(response) { it.scheduledOperations ?: emptyList() }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تحميل العمليات المفلترة: ${e.message}"))
        }
    }
    
    /**
     * Get operations by type
     * @param operationType Operation type to filter by
     * @return Filtered list of operations or error message
     */
    suspend fun getOperationsByType(operationType: String): Result<List<ScheduledOperation>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getScheduledOperations(operationType = operationType, limit = 100)
            handleResponse(response) { it.scheduledOperations ?: emptyList() }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تحميل العمليات المفلترة: ${e.message}"))
        }
    }
    
    /**
     * Get active operations
     * @return List of active operations or error message
     */
    suspend fun getActiveOperations(): Result<List<ScheduledOperation>> = withContext(Dispatchers.IO) {
        try {
            getOperationsByStatus("active")
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تحميل العمليات النشطة: ${e.message}"))
        }
    }
    
    /**
     * Get operations due soon (within 24 hours)
     * @return List of operations due soon or error message
     */
    suspend fun getOperationsDueSoon(): Result<List<ScheduledOperation>> = withContext(Dispatchers.IO) {
        try {
            val activeResult = getActiveOperations()
            if (activeResult.isSuccess) {
                val activeOperations = activeResult.getOrNull() ?: emptyList()
                val dueSoon = activeOperations.filter { it.isDueSoon() }
                Result.success(dueSoon)
            } else {
                activeResult
            }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تحميل العمليات المستحقة قريباً: ${e.message}"))
        }
    }
    
    /**
     * Check if merchant can create more operations
     * @return Boolean indicating if more operations can be created
     */
    suspend fun canCreateMoreOperations(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val statsResult = getScheduledOperationStats()
            if (statsResult.isSuccess) {
                val stats = statsResult.getOrNull()
                Result.success(stats?.let { !it.isAtCapacity() } ?: true)
            } else {
                statsResult.map { false }
            }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في التحقق من إمكانية إنشاء عمليات جديدة: ${e.message}"))
        }
    }
    
    /**
     * Get operations summary by type
     * @return Map of operation types with counts
     */
    suspend fun getOperationsSummary(): Result<Map<String, Int>> = withContext(Dispatchers.IO) {
        try {
            val statsResult = getScheduledOperationStats()
            if (statsResult.isSuccess) {
                val stats = statsResult.getOrNull()!!
                val summary = mapOf(
                    "invoice" to stats.invoiceOperations,
                    "salary" to stats.salaryOperations,
                    "transfer" to stats.transferOperations
                )
                Result.success(summary)
            } else {
                Result.failure(Exception("فشل في تحميل ملخص العمليات"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تحميل ملخص العمليات: ${e.message}"))
        }
    }
    
    /**
     * Bulk update operations status
     * @param operationIds List of operation IDs to update
     * @param status New status for all operations
     * @return Number of successfully updated operations
     */
    suspend fun bulkUpdateOperationsStatus(operationIds: List<String>, status: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var successCount = 0
            operationIds.forEach { operationId ->
                val request = UpdateScheduledOperationRequest(status = status)
                val result = updateScheduledOperation(operationId, request)
                if (result.isSuccess) {
                    successCount++
                }
            }
            Result.success(successCount)
        } catch (e: Exception) {
            Result.failure(Exception("فشل في التحديث المجمع للعمليات: ${e.message}"))
        }
    }
    
    /**
     * Generate operations usage report
     * @return Usage report with recommendations
     */
    suspend fun generateUsageReport(): Result<OperationsUsageReport> = withContext(Dispatchers.IO) {
        try {
            val statsResult = getScheduledOperationStats()
            val operationsResult = getScheduledOperations(limit = 100)
            
            if (statsResult.isSuccess && operationsResult.isSuccess) {
                val stats = statsResult.getOrNull()!!
                val operations = operationsResult.getOrNull()?.scheduledOperations ?: emptyList()
                
                val dueSoonCount = operations.count { it.isDueSoon() }
                val pausedCount = stats.pausedOperations
                val unusedSlots = stats.getRemainingSlots()
                
                val recommendations = mutableListOf<String>()
                
                if (pausedCount > 0) {
                    recommendations.add("يوجد $pausedCount عمليات متوقفة يمكن تفعيلها أو إلغاؤها")
                }
                
                if (dueSoonCount > 5) {
                    recommendations.add("يوجد $dueSoonCount عمليات مستحقة قريباً، تأكد من رصيد الحساب")
                }
                
                if (stats.isAtCapacity()) {
                    recommendations.add("تم الوصول للحد الأقصى من العمليات المجدولة (${stats.maxOperations})")
                }
                
                if (stats.activeOperations == 0 && stats.totalOperations > 0) {
                    recommendations.add("لا توجد عمليات نشطة، تحقق من إعدادات الجدولة")
                }
                
                val report = OperationsUsageReport(
                    totalOperations = stats.totalOperations,
                    activeOperations = stats.activeOperations,
                    dueSoonOperations = dueSoonCount,
                    pausedOperations = pausedCount,
                    utilizationPercentage = stats.getUtilizationPercentage(),
                    recommendations = recommendations
                )
                
                Result.success(report)
            } else {
                Result.failure(Exception("فشل في إنشاء تقرير الاستخدام"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في إنشاء تقرير الاستخدام: ${e.message}"))
        }
    }
    
    /**
     * Generic response handler for API responses
     * @param response API response
     * @param dataExtractor Function to extract data from successful response
     * @return Result with extracted data or error message
     */
    private fun <T, R> handleResponse(
        response: Response<T>,
        dataExtractor: (T) -> R?
    ): Result<R> {
        return try {
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val data = dataExtractor(body)
                    if (data != null) {
                        Result.success(data)
                    } else {
                        Result.failure(Exception("لا توجد بيانات متاحة"))
                    }
                } else {
                    Result.failure(Exception("استجابة فارغة من الخادم"))
                }
            } else {
                val errorMessage = when (response.code()) {
                    403 -> "الجدولة التلقائية متاحة حصرياً لمشتركي الخطة الاحترافية"
                    404 -> "العملية المجدولة غير موجودة"
                    409 -> "تم الوصول للحد الأقصى من العمليات المجدولة"
                    400 -> "بيانات غير صحيحة أو هدف غير متاح"
                    500 -> "خطأ في الخادم، يرجى المحاولة لاحقاً"
                    else -> "حدث خطأ: ${response.code()}"
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("خطأ في الشبكة: ${e.message}"))
        }
    }
}

/**
 * Operations Usage Report data class
 */
data class OperationsUsageReport(
    val totalOperations: Int,
    val activeOperations: Int,
    val dueSoonOperations: Int,
    val pausedOperations: Int,
    val utilizationPercentage: Float,
    val recommendations: List<String>
) {
    fun getEfficiencyScore(): Int {
        return when {
            activeOperations == 0 -> 0
            pausedOperations > activeOperations -> 30
            dueSoonOperations > 10 -> 60
            utilizationPercentage > 90 -> 95
            else -> 80
        }
    }
    
    fun hasRecommendations(): Boolean {
        return recommendations.isNotEmpty()
    }
}