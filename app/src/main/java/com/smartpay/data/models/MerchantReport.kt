package com.smartpay.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class MerchantReport(
    @SerializedName("sentTotal")
    val sentTotal: BigDecimal,
    
    @SerializedName("receivedTotal")
    val receivedTotal: BigDecimal,
    
    @SerializedName("expensesTotal")
    val expensesTotal: BigDecimal,
    
    @SerializedName("salariesTotal")
    val salariesTotal: BigDecimal,
    
    @SerializedName("invoiceSummary")
    val invoiceSummary: InvoiceSummary,
    
    @SerializedName("pendingRequests")
    val pendingRequests: Int
)

data class InvoiceSummary(
    @SerializedName("total")
    val total: Int,
    
    @SerializedName("unpaid")
    val unpaid: Int,
    
    @SerializedName("paid")
    val paid: Int,
    
    @SerializedName("totalAmount")
    val totalAmount: BigDecimal
)