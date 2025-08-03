package com.smartpay.models

import com.google.gson.annotations.SerializedName

data class Transaction(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("type")
    val type: String,
    
    @SerializedName("amount")
    val amount: Long,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("timestamp")
    val timestamp: Long,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("fromUser")
    val fromUser: String? = null,
    
    @SerializedName("toUser")
    val toUser: String? = null
)