package com.smartpay.data.models

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    @SerializedName("phone")
    val phone: String,

    @SerializedName("full_name")
    val name: String,

    @SerializedName("national_id")
    val nationalId: String? = null,

    @SerializedName("password")
    val password: String,

    @SerializedName("user_type")
    val userType: String = "personal" // أعدناها إلى personal
)
