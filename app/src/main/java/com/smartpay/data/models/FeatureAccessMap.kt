package com.smartpay.models

/**
 * Centralized Feature Access Control Map for SmartPay Android
 * 
 * This class provides centralized access control logic for all SmartPay features
 * across different subscription plans (Free, Standard, Pro).
 * 
 * Usage:
 * - Use FeatureAccessMap.hasAccess(plan, feature) to check feature access
 * - All Android models should use this centralized logic
 * - Feature keys must match between backend and Android implementations
 * 
 * Plan Hierarchy:
 * - Free: Basic features only
 * - Standard: All Free features + Standard-tier features  
 * - Pro: All Free + Standard + Pro-tier features
 * 
 * @author SmartPay Development Team
 * @version 1.0.0
 */
object FeatureAccessMap {
    
    /**
     * Enum for all available subscription plans
     */
    enum class Plan(val displayName: String, val price: String) {
        FREE("مجاني", "مجاني"),
        STANDARD("قياسي", "$10.99/شهر"),
        PRO("احترافي", "$25.99/شهر");
        
        companion object {
            fun fromString(planName: String?): Plan {
                return when (planName?.lowercase()) {
                    "standard" -> STANDARD
                    "pro" -> PRO
                    else -> FREE
                }
            }
        }
    }
    
    /**
     * Enum for all available features with their display names and descriptions
     */
    enum class Feature(
        val key: String,
        val displayName: String,
        val description: String,
        val emoji: String
    ) {
        // Pro Plan Exclusive Features
        SCHEDULED_OPERATIONS("SCHEDULED_OPERATIONS", "الجدولة التلقائية", "إنشاء عمليات تلقائية متكررة للرواتب والفواتير والتحويلات", "📅"),
        TAX_MANAGEMENT("TAX_MANAGEMENT", "إدارة الضرائب", "إدارة الضرائب والتقارير الضريبية", "🧾"),
        EMPLOYEE_MANAGEMENT("EMPLOYEE_MANAGEMENT", "إدارة الموظفين", "إدارة شاملة للموظفين مع تحديد الأدوار والصلاحيات", "👥"),
        FINANCIAL_REPORTS("FINANCIAL_REPORTS", "التقارير المالية المتقدمة", "تحليلات مالية شاملة وتصدير التقارير", "📊"),
        UNIFIED_REPORTS("UNIFIED_REPORTS", "التقارير الموحدة", "لوحات تحكم تفاعلية مع إمكانية التصدير", "📈"),
        API_INTEGRATION("API_INTEGRATION", "تكامل API", "مفاتيح API للتكامل مع الأنظمة الخارجية", "🔌"),
        
        // Standard + Pro Plan Features
        REMINDERS("REMINDERS", "التذكيرات", "تذكيرات تلقائية للمدفوعات والمتابعات", "🔔"),
        TRANSACTION_CATEGORIES("TRANSACTION_CATEGORIES", "تصنيف المعاملات", "تصنيف المعاملات المالية مع الألوان", "📋"),
        SUB_ACCOUNTS("SUB_ACCOUNTS", "الحسابات الفرعية", "إدارة حسابات فرعية بصلاحيات محددة", "👤"),
        
        // Free + Standard + Pro Plan Features (Basic Features)
        BASIC_INVOICES("BASIC_INVOICES", "الفواتير الأساسية", "إنشاء وإرسال الفواتير", "💳"),
        SEND_MONEY("SEND_MONEY", "إرسال الأموال", "تحويل الأموال للآخرين", "💸"),
        RECEIVE_MONEY("RECEIVE_MONEY", "استقبال الأموال", "استقبال التحويلات والمدفوعات", "💰"),
        TRANSACTION_HISTORY("TRANSACTION_HISTORY", "سجل المعاملات", "عرض تاريخ جميع المعاملات", "📜"),
        BASIC_DASHBOARD("BASIC_DASHBOARD", "لوحة التحكم الأساسية", "عرض الرصيد والمعاملات الأساسية", "🏠"),
        QR_CODE_PAYMENTS("QR_CODE_PAYMENTS", "مدفوعات QR", "الدفع والاستقبال عبر رمز QR", "📱"),
        WALLET_MANAGEMENT("WALLET_MANAGEMENT", "إدارة المحفظة", "إدارة الرصيد والمحفظة الإلكترونية", "👛"),
        BASIC_PROFILE("BASIC_PROFILE", "الملف الشخصي الأساسي", "إدارة المعلومات الشخصية الأساسية", "👤"),
        PIN_SECURITY("PIN_SECURITY", "حماية PIN", "حماية الحساب برقم PIN", "🔒");
        
        companion object {
            fun fromKey(key: String): Feature? {
                return values().find { it.key == key }
            }
            
            fun getAllFeatures(): List<Feature> = values().toList()
            
            fun getFeaturesByPlan(plan: Plan): List<Feature> {
                return values().filter { hasAccess(plan, it) }
            }
        }
    }
    
