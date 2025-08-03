package com.smartpay.models

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Automatic Reminders System - Standard Plan Feature
 * 
 * This feature allows merchants to create and manage automatic reminders for 
 * invoices, payments, and custom notifications.
 * 
 * Access Requirements:
 * - Standard Plan ($10.99/month) or higher
 * - Pro Plan ($25.99/month) 
 * - NOT available for Free Plan users
 */

data class AutomaticReminderResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("reminders")
    val reminders: List<AutomaticReminder>? = null,
    
    @SerializedName("reminder")
    val reminder: AutomaticReminder? = null,
    
    @SerializedName("total")
    val total: Int? = null,
    
    @SerializedName("message")
    val message: String?
)

data class AutomaticReminder(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("merchant_id")
    val merchantId: String,
    
    @SerializedName("user_id")
    val userId: String?,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("message")
    val message: String?,
    
    @SerializedName("reminder_type")
    val reminderType: String,
    
    @SerializedName("scheduled_at")
    val scheduledAt: String,
    
    @SerializedName("is_recurring")
    val isRecurring: Boolean,
    
    @SerializedName("recurrence_interval")
    val recurrenceInterval: String?,
    
    @SerializedName("is_sent")
    val isSent: Boolean,
    
    @SerializedName("created_at")
    val createdAt: String
) {
    companion object {
        /**
         * Check if user has access to automatic reminders feature
         * Only Standard and Pro plan subscribers can access this feature
         */
        fun hasFeatureAccess(subscriptionPlan: String?): Boolean {
            return subscriptionPlan == "Standard" || subscriptionPlan == "Pro"
        }
        
        /**
         * Get upgrade message for Free plan users
         */
        fun getUpgradeMessage(): String {
            return "التذكيرات التلقائية متاحة فقط لمشتركي الخطة القياسية والاحترافية. قم بترقية اشتراكك لإنشاء تذكيرات ذكية للمدفوعات والفواتير."
        }
        
        /**
         * Get reminder type display names in Arabic
         */
        fun getReminderTypeDisplayNames(): Map<String, String> {
            return mapOf(
                "invoice" to "فاتورة",
                "payment" to "دفعة",
                "custom" to "مخصص"
            )
        }
        
        /**
         * Get reminder type emojis
         */
        fun getReminderTypeEmojis(): Map<String, String> {
            return mapOf(
                "invoice" to "📄",
                "payment" to "💰",
                "custom" to "📌"
            )
        }
        
        /**
         * Get recurrence interval display names in Arabic
         */
        fun getRecurrenceIntervalDisplayNames(): Map<String, String> {
            return mapOf(
                "daily" to "يومياً",
                "weekly" to "أسبوعياً",
                "monthly" to "شهرياً"
            )
        }
        
        /**
         * Get recurrence interval emojis
         */
        fun getRecurrenceIntervalEmojis(): Map<String, String> {
            return mapOf(
                "daily" to "📅",
                "weekly" to "📆",
                "monthly" to "🗓️"
            )
        }
        
        /**
         * Get status display text
         */
        fun getStatusDisplay(isSent: Boolean, scheduledAt: String): String {
            return try {
                val scheduledDateTime = LocalDateTime.parse(scheduledAt.substring(0, 19))
                val now = LocalDateTime.now()
                
                when {
                    isSent -> "تم الإرسال"
                    scheduledDateTime.isBefore(now) -> "متأخر"
                    else -> "قادم"
                }
            } catch (e: Exception) {
                if (isSent) "تم الإرسال" else "قادم"
            }
        }
        
        /**
         * Get status color
         */
        fun getStatusColor(isSent: Boolean, scheduledAt: String): Long {
            return try {
                val scheduledDateTime = LocalDateTime.parse(scheduledAt.substring(0, 19))
                val now = LocalDateTime.now()
                
                when {
                    isSent -> 0xFF00D632 // Green - sent
                    scheduledDateTime.isBefore(now) -> 0xFFE53E3E // Red - overdue
                    else -> 0xFF2196F3 // Blue - upcoming
                }
            } catch (e: Exception) {
                if (isSent) 0xFF00D632 else 0xFF2196F3
            }
        }
        
        /**
         * Format scheduled date for display
         */
        fun formatScheduledDate(scheduledAt: String): String {
            return try {
                val dateTime = LocalDateTime.parse(scheduledAt.substring(0, 19))
                val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm", Locale("ar"))
                dateTime.format(formatter)
            } catch (e: Exception) {
                scheduledAt
            }
        }
        
        /**
         * Check if reminder is overdue
         */
        fun isOverdue(isSent: Boolean, scheduledAt: String): Boolean {
            return try {
                if (isSent) return false
                val scheduledDateTime = LocalDateTime.parse(scheduledAt.substring(0, 19))
                val now = LocalDateTime.now()
                scheduledDateTime.isBefore(now)
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Get formatted reminder type with emoji
     */
    fun getReminderTypeDisplay(): String {
        val emoji = getReminderTypeEmojis()[reminderType] ?: "📌"
        val displayName = getReminderTypeDisplayNames()[reminderType] ?: reminderType
        return "$emoji $displayName"
    }
    
    /**
     * Get formatted recurrence display
     */
    fun getRecurrenceDisplay(): String {
        return if (isRecurring && recurrenceInterval != null) {
            val emoji = getRecurrenceIntervalEmojis()[recurrenceInterval] ?: "🔁"
            val displayName = getRecurrenceIntervalDisplayNames()[recurrenceInterval] ?: recurrenceInterval
            "$emoji $displayName"
        } else {
            "مرة واحدة"
        }
    }
    
    /**
     * Get time until reminder (in Arabic)
     */
    fun getTimeUntilReminder(): String {
        return try {
            if (isSent) return "تم الإرسال"
            
            val scheduledDateTime = LocalDateTime.parse(scheduledAt.substring(0, 19))
            val now = LocalDateTime.now()
            
            if (scheduledDateTime.isBefore(now)) {
                return "متأخر"
            }
            
            val duration = java.time.Duration.between(now, scheduledDateTime)
            val days = duration.toDays()
            val hours = duration.toHours() % 24
            val minutes = duration.toMinutes() % 60
            
            when {
                days > 0 -> "خلال ${days} أيام"
                hours > 0 -> "خلال ${hours} ساعات"
                minutes > 0 -> "خلال ${minutes} دقائق"
                else -> "الآن"
            }
        } catch (e: Exception) {
            "غير محدد"
        }
    }
}

data class CreateAutomaticReminderRequest(
    @SerializedName("user_id")
    val userId: String?,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("message")
    val message: String?,
    
    @SerializedName("reminder_type")
    val reminderType: String,
    
    @SerializedName("scheduled_at")
    val scheduledAt: String,
    
    @SerializedName("is_recurring")
    val isRecurring: Boolean,
    
    @SerializedName("recurrence_interval")
    val recurrenceInterval: String?
)

data class UpdateAutomaticReminderRequest(
    @SerializedName("title")
    val title: String?,
    
    @SerializedName("message")
    val message: String?,
    
    @SerializedName("reminder_type")
    val reminderType: String?,
    
    @SerializedName("scheduled_at")
    val scheduledAt: String?,
    
    @SerializedName("is_recurring")
    val isRecurring: Boolean?,
    
    @SerializedName("recurrence_interval")
    val recurrenceInterval: String?
)

enum class ReminderType(val value: String, val displayName: String, val emoji: String) {
    INVOICE("invoice", "فاتورة", "📄"),
    PAYMENT("payment", "دفعة", "💰"),
    CUSTOM("custom", "مخصص", "📌");
    
    companion object {
        fun fromValue(value: String): ReminderType? {
            return values().find { it.value == value }
        }
        
        fun getAllOptions(): List<ReminderType> {
            return values().toList()
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