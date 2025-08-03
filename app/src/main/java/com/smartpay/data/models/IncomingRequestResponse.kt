package com.smartpay.data.models

data class IncomingRequestResponse(
    val id: String,
    val phone: String,      // رقم الشخص الذي طلب منك
    val amount: Long,
    val createdAt: Long
)
