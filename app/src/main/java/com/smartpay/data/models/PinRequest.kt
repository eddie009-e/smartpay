package com.smartpay.data.models

import com.google.gson.annotations.SerializedName

data class PinRequest(
    @SerializedName("pin")
    val pin: String
)