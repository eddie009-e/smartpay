package com.smartpay.models

import com.google.gson.annotations.SerializedName

data class MerchantRequestDto(
    @SerializedName("id")
    val id: String? = null,
    
    @SerializedName("clientPhone")
    val clientPhone: String,
    
    @SerializedName("amount")
    val amount: Long,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("status")
    val status: String = "pending",
    
    @SerializedName("merchantId")
    val merchantId: String? = null,
    
    @SerializedName("createdAt")
    val createdAt: Long? = null,
    
    @SerializedName("updatedAt")
    val updatedAt: Long? = null
)