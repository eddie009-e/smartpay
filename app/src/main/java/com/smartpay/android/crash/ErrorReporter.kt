package com.smartpay.android.crash

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.smartpay.data.network.ApiClient
import com.smartpay.data.network.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Handles preparation and sending of crash reports to the backend
 */
class ErrorReporter(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val reporterScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val apiService: ApiService = ApiClient.createService(ApiService::class.java)
    
    companion object {
        private const val PREFS_NAME = "crash_reporter_prefs"
        private const val KEY_PENDING_CRASHES = "pending_crashes"
        private const val MAX_PENDING_CRASHES = 50
    }
    
    /**
     * Send crash report to backend
     */
    fun sendCrashReport(crashData: CrashData) {
        // Convert to request model
        val request = CrashReportRequest(
            source = crashData.source,
            error = crashData.error,
            stackTrace = crashData.stackTrace,
            device = crashData.device,
            sdk = crashData.sdk,
            appVersion = crashData.appVersion,
            timestamp = crashData.timestamp
        )
        
        // Send via Retrofit
        apiService.logMobileError(request).enqueue(object : Callback<CrashReportResponse> {
            override fun onResponse(
                call: Call<CrashReportResponse>,
                response: Response<CrashReportResponse>
            ) {
                if (response.isSuccessful) {
                    // Success - no UI feedback needed
                    println("Crash report sent successfully: ${response.body()?.errorId}")
                } else {
                    // Save for retry
                    savePendingCrash(crashData)
                }
            }
            
            override fun onFailure(call: Call<CrashReportResponse>, t: Throwable) {
                // Network failure - save for retry
                savePendingCrash(crashData)
            }
        })
    }
    
    /**
     * Save crash data for later retry
     */
    fun savePendingCrash(crashData: CrashData) {
        try {
            val pendingCrashes = getPendingCrashes().toMutableList()
            
            // Add new crash (limit total stored crashes)
            if (pendingCrashes.size < MAX_PENDING_CRASHES) {
                pendingCrashes.add(crashData)
                
                // Save to SharedPreferences
                val json = gson.toJson(pendingCrashes)
                prefs.edit().putString(KEY_PENDING_CRASHES, json).apply()
            }
        } catch (e: Exception) {
            // Ignore storage errors
        }
    }
    
    /**
     * Get list of pending crash reports
     */
    private fun getPendingCrashes(): List<CrashData> {
        return try {
            val json = prefs.getString(KEY_PENDING_CRASHES, null) ?: return emptyList()
            val type = object : TypeToken<List<CrashData>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Clear pending crashes after successful send
     */
    private fun clearPendingCrashes() {
        prefs.edit().remove(KEY_PENDING_CRASHES).apply()
    }
    
    /**
     * Retry sending pending crash reports
     */
    fun retryPendingCrashes() {
        reporterScope.launch {
            val pendingCrashes = getPendingCrashes()
            if (pendingCrashes.isEmpty()) return@launch
            
            val successfulSends = mutableListOf<CrashData>()
            
            pendingCrashes.forEach { crashData ->
                try {
                    // Try to send each pending crash
                    sendCrashReportSync(crashData)
                    successfulSends.add(crashData)
                } catch (e: Exception) {
                    // Keep in pending if failed
                }
            }
            
            // Update pending list (remove successful ones)
            if (successfulSends.isNotEmpty()) {
                val remaining = pendingCrashes - successfulSends.toSet()
                if (remaining.isEmpty()) {
                    clearPendingCrashes()
                } else {
                    val json = gson.toJson(remaining)
                    prefs.edit().putString(KEY_PENDING_CRASHES, json).apply()
                }
            }
        }
    }
    
    /**
     * Synchronous version for retry logic
     */
    private fun sendCrashReportSync(crashData: CrashData) {
        val request = CrashReportRequest(
            source = crashData.source,
            error = crashData.error,
            stackTrace = crashData.stackTrace,
            device = crashData.device,
            sdk = crashData.sdk,
            appVersion = crashData.appVersion,
            timestamp = crashData.timestamp
        )
        
        val response = apiService.logMobileError(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to send crash report: ${response.code()}")
        }
    }
}

/**
 * Request model for crash report API
 */
data class CrashReportRequest(
    val source: String,
    val error: String,
    val stackTrace: String,
    val device: String,
    val sdk: Int,
    val appVersion: String,
    val timestamp: String
)

/**
 * Response model for crash report API
 */
data class CrashReportResponse(
    val success: Boolean,
    val errorId: String?,
    val message: String
)