    /**
     * Internal feature access mapping
     * Maps each feature to the plans that can access it
     */
    private val featureAccessMap = mapOf(
        // Pro Plan Exclusive Features
        Feature.SCHEDULED_OPERATIONS to listOf(Plan.PRO),
        Feature.TAX_MANAGEMENT to listOf(Plan.PRO),
        Feature.EMPLOYEE_MANAGEMENT to listOf(Plan.PRO),
        Feature.FINANCIAL_REPORTS to listOf(Plan.PRO),
        Feature.UNIFIED_REPORTS to listOf(Plan.PRO),
        Feature.API_INTEGRATION to listOf(Plan.PRO),
        
        // Standard + Pro Plan Features
        Feature.REMINDERS to listOf(Plan.STANDARD, Plan.PRO),
        Feature.TRANSACTION_CATEGORIES to listOf(Plan.STANDARD, Plan.PRO),
        Feature.SUB_ACCOUNTS to listOf(Plan.STANDARD, Plan.PRO),
        
        // Free + Standard + Pro Plan Features (Basic Features)
        Feature.BASIC_INVOICES to listOf(Plan.FREE, Plan.STANDARD, Plan.PRO),
        Feature.SEND_MONEY to listOf(Plan.FREE, Plan.STANDARD, Plan.PRO),
        Feature.RECEIVE_MONEY to listOf(Plan.FREE, Plan.STANDARD, Plan.PRO),
        Feature.TRANSACTION_HISTORY to listOf(Plan.FREE, Plan.STANDARD, Plan.PRO),
        Feature.BASIC_DASHBOARD to listOf(Plan.FREE, Plan.STANDARD, Plan.PRO),
        Feature.QR_CODE_PAYMENTS to listOf(Plan.FREE, Plan.STANDARD, Plan.PRO),
        Feature.WALLET_MANAGEMENT to listOf(Plan.FREE, Plan.STANDARD, Plan.PRO),
        Feature.BASIC_PROFILE to listOf(Plan.FREE, Plan.STANDARD, Plan.PRO),
        Feature.PIN_SECURITY to listOf(Plan.FREE, Plan.STANDARD, Plan.PRO)
    )
    
    /**
     * Check if a subscription plan has access to a specific feature
     * 
     * @param plan Subscription plan
     * @param feature Feature to check access for
     * @return True if plan has access to feature, false otherwise
     * 
     * @example
     * hasAccess(Plan.STANDARD, Feature.REMINDERS) // returns true
     * hasAccess(Plan.FREE, Feature.EMPLOYEE_MANAGEMENT) // returns false
     * hasAccess(Plan.PRO, Feature.BASIC_INVOICES) // returns true
     */
    fun hasAccess(plan: Plan, feature: Feature): Boolean {
        return featureAccessMap[feature]?.contains(plan) ?: false
    }
    
    /**
     * Check if a subscription plan (as string) has access to a specific feature
     * 
     * @param subscriptionPlan Subscription plan name ('Free', 'Standard', 'Pro')
     * @param feature Feature to check access for
     * @return True if plan has access to feature, false otherwise
     */
    fun hasAccess(subscriptionPlan: String?, feature: Feature): Boolean {
        val plan = Plan.fromString(subscriptionPlan)
        return hasAccess(plan, feature)
    }
    
    /**
     * Check if a subscription plan has access to a feature by key
     * 
     * @param subscriptionPlan Subscription plan name ('Free', 'Standard', 'Pro')
     * @param featureKey Feature key string
     * @return True if plan has access to feature, false otherwise
     */
    fun hasAccess(subscriptionPlan: String?, featureKey: String): Boolean {
        val plan = Plan.fromString(subscriptionPlan)
        val feature = Feature.fromKey(featureKey) ?: return false
        return hasAccess(plan, feature)
    }
    
