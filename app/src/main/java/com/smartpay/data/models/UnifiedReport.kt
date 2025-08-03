package com.smartpay.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Unified Business Reports - Pro Plan Only Feature
 * 
 * This feature allows Pro Plan merchants to create and view advanced financial reports
 * covering key financial metrics including:
 * - Expenses, Income, Transfers, Debts, Salaries, Invoices
 * - Visual analytics with charts and graphs
 * - Advanced filtering and report presets
 * - Export functionality (PDF/Excel)
 * - Historical snapshots with notes
 * 
 * Access Requirements:
 * - Pro Plan ($25.99/month) ONLY
 * - NOT available for Free or Standard Plan users
 */

data class UnifiedReportDashboardResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("dashboard")
    val dashboard: UnifiedReportDashboard? = null,
    
    @SerializedName("message")
    val message: String?
)

data class UnifiedReportGenerateResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("report")
    val report: UnifiedReportContent? = null,
    
    @SerializedName("generated_file_url")
    val generatedFileUrl: String? = null,
    
    @SerializedName("message")
    val message: String?
)

data class UnifiedReportSettingsResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("settings")
    val settings: List<UnifiedReportSetting>? = null,
    
    @SerializedName("setting")
    val setting: UnifiedReportSetting? = null,
    
    @SerializedName("message")
    val message: String?
)

data class UnifiedReportSnapshotsResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("snapshots")
    val snapshots: List<UnifiedReportSnapshot>? = null,
    
    @SerializedName("total")
    val total: Int? = null,
    
    @SerializedName("message")
    val message: String?
)

data class UnifiedReportDashboard(
    @SerializedName("summary")
    val summary: ReportSummary,
    
    @SerializedName("report_type_breakdown")
    val reportTypeBreakdown: List<ReportTypeData>,
    
    @SerializedName("time_series")
    val timeSeries: List<TimeSeriesData>,
    
    @SerializedName("category_breakdown")
    val categoryBreakdown: List<CategoryData>,
    
    @SerializedName("time_period")
    val timePeriod: String,
    
    @SerializedName("date_range")
    val dateRange: DateRange
) {
    companion object {
        /**
         * Check if user has access to unified reports feature
         * Only Pro plan subscribers can access this feature
         */
        fun hasFeatureAccess(subscriptionPlan: String?): Boolean {
            return subscriptionPlan == "Pro"
        }
        
        /**
         * Get upgrade message for Free/Standard plan users
         */
        fun getUpgradeMessage(): String {
            return "التقارير الموحدة متاحة حصرياً لمشتركي الخطة الاحترافية. احصل على تحليلات مالية متقدمة ولوحات تحكم تفاعلية مع إمكانية التصدير."
        }
        
        /**
         * Format amount for display
         */
        fun formatAmount(amount: BigDecimal): String {
            val formatter = NumberFormat.getNumberInstance(Locale("ar"))
            return "${formatter.format(amount)} ل.س"
        }
        
        fun formatAmount(amount: Double): String {
            val formatter = NumberFormat.getNumberInstance(Locale("ar"))
            return "${formatter.format(amount)} ل.س"
        }
    }
    
    /**
     * Get net income color (green for positive, red for negative)
     */
    fun getNetIncomeColor(): Long {
        return if (summary.netIncome >= 0) 0xFF00D632 else 0xFFE53E3E
    }
    
    /**
     * Get formatted time period display
     */
    fun getFormattedTimePeriod(): String {
        return when (timePeriod) {
            "daily" -> "يومي"
            "weekly" -> "أسبوعي"
            "monthly" -> "شهري"
            "custom" -> "مخصص"
            else -> timePeriod
        }
    }
}

data class ReportSummary(
    @SerializedName("total_income")
    val totalIncome: Double,
    
    @SerializedName("total_expenses")
    val totalExpenses: Double,
    
    @SerializedName("net_income")
    val netIncome: Double,
    
    @SerializedName("transaction_count")
    val transactionCount: Int
)

