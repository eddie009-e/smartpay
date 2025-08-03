package com.smartpay.models

import com.google.gson.annotations.SerializedName

data class TransferRequest(
    @SerializedName("receiverPhone")
    val receiverPhone: String,
    
    @SerializedName("amount")
    val amount: Long,
    
    @SerializedName("note")
    val note: String? = null
)