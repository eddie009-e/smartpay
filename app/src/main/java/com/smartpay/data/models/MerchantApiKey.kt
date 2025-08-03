package com.smartpay.models

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Merchant API Integration - Pro Plan Only Feature
 * 
 * This feature allows Pro Plan merchants to generate and manage API keys for:
 * - External system integrations (ERP, CRM, Accounting)
 * - Third-party application access
 * - Automated data synchronization
 * - Custom business integrations
 * 
 * Access Requirements:
 * - Pro Plan ($25.99/month) ONLY
 * - NOT available for Free or Standard Plan users
 * - Maximum 10 API keys per merchant
 */

data class MerchantApiKeyResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("api_keys")
    val apiKeys: List<MerchantApiKey>? = null,
    
    @SerializedName("api_key")
    val apiKey: MerchantApiKey? = null,
    
    @SerializedName("total")
    val total: Int? = null,
    
    @SerializedName("message")
    val message: String?
)

data class MerchantApiKeyStatsResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("stats")
    val stats: ApiKeyStats? = null,
    
    @SerializedName("message")
    val message: String?
)

data class MerchantApiKey(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("masked_key")
    val maskedKey: String? = null,
    
    @SerializedName("api_key")
    val apiKey: String? = null, // Only present during creation
    
    @SerializedName("is_active")
    val isActive: Boolean,
    
    @SerializedName("created_at")
    val createdAt: String,
    
    @SerializedName("last_used_at")
    val lastUsedAt: String?
) {
    companion object {
        /**
         * Check if user has access to merchant API keys feature
         * Only Pro plan subscribers can access this feature
         */
        fun hasFeatureAccess(subscriptionPlan: String?): Boolean {
            return subscriptionPlan == "Pro"
        }
        
        /**
         * Get upgrade message for Free/Standard plan users
         */
        fun getUpgradeMessage(): String {
            return "تكامل API متاح حصرياً لمشتركي الخطة الاحترافية. قم بترقية اشتراكك للخطة الاحترافية للوصول إلى مفاتيح API والتكامل مع الأنظمة الخارجية."
        }
        
        /**
         * Get feature description for settings
         */
        fun getFeatureDescription(): String {
            return "إنشاء مفاتيح API للتكامل مع الأنظمة الخارجية والتطبيقات"
        }
        
        /**
         * Get API key usage instructions
         */
        fun getUsageInstructions(): String {
            return """
                استخدم مفتاح API في طلبات HTTP:
                
                Header: X-API-Key: your_api_key_here
                أو
                Header: Authorization: Bearer your_api_key_here
                
                مثال:
                curl -H "X-API-Key: sp_abc123..." https://api.smartpay.sy/v1/transactions
            """.trimIndent()
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
     * Get formatted last used date
     */
    fun getFormattedLastUsedDate(): String {
        return if (lastUsedAt != null) {
            try {
                val dateTime = LocalDateTime.parse(lastUsedAt.substring(0, 19))
                val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm", Locale("ar"))
                dateTime.format(formatter)
            } catch (e: Exception) {
                lastUsedAt
            }
        } else {
            "لم يُستخدم بعد"
        }
    }
    
    /**
     * Get status display text
     */
    fun getStatusDisplay(): String {
        return if (isActive) "نشط" else "غير نشط"
    }
    
    /**
     * Get status color
     */
    fun getStatusColor(): Long {
        return if (isActive) 0xFF00D632 else 0xFF999999
    }
    
    /**
     * Get usage status display
     */
    fun getUsageStatusDisplay(): String {
        return if (lastUsedAt != null) "مُستخدم" else "غير مُستخدم"
    }
    
    /**
     * Get usage status color
     */
    fun getUsageStatusColor(): Long {
        return if (lastUsedAt != null) 0xFF2196F3 else 0xFF999999
    }
    
    /**
     * Get days since creation
     */
    fun getDaysSinceCreation(): Int {
        return try {
            val createdDate = LocalDateTime.parse(createdAt.substring(0, 19))
            val now = LocalDateTime.now()
            java.time.Duration.between(createdDate, now).toDays().toInt()
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Get days since last use
     */
    fun getDaysSinceLastUse(): Int? {
        return if (lastUsedAt != null) {
            try {
                val lastUsedDate = LocalDateTime.parse(lastUsedAt.substring(0, 19))
                val now = LocalDateTime.now()
                java.time.Duration.between(lastUsedDate, now).toDays().toInt()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
    
    /**
     * Check if API key is recently used (within 7 days)
     */
    fun isRecentlyUsed(): Boolean {
        val daysSinceLastUse = getDaysSinceLastUse()
        return daysSinceLastUse != null && daysSinceLastUse <= 7
    }
    
    /**
     * Get security level indicator
     */
    fun getSecurityLevel(): String {
        val daysSinceCreation = getDaysSinceCreation()
        val isUsed = lastUsedAt != null
        
        return when {
            !isActive -> "معطل"
            !isUsed && daysSinceCreation > 30 -> "غير مُستخدم"
            isRecentlyUsed() -> "نشط"
            else -> "خامل"
        }
    }
    
    /**
     * Get security level color
     */
    fun getSecurityLevelColor(): Long {
        return when (getSecurityLevel()) {
            "معطل" -> 0xFF999999
            "غير مُستخدم" -> 0xFFFF9800
            "نشط" -> 0xFF00D632
            "خامل" -> 0xFF2196F3
            else -> 0xFF666666
        }
    }
}

data class ApiKeyStats(
    @SerializedName("total_keys")
    val totalKeys: Int,
    
    @SerializedName("active_keys")
    val activeKeys: Int,
    
    @SerializedName("inactive_keys")
    val inactiveKeys: Int,
    
    @SerializedName("used_keys")
    val usedKeys: Int,
    
    @SerializedName("last_activity")
    val lastActivity: String?,
    
    @SerializedName("max_keys")
    val maxKeys: Int
) {
    /**
     * Get usage percentage
     */
    fun getUsagePercentage(): Float {
        return if (totalKeys > 0) {
            (usedKeys.toFloat() / totalKeys.toFloat()) * 100
        } else {
            0f
        }
    }
    
    /**
     * Get remaining slots
     */
    fun getRemainingSlots(): Int {
        return maxKeys - totalKeys
    }
    
    /**
     * Check if at capacity
     */
    fun isAtCapacity(): Boolean {
        return totalKeys >= maxKeys
    }
    
    /**
     * Get formatted last activity date
     */
    fun getFormattedLastActivity(): String {
        return if (lastActivity != null) {
            try {
                val dateTime = LocalDateTime.parse(lastActivity.substring(0, 19))
                val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm", Locale("ar"))
                dateTime.format(formatter)
            } catch (e: Exception) {
                lastActivity
            }
        } else {
            "لا توجد أنشطة"
        }
    }
}

data class CreateApiKeyRequest(
    @SerializedName("description")
    val description: String
)

data class UpdateApiKeyRequest(
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("is_active")
    val isActive: Boolean? = null
)

/**
 * API key integration types for different use cases
 */
enum class ApiKeyIntegrationType(val value: String, val displayName: String, val description: String, val emoji: String) {
    ERP("erp", "أنظمة ERP", "التكامل مع أنظمة تخطيط موارد المؤسسات", "🏢"),
    CRM("crm", "أنظمة CRM", "التكامل مع أنظمة إدارة علاقات العملاء", "👥"),
    ACCOUNTING("accounting", "أنظمة المحاسبة", "التكامل مع برامج المحاسبة والتقارير المالية", "📊"),
    ECOMMERCE("ecommerce", "منصات التجارة الإلكترونية", "التكامل مع متاجر إلكترونية ومنصات البيع", "🛒"),
    INVENTORY("inventory", "إدارة المخزون", "التكامل مع أنظمة إدارة المخزون والمستودعات", "📦"),
    ANALYTICS("analytics", "أدوات التحليل", "التكامل مع أدوات تحليل البيانات والتقارير", "📈"),
    CUSTOM("custom", "تطبيق مخصص", "التكامل مع تطبيقات مخصصة أو حلول خاصة", "🔧");
    
    companion object {
        fun getAllTypes(): List<ApiKeyIntegrationType> {
            return values().toList()
        }
        
        fun fromValue(value: String): ApiKeyIntegrationType? {
            return values().find { it.value == value }
        }
    }
}

/**
 * API key permissions for fine-grained access control
 */
enum class ApiKeyPermission(val value: String, val displayName: String, val description: String) {
    READ_TRANSACTIONS("read_transactions", "قراءة المعاملات", "عرض المعاملات والتحويلات"),
    WRITE_TRANSACTIONS("write_transactions", "كتابة المعاملات", "إنشاء معاملات وتحويلات جديدة"),
    READ_INVOICES("read_invoices", "قراءة الفواتير", "عرض الفواتير والطلبات"),
    WRITE_INVOICES("write_invoices", "كتابة الفواتير", "إنشاء وتعديل الفواتير"),
    READ_REPORTS("read_reports", "قراءة التقارير", "الوصول إلى التقارير والإحصائيات"),
    READ_CUSTOMERS("read_customers", "قراءة العملاء", "عرض بيانات العملاء"),
    WRITE_CUSTOMERS("write_customers", "كتابة العملاء", "إنشاء وتعديل بيانات العملاء"),
    WEBHOOK_ACCESS("webhook_access", "إشعارات الويب", "استقبال إشعارات الأحداث");
    
    companion object {
        fun getAllPermissions(): List<ApiKeyPermission> {
            return values().toList()
        }
        
        fun getDefaultPermissions(): List<ApiKeyPermission> {
            return listOf(READ_TRANSACTIONS, READ_INVOICES, READ_REPORTS)
        }
        
        fun fromValue(value: String): ApiKeyPermission? {
            return values().find { it.value == value }
        }
    }
}