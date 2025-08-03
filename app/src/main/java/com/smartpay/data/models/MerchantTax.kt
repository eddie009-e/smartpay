package com.smartpay.models

import com.google.gson.annotations.SerializedName
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Merchant Tax Management - Pro Plan Only Feature
 * 
 * This feature allows Pro Plan merchants to create and manage custom tax settings:
 * - Define multiple tax rates and names
 * - Apply taxes to invoices automatically
 * - Generate comprehensive tax reports
 * - Track tax compliance and collection
 * 
 * Access Requirements:
 * - Pro Plan ($25.99/month) ONLY
 * - NOT available for Free or Standard Plan users
 * - Complete tax reporting and analytics
 */

data class MerchantTaxResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("taxes")
    val taxes: List<MerchantTax>? = null,
    
    @SerializedName("tax")
    val tax: MerchantTax? = null,
    
    @SerializedName("total")
    val total: Int? = null,
    
    @SerializedName("message")
    val message: String?
)

data class TaxReportResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("report")
    val report: TaxReport? = null,
    
    @SerializedName("message")
    val message: String?
)

data class TaxStatsResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("stats")
    val stats: TaxStats? = null,
    
    @SerializedName("message")
    val message: String?
)

data class MerchantTax(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("rate")
    val rate: Double,
    
    @SerializedName("is_default")
    val isDefault: Boolean,
    
    @SerializedName("invoice_count")
    val invoiceCount: Int,
    
    @SerializedName("created_at")
    val createdAt: String
) {
    companion object {
        /**
         * Check if user has access to tax management feature
         * Only Pro plan subscribers can access this feature
         */
        fun hasFeatureAccess(subscriptionPlan: String?): Boolean {
            return subscriptionPlan == "Pro"
        }
        
        /**
         * Get upgrade message for Free/Standard plan users
         */
        fun getUpgradeMessage(): String {
            return "إدارة الضرائب متاحة حصرياً لمشتركي الخطة الاحترافية. قم بترقية اشتراكك للخطة الاحترافية لإدارة الضرائب وإنشاء التقارير الضريبية."
        }
        
        /**
         * Get feature description for settings
         */
        fun getFeatureDescription(): String {
            return "إدارة الضرائب وإنشاء التقارير الضريبية المتقدمة"
        }
        
        /**
         * Format tax rate for display
         */
        fun formatTaxRate(rate: Double): String {
            return "${String.format("%.2f", rate)}%"
        }
        
        /**
         * Format amount for display
         */
        fun formatAmount(amount: Double): String {
            val formatter = NumberFormat.getNumberInstance(Locale("ar"))
            return "${formatter.format(amount)} ل.س"
        }
        
        /**
         * Calculate tax amount from taxable amount and rate
         */
        fun calculateTaxAmount(taxableAmount: Double, taxRate: Double): Double {
            return (taxableAmount * taxRate) / 100.0
        }
        
        /**
         * Calculate total amount including tax
         */
        fun calculateTotalWithTax(taxableAmount: Double, taxRate: Double): Double {
            return taxableAmount + calculateTaxAmount(taxableAmount, taxRate)
        }
        
        /**
         * Validate tax rate (must be between 0 and 100)
         */
        fun isValidTaxRate(rate: Double): Boolean {
            return rate >= 0.0 && rate <= 100.0
        }
        
        /**
         * Validate tax name (must be 2-100 characters)
         */
        fun isValidTaxName(name: String): Boolean {
            return name.trim().length in 2..100
        }
    }
    
    /**
     * Get formatted tax rate
     */
    fun getFormattedRate(): String {
        return formatTaxRate(rate)
    }
    
    /**
     * Get formatted creation date
     */
    fun getFormattedCreatedDate(): String {
        return try {
            val dateTime = LocalDateTime.parse(createdAt.substring(0, 19))
            val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale("ar"))
            dateTime.format(formatter)
        } catch (e: Exception) {
            createdAt
        }
    }
    
    /**
     * Calculate tax amount for given taxable amount
     */
    fun calculateTaxFor(taxableAmount: Double): Double {
        return calculateTaxAmount(taxableAmount, rate)
    }
    
    /**
     * Calculate total amount including this tax
     */
    fun calculateTotalFor(taxableAmount: Double): Double {
        return calculateTotalWithTax(taxableAmount, rate)
    }
    
    /**
     * Get tax usage status
     */
    fun getUsageStatus(): String {
        return when {
            invoiceCount == 0 -> "غير مستخدم"
            invoiceCount == 1 -> "فاتورة واحدة"
            invoiceCount < 10 -> "$invoiceCount فواتير"
            else -> "$invoiceCount فاتورة"
        }
    }
    
    /**
     * Check if tax can be deleted (not used in invoices)
     */
    fun canBeDeleted(): Boolean {
        return invoiceCount == 0
    }
    
    /**
     * Get tax category based on rate
     */
    fun getTaxCategory(): String {
        return when {
            rate == 0.0 -> "معفى من الضريبة"
            rate < 5.0 -> "ضريبة منخفضة"
            rate < 15.0 -> "ضريبة متوسطة"
            rate < 25.0 -> "ضريبة عالية"
            else -> "ضريبة مرتفعة جداً"
        }
    }
    
    /**
     * Get tax category color
     */
    fun getTaxCategoryColor(): Long {
        return when {
            rate == 0.0 -> 0xFF999999
            rate < 5.0 -> 0xFF00D632
            rate < 15.0 -> 0xFF2196F3
            rate < 25.0 -> 0xFFFF9800
            else -> 0xFFE53E3E
        }
    }
    
    /**
     * Get default status display
     */
    fun getDefaultStatusDisplay(): String {
        return if (isDefault) "افتراضي" else ""
    }
    
    /**
     * Get default status color
     */
    fun getDefaultStatusColor(): Long {
        return if (isDefault) 0xFF00D632 else 0xFF999999
    }
}

