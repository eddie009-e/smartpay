package com.smartpay.repositories

import com.smartpay.data.network.ApiService
import com.smartpay.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Repository for Merchant API Integration - Pro Plan Only Feature
 * 
 * Handles all API calls for merchant API key management including:
 * - API key creation and management
 * - Key status updates (activate/deactivate)
 * - Usage statistics and monitoring
 * - Security and access control
 * 
 * This repository implements proper error handling and coroutine support
 * for all merchant API key operations.
 */
class MerchantApiKeyRepository(private val apiService: ApiService) {
    
    /**
     * Get all API keys for the merchant
     * @return List of API keys or error message
     */
    suspend fun getApiKeys(): Result<List<MerchantApiKey>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMerchantApiKeys()
            handleResponse(response) { it.apiKeys ?: emptyList() }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تحميل مفاتيح API: ${e.message}"))
        }
    }
    
    /**
     * Create new API key
     * @param request Create API key request with description
     * @return Created API key with actual key value or error message
     */
    suspend fun createApiKey(request: CreateApiKeyRequest): Result<MerchantApiKey> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.createMerchantApiKey(request)
            handleResponse(response) { it.apiKey }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في إنشاء مفتاح API: ${e.message}"))
        }
    }
    
    /**
     * Update API key description or status
     * @param keyId ID of the API key to update
     * @param request Update request with new description or status
     * @return Updated API key or error message
     */
    suspend fun updateApiKey(keyId: String, request: UpdateApiKeyRequest): Result<MerchantApiKey> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateMerchantApiKey(keyId, request)
            handleResponse(response) { it.apiKey }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تحديث مفتاح API: ${e.message}"))
        }
    }
    
    /**
     * Delete API key
     * @param keyId ID of the API key to delete
     * @return Success result or error message
     */
    suspend fun deleteApiKey(keyId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteMerchantApiKey(keyId)
            handleResponse(response) { Unit }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في حذف مفتاح API: ${e.message}"))
        }
    }
    
    /**
     * Get API key usage statistics
     * @return Statistics about API key usage or error message
     */
    suspend fun getApiKeyStats(): Result<ApiKeyStats> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMerchantApiKeyStats()
            handleResponse(response) { it.stats }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تحميل إحصائيات مفاتيح API: ${e.message}"))
        }
    }
    
    /**
     * Toggle API key status (activate/deactivate)
     * @param keyId ID of the API key to toggle
     * @param isActive New status (true for active, false for inactive)
     * @return Updated API key or error message
     */
    suspend fun toggleApiKeyStatus(keyId: String, isActive: Boolean): Result<MerchantApiKey> = withContext(Dispatchers.IO) {
        try {
            val request = UpdateApiKeyRequest(isActive = isActive)
            val response = apiService.updateMerchantApiKey(keyId, request)
            handleResponse(response) { it.apiKey }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في ${if (isActive) "تفعيل" else "إلغاء تفعيل"} مفتاح API: ${e.message}"))
        }
    }
    
    /**
     * Update API key description only
     * @param keyId ID of the API key to update
     * @param description New description
     * @return Updated API key or error message
     */
    suspend fun updateApiKeyDescription(keyId: String, description: String): Result<MerchantApiKey> = withContext(Dispatchers.IO) {
        try {
            val request = UpdateApiKeyRequest(description = description)
            val response = apiService.updateMerchantApiKey(keyId, request)
            handleResponse(response) { it.apiKey }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تحديث وصف مفتاح API: ${e.message}"))
        }
    }
    
    /**
     * Check if merchant can create more API keys
     * @return Boolean indicating if more keys can be created
     */
    suspend fun canCreateMoreKeys(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val statsResult = getApiKeyStats()
            if (statsResult.isSuccess) {
                val stats = statsResult.getOrNull()
                Result.success(stats?.let { !it.isAtCapacity() } ?: true)
            } else {
                statsResult.map { false }
            }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في التحقق من إمكانية إنشاء مفاتيح جديدة: ${e.message}"))
        }
    }
    
    /**
     * Get API keys filtered by status
     * @param activeOnly If true, returns only active keys
     * @return Filtered list of API keys or error message
     */
    suspend fun getApiKeysByStatus(activeOnly: Boolean = false): Result<List<MerchantApiKey>> = withContext(Dispatchers.IO) {
        try {
            val allKeysResult = getApiKeys()
            if (allKeysResult.isSuccess) {
                val allKeys = allKeysResult.getOrNull() ?: emptyList()
                val filteredKeys = if (activeOnly) {
                    allKeys.filter { it.isActive }
                } else {
                    allKeys
                }
                Result.success(filteredKeys)
            } else {
                allKeysResult
            }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تحميل مفاتيح API المفلترة: ${e.message}"))
        }
    }
    
    /**
     * Get recently used API keys (used within last 7 days)
     * @return List of recently used API keys or error message
     */
    suspend fun getRecentlyUsedKeys(): Result<List<MerchantApiKey>> = withContext(Dispatchers.IO) {
        try {
            val allKeysResult = getApiKeys()
            if (allKeysResult.isSuccess) {
                val allKeys = allKeysResult.getOrNull() ?: emptyList()
                val recentKeys = allKeys.filter { it.isRecentlyUsed() }
                Result.success(recentKeys)
            } else {
                allKeysResult
            }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تحميل مفاتيح API المستخدمة مؤخراً: ${e.message}"))
        }
    }
    
    /**
     * Get unused API keys (never used)
     * @return List of unused API keys or error message
     */
    suspend fun getUnusedKeys(): Result<List<MerchantApiKey>> = withContext(Dispatchers.IO) {
        try {
            val allKeysResult = getApiKeys()
            if (allKeysResult.isSuccess) {
                val allKeys = allKeysResult.getOrNull() ?: emptyList()
                val unusedKeys = allKeys.filter { it.lastUsedAt == null }
                Result.success(unusedKeys)
            } else {
                allKeysResult
            }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تحميل مفاتيح API غير المستخدمة: ${e.message}"))
        }
    }
    
    /**
     * Bulk update API key status
     * @param keyIds List of API key IDs to update
     * @param isActive New status for all keys
     * @return Number of successfully updated keys
     */
    suspend fun bulkUpdateApiKeyStatus(keyIds: List<String>, isActive: Boolean): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var successCount = 0
            keyIds.forEach { keyId ->
                val result = toggleApiKeyStatus(keyId, isActive)
                if (result.isSuccess) {
                    successCount++
                }
            }
            Result.success(successCount)
        } catch (e: Exception) {
            Result.failure(Exception("فشل في التحديث المجمع لحالة مفاتيح API: ${e.message}"))
        }
    }
    
    /**
     * Generate API key usage report
     * @return Usage report with statistics and recommendations
     */
    suspend fun generateUsageReport(): Result<ApiKeyUsageReport> = withContext(Dispatchers.IO) {
        try {
            val statsResult = getApiKeyStats()
            val keysResult = getApiKeys()
            
            if (statsResult.isSuccess && keysResult.isSuccess) {
                val stats = statsResult.getOrNull()!!
                val keys = keysResult.getOrNull()!!
                
                val recentlyUsedCount = keys.count { it.isRecentlyUsed() }
                val unusedCount = keys.count { it.lastUsedAt == null }
                val inactiveCount = keys.count { !it.isActive }
                
                val recommendations = mutableListOf<String>()
                
                if (unusedCount > 0) {
                    recommendations.add("يوجد $unusedCount مفاتيح غير مستخدمة يمكن حذفها لتوفير مساحة")
                }
                
                if (inactiveCount > 0) {
                    recommendations.add("يوجد $inactiveCount مفاتيح غير نشطة يمكن تفعيلها أو حذفها")
                }
                
                if (stats.isAtCapacity()) {
                    recommendations.add("تم الوصول للحد الأقصى من مفاتيح API (${stats.maxKeys})")
                }
                
                if (recentlyUsedCount == 0 && keys.isNotEmpty()) {
                    recommendations.add("لا يوجد مفاتيح API مستخدمة مؤخراً، تحقق من التكاملات")
                }
                
                val report = ApiKeyUsageReport(
                    totalKeys = stats.totalKeys,
                    activeKeys = stats.activeKeys,
                    recentlyUsedKeys = recentlyUsedCount,
                    unusedKeys = unusedCount,
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
                    403 -> "تكامل API متاح حصرياً لمشتركي الخطة الاحترافية"
                    404 -> "مفتاح API غير موجود"
                    409 -> "تم الوصول للحد الأقصى من مفاتيح API"
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
 * API Key Usage Report data class
 */
data class ApiKeyUsageReport(
    val totalKeys: Int,
    val activeKeys: Int,
    val recentlyUsedKeys: Int,
    val unusedKeys: Int,
    val recommendations: List<String>
) {
    fun getUsageEfficiency(): Float {
        return if (totalKeys > 0) {
            (recentlyUsedKeys.toFloat() / totalKeys.toFloat()) * 100
        } else {
            0f
        }
    }
    
    fun hasRecommendations(): Boolean {
        return recommendations.isNotEmpty()
    }
}