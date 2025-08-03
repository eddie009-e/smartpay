package com.smartpay.models

import com.google.gson.annotations.SerializedName

/**
 * Employee Management - Standard/Pro Plan Feature
 * 
 * This feature allows merchants to manage their employees with:
 * - Employee registration and profile management
 * - Access control and permissions
 * - Employee hierarchy and roles
 * - Performance tracking and reporting
 * 
 * Access Requirements:
 * - Pro Plan ($25.99/month) ONLY
 * - NOT available for Free or Standard Plan users
 */

data class EmployeeResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("employees")
    val employees: List<Employee>? = null,
    
    @SerializedName("employee")
    val employee: Employee? = null,
    
    @SerializedName("total")
    val total: Int? = null,
    
    @SerializedName("message")
    val message: String?
)

data class Employee(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("merchant_id")
    val merchantId: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("email")
    val email: String?,
    
    @SerializedName("phone")
    val phone: String?,
    
    @SerializedName("position")
    val position: String?,
    
    @SerializedName("department")
    val department: String?,
    
    @SerializedName("hire_date")
    val hireDate: String?,
    
    @SerializedName("salary")
    val salary: Double?,
    
    @SerializedName("is_active")
    val isActive: Boolean,
    
    @SerializedName("permissions")
    val permissions: List<String>? = null,
    
    @SerializedName("created_at")
    val createdAt: String,
    
    @SerializedName("updated_at")
    val updatedAt: String?
) {
    companion object {
        /**
         * Check if user has access to employee management feature
         * Uses centralized FeatureAccessMap for consistency
         */
        fun hasFeatureAccess(subscriptionPlan: String?): Boolean {
            return FeatureAccessMap.hasEmployeeAccess(subscriptionPlan)
        }
        
        /**
         * Get upgrade message for Free/Standard plan users
         * Uses centralized FeatureAccessMap for consistency
         */
        fun getUpgradeMessage(): String {
            return FeatureAccessMap.getUpgradeMessage(null, FeatureAccessMap.Feature.EMPLOYEE_MANAGEMENT)
        }
        
        /**
         * Get feature description for settings
         * Uses centralized FeatureAccessMap for consistency
         */
        fun getFeatureDescription(): String {
            return FeatureAccessMap.getFeatureDescription(FeatureAccessMap.Feature.EMPLOYEE_MANAGEMENT)
        }
    }
    
    /**
     * Get formatted hire date
     */
    fun getFormattedHireDate(): String {
        return if (hireDate != null) {
            try {
                val date = java.time.LocalDate.parse(hireDate)
                val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd", java.util.Locale("ar"))
                date.format(formatter)
            } catch (e: Exception) {
                hireDate
            }
        } else {
            "غير محدد"
        }
    }
    
    /**
     * Get formatted salary
     */
    fun getFormattedSalary(): String {
        return if (salary != null && salary > 0) {
            val formatter = java.text.NumberFormat.getNumberInstance(java.util.Locale("ar"))
            "${formatter.format(salary)} ل.س"
        } else {
            "غير محدد"
        }
    }
    
    /**
     * Get employee status display
     */
    fun getStatusDisplay(): String {
        return if (isActive) "نشط" else "غير نشط"
    }
    
    /**
     * Get employee status color
     */
    fun getStatusColor(): Long {
        return if (isActive) 0xFF00D632 else 0xFF999999
    }
    
    /**
     * Get employee initials for avatar
     */
    fun getInitials(): String {
        return name.split(" ")
            .take(2)
            .joinToString("") { it.firstOrNull()?.toString() ?: "" }
            .takeIf { it.isNotEmpty() } ?: "موظف"
    }
    
    /**
     * Check if employee has specific permission
     */
    fun hasPermission(permission: String): Boolean {
        return permissions?.contains(permission) == true
    }
    
    /**
     * Get permissions display text
     */
    fun getPermissionsDisplay(): String {
        return if (permissions.isNullOrEmpty()) {
            "لا توجد صلاحيات محددة"
        } else {
            permissions.joinToString(", ") { permission ->
                when (permission) {
                    "manage_sales" -> "إدارة المبيعات"
                    "manage_inventory" -> "إدارة المخزون"
                    "view_reports" -> "عرض التقارير"
                    "manage_customers" -> "إدارة العملاء"
                    "handle_payments" -> "معالجة المدفوعات"
                    "manage_employees" -> "إدارة الموظفين"
                    else -> permission
                }
            }
        }
    }
}

data class CreateEmployeeRequest(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("email")
    val email: String?,
    
    @SerializedName("phone")
    val phone: String?,
    
    @SerializedName("position")
    val position: String?,
    
    @SerializedName("department")
    val department: String?,
    
    @SerializedName("hire_date")
    val hireDate: String?,
    
    @SerializedName("salary")
    val salary: Double?,
    
    @SerializedName("permissions")
    val permissions: List<String>?
)

data class UpdateEmployeeRequest(
    @SerializedName("name")
    val name: String?,
    
    @SerializedName("email")
    val email: String?,
    
    @SerializedName("phone")
    val phone: String?,
    
    @SerializedName("position")
    val position: String?,
    
    @SerializedName("department")
    val department: String?,
    
    @SerializedName("hire_date")
    val hireDate: String?,
    
    @SerializedName("salary")
    val salary: Double?,
    
    @SerializedName("is_active")
    val isActive: Boolean?,
    
    @SerializedName("permissions")
    val permissions: List<String>?
)

/**
 * Employee permission options
 */
enum class EmployeePermission(val value: String, val displayName: String, val description: String) {
    MANAGE_SALES("manage_sales", "إدارة المبيعات", "إضافة وتعديل وحذف المبيعات"),
    MANAGE_INVENTORY("manage_inventory", "إدارة المخزون", "إدارة المنتجات والمخزون"),
    VIEW_REPORTS("view_reports", "عرض التقارير", "الوصول إلى التقارير والإحصائيات"),
    MANAGE_CUSTOMERS("manage_customers", "إدارة العملاء", "إضافة وتعديل بيانات العملاء"),
    HANDLE_PAYMENTS("handle_payments", "معالجة المدفوعات", "استقبال ومعالجة المدفوعات"),
    MANAGE_EMPLOYEES("manage_employees", "إدارة الموظفين", "إدارة فريق العمل والصلاحيات");
    
    companion object {
        fun getAllPermissions(): List<EmployeePermission> {
            return values().toList()
        }
        
        fun fromValue(value: String): EmployeePermission? {
            return values().find { it.value == value }
        }
    }
}

/**
 * Employee department options
 */
enum class EmployeeDepartment(val value: String, val displayName: String) {
    SALES("sales", "المبيعات"),
    INVENTORY("inventory", "المخزون"),
    CUSTOMER_SERVICE("customer_service", "خدمة العملاء"),
    FINANCE("finance", "المالية"),
    MANAGEMENT("management", "الإدارة"),
    OTHER("other", "أخرى");
    
    companion object {
        fun getAllDepartments(): List<EmployeeDepartment> {
            return values().toList()
        }
        
        fun fromValue(value: String): EmployeeDepartment? {
            return values().find { it.value == value }
        }
    }
}