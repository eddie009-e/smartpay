package com.smartpay.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class RecurringInvoiceResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("recurring_invoices")
    val recurringInvoices: List<RecurringInvoice>? = null,
    
    @SerializedName("recurring_invoice")
    val recurringInvoice: RecurringInvoice? = null,
    
    @SerializedName("total")
    val total: Int? = null,
    
    @SerializedName("message")
    val message: String?
)

data class RecurringInvoice(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("merchant_id")
    val merchantId: String,
    
    @SerializedName("customer_id")
    val customerId: String,
    
    @SerializedName("amount")
    val amount: BigDecimal,
    
    @SerializedName("description")
    val description: String?,
    
    @SerializedName("recurrence_interval")
    val recurrenceInterval: String,
    
    @SerializedName("next_run_date")
    val nextRunDate: String,
    
    @SerializedName("end_date")
    val endDate: String?,
    
    @SerializedName("is_active")
    val isActive: Boolean,
    
    @SerializedName("created_at")
    val createdAt: String,
    
    @SerializedName("customer_name")
    val customerName: String?,
    
    @SerializedName("customer_phone")
    val customerPhone: String?
) {
    companion object {
        /**
         * Get Arabic translation for recurrence intervals
         */
        fun getRecurrenceDisplayName(interval: String): String {
            return when (interval) {
                "daily" -> "يومياً"
                "weekly" -> "أسبوعياً"
                "monthly" -> "شهرياً"
                else -> interval
            }
        }
        
        /**
         * Get recurrence interval emoji
         */
        fun getRecurrenceEmoji(interval: String): String {
            return when (interval) {
                "daily" -> "📅"
                "weekly" -> "📆"
                "monthly" -> "🗓️"
                else -> "⏰"
            }
        }
        
        /**
         * Get status color
         */
        fun getStatusColor(isActive: Boolean, nextRunDate: String?): Long {
            return if (isActive) {
                // Check if next run date is soon (within 3 days)
                val daysDiff = try {
                    val nextDate = java.time.LocalDate.parse(nextRunDate)
                    val today = java.time.LocalDate.now()
                    java.time.temporal.ChronoUnit.DAYS.between(today, nextDate)
                } catch (e: Exception) {
                    999
                }
                
                when {
                    daysDiff <= 0 -> 0xFFE53E3E // Red - overdue
                    daysDiff <= 3 -> 0xFFFF9800 // Orange - soon
                    else -> 0xFF00D632 // Green - active
                }
            } else {
                0xFF666666 // Grey - inactive
            }
        }
        
        /**
         * Get status display text
         */
        fun getStatusDisplay(isActive: Boolean): String {
            return if (isActive) "نشط" else "ملغي"
        }
    }
}

data class CreateRecurringInvoiceRequest(
    @SerializedName("customer_id")
    val customerId: String,
    
    @SerializedName("amount")
    val amount: BigDecimal,
    
    @SerializedName("description")
    val description: String?,
    
    @SerializedName("recurrence_interval")
    val recurrenceInterval: String,
    
    @SerializedName("next_run_date")
    val nextRunDate: String,
    
    @SerializedName("end_date")
    val endDate: String?
)

data class Customer(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("phone")
    val phone: String?,
    
    @SerializedName("email")
    val email: String?
) {
    fun getDisplayName(): String {
        return when {
            name.isNotBlank() -> name
            !phone.isNullOrBlank() -> phone
            !email.isNullOrBlank() -> email
            else -> "عميل غير معروف"
        }
    }
}

enum class RecurrenceInterval(val value: String, val displayName: String, val emoji: String) {
    DAILY("daily", "يومياً", "📅"),
    WEEKLY("weekly", "أسبوعياً", "📆"),
    MONTHLY("monthly", "شهرياً", "🗓️");
    
    companion object {
        fun fromValue(value: String): RecurrenceInterval? {
            return values().find { it.value == value }
        }
        
        fun getAllOptions(): List<RecurrenceInterval> {
            return values().toList()
        }
    }
}