data class ReportTypeData(
    @SerializedName("type")
    val type: String,
    
    @SerializedName("count")
    val count: Int,
    
    @SerializedName("total_amount")
    val totalAmount: Double,
    
    @SerializedName("avg_amount")
    val avgAmount: Double
) {
    fun getTypeDisplayName(): String {
        return when (type) {
            "expense" -> "مصروفات"
            "income" -> "دخل"
            "transfer" -> "تحويلات"
            "debt" -> "ديون"
            "salary" -> "رواتب"
            "invoice" -> "فواتير"
            else -> type
        }
    }
    
    fun getTypeEmoji(): String {
        return when (type) {
            "expense" -> "💸"
            "income" -> "💰"
            "transfer" -> "🔄"
            "debt" -> "🧾"
            "salary" -> "👥"
            "invoice" -> "💳"
            else -> "📊"
        }
    }
    
    fun getTypeColor(): Long {
        return when (type) {
            "expense" -> 0xFFE53E3E
            "income" -> 0xFF00D632
            "transfer" -> 0xFF2196F3
            "debt" -> 0xFFFF9800
            "salary" -> 0xFF9C27B0
            "invoice" -> 0xFF00BCD4
            else -> 0xFF666666
        }
    }
}

data class TimeSeriesData(
    @SerializedName("date")
    val date: String,
    
    @SerializedName("report_type")
    val reportType: String,
    
    @SerializedName("daily_total")
    val dailyTotal: Double
)

data class CategoryData(
    @SerializedName("category")
    val category: String,
    
    @SerializedName("count")
    val count: Int,
    
    @SerializedName("total_amount")
    val totalAmount: Double
)

data class DateRange(
    @SerializedName("start")
    val start: String?,
    
    @SerializedName("end")
    val end: String?
)

data class UnifiedReportContent(
    @SerializedName("filters")
    val filters: ReportFilters,
    
    @SerializedName("summary")
    val summary: ReportContentSummary,
    
    @SerializedName("data")
    val data: List<ReportDataItem>
)

data class ReportContentSummary(
    @SerializedName("total_records")
    val totalRecords: Int,
    
    @SerializedName("total_amount")
    val totalAmount: Double,
    
    @SerializedName("by_type")
    val byType: Map<String, TypeSummary>
)

data class TypeSummary(
    @SerializedName("count")
    val count: Int,
    
    @SerializedName("total")
    val total: Double
)

data class ReportDataItem(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("report_type")
    val reportType: String,
    
    @SerializedName("amount")
    val amount: Double,
    
    @SerializedName("note")
    val note: String?,
    
    @SerializedName("occurred_at")
    val occurredAt: String,
    
    @SerializedName("category_name")
    val categoryName: String?
) {
    fun getFormattedAmount(): String {
        return UnifiedReportDashboard.formatAmount(amount)
    }
    
    fun getFormattedDate(): String {
        return try {
            val dateTime = LocalDateTime.parse(occurredAt.substring(0, 19))
            val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm", Locale("ar"))
            dateTime.format(formatter)
        } catch (e: Exception) {
            occurredAt
        }
    }
    
    fun getTypeDisplayName(): String {
        return when (reportType) {
            "expense" -> "مصروف"
            "income" -> "دخل"
            "transfer" -> "تحويل"
            "debt" -> "دين"
            "salary" -> "راتب"
            "invoice" -> "فاتورة"
            else -> reportType
        }
    }
}

data class UnifiedReportSetting(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("filters")
    val filters: ReportFilters,
    
    @SerializedName("created_at")
    val createdAt: String,
    
    @SerializedName("updated_at")
    val updatedAt: String
) {
    fun getFormattedCreatedDate(): String {
        return try {
            val dateTime = LocalDateTime.parse(createdAt.substring(0, 19))
            val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale("ar"))
            dateTime.format(formatter)
        } catch (e: Exception) {
            createdAt
        }
    }
}

