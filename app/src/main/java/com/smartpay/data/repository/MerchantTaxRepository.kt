package com.smartpay.repositories

import com.smartpay.data.network.ApiService
import com.smartpay.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Repository for Merchant Tax Management - Pro Plan Only Feature
 * 
 * Handles all API calls for tax management including:
 * - Creating and managing custom tax rates
 * - Tax reporting and analytics
 * - Tax application to invoices
 * - Compliance tracking
 * 
 * This repository implements proper error handling and coroutine support
 * for all tax management operations.
 */
class MerchantTaxRepository(private val apiService: ApiService) {
    
    /**
     * Get all merchant taxes
     * @return List of merchant taxes or error message
     */
    suspend fun getMerchantTaxes(): Result<List<MerchantTax>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMerchantTaxes()
            handleResponse(response) { it.taxes ?: emptyList() }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تحميل إعدادات الضرائب: ${e.message}"))
        }
    }
    
    /**
     * Create new merchant tax
     * @param request Create tax request
     * @return Created tax or error message
     */
    suspend fun createMerchantTax(request: CreateMerchantTaxRequest): Result<MerchantTax> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.createMerchantTax(request)
            handleResponse(response) { it.tax!! }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في إنشاء الضريبة: ${e.message}"))
        }
    }
    
    /**
     * Update merchant tax
     * @param taxId ID of the tax to update
     * @param request Update request with new values
     * @return Updated tax or error message
     */
    suspend fun updateMerchantTax(taxId: String, request: UpdateMerchantTaxRequest): Result<MerchantTax> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateMerchantTax(taxId, request)
            handleResponse(response) { it.tax!! }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تحديث الضريبة: ${e.message}"))
        }
    }
    
    /**
     * Delete merchant tax
     * @param taxId ID of the tax to delete
     * @return Success result or error message
     */
    suspend fun deleteMerchantTax(taxId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteMerchantTax(taxId)
            handleResponse(response) { Unit }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في حذف الضريبة: ${e.message}"))
        }
    }
    
    /**
     * Generate tax report
     * @param startDate Optional start date filter
     * @param endDate Optional end date filter
     * @return Tax report or error message
     */
    suspend fun generateTaxReport(startDate: String? = null, endDate: String? = null): Result<TaxReport> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.generateTaxReport(startDate, endDate)
            handleResponse(response) { it.report!! }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في إنشاء التقرير الضريبي: ${e.message}"))
        }
    }
    
    /**
     * Get tax management statistics
     * @return Tax statistics or error message
     */
    suspend fun getTaxStats(): Result<TaxStats> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getTaxStats()
            handleResponse(response) { it.stats!! }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تحميل إحصائيات الضرائب: ${e.message}"))
        }
    }
    
    /**
     * Get default tax for the merchant
     * @return Default tax or null if no default tax exists
     */
    suspend fun getDefaultTax(): Result<MerchantTax?> = withContext(Dispatchers.IO) {
        try {
            val taxesResult = getMerchantTaxes()
            if (taxesResult.isSuccess) {
                val taxes = taxesResult.getOrNull() ?: emptyList()
                val defaultTax = taxes.firstOrNull { it.isDefault }
                Result.success(defaultTax)
            } else {
                taxesResult.map { null }
            }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تحميل الضريبة الافتراضية: ${e.message}"))
        }
    }
    
    /**
     * Set tax as default
     * @param taxId ID of the tax to set as default
     * @return Updated tax or error message
     */
    suspend fun setAsDefaultTax(taxId: String): Result<MerchantTax> = withContext(Dispatchers.IO) {
        try {
            val request = UpdateMerchantTaxRequest(isDefault = true)
            updateMerchantTax(taxId, request)
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تعيين الضريبة الافتراضية: ${e.message}"))
        }
    }
    
    /**
     * Remove default status from tax
     * @param taxId ID of the tax to remove default status from
     * @return Updated tax or error message
     */
    suspend fun removeDefaultStatus(taxId: String): Result<MerchantTax> = withContext(Dispatchers.IO) {
        try {
            val request = UpdateMerchantTaxRequest(isDefault = false)
            updateMerchantTax(taxId, request)
        } catch (e: Exception) {
            Result.failure(Exception("فشل في إزالة الضريبة الافتراضية: ${e.message}"))
        }
    }
    
    /**
     * Get taxes by category
     * @param category Tax category to filter by
     * @return Filtered list of taxes or error message
     */
    suspend fun getTaxesByCategory(category: TaxCategory): Result<List<MerchantTax>> = withContext(Dispatchers.IO) {
        try {
            val taxesResult = getMerchantTaxes()
            if (taxesResult.isSuccess) {
                val taxes = taxesResult.getOrNull() ?: emptyList()
                val filteredTaxes = taxes.filter { tax ->
                    TaxCategory.fromRate(tax.rate) == category
                }
                Result.success(filteredTaxes)
            } else {
                taxesResult
            }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تحميل الضرائب المفلترة: ${e.message}"))
        }
    }
    
    /**
     * Search taxes by name
     * @param query Search query
     * @return Filtered list of taxes or error message
     */
    suspend fun searchTaxes(query: String): Result<List<MerchantTax>> = withContext(Dispatchers.IO) {
        try {
            val taxesResult = getMerchantTaxes()
            if (taxesResult.isSuccess) {
                val taxes = taxesResult.getOrNull() ?: emptyList()
                val filteredTaxes = taxes.filter { tax ->
                    tax.name.contains(query, ignoreCase = true)
                }
                Result.success(filteredTaxes)
            } else {
                taxesResult
            }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في البحث في الضرائب: ${e.message}"))
        }
    }
    
    /**
     * Get unused taxes (taxes not applied to any invoices)
     * @return List of unused taxes or error message
     */
    suspend fun getUnusedTaxes(): Result<List<MerchantTax>> = withContext(Dispatchers.IO) {
        try {
            val taxesResult = getMerchantTaxes()
            if (taxesResult.isSuccess) {
                val taxes = taxesResult.getOrNull() ?: emptyList()
                val unusedTaxes = taxes.filter { it.invoiceCount == 0 }
                Result.success(unusedTaxes)
            } else {
                taxesResult
            }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تحميل الضرائب غير المستخدمة: ${e.message}"))
        }
    }
    
    /**
     * Get most used taxes
     * @param limit Number of taxes to return
     * @return List of most used taxes or error message
     */
    suspend fun getMostUsedTaxes(limit: Int = 5): Result<List<MerchantTax>> = withContext(Dispatchers.IO) {
        try {
            val taxesResult = getMerchantTaxes()
            if (taxesResult.isSuccess) {
                val taxes = taxesResult.getOrNull() ?: emptyList()
                val sortedTaxes = taxes.sortedByDescending { it.invoiceCount }.take(limit)
                Result.success(sortedTaxes)
            } else {
                taxesResult
            }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في تحميل الضرائب الأكثر استخداماً: ${e.message}"))
        }
    }
    
    /**
     * Calculate tax summary for given taxable amount
     * @param taxableAmount Amount to calculate tax for
     * @return Map of tax ID to calculated tax amount
     */
    suspend fun calculateTaxSummary(taxableAmount: Double): Result<Map<String, Double>> = withContext(Dispatchers.IO) {
        try {
            val taxesResult = getMerchantTaxes()
            if (taxesResult.isSuccess) {
                val taxes = taxesResult.getOrNull() ?: emptyList()
                val taxSummary = taxes.associate { tax ->
                    tax.id to tax.calculateTaxFor(taxableAmount)
                }
                Result.success(taxSummary)
            } else {
                Result.failure(Exception("فشل في حساب ملخص الضرائب"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في حساب ملخص الضرائب: ${e.message}"))
        }
    }
    
    /**
     * Validate tax before creation/update
     * @param name Tax name
     * @param rate Tax rate
     * @param existingTaxId Optional existing tax ID for updates
     * @return Validation result
     */
    suspend fun validateTax(name: String, rate: Double, existingTaxId: String? = null): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // Validate tax name
            if (!MerchantTax.isValidTaxName(name)) {
                return@withContext Result.failure(Exception("اسم الضريبة يجب أن يكون بين 2 و 100 حرف"))
            }
            
            // Validate tax rate
            if (!MerchantTax.isValidTaxRate(rate)) {
                return@withContext Result.failure(Exception("معدل الضريبة يجب أن يكون بين 0% و 100%"))
            }
            
            // Check for duplicate names
            val taxesResult = getMerchantTaxes()
            if (taxesResult.isSuccess) {
                val taxes = taxesResult.getOrNull() ?: emptyList()
                val duplicateTax = taxes.find { 
                    it.name.equals(name.trim(), ignoreCase = true) && it.id != existingTaxId
                }
                if (duplicateTax != null) {
                    return@withContext Result.failure(Exception("يوجد ضريبة بنفس الاسم مسبقاً"))
                }
            }
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(Exception("فشل في التحقق من صحة الضريبة: ${e.message}"))
        }
    }
    
    /**
     * Bulk delete unused taxes
     * @return Number of successfully deleted taxes
     */
    suspend fun bulkDeleteUnusedTaxes(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val unusedTaxesResult = getUnusedTaxes()
            if (unusedTaxesResult.isSuccess) {
                val unusedTaxes = unusedTaxesResult.getOrNull() ?: emptyList()
                var deletedCount = 0
                
                unusedTaxes.forEach { tax ->
                    val deleteResult = deleteMerchantTax(tax.id)
                    if (deleteResult.isSuccess) {
                        deletedCount++
                    }
                }
                
                Result.success(deletedCount)
            } else {
                Result.failure(Exception("فشل في تحميل الضرائب غير المستخدمة"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في الحذف المجمع للضرائب: ${e.message}"))
        }
    }
    
    /**
     * Generate tax compliance report
     * @return Tax compliance summary
     */
    suspend fun generateComplianceReport(): Result<TaxComplianceReport> = withContext(Dispatchers.IO) {
        try {
            val reportResult = generateTaxReport()
            val statsResult = getTaxStats()
            
            if (reportResult.isSuccess && statsResult.isSuccess) {
                val report = reportResult.getOrNull()!!
                val stats = statsResult.getOrNull()!!
                
                val compliance = TaxComplianceReport(
                    totalTaxes = stats.totalTaxes,
                    activeTaxes = report.taxBreakdown.count { it.invoiceCount > 0 },
                    compliancePercentage = report.summary.getTaxCompliancePercentage(),
                    totalTaxCollected = report.summary.totalTaxCollected,
                    averageTaxRate = report.summary.averageTaxRate,
                    recommendations = generateComplianceRecommendations(report, stats)
                )
                
                Result.success(compliance)
            } else {
                Result.failure(Exception("فشل في إنشاء تقرير الامتثال الضريبي"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("فشل في إنشاء تقرير الامتثال: ${e.message}"))
        }
    }
    
    /**
     * Generate compliance recommendations based on tax data
     */
    private fun generateComplianceRecommendations(report: TaxReport, stats: TaxStats): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (report.summary.invoicesWithoutTax > report.summary.totalInvoicesWithTax) {
            recommendations.add("يوجد عدد كبير من الفواتير بدون ضرائب (${report.summary.invoicesWithoutTax})")
        }
        
        if (stats.totalTaxes == 0) {
            recommendations.add("لم يتم إنشاء أي ضرائب بعد")
        } else if (stats.totalTaxes == 1) {
            recommendations.add("يُنصح بإنشاء ضرائب متعددة لتصنيف أفضل")
        }
        
        if (!stats.hasDefaultTax()) {
            recommendations.add("يُنصح بتعيين ضريبة افتراضية لسهولة الاستخدام")
        }
        
        if (stats.averageTaxRate > 20) {
            recommendations.add("متوسط معدل الضريبة مرتفع (${stats.getFormattedAverageTaxRate()})")
        }
        
        val lowUsageTaxes = report.taxBreakdown.count { it.invoiceCount == 0 }
        if (lowUsageTaxes > 0) {
            recommendations.add("يوجد $lowUsageTaxes ضرائب غير مستخدمة يمكن حذفها")
        }
        
        return recommendations
    }
    
    /**
     * Generic response handler for API responses
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
                    403 -> "إدارة الضرائب متاحة حصرياً لمشتركي الخطة الاحترافية"
                    404 -> "الضريبة غير موجودة"
                    409 -> when {
                        response.message().contains("duplicate") -> "يوجد ضريبة بنفس الاسم مسبقاً"
                        response.message().contains("used") -> "لا يمكن حذف الضريبة لأنها مستخدمة في فواتير"
                        else -> "تعارض في البيانات"
                    }
                    400 -> "بيانات غير صحيحة"
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
 * Tax Compliance Report data class
 */
data class TaxComplianceReport(
    val totalTaxes: Int,
    val activeTaxes: Int,
    val compliancePercentage: Double,
    val totalTaxCollected: Double,
    val averageTaxRate: Double,
    val recommendations: List<String>
) {
    fun getComplianceScore(): Int {
        return when {
            compliancePercentage >= 90 -> 95
            compliancePercentage >= 75 -> 85
            compliancePercentage >= 50 -> 70
            compliancePercentage >= 25 -> 50
            else -> 25
        }
    }
    
    fun getComplianceLevel(): String {
        return when {
            compliancePercentage >= 90 -> "ممتاز"
            compliancePercentage >= 75 -> "جيد جداً"
            compliancePercentage >= 50 -> "جيد"
            compliancePercentage >= 25 -> "متوسط"
            else -> "ضعيف"
        }
    }
    
    fun hasRecommendations(): Boolean {
        return recommendations.isNotEmpty()
    }
}