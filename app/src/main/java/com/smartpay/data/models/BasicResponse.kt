package com.smartpay.data.models

import com.google.gson.annotations.SerializedName

data class BasicResponse(
    @SerializedName("message")
    val message: String,

    @SerializedName("success")
    val success: Boolean? = null,

    @SerializedName("error")
    val error: String? = null
)