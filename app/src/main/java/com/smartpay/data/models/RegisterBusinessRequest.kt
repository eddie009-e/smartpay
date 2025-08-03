package com.smartpay.data.models

import com.google.gson.annotations.SerializedName

data class RegisterBusinessRequest(
    @SerializedName("phone")
    val phone: String,

    @SerializedName("business_name")
    val businessName: String,

    @SerializedName("owner_name")
    val ownerName: String? = null,

    @SerializedName("password")
    val password: String,

    @SerializedName("national_id")
    val nationalId: String? = null,

    @SerializedName("user_type")
    val userType: String = "merchant"
)