data class UnifiedReportSnapshot(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("report_type")
    val reportType: String,
    
    @SerializedName("time_range")
    val timeRange: String,
    
    @SerializedName("generated_file_url")
    val generatedFileUrl: String?,
    
    @SerializedName("notes")
    val notes: String?,
    
    @SerializedName("created_at")
    val createdAt: String
) {
    fun getFormattedCreatedDate(): String {
        return try {
            val dateTime = LocalDateTime.parse(createdAt.substring(0, 19))
            val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm", Locale("ar"))
            dateTime.format(formatter)
        } catch (e: Exception) {
            createdAt
        }
    }
    
    fun hasFile(): Boolean {
        return !generatedFileUrl.isNullOrEmpty()
    }
}

data class ReportFilters(
    @SerializedName("report_types")
    val reportTypes: List<String>? = null,
    
    @SerializedName("categories")
    val categories: List<String>? = null,
    
    @SerializedName("time_period")
    val timePeriod: String,
    
    @SerializedName("start_date")
    val startDate: String? = null,
    
    @SerializedName("end_date")
    val endDate: String? = null,
    
    @SerializedName("min_amount")
    val minAmount: Double? = null,
    
    @SerializedName("max_amount")
    val maxAmount: Double? = null,
    
    @SerializedName("notes_filter")
    val notesFilter: String? = null
) {
    fun getDisplaySummary(): String {
        val parts = mutableListOf<String>()
        
        reportTypes?.let {
            if (it.isNotEmpty()) {
                parts.add("الأنواع: ${it.size}")
            }
        }
        
        categories?.let {
            if (it.isNotEmpty()) {
                parts.add("التصنيفات: ${it.size}")
            }
        }
        
        parts.add("الفترة: ${getTimePeriodDisplay()}")
        
        return parts.joinToString(" • ")
    }
    
    private fun getTimePeriodDisplay(): String {
        return when (timePeriod) {
            "daily" -> "يومي"
            "weekly" -> "أسبوعي"
            "monthly" -> "شهري"
            "custom" -> "مخصص"
            else -> timePeriod
        }
    }
}

data class CreateReportSettingRequest(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("filters")
    val filters: ReportFilters
)

data class GenerateReportRequest(
    @SerializedName("report_types")
    val reportTypes: List<String>,
    
    @SerializedName("categories")
    val categories: List<String>? = null,
    
    @SerializedName("time_period")
    val timePeriod: String,
    
    @SerializedName("start_date")
    val startDate: String? = null,
    
    @SerializedName("end_date")
    val endDate: String? = null,
    
    @SerializedName("min_amount")
    val minAmount: Double? = null,
    
    @SerializedName("max_amount")
    val maxAmount: Double? = null,
    
    @SerializedName("notes_filter")
    val notesFilter: String? = null,
    
    @SerializedName("export_format")
    val exportFormat: String? = null,
    
    @SerializedName("snapshot_notes")
    val snapshotNotes: String? = null
)

/**
 * Time period options for reports
 */
enum class TimePeriod(val value: String, val displayName: String, val emoji: String) {
    DAILY("daily", "يومي", "📅"),
    WEEKLY("weekly", "أسبوعي", "📆"),
    MONTHLY("monthly", "شهري", "🗓️"),
    CUSTOM("custom", "مخصص", "⚙️");
    
    companion object {
        fun getAllOptions(): List<TimePeriod> {
            return values().toList()
        }
        
        fun fromValue(value: String): TimePeriod? {
            return values().find { it.value == value }
        }
    }
}

/**
 * Export format options
 */
enum class ExportFormat(val value: String, val displayName: String, val emoji: String) {
    PDF("pdf", "PDF", "📄"),
    EXCEL("excel", "Excel", "📊");
    
    companion fun getAllOptions(): List<ExportFormat> {
        return values().toList()
    }
}