package com.smartpay.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class SubscriptionResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("subscription")
    val subscription: SubscriptionModel?,
    
    @SerializedName("message")
    val message: String?
)

data class SubscriptionModel(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("merchant_id")
    val merchantId: String,
    
    @SerializedName("plan_name")
    val planName: String,
    
    @SerializedName("monthly_fee")
    val monthlyFee: BigDecimal,
    
    @SerializedName("start_date")
    val startDate: String,
    
    @SerializedName("end_date")
    val endDate: String,
    
    @SerializedName("is_active")
    val isActive: Boolean,
    
    @SerializedName("status")
    val status: String? = null,
    
    @SerializedName("created_at")
    val createdAt: String
)

data class SubscriptionRequest(
    @SerializedName("planName")
    val planName: String
)

data class SubscriptionPlan(
    val name: String,
    val displayName: String,
    val monthlyFee: BigDecimal,
    val currency: String = "ل.س",
    val features: List<String>,
    val color: Long,
    val isRecommended: Boolean = false
) {
    companion object {
        fun getAvailablePlans(): List<SubscriptionPlan> = listOf(
            SubscriptionPlan(
                name = "Free",
                displayName = "مجاني",
                monthlyFee = BigDecimal("0.00"),
                features = listOf(
                    "حتى 10 معاملات شهرياً",
                    "دعم أساسي",
                    "تقارير بسيطة"
                ),
                color = 0xFF9E9E9E
            ),
            SubscriptionPlan(
                name = "Standard",
                displayName = "قياسي",
                monthlyFee = BigDecimal("10.99"),
                features = listOf(
                    "حتى 100 معاملة شهرياً",
                    "دعم عبر البريد الإلكتروني",
                    "تقارير متقدمة",
                    "تصدير PDF و Excel"
                ),
                color = 0xFF2196F3,
                isRecommended = true
            ),
            SubscriptionPlan(
                name = "Pro",
                displayName = "احترافي",
                monthlyFee = BigDecimal("25.99"),
                features = listOf(
                    "معاملات غير محدودة",
                    "دعم على مدار الساعة",
                    "تقارير تفصيلية ومخصصة",
                    "API متقدم",
                    "تكاملات خارجية",
                    "أولوية في المعالجة"
                ),
                color = 0xFFFF9800
            )
        )
        
        fun getPlanByName(name: String): SubscriptionPlan? = 
            getAvailablePlans().find { it.name == name }
    }
}