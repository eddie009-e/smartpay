package com.smartpay.data.models

data class Invoice(
    val id: String? = null,            // معرّف الفاتورة (قد يكون null لو لم يرسله السيرفر)
    val toPhone: String,               // رقم هاتف المستلم
    val amount: Long,                  // قيمة الفاتورة
    val description: String? = null,   // وصف الفاتورة (اختياري)
    val status: String? = "pending",   // حالة الفاتورة (pending, paid, canceled)
    val issuedAt: Long,                // وقت إنشاء الفاتورة (timestamp)
    val paidAt: Long? = null           // وقت الدفع (اختياري)
)
