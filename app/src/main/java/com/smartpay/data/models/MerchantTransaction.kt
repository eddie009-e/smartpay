package com.smartpay.data.models

data class MerchantTransaction(
    val id: String,
    val amount: Long,
    val type: String,
    val createdAt: String
)