data class TaxReport(
    @SerializedName("period")
    val period: ReportPeriod,
    
    @SerializedName("summary")
    val summary: TaxReportSummary,
    
    @SerializedName("tax_breakdown")
    val taxBreakdown: List<TaxBreakdownItem>
)

data class ReportPeriod(
    @SerializedName("start_date")
    val startDate: String?,
    
    @SerializedName("end_date")
    val endDate: String?
) {
    fun getFormattedPeriod(): String {
        return when {
            startDate == null && endDate == null -> "جميع الفترات"
            startDate != null && endDate != null -> {
                val start = try {
                    LocalDateTime.parse(startDate.substring(0, 19)).format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                } catch (e: Exception) { startDate }
                val end = try {
                    LocalDateTime.parse(endDate.substring(0, 19)).format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                } catch (e: Exception) { endDate }
                "من $start إلى $end"
            }
            startDate != null -> {
                val start = try {
                    LocalDateTime.parse(startDate.substring(0, 19)).format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                } catch (e: Exception) { startDate }
                "من $start"
            }
            endDate != null -> {
                val end = try {
                    LocalDateTime.parse(endDate.substring(0, 19)).format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                } catch (e: Exception) { endDate }
                "حتى $end"
            }
            else -> "غير محدد"
        }
    }
}

data class TaxReportSummary(
    @SerializedName("total_invoices_with_tax")
    val totalInvoicesWithTax: Int,
    
    @SerializedName("total_tax_collected")
    val totalTaxCollected: Double,
    
    @SerializedName("total_taxable_amount")
    val totalTaxableAmount: Double,
    
    @SerializedName("total_invoice_amount")
    val totalInvoiceAmount: Double,
    
    @SerializedName("unique_taxes_used")
    val uniqueTaxesUsed: Int,
    
    @SerializedName("invoices_without_tax")
    val invoicesWithoutTax: Int,
    
    @SerializedName("average_tax_rate")
    val averageTaxRate: Double
) {
    fun getFormattedTotalTaxCollected(): String {
        return MerchantTax.formatAmount(totalTaxCollected)
    }
    
    fun getFormattedTotalTaxableAmount(): String {
        return MerchantTax.formatAmount(totalTaxableAmount)
    }
    
    fun getFormattedTotalInvoiceAmount(): String {
        return MerchantTax.formatAmount(totalInvoiceAmount)
    }
    
    fun getFormattedAverageTaxRate(): String {
        return MerchantTax.formatTaxRate(averageTaxRate)
    }
    
    fun getTotalInvoices(): Int {
        return totalInvoicesWithTax + invoicesWithoutTax
    }
    
    fun getTaxCompliancePercentage(): Double {
        val total = getTotalInvoices()
        return if (total > 0) (totalInvoicesWithTax.toDouble() / total.toDouble()) * 100 else 0.0
    }
    
    fun getFormattedTaxCompliancePercentage(): String {
        return "${String.format("%.1f", getTaxCompliancePercentage())}%"
    }
}

