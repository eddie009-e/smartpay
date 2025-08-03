package com.smartpay.models

import com.google.gson.annotations.SerializedName

/**
 * Sub-Accounts Management - Standard Plan Feature
 * 
 * This feature allows merchants to create restricted sub-users (e.g., employees) 
 * with specific access permissions under their main merchant account.
 * 
 * Access Requirements:
 * - Standard Plan ($10.99/month) or higher
 * - Pro Plan ($25.99/month) 
 * - NOT available for Free Plan users
 */

data class SubAccountResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("sub_accounts")
    val subAccounts: List<SubAccount>? = null,
    
    @SerializedName("sub_account")
    val subAccount: SubAccount? = null,
    
    @SerializedName("total")
    val total: Int? = null,
    
    @SerializedName("message")
    val message: String?
)

data class SubAccount(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("merchant_id")
    val merchantId: String,
    
    @SerializedName("full_name")
    val fullName: String,
    
    @SerializedName("phone")
    val phone: String,
    
    @SerializedName("permissions")
    val permissions: SubAccountPermissions,
    
    @SerializedName("is_active")
    val isActive: Boolean,
    
    @SerializedName("created_at")
    val createdAt: String
) {
    companion object {
        /**
         * Check if user has access to sub-accounts feature
         * Only Standard and Pro plan subscribers can access this feature
         */
        fun hasFeatureAccess(subscriptionPlan: String?): Boolean {
            return subscriptionPlan == "Standard" || subscriptionPlan == "Pro"
        }
        
        /**
         * Get upgrade message for Free plan users
         */
        fun getUpgradeMessage(): String {
            return "إدارة الحسابات الفرعية متاحة فقط لمشتركي الخطة القياسية والاحترافية. قم بترقية اشتراكك لإضافة موظفين بصلاحيات محددة."
        }
        
        /**
         * Get default permissions for new sub-accounts
         */
        fun getDefaultPermissions(): SubAccountPermissions {
            return SubAccountPermissions(
                send = false,
                receive = true,
                salary = false,
                reports = false
            )
        }
        
        /**
         * Get permission display names in Arabic
         */
        fun getPermissionDisplayNames(): Map<String, String> {
            return mapOf(
                "send" to "إرسال الأموال",
                "receive" to "استلام الأموال",
                "salary" to "دفع الرواتب",
                "reports" to "عرض التقارير"
            )
        }
        
        /**
         * Get status display text
         */
        fun getStatusDisplay(isActive: Boolean): String {
            return if (isActive) "نشط" else "معطل"
        }
        
        /**
         * Get status color
         */
        fun getStatusColor(isActive: Boolean): Long {
            return if (isActive) 0xFF00D632 else 0xFF666666
        }
    }
    
    /**
     * Get formatted permissions text for display
     */
    fun getPermissionsText(): String {
        val permissionNames = getPermissionDisplayNames()
        val activePermissions = mutableListOf<String>()
        
        if (permissions.send) activePermissions.add(permissionNames["send"]!!)
        if (permissions.receive) activePermissions.add(permissionNames["receive"]!!)
        if (permissions.salary) activePermissions.add(permissionNames["salary"]!!)
        if (permissions.reports) activePermissions.add(permissionNames["reports"]!!)
        
        return if (activePermissions.isEmpty()) {
            "لا توجد صلاحيات"
        } else {
            activePermissions.joinToString("، ")
        }
    }
    
    /**
     * Count active permissions
     */
    fun getActivePermissionsCount(): Int {
        var count = 0
        if (permissions.send) count++
        if (permissions.receive) count++
        if (permissions.salary) count++
        if (permissions.reports) count++
        return count
    }
}

data class SubAccountPermissions(
    @SerializedName("send")
    val send: Boolean = false,
    
    @SerializedName("receive")
    val receive: Boolean = false,
    
    @SerializedName("salary")
    val salary: Boolean = false,
    
    @SerializedName("reports")
    val reports: Boolean = false
)

data class CreateSubAccountRequest(
    @SerializedName("full_name")
    val fullName: String,
    
    @SerializedName("phone")
    val phone: String,
    
    @SerializedName("password")
    val password: String,
    
    @SerializedName("permissions")
    val permissions: SubAccountPermissions
)

data class UpdateSubAccountRequest(
    @SerializedName("full_name")
    val fullName: String?,
    
    @SerializedName("permissions")
    val permissions: SubAccountPermissions?,
    
    @SerializedName("is_active")
    val isActive: Boolean?
)

data class SubAccountLoginRequest(
    @SerializedName("phone")
    val phone: String,
    
    @SerializedName("password")
    val password: String
)

data class SubAccountLoginResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("token")
    val token: String?,
    
    @SerializedName("sub_account")
    val subAccount: SubAccount?,
    
    @SerializedName("merchant_name")
    val merchantName: String?,
    
    @SerializedName("message")
    val message: String?
)