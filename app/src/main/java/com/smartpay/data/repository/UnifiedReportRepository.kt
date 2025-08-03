package com.smartpay.repositories

import com.smartpay.api.ApiService
import com.smartpay.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Repository for Unified Business Reports - Pro Plan Only Feature
 * 
 * Handles all API calls for unified business reports including:
 * - Dashboard analytics with visual charts
 * - Report generation with advanced filtering
 * - Report preset management
 * - Export functionality and snapshots
 * 
 * This repository implements proper error handling and coroutine support
 * for all unified reports operations.
 */
class UnifiedReportRepository(private val apiService: ApiService) {
    
    /**
     * Get dashboard analytics data
     * @param timePeriod Time period filter (daily, weekly, monthly, custom)
     * @param startDate Start date for custom period (optional)
     * @param endDate End date for custom period (optional)
     * @return Dashboard data with analytics or error message
     */
    suspend fun getDashboard(
        timePeriod: String = "monthly",
        startDate: String? = null,
        endDate: String? = null
    ): Result<UnifiedReportDashboard> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getUnifiedReportsDashboard(timePeriod, startDate, endDate)
            handleResponse(response) { it.dashboard }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تحميل لوحة التقارير الموحدة: ${e.message}"))
        }
    }
    
    /**
     * Generate comprehensive report with optional export
     * @param request Report generation request with filters
     * @return Generated report content or error message
     */
    suspend fun generateReport(request: GenerateReportRequest): Result<UnifiedReportGenerateResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.generateUnifiedReport(request)
            handleResponse(response) { it }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في إنشاء التقرير الموحد: ${e.message}"))
        }
    }
    
    /**
     * Get all saved report settings/presets
     * @return List of saved report presets or error message
     */
    suspend fun getReportSettings(): Result<List<UnifiedReportSetting>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getUnifiedReportSettings()
            handleResponse(response) { it.settings ?: emptyList() }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تحميل إعدادات التقارير: ${e.message}"))
        }
    }
    
    /**
     * Save new report setting/preset
     * @param request Create report setting request
     * @return Created report setting or error message
     */
    suspend fun saveReportSetting(request: CreateReportSettingRequest): Result<UnifiedReportSetting> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.saveUnifiedReportSetting(request)
            handleResponse(response) { it.setting }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في حفظ إعداد التقرير: ${e.message}"))
        }
    }
    
    /**
     * Delete report setting/preset
     * @param settingId ID of the setting to delete
     * @return Success result or error message
     */
    suspend fun deleteReportSetting(settingId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteUnifiedReportSetting(settingId)
            handleResponse(response) { Unit }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في حذف إعداد التقرير: ${e.message}"))
        }
    }
    
    /**
     * Get report snapshots history
     * @param limit Number of snapshots to retrieve
     * @param offset Offset for pagination
     * @return List of report snapshots or error message
     */
    suspend fun getReportSnapshots(
        limit: Int = 20,
        offset: Int = 0
    ): Result<UnifiedReportSnapshotsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getUnifiedReportSnapshots(limit, offset)
            handleResponse(response) { it }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تحميل سجل التقارير: ${e.message}"))
        }
    }
    
    /**
     * Apply saved report preset and generate report
     * @param settingId ID of the saved setting to apply
     * @param exportFormat Optional export format (pdf, excel)
     * @param snapshotNotes Optional notes for snapshot
     * @return Generated report or error message
     */
    suspend fun applyReportPreset(
        settingId: String,
        exportFormat: String? = null,
        snapshotNotes: String? = null
    ): Result<UnifiedReportGenerateResponse> = withContext(Dispatchers.IO) {
        try {
            // First get the setting
            val settingsResponse = apiService.getUnifiedReportSettings()
            val settingsResult = handleResponse(settingsResponse) { it.settings ?: emptyList() }
            
            if (settingsResult.isFailure) {
                return@withContext settingsResult.map { UnifiedReportGenerateResponse(false, null, null, "فشل في تحميل الإعداد") }
            }
            
            val setting = settingsResult.getOrNull()?.find { it.id == settingId }
                ?: return@withContext Result.failure(Exception("الإعداد غير موجود"))
            
            // Create generate request from setting
            val generateRequest = GenerateReportRequest(
                reportTypes = setting.filters.reportTypes ?: emptyList(),
                categories = setting.filters.categories,
                timePeriod = setting.filters.timePeriod,
                startDate = setting.filters.startDate,
                endDate = setting.filters.endDate,
                minAmount = setting.filters.minAmount,
                maxAmount = setting.filters.maxAmount,
                notesFilter = setting.filters.notesFilter,
                exportFormat = exportFormat,
                snapshotNotes = snapshotNotes
            )
            
            generateReport(generateRequest)
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تطبيق الإعداد المحفوظ: ${e.message}"))
        }
    }
    
    /**
     * Get available report categories for filtering
     * @return List of available categories or error message
     */
    suspend fun getReportCategories(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            // This would typically be a separate API call
            // For now, returning common categories
            Result.success(listOf(
                "expense", "income", "transfer", "debt", "salary", "invoice"
            ))
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تحميل التصنيفات: ${e.message}"))
        }
    }
    
    /**
     * Generate quick summary report for dashboard
     * @param reportTypes Types of reports to include in summary
     * @param days Number of days to include (default 30)
     * @return Quick summary data or error message
     */
    suspend fun getQuickSummary(
        reportTypes: List<String> = listOf("expense", "income", "transfer", "debt", "salary", "invoice"),
        days: Int = 30
    ): Result<ReportSummary> = withContext(Dispatchers.IO) {
        try {
            val generateRequest = GenerateReportRequest(
                reportTypes = reportTypes,
                timePeriod = "custom",
                startDate = java.time.LocalDate.now().minusDays(days.toLong()).toString(),
                endDate = java.time.LocalDate.now().toString()
            )
            
            val response = apiService.generateUnifiedReport(generateRequest)
            handleResponse(response) { 
                it.report?.summary?.let { contentSummary ->
                    ReportSummary(
                        totalIncome = contentSummary.byType.filter { (type, _) -> 
                            listOf("income", "invoice").contains(type) 
                        }.values.sumOf { it.total },
                        totalExpenses = contentSummary.byType.filter { (type, _) -> 
                            listOf("expense", "debt", "salary", "transfer").contains(type) 
                        }.values.sumOf { it.total },
                        netIncome = contentSummary.byType.filter { (type, _) -> 
                            listOf("income", "invoice").contains(type) 
                        }.values.sumOf { it.total } - contentSummary.byType.filter { (type, _) -> 
                            listOf("expense", "debt", "salary", "transfer").contains(type) 
                        }.values.sumOf { it.total },
                        transactionCount = contentSummary.totalRecords
                    )
                } ?: ReportSummary(0.0, 0.0, 0.0, 0)
            }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تحميل الملخص السريع: ${e.message}"))
        }
    }
    
    /**
     * Download generated report file
     * @param fileUrl URL of the generated file
     * @return Success result or error message
     */
    suspend fun downloadReportFile(fileUrl: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // This would implement actual file download logic
            // For now, returning the URL as-is
            Result.success(fileUrl)
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تحميل الملف: ${e.message}"))
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
                    403 -> "التقارير الموحدة متاحة حصرياً لمشتركي الخطة الاحترافية"
                    404 -> "الطلب غير موجود"
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