data class TaxBreakdownItem(
    @SerializedName("tax_id")
    val taxId: String,
    
    @SerializedName("tax_name")
    val taxName: String,
    
    @SerializedName("tax_rate")
    val taxRate: Double,
    
    @SerializedName("invoice_count")
    val invoiceCount: Int,
    
    @SerializedName("total_taxable_amount")
    val totalTaxableAmount: Double,
    
    @SerializedName("total_tax_collected")
    val totalTaxCollected: Double,
    
    @SerializedName("total_invoice_amount")
    val totalInvoiceAmount: Double
) {
    fun getFormattedTaxRate(): String {
        return MerchantTax.formatTaxRate(taxRate)
    }
    
    fun getFormattedTotalTaxCollected(): String {
        return MerchantTax.formatAmount(totalTaxCollected)
    }
    
    fun getFormattedTotalTaxableAmount(): String {
        return MerchantTax.formatAmount(totalTaxableAmount)
    }
    
    fun getFormattedTotalInvoiceAmount(): String {
        return MerchantTax.formatAmount(totalInvoiceAmount)
    }
    
    fun getInvoiceCountDisplay(): String {
        return when {
            invoiceCount == 0 -> "لا توجد فواتير"
            invoiceCount == 1 -> "فاتورة واحدة"
            invoiceCount < 11 -> "$invoiceCount فواتير"
            else -> "$invoiceCount فاتورة"
        }
    }
    
    fun getContributionPercentage(totalTaxCollected: Double): Double {
        return if (totalTaxCollected > 0) (this.totalTaxCollected / totalTaxCollected) * 100 else 0.0
    }
    
    fun getFormattedContributionPercentage(totalTaxCollected: Double): String {
        return "${String.format("%.1f", getContributionPercentage(totalTaxCollected))}%"
    }
}

data class TaxStats(
    @SerializedName("total_taxes")
    val totalTaxes: Int,
    
    @SerializedName("default_taxes")
    val defaultTaxes: Int,
    
    @SerializedName("average_tax_rate")
    val averageTaxRate: Double,
    
    @SerializedName("highest_tax_rate")
    val highestTaxRate: Double,
    
    @SerializedName("lowest_tax_rate")
    val lowestTaxRate: Double,
    
    @SerializedName("invoices_with_tax_this_month")
    val invoicesWithTaxThisMonth: Int,
    
    @SerializedName("total_tax_collected_this_month")
    val totalTaxCollectedThisMonth: Double
) {
    fun getFormattedAverageTaxRate(): String {
        return MerchantTax.formatTaxRate(averageTaxRate)
    }
    
    fun getFormattedHighestTaxRate(): String {
        return MerchantTax.formatTaxRate(highestTaxRate)
    }
    
    fun getFormattedLowestTaxRate(): String {
        return MerchantTax.formatTaxRate(lowestTaxRate)
    }
    
    fun getFormattedTotalTaxCollectedThisMonth(): String {
        return MerchantTax.formatAmount(totalTaxCollectedThisMonth)
    }
    
    fun hasMultipleTaxes(): Boolean {
        return totalTaxes > 1
    }
    
    fun hasDefaultTax(): Boolean {
        return defaultTaxes > 0
    }
}

// Request DTOs
data class CreateMerchantTaxRequest(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("rate")
    val rate: Double,
    
    @SerializedName("is_default")
    val isDefault: Boolean = false
)

data class UpdateMerchantTaxRequest(
    @SerializedName("name")
    val name: String? = null,
    
    @SerializedName("rate")
    val rate: Double? = null,
    
    @SerializedName("is_default")
    val isDefault: Boolean? = null
)

data class TaxReportRequest(
    @SerializedName("start_date")
    val startDate: String? = null,
    
    @SerializedName("end_date")
    val endDate: String? = null
)

/**
 * Tax categories for better organization
 */
enum class TaxCategory(val displayName: String, val description: String, val color: Long) {
    TAX_EXEMPT("معفى من الضريبة", "معدل 0%", 0xFF999999),
    LOW_TAX("ضريبة منخفضة", "أقل من 5%", 0xFF00D632),
    MEDIUM_TAX("ضريبة متوسطة", "5% - 15%", 0xFF2196F3),
    HIGH_TAX("ضريبة عالية", "15% - 25%", 0xFFFF9800),
    VERY_HIGH_TAX("ضريبة مرتفعة جداً", "أكثر من 25%", 0xFFE53E3E);
    
    companion object {
        fun fromRate(rate: Double): TaxCategory {
            return when {
                rate == 0.0 -> TAX_EXEMPT
                rate < 5.0 -> LOW_TAX
                rate < 15.0 -> MEDIUM_TAX
                rate < 25.0 -> HIGH_TAX
                else -> VERY_HIGH_TAX
            }
        }
    }
}

/**
 * Predefined tax templates for common Syrian tax rates
 */
enum class SyrianTaxTemplate(val displayName: String, val rate: Double, val description: String) {
    NO_TAX("بدون ضريبة", 0.0, "معفى من الضريبة"),
    VAT_STANDARD("ضريبة القيمة المضافة", 11.0, "المعدل القياسي للضريبة في سوريا"),
    SALES_TAX_LOW("ضريبة مبيعات منخفضة", 5.0, "ضريبة مبيعات مخفضة"),
    SALES_TAX_STANDARD("ضريبة مبيعات قياسية", 10.0, "ضريبة مبيعات عادية"),
    SERVICE_TAX("ضريبة خدمات", 15.0, "ضريبة على الخدمات"),
    LUXURY_TAX("ضريبة كماليات", 20.0, "ضريبة على السلع الكمالية");
    
    companion object {
        fun getAllTemplates(): List<SyrianTaxTemplate> {
            return values().toList()
        }
    }
}