    /**
     * Get all features available for a specific subscription plan
     * 
     * @param plan Subscription plan
     * @return List of features available for the plan
     */
    fun getAvailableFeatures(plan: Plan): List<Feature> {
        return featureAccessMap.filterValues { it.contains(plan) }.keys.toList()
    }
    
    /**
     * Get all features available for a specific subscription plan (by string)
     * 
     * @param subscriptionPlan Subscription plan name
     * @return List of features available for the plan
     */
    fun getAvailableFeatures(subscriptionPlan: String?): List<Feature> {
        val plan = Plan.fromString(subscriptionPlan)
        return getAvailableFeatures(plan)
    }
    
    /**
     * Get subscription plans that have access to a specific feature
     * 
     * @param feature Feature to check
     * @return List of plans that can access the feature
     */
    fun getPlansForFeature(feature: Feature): List<Plan> {
        return featureAccessMap[feature] ?: emptyList()
    }
    
    /**
     * Get subscription plans that have access to a specific feature (by key)
     * 
     * @param featureKey Feature key string
     * @return List of plans that can access the feature
     */
    fun getPlansForFeature(featureKey: String): List<Plan> {
        val feature = Feature.fromKey(featureKey) ?: return emptyList()
        return getPlansForFeature(feature)
    }
    
    /**
     * Get upgrade message for features not available in current plan
     * 
     * @param currentPlan Current subscription plan name
     * @param feature Feature that requires upgrade
     * @return Localized upgrade message in Arabic
     */
    fun getUpgradeMessage(currentPlan: String?, feature: Feature): String {
        val plan = Plan.fromString(currentPlan)
        val requiredPlans = getPlansForFeature(feature)
        
        if (requiredPlans.isEmpty()) {
            return "هذه الميزة غير متاحة حالياً"
        }
        
        // Find the minimum required plan
        val planHierarchy = listOf(Plan.FREE, Plan.STANDARD, Plan.PRO)
        val minRequiredPlan = requiredPlans
            .mapNotNull { planHierarchy.indexOf(it).takeIf { index -> index != -1 } }
            .minOrNull() ?: return "هذه الميزة غير متاحة حالياً"
        
        val targetPlan = planHierarchy[minRequiredPlan]
        
        return when (targetPlan) {
            Plan.STANDARD -> "هذه الميزة متاحة للخطة القياسية (\$10.99/شهر) أو أعلى. قم بترقية اشتراكك للاستفادة من هذه الميزة."
            Plan.PRO -> "هذه الميزة متاحة حصرياً للخطة الاحترافية (\$25.99/شهر). قم بترقية اشتراكك للخطة الاحترافية للاستفادة من هذه الميزة."
            else -> "قم بترقية خطتك للاستفادة من هذه الميزة."
        }
    }
    
    /**
     * Get upgrade message for features not available in current plan (by feature key)
     * 
     * @param currentPlan Current subscription plan name
     * @param featureKey Feature key that requires upgrade
     * @return Localized upgrade message in Arabic
     */
    fun getUpgradeMessage(currentPlan: String?, featureKey: String): String {
        val feature = Feature.fromKey(featureKey) ?: return "هذه الميزة غير متاحة حالياً"
        return getUpgradeMessage(currentPlan, feature)
    }
    
    /**
     * Get feature description for settings/UI display
     * 
     * @param feature Feature to get description for
     * @return Feature description string
     */
    fun getFeatureDescription(feature: Feature): String {
        return feature.description
    }
    
    /**
     * Get feature display name for UI
     * 
     * @param feature Feature to get display name for
     * @return Feature display name string
     */
    fun getFeatureDisplayName(feature: Feature): String {
        return feature.displayName
    }
    
    /**
     * Get feature emoji for UI
     * 
     * @param feature Feature to get emoji for
     * @return Feature emoji string
     */
    fun getFeatureEmoji(feature: Feature): String {
        return feature.emoji
    }
    
