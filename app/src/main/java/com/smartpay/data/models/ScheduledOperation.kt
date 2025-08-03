package com.smartpay.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Scheduled Operations - Pro Plan Only Feature
 * 
 * This feature allows Pro Plan merchants to create automated recurring operations:
 * - Automated invoice generation
 * - Scheduled salary payments
 * - Recurring transfers
 * - Custom scheduling (daily, weekly, monthly)
 * 
 * Access Requirements:
 * - Pro Plan ($25.99/month) ONLY
 * - NOT available for Free or Standard Plan users
 * - Maximum 50 scheduled operations per merchant
 */

data class ScheduledOperationResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("scheduled_operations")
    val scheduledOperations: List<ScheduledOperation>? = null,
    
    @SerializedName("scheduled_operation")
    val scheduledOperation: ScheduledOperation? = null,
    
    @SerializedName("total")
    val total: Int? = null,
    
    @SerializedName("limit")
    val limit: Int? = null,
    
    @SerializedName("offset")
    val offset: Int? = null,
    
    @SerializedName("message")
    val message: String?
)

data class ScheduledOperationStatsResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("stats")
    val stats: ScheduledOperationStats? = null,
    
    @SerializedName("message")
    val message: String?
)

data class ScheduledOperation(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("operation_type")
    val operationType: String,
    
    @SerializedName("amount")
    val amount: Double,
    
    @SerializedName("target_id")
    val targetId: String,
    
    @SerializedName("recurrence")
    val recurrence: String,
    
    @SerializedName("start_date")
    val startDate: String,
    
    @SerializedName("next_run")
    val nextRun: String,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("created_at")
    val createdAt: String,
    
    @SerializedName("updated_at")
    val updatedAt: String?
) {
    companion object {
        /**
         * Check if user has access to scheduled operations feature
         * Uses centralized FeatureAccessMap for consistency
         */
        fun hasFeatureAccess(subscriptionPlan: String?): Boolean {
            return FeatureAccessMap.hasScheduledOperationsAccess(subscriptionPlan)
        }
        
        /**
         * Get upgrade message for Free/Standard plan users
         * Uses centralized FeatureAccessMap for consistency
         */
        fun getUpgradeMessage(): String {
            return FeatureAccessMap.getUpgradeMessage(null, FeatureAccessMap.Feature.SCHEDULED_OPERATIONS)
        }
        
        /**
         * Get feature description for settings
         * Uses centralized FeatureAccessMap for consistency
         */
        fun getFeatureDescription(): String {
            return FeatureAccessMap.getFeatureDescription(FeatureAccessMap.Feature.SCHEDULED_OPERATIONS)
        }
        
        /**
         * Format amount for display
         */
        fun formatAmount(amount: Double): String {
            val formatter = NumberFormat.getNumberInstance(Locale("ar"))
            return "${formatter.format(amount)} ل.س"
        }
    }
    
    /**
     * Get formatted amount
     */
    fun getFormattedAmount(): String {
        return formatAmount(amount)
    }
    
    /**
     * Get formatted start date
     */
    fun getFormattedStartDate(): String {
        return try {
            val dateTime = LocalDateTime.parse(startDate.substring(0, 19))
            val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm", Locale("ar"))
            dateTime.format(formatter)
        } catch (e: Exception) {
            startDate
        }
    }
    
    /**
     * Get formatted next run date
     */
    fun getFormattedNextRun(): String {
        return try {
            val dateTime = LocalDateTime.parse(nextRun.substring(0, 19))
            val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm", Locale("ar"))
            dateTime.format(formatter)
        } catch (e: Exception) {
            nextRun
        }
    }
    
    /**
     * Get formatted creation date
     */
    fun getFormattedCreatedDate(): String {
        return try {
            val dateTime = LocalDateTime.parse(createdAt.substring(0, 19))
            val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm", Locale("ar"))
            dateTime.format(formatter)
        } catch (e: Exception) {
            createdAt
        }
    }
    
    /**
     * Get operation type display name
     */
    fun getOperationTypeDisplay(): String {
        return when (operationType) {
            "invoice" -> "فاتورة"
            "salary" -> "راتب"
            "transfer" -> "تحويل"
            else -> operationType
        }
    }
    
    /**
     * Get operation type emoji
     */
    fun getOperationTypeEmoji(): String {
        return when (operationType) {
            "invoice" -> "💳"
            "salary" -> "💰"
            "transfer" -> "🔄"
            else -> "📅"
        }
    }
    
    /**
     * Get operation type color
     */
    fun getOperationTypeColor(): Long {
        return when (operationType) {
            "invoice" -> 0xFF2196F3
            "salary" -> 0xFF00D632
            "transfer" -> 0xFF9C27B0
            else -> 0xFF666666
        }
    }
    
    /**
     * Get recurrence display name
     */
    fun getRecurrenceDisplay(): String {
        return when (recurrence) {
            "daily" -> "يومياً"
            "weekly" -> "أسبوعياً"
            "monthly" -> "شهرياً"
            else -> recurrence
        }
    }
    
    /**
     * Get recurrence emoji
     */
    fun getRecurrenceEmoji(): String {
        return when (recurrence) {
            "daily" -> "📅"
            "weekly" -> "📆"
            "monthly" -> "🗓️"
            else -> "🔄"
        }
    }
    
    /**
     * Get status display name
     */
    fun getStatusDisplay(): String {
        return when (status) {
            "active" -> "نشط"
            "paused" -> "متوقف"
            "cancelled" -> "ملغي"
            else -> status
        }
    }
    
    /**
     * Get status color
     */
    fun getStatusColor(): Long {
        return when (status) {
            "active" -> 0xFF00D632
            "paused" -> 0xFFFF9800
            "cancelled" -> 0xFF999999
            else -> 0xFF666666
        }
    }
    
    /**
     * Check if operation is active
     */
    fun isActive(): Boolean {
        return status == "active"
    }
    
    /**
     * Check if operation is due soon (within 24 hours)
     */
    fun isDueSoon(): Boolean {
        return try {
            val nextRunDate = LocalDateTime.parse(nextRun.substring(0, 19))
            val now = LocalDateTime.now()
            val hoursUntilRun = java.time.Duration.between(now, nextRunDate).toHours()
            hoursUntilRun in 0..24 && isActive()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get time until next run
     */
    fun getTimeUntilNextRun(): String {
        return try {
            val nextRunDate = LocalDateTime.parse(nextRun.substring(0, 19))
            val now = LocalDateTime.now()
            val duration = java.time.Duration.between(now, nextRunDate)
            
            when {
                duration.isNegative -> "متأخر"
                duration.toDays() > 0 -> "${duration.toDays()} يوم"
                duration.toHours() > 0 -> "${duration.toHours()} ساعة"
                duration.toMinutes() > 0 -> "${duration.toMinutes()} دقيقة"
                else -> "قريباً"
            }
        } catch (e: Exception) {
            "غير محدد"
        }
    }
    
    /**
     * Get priority level based on due time
     */
    fun getPriorityLevel(): String {
        return when {
            !isActive() -> "معطل"
            isDueSoon() -> "عالي"
            getTimeUntilNextRun().contains("يوم") -> "متوسط"
            else -> "منخفض"
        }
    }
    
    /**
     * Get priority color
     */
    fun getPriorityColor(): Long {
        return when (getPriorityLevel()) {
            "عالي" -> 0xFFE53E3E
            "متوسط" -> 0xFFFF9800
            "منخفض" -> 0xFF00D632
            "معطل" -> 0xFF999999
            else -> 0xFF666666
        }
    }
}

data class ScheduledOperationStats(
    @SerializedName("total_operations")
    val totalOperations: Int,
    
    @SerializedName("active_operations")
    val activeOperations: Int,
    
    @SerializedName("paused_operations")
    val pausedOperations: Int,
    
    @SerializedName("cancelled_operations")
    val cancelledOperations: Int,
    
    @SerializedName("invoice_operations")
    val invoiceOperations: Int,
    
    @SerializedName("salary_operations")
    val salaryOperations: Int,
    
    @SerializedName("transfer_operations")
    val transferOperations: Int,
    
    @SerializedName("due_soon")
    val dueSoon: Int,
    
    @SerializedName("total_scheduled_amount")
    val totalScheduledAmount: Double,
    
    @SerializedName("max_operations")
    val maxOperations: Int,
    
    @SerializedName("upcoming_operations")
    val upcomingOperations: List<UpcomingOperation>? = null
) {
    /**
     * Get utilization percentage
     */
    fun getUtilizationPercentage(): Float {
        return if (maxOperations > 0) {
            (totalOperations.toFloat() / maxOperations.toFloat()) * 100
        } else {
            0f
        }
    }
    
    /**
     * Get remaining slots
     */
    fun getRemainingSlots(): Int {
        return maxOperations - totalOperations
    }
    
    /**
     * Check if at capacity
     */
    fun isAtCapacity(): Boolean {
        return totalOperations >= maxOperations
    }
    
    /**
     * Get active percentage
     */
    fun getActivePercentage(): Float {
        return if (totalOperations > 0) {
            (activeOperations.toFloat() / totalOperations.toFloat()) * 100
        } else {
            0f
        }
    }
    
    /**
     * Get formatted total scheduled amount
     */
    fun getFormattedTotalAmount(): String {
        return ScheduledOperation.formatAmount(totalScheduledAmount)
    }
}

data class UpcomingOperation(
    @SerializedName("operation_type")
    val operationType: String,
    
    @SerializedName("amount")
    val amount: Double,
    
    @SerializedName("next_run")
    val nextRun: String,
    
    @SerializedName("recurrence")
    val recurrence: String
) {
    fun getFormattedAmount(): String {
        return ScheduledOperation.formatAmount(amount)
    }
    
    fun getFormattedNextRun(): String {
        return try {
            val dateTime = LocalDateTime.parse(nextRun.substring(0, 19))
            val formatter = DateTimeFormatter.ofPattern("MM/dd HH:mm", Locale("ar"))
            dateTime.format(formatter)
        } catch (e: Exception) {
            nextRun
        }
    }
    
    fun getOperationTypeEmoji(): String {
        return when (operationType) {
            "invoice" -> "💳"
            "salary" -> "💰"
            "transfer" -> "🔄"
            else -> "📅"
        }
    }
}

data class CreateScheduledOperationRequest(
    @SerializedName("operation_type")
    val operationType: String,
    
    @SerializedName("amount")
    val amount: Double,
    
    @SerializedName("target_id")
    val targetId: String,
    
    @SerializedName("recurrence")
    val recurrence: String,
    
    @SerializedName("start_date")
    val startDate: String,
    
    @SerializedName("description")
    val description: String? = null
)

data class UpdateScheduledOperationRequest(
    @SerializedName("operation_type")
    val operationType: String? = null,
    
    @SerializedName("amount")
    val amount: Double? = null,
    
    @SerializedName("target_id")
    val targetId: String? = null,
    
    @SerializedName("recurrence")
    val recurrence: String? = null,
    
    @SerializedName("start_date")
    val startDate: String? = null,
    
    @SerializedName("status")
    val status: String? = null,
    
    @SerializedName("description")
    val description: String? = null
)

/**
 * Operation types for scheduled operations
 */
enum class OperationType(val value: String, val displayName: String, val description: String, val emoji: String) {
    INVOICE("invoice", "فاتورة", "إنشاء فواتير تلقائية للعملاء", "💳"),
    SALARY("salary", "راتب", "دفع رواتب الموظفين تلقائياً", "💰"),
    TRANSFER("transfer", "تحويل", "تحويلات مالية متكررة", "🔄");
    
    companion object {
        fun getAllTypes(): List<OperationType> {
            return values().toList()
        }
        
        fun fromValue(value: String): OperationType? {
            return values().find { it.value == value }
        }
    }
}

/**
 * Recurrence types for scheduled operations
 */
enum class RecurrenceType(val value: String, val displayName: String, val description: String, val emoji: String) {
    DAILY("daily", "يومياً", "تكرار كل يوم", "📅"),
    WEEKLY("weekly", "أسبوعياً", "تكرار كل أسبوع", "📆"),
    MONTHLY("monthly", "شهرياً", "تكرار كل شهر", "🗓️");
    
    companion object {
        fun getAllTypes(): List<RecurrenceType> {
            return values().toList()
        }
        
        fun fromValue(value: String): RecurrenceType? {
            return values().find { it.value == value }
        }
    }
}

/**
 * Status types for scheduled operations
 */
enum class OperationStatus(val value: String, val displayName: String, val description: String, val color: Long) {
    ACTIVE("active", "نشط", "العملية قيد التشغيل", 0xFF00D632),
    PAUSED("paused", "متوقف", "العملية متوقفة مؤقتاً", 0xFFFF9800),
    CANCELLED("cancelled", "ملغي", "العملية ملغاة نهائياً", 0xFF999999);
    
    companion object {
        fun getAllStatuses(): List<OperationStatus> {
            return values().toList()
        }
        
        fun fromValue(value: String): OperationStatus? {
            return values().find { it.value == value }
        }
    }
}