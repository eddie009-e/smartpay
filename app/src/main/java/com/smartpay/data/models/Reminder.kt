package com.smartpay.models

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Automatic Reminders - Standard/Pro Plan Feature
 * 
 * This feature allows merchants to create, view, and manage recurring reminders such as:
 * - Send payment reminders
 * - Follow-up alerts
 * - Salary-related alerts
 * - General notes (custom purpose)
 * 
 * Access Requirements:
 * - Pro Plan ($25.99/month) ONLY
 * - NOT available for Free or Standard Plan users
 */

data class ReminderResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("reminders")
    val reminders: List<Reminder>? = null,
    
    @SerializedName("reminder")
    val reminder: Reminder? = null,
    
    @SerializedName("total")
    val total: Int? = null,
    
    @SerializedName("message")
    val message: String?
)

data class ReminderStatsResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("stats")
    val stats: ReminderStats? = null,
    
    @SerializedName("message")
    val message: String?
)

data class Reminder(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("merchant_id")
    val merchantId: String,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("description")
    val description: String?,
    
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
         * Check if user has access to reminders feature
         * Uses centralized FeatureAccessMap for consistency (Standard + Pro plans)
         */
        fun hasFeatureAccess(subscriptionPlan: String?): Boolean {
            return FeatureAccessMap.hasReminderAccess(subscriptionPlan)
        }
        
        /**
         * Get upgrade message for Free plan users
         * Uses centralized FeatureAccessMap for consistency
         */
        fun getUpgradeMessage(): String {
            return FeatureAccessMap.getUpgradeMessage(null, FeatureAccessMap.Feature.REMINDERS)
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
        
        /**
         * Format time for display (time only)
         */
        fun formatTimeOnly(dateString: String): String {
            return try {
                val dateTime = LocalDateTime.parse(dateString.substring(0, 19))
                val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale("ar"))
                dateTime.format(formatter)
            } catch (e: Exception) {
                dateString
            }
        }
        
        /**
         * Get all recurrence options for dropdown
         */
        fun getRecurrenceOptions(): List<RecurrenceOption> {
            return listOf(
                RecurrenceOption("none", "لا يتكرر", "📅"),
                RecurrenceOption("daily", "يومياً", "🔄"),
                RecurrenceOption("weekly", "أسبوعياً", "📆"),
                RecurrenceOption("monthly", "شهرياً", "🗓️")
            )
        }
    }
    
    /**
     * Get formatted scheduled date
     */
    fun getFormattedScheduledDate(): String {
        return formatDate(scheduledAt)
    }
    
    /**
     * Get formatted scheduled date (date only)
     */
    fun getFormattedScheduledDateOnly(): String {
        return formatDateOnly(scheduledAt)
    }
    
    /**
     * Get formatted scheduled time (time only)
     */
    fun getFormattedScheduledTimeOnly(): String {
        return formatTimeOnly(scheduledAt)
    }
    
    /**
     * Get recurrence display text
     */
    fun getRecurrenceDisplay(): String {
        return when (recurrenceInterval) {
            "daily" -> "🔄 يومياً"
            "weekly" -> "📆 أسبوعياً"
            "monthly" -> "🗓️ شهرياً"
            else -> "📅 لا يتكرر"
        }
    }
    
    /**
     * Get status display text and color
     */
    fun getStatusDisplay(): StatusInfo {
        return if (isSent) {
            StatusInfo("✅ تم الإرسال", 0xFF00D632)
        } else {
            try {
                val scheduledDateTime = LocalDateTime.parse(scheduledAt.substring(0, 19))
                val now = LocalDateTime.now()
                
                when {
                    scheduledDateTime.isBefore(now) -> StatusInfo("⏰ متأخر", 0xFFFF9800)
                    ChronoUnit.HOURS.between(now, scheduledDateTime) <= 24 -> StatusInfo("🔔 قريباً", 0xFF2196F3)
                    else -> StatusInfo("📅 قادم", 0xFF666666)
                }
            } catch (e: Exception) {
                StatusInfo("📅 قادم", 0xFF666666)
            }
        }
    }
    
    /**
     * Check if reminder is overdue
     */
    fun isOverdue(): Boolean {
        return try {
            if (isSent) return false
            val scheduledDateTime = LocalDateTime.parse(scheduledAt.substring(0, 19))
            val now = LocalDateTime.now()
            scheduledDateTime.isBefore(now)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if reminder is due soon (within 24 hours)
     */
    fun isDueSoon(): Boolean {
        return try {
            if (isSent) return false
            val scheduledDateTime = LocalDateTime.parse(scheduledAt.substring(0, 19))
            val now = LocalDateTime.now()
            val hoursUntil = ChronoUnit.HOURS.between(now, scheduledDateTime)
            hoursUntil in 0..24
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get time until reminder (human readable)
     */
    fun getTimeUntilReminder(): String {
        return try {
            if (isSent) return "تم الإرسال"
            
            val scheduledDateTime = LocalDateTime.parse(scheduledAt.substring(0, 19))
            val now = LocalDateTime.now()
            
            when {
                scheduledDateTime.isBefore(now) -> {
                    val hoursAgo = ChronoUnit.HOURS.between(scheduledDateTime, now)
                    when {
                        hoursAgo < 1 -> "متأخر منذ دقائق"
                        hoursAgo < 24 -> "متأخر منذ ${hoursAgo} ساعة"
                        else -> {
                            val daysAgo = ChronoUnit.DAYS.between(scheduledDateTime, now)
                            "متأخر منذ ${daysAgo} يوم"
                        }
                    }
                }
                else -> {
                    val hoursUntil = ChronoUnit.HOURS.between(now, scheduledDateTime)
                    when {
                        hoursUntil < 1 -> {
                            val minutesUntil = ChronoUnit.MINUTES.between(now, scheduledDateTime)
                            "خلال ${minutesUntil} دقيقة"
                        }
                        hoursUntil < 24 -> "خلال ${hoursUntil} ساعة"
                        else -> {
                            val daysUntil = ChronoUnit.DAYS.between(now, scheduledDateTime)
                            "خلال ${daysUntil} يوم"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            "غير محدد"
        }
    }
    
    /**
     * Get reminder type icon based on title/description content
     */
    fun getReminderTypeIcon(): String {
        val content = (title + " " + (description ?: "")).lowercase()
        return when {
            content.contains("راتب") || content.contains("salary") -> "💰"
            content.contains("دفع") || content.contains("payment") -> "💳"
            content.contains("متابعة") || content.contains("follow") -> "📞"
            content.contains("اجتماع") || content.contains("meeting") -> "👥"
            content.contains("مكالمة") || content.contains("call") -> "📱"
            content.contains("إيميل") || content.contains("email") -> "📧"
            else -> "🔔"
        }
    }
}

data class ReminderStats(
    @SerializedName("total_reminders")
    val totalReminders: Int,
    
    @SerializedName("upcoming_reminders")
    val upcomingReminders: Int,
    
    @SerializedName("past_reminders")
    val pastReminders: Int,
    
    @SerializedName("sent_reminders")
    val sentReminders: Int,
    
    @SerializedName("recurring_reminders")
    val recurringReminders: Int
)

data class CreateReminderRequest(
    @SerializedName("title")
    val title: String,
    
    @SerializedName("description")
    val description: String?,
    
    @SerializedName("scheduled_at")
    val scheduledAt: String,
    
    @SerializedName("is_recurring")
    val isRecurring: Boolean,
    
    @SerializedName("recurrence_interval")
    val recurrenceInterval: String?
)

data class UpdateReminderRequest(
    @SerializedName("title")
    val title: String?,
    
    @SerializedName("description")
    val description: String?,
    
    @SerializedName("scheduled_at")
    val scheduledAt: String?,
    
    @SerializedName("is_recurring")
    val isRecurring: Boolean?,
    
    @SerializedName("recurrence_interval")
    val recurrenceInterval: String?
)

data class RecurrenceOption(
    val value: String,
    val displayName: String,
    val emoji: String
)

data class StatusInfo(
    val text: String,
    val color: Long
)

/**
 * Enum for reminder filter options
 */
enum class ReminderFilter(val value: String, val displayName: String, val emoji: String) {
    ALL("all", "الكل", "📋"),
    UPCOMING("upcoming", "القادمة", "📅"),
    PAST("past", "السابقة", "📆"),
    SENT("sent", "المرسلة", "✅");
    
    companion object {
        fun fromValue(value: String): ReminderFilter? {
            return values().find { it.value == value }
        }
        
        fun getAllOptions(): List<ReminderFilter> {
            return values().toList()
        }
    }
}