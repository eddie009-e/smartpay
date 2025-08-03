package com.smartpay.data.models

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("token")
    val token: String,

    @SerializedName("user")
    val user: UserInfo
)

data class UserInfo(
    @SerializedName("id")
    val id: String,

    @SerializedName("full_name")
    val fullname: String? = null, // للأفراد

    @SerializedName("business_name")
    val businessName: String? = null, // للتجار

    @SerializedName("phone")
    val phone: String,

    @SerializedName("user_type")
    val userType: String
)
