package com.smartpay.models

import com.google.gson.annotations.SerializedName

/**
 * Transaction Categories - Standard Plan Feature
 * 
 * This feature allows merchants to create, edit, delete, and manage custom 
 * transaction categories with color-coding and Arabic/English names.
 * 
 * Access Requirements:
 * - Standard Plan ($10.99/month) or higher
 * - Pro Plan ($25.99/month) 
 * - NOT available for Free Plan users
 */

data class TransactionCategoryResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("categories")
    val categories: List<TransactionCategory>? = null,
    
    @SerializedName("category")
    val category: TransactionCategory? = null,
    
    @SerializedName("total")
    val total: Int? = null,
    
    @SerializedName("message")
    val message: String?
)

data class TransactionCategory(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("merchant_id")
    val merchantId: String,
    
    @SerializedName("name_ar")
    val nameAr: String,
    
    @SerializedName("name_en")
    val nameEn: String,
    
    @SerializedName("color")
    val color: String,
    
    @SerializedName("created_at")
    val createdAt: String
) {
    companion object {
        /**
         * Get predefined color options for categories
         */
        fun getColorOptions(): List<CategoryColor> {
            return listOf(
                CategoryColor("#F44336", "أحمر", "Red"),
                CategoryColor("#E91E63", "وردي", "Pink"),
                CategoryColor("#9C27B0", "بنفسجي", "Purple"),
                CategoryColor("#673AB7", "بنفسجي داكن", "Deep Purple"),
                CategoryColor("#3F51B5", "أزرق نيلي", "Indigo"),
                CategoryColor("#2196F3", "أزرق", "Blue"),
                CategoryColor("#03A9F4", "أزرق فاتح", "Light Blue"),
                CategoryColor("#00BCD4", "سماوي", "Cyan"),
                CategoryColor("#009688", "أخضر مزرق", "Teal"),
                CategoryColor("#4CAF50", "أخضر", "Green"),
                CategoryColor("#8BC34A", "أخضر فاتح", "Light Green"),
                CategoryColor("#CDDC39", "أخضر ليموني", "Lime"),
                CategoryColor("#FFEB3B", "أصفر", "Yellow"),
                CategoryColor("#FFC107", "كهرماني", "Amber"),
                CategoryColor("#FF9800", "برتقالي", "Orange"),
                CategoryColor("#FF5722", "برتقالي محمر", "Deep Orange"),
                CategoryColor("#795548", "بني", "Brown"),
                CategoryColor("#607D8B", "رمادي مزرق", "Blue Grey")
            )
        }
        
        /**
         * Get color name in Arabic
         */
        fun getColorNameAr(color: String): String {
            return getColorOptions().find { it.hex == color }?.nameAr ?: "لون مخصص"
        }
        
        /**
         * Validate hex color format
         */
        fun isValidColor(color: String): Boolean {
            return color.matches(Regex("^#[0-9A-Fa-f]{6}$"))
        }
        
        /**
         * Check if user has access to transaction categories feature
         * Uses centralized FeatureAccessMap for consistency (Standard + Pro plans)
         */
        fun hasFeatureAccess(subscriptionPlan: String?): Boolean {
            return FeatureAccessMap.hasTransactionCategoriesAccess(subscriptionPlan)
        }
        
        /**
         * Get upgrade message for Free plan users
         * Uses centralized FeatureAccessMap for consistency
         */
        fun getUpgradeMessage(): String {
            return FeatureAccessMap.getUpgradeMessage(null, FeatureAccessMap.Feature.TRANSACTION_CATEGORIES)
        }
    }
}

data class CategoryColor(
    val hex: String,
    val nameAr: String,
    val nameEn: String
)

data class CreateTransactionCategoryRequest(
    @SerializedName("name_ar")
    val nameAr: String,
    
    @SerializedName("name_en")
    val nameEn: String,
    
    @SerializedName("color")
    val color: String
)

data class UpdateTransactionCategoryRequest(
    @SerializedName("name_ar")
    val nameAr: String,
    
    @SerializedName("name_en")
    val nameEn: String,
    
    @SerializedName("color")
    val color: String
)