package com.smartpay.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class SalaryPayment(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("employee_id")
    val employeeId: String,
    
    @SerializedName("business_id")
    val businessId: String,
    
    @SerializedName("amount")
    val amount: BigDecimal,
    
    @SerializedName("paid_at")
    val paidAt: String, // ISO 8601 timestamp string
    
    @SerializedName("note")
    val note: String? = null,
    
    @SerializedName("status")
    val status: String = "completed"
)

// Request model for creating salary payments
data class SalaryPaymentRequest(
    @SerializedName("employee_id")
    val employeeId: String,
    
    @SerializedName("business_id")
    val businessId: String,
    
    @SerializedName("amount")
    val amount: BigDecimal,
    
    @SerializedName("note")
    val note: String? = null
)