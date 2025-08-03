package com.smartpay.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Advanced Financial Reports - Pro Plan Feature
 * 
 * This feature allows merchants to record and analyze financial events such as
 * expenses, income, transfers, invoices, salary payments, and debts.
 * 
 * Access Requirements:
 * - Pro Plan ($25.99/month) ONLY
 * - NOT available for Free or Standard Plan users
 */

data class FinancialReportResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("reports")
    val reports: List<FinancialReport>? = null,
    
    @SerializedName("report")
    val report: FinancialReport? = null,
    
    @SerializedName("total")
    val total: Int? = null,
    
    @SerializedName("message")
    val message: String?
)

data class FinancialReportSummaryResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("summary")
    val summary: FinancialReportSummary? = null,
    
    @SerializedName("message")
    val message: String?
)

data class FinancialReportGraphResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("graph_data")
    val graphData: List<GraphDataPoint>? = null,
    
    @SerializedName("message")
    val message: String?
)

data class FinancialReport(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("merchant_id")
    val merchantId: String,
    
    @SerializedName("report_type")
    val reportType: String,
    
    @SerializedName("amount")
    val amount: BigDecimal,
    
    @SerializedName("note")
    val note: String?,
    
    @SerializedName("category_id")
    val categoryId: String?,
    
    @SerializedName("category_name")
    val categoryName: String?,
    
    @SerializedName("occurred_at")
    val occurredAt: String,
    
    @SerializedName("created_at")
    val createdAt: String
) {
    companion object {
        /**
         * Check if user has access to financial reports feature
         * Only Pro plan subscribers can access this feature
         */
        fun hasFeatureAccess(subscriptionPlan: String?): Boolean {
            return subscriptionPlan == "Pro"
        }
        
        /**
         * Get upgrade message for Free/Standard plan users
         */
        fun getUpgradeMessage(): String {
            return "التقارير المالية المتقدمة متاحة فقط لمشتركي الخطة الاحترافية. قم بترقية اشتراكك للحصول على تحليلات مالية شاملة وتصدير التقارير."
        }
        
        /**
         * Format amount for display
         */
        fun formatAmount(amount: BigDecimal): String {
            val formatter = NumberFormat.getNumberInstance(Locale("ar"))
            return "${formatter.format(amount)} ل.س"
        }
        
        /**
         * Format amount for display - Double overload
         */
        fun formatAmount(amount: Double): String {
            val formatter = NumberFormat.getNumberInstance(Locale("ar"))
            return "${formatter.format(amount)} ل.س"
        }
        
        /**
         * Format date for display
         */
        fun formatDate(dateString: String): String {
            return try {
                val dateTime = LocalDateTime.parse(dateString.substring(0, 19))
                val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm", Locale("ar"))
                dateTime.format(formatter)
            } catch (e: Exception) {
                dateString
            }
        }
        
        /**
         * Format date for display (date only)
         */
        fun formatDateOnly(dateString: String): String {
            return try {
                val dateTime = LocalDateTime.parse(dateString.substring(0, 19))
                val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale("ar"))
                dateTime.format(formatter)
            } catch (e: Exception) {
                dateString
            }
        }
    }
    
    /**
     * Get formatted report type with emoji
     */
    fun getReportTypeDisplay(): String {
        val type = ReportType.fromValue(reportType)
        return "${type?.emoji ?: "📊"} ${type?.displayName ?: reportType}"
    }
    
    /**
     * Get formatted amount with currency
     */
    fun getFormattedAmount(): String {
        return formatAmount(amount)
    }
    
    /**
     * Get formatted occurred date
     */
    fun getFormattedDate(): String {
        return formatDate(occurredAt)
    }
    
    /**
     * Get color for report type
     */
    fun getTypeColor(): Long {
        return ReportType.fromValue(reportType)?.color ?: 0xFF666666
    }
}

data class FinancialReportSummary(
    @SerializedName("total_income")
    val totalIncome: BigDecimal,
    
    @SerializedName("total_expenses")
    val totalExpenses: BigDecimal,
    
    @SerializedName("total_transfers")
    val totalTransfers: BigDecimal,
    
    @SerializedName("total_debts")
    val totalDebts: BigDecimal,
    
    @SerializedName("total_salaries")
    val totalSalaries: BigDecimal,
    
    @SerializedName("total_invoices")
    val totalInvoices: BigDecimal,
    
    @SerializedName("net_income")
    val netIncome: BigDecimal,
    
    @SerializedName("report_count")
    val reportCount: Int,
    
    @SerializedName("period_start")
    val periodStart: String?,
    
    @SerializedName("period_end")
    val periodEnd: String?
) {
    /**
     * Get formatted net income with color
     */
    fun getFormattedNetIncome(): String {
        return FinancialReport.formatAmount(netIncome)
    }
    
    /**
     * Get net income color (green for positive, red for negative)
     */
    fun getNetIncomeColor(): Long {
        return if (netIncome >= BigDecimal.ZERO) 0xFF00D632 else 0xFFE53E3E
    }
    
    /**
     * Get period display text
     */
    fun getPeriodDisplay(): String {
        return if (periodStart != null && periodEnd != null) {
            "${FinancialReport.formatDateOnly(periodStart)} - ${FinancialReport.formatDateOnly(periodEnd)}"
        } else {
            "جميع الفترات"
        }
    }
}

data class GraphDataPoint(
    @SerializedName("label")
    val label: String,
    
    @SerializedName("value")
    val value: BigDecimal,
    
    @SerializedName("count")
    val count: Int? = null,
    
    @SerializedName("color")
    val color: String? = null
)

// Type alias for easier usage in charts
typealias FinancialReportGraphData = GraphDataPoint

data class CreateFinancialReportRequest(
    @SerializedName("report_type")
    val reportType: String,
    
    @SerializedName("amount")
    val amount: BigDecimal,
    
    @SerializedName("note")
    val note: String?,
    
    @SerializedName("category_id")
    val categoryId: String?,
    
    @SerializedName("occurred_at")
    val occurredAt: String
)

enum class ReportType(
    val value: String, 
    val displayName: String, 
    val emoji: String, 
    val color: Long,
    val isIncome: Boolean = false
) {
    EXPENSE("expense", "مصروف", "💸", 0xFFE53E3E, false),
    INCOME("income", "دخل", "💰", 0xFF00D632, true),
    TRANSFER("transfer", "تحويل", "🔄", 0xFF2196F3, false),
    DEBT("debt", "دين", "🧾", 0xFFFF9800, false),
    SALARY("salary", "راتب", "👥", 0xFF9C27B0, false),
    INVOICE("invoice", "فاتورة", "💳", 0xFF00BCD4, true);
    
    companion object {
        fun fromValue(value: String): ReportType? {
            return values().find { it.value == value }
        }
        
        fun getAllOptions(): List<ReportType> {
            return values().toList()
        }
        
        fun getIncomeTypes(): List<ReportType> {
            return values().filter { it.isIncome }
        }
        
        fun getExpenseTypes(): List<ReportType> {
            return values().filter { !it.isIncome }
        }
    }
}

data class ReportFilters(
    val reportType: String? = null,
    val categoryId: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val minAmount: BigDecimal? = null,
    val maxAmount: BigDecimal? = null
)