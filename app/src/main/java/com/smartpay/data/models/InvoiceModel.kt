package com.smartpay.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class InvoiceModel(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("business_id")
    val businessId: String? = null,
    
    @SerializedName("customer_id")
    val customerId: String? = null,
    
    @SerializedName("amount")
    val amount: BigDecimal,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("due_date")
    val dueDate: String? = null,
    
    @SerializedName("status")
    val status: String = "pending", // pending, paid, cancelled
    
    @SerializedName("created_at")
    val createdAt: String,
    
    @SerializedName("paid_at")
    val paidAt: String? = null
)

// Request model for creating invoices
data class CreateInvoiceRequest(
    @SerializedName("business_id")
    val businessId: String? = null,
    
    @SerializedName("customer_id")
    val customerId: String? = null,
    
    @SerializedName("amount")
    val amount: BigDecimal,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("due_date")
    val dueDate: String? = null
)

// Request model for updating invoice status
data class UpdateInvoiceStatusRequest(
    @SerializedName("status")
    val status: String
)