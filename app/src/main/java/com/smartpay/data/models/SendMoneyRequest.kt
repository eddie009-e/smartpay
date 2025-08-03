package com.smartpay.data.models

data class SendMoneyRequest(
    val receiverPhone: String,
    val amount: Long,
    val note: String? = null
)