    /**
     * Check if a plan is higher tier than another
     * 
     * @param plan1 First plan to compare
     * @param plan2 Second plan to compare
     * @return True if plan1 is higher tier than plan2
     */
    fun isHigherTier(plan1: Plan, plan2: Plan): Boolean {
        val planHierarchy = listOf(Plan.FREE, Plan.STANDARD, Plan.PRO)
        val index1 = planHierarchy.indexOf(plan1)
        val index2 = planHierarchy.indexOf(plan2)
        return index1 > index2
    }
    
    /**
     * Get the minimum plan required for a feature
     * 
     * @param feature Feature to check
     * @return Minimum required plan, or null if feature is not available
     */
    fun getMinimumRequiredPlan(feature: Feature): Plan? {
        val requiredPlans = getPlansForFeature(feature)
        if (requiredPlans.isEmpty()) return null
        
        val planHierarchy = listOf(Plan.FREE, Plan.STANDARD, Plan.PRO)
        return requiredPlans.minByOrNull { planHierarchy.indexOf(it) }
    }
    
    /**
     * Utility class for feature access results
     */
    data class AccessResult(
        val hasAccess: Boolean,
        val currentPlan: Plan,
        val feature: Feature,
        val upgradeMessage: String? = null
    )
    
    /**
     * Comprehensive feature access check with detailed result
     * 
     * @param subscriptionPlan Current subscription plan name
     * @param feature Feature to check
     * @return AccessResult with detailed information
     */
    fun checkFeatureAccess(subscriptionPlan: String?, feature: Feature): AccessResult {
        val plan = Plan.fromString(subscriptionPlan)
        val accessGranted = hasAccess(plan, feature)
        
        return AccessResult(
            hasAccess = accessGranted,
            currentPlan = plan,
            feature = feature,
            upgradeMessage = if (accessGranted) null else getUpgradeMessage(subscriptionPlan, feature)
        )
    }
}

// Extension functions for backward compatibility with existing models

/**
 * Extension function for Employee model compatibility
 */
fun FeatureAccessMap.hasEmployeeAccess(subscriptionPlan: String?): Boolean {
    return hasAccess(subscriptionPlan, FeatureAccessMap.Feature.EMPLOYEE_MANAGEMENT)
}

/**
 * Extension function for ScheduledOperation model compatibility
 */
fun FeatureAccessMap.hasScheduledOperationsAccess(subscriptionPlan: String?): Boolean {
    return hasAccess(subscriptionPlan, FeatureAccessMap.Feature.SCHEDULED_OPERATIONS)
}

/**
 * Extension function for MerchantTax model compatibility
 */
fun FeatureAccessMap.hasTaxManagementAccess(subscriptionPlan: String?): Boolean {
    return hasAccess(subscriptionPlan, FeatureAccessMap.Feature.TAX_MANAGEMENT)
}

/**
 * Extension function for Reminder model compatibility
 */
fun FeatureAccessMap.hasReminderAccess(subscriptionPlan: String?): Boolean {
    return hasAccess(subscriptionPlan, FeatureAccessMap.Feature.REMINDERS)
}

/**
 * Extension function for FinancialReport model compatibility
 */
fun FeatureAccessMap.hasFinancialReportsAccess(subscriptionPlan: String?): Boolean {
    return hasAccess(subscriptionPlan, FeatureAccessMap.Feature.FINANCIAL_REPORTS)
}

/**
 * Extension function for UnifiedReport model compatibility
 */
fun FeatureAccessMap.hasUnifiedReportsAccess(subscriptionPlan: String?): Boolean {
    return hasAccess(subscriptionPlan, FeatureAccessMap.Feature.UNIFIED_REPORTS)
}

/**
 * Extension function for MerchantApiKey model compatibility
 */
fun FeatureAccessMap.hasApiIntegrationAccess(subscriptionPlan: String?): Boolean {
    return hasAccess(subscriptionPlan, FeatureAccessMap.Feature.API_INTEGRATION)
}

/**
 * Extension function for TransactionCategory model compatibility
 */
fun FeatureAccessMap.hasTransactionCategoriesAccess(subscriptionPlan: String?): Boolean {
    return hasAccess(subscriptionPlan, FeatureAccessMap.Feature.TRANSACTION_CATEGORIES)
}

/**
 * Extension function for SubAccount model compatibility
 */
fun FeatureAccessMap.hasSubAccountsAccess(subscriptionPlan: String?): Boolean {
    return hasAccess(subscriptionPlan, FeatureAccessMap.Feature.SUB_ACCOUNTS)
}