package com.smartpay.data.models

data class InvoiceRequest(
    val toPhone: String,
    val amount: Long,
    val description: String
)
