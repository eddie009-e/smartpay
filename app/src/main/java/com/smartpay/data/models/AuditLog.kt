package com.smartpay.models

import com.google.gson.annotations.SerializedName

data class AuditLogResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("logs")
    val logs: List<AuditLog>,
    
    @SerializedName("total")
    val total: Int,
    
    @SerializedName("message")
    val message: String?
)

data class AuditLog(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("merchant_id")
    val merchantId: String,
    
    @SerializedName("action")
    val action: String,
    
    @SerializedName("description")
    val description: String?,
    
    @SerializedName("timestamp")
    val timestamp: String,
    
    @SerializedName("source")
    val source: String?,
    
    @SerializedName("ip_address")
    val ipAddress: String?,
    
    @SerializedName("user_agent")
    val userAgent: String?
) {
    companion object {
        /**
         * Get Arabic translation for action types
         */
        fun getActionTitle(action: String): String {
            return when (action) {
                "Login" -> "🔐 تسجيل الدخول"
                "Send Money" -> "💸 إرسال أموال"
                "Create Invoice" -> "🧾 إنشاء فاتورة"
                "Salary Payment" -> "👥 دفع راتب"
                "Change Subscription" -> "🔔 تغيير الاشتراك"
                "Cancel Subscription" -> "❌ إلغاء الاشتراك"
                "Create Request" -> "📝 إنشاء طلب"
                "Pay Invoice" -> "💳 دفع فاتورة"
                "Update Profile" -> "👤 تحديث الملف الشخصي"
                "View Report" -> "📊 عرض التقارير"
                "View Audit Logs" -> "📋 عرض سجل التدقيق"
                else -> "⚡ $action"
            }
        }
        
        /**
         * Get color for action type
         */
        fun getActionColor(action: String): Long {
            return when (action) {
                "Login" -> 0xFF4CAF50 // Green
                "Send Money" -> 0xFFE53E3E // Red
                "Create Invoice", "Pay Invoice" -> 0xFF2196F3 // Blue
                "Salary Payment" -> 0xFF9C27B0 // Purple
                "Change Subscription", "Cancel Subscription" -> 0xFFFF9800 // Orange
                "Create Request" -> 0xFF00BCD4 // Cyan
                "Update Profile" -> 0xFF607D8B // Blue Grey
                "View Report", "View Audit Logs" -> 0xFF795548 // Brown
                else -> 0xFF666666 // Default grey
            }
        }
        
        /**
         * Get source display name
         */
        fun getSourceDisplay(source: String?): String {
            return when (source) {
                "Android" -> "📱 التطبيق"
                "API" -> "🌐 API"
                "Web" -> "💻 الويب"
                null, "" -> "❓ غير محدد"
                else -> source
            }
        }
    }
}