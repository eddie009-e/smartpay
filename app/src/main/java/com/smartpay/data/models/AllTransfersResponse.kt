package com.smartpay.models

import com.google.gson.annotations.SerializedName

data class AllTransfersResponse(
    val success: Boolean,
    val message: String,
    val data: TransfersData,
    val timestamp: String
)

data class TransfersData(
    val transfers: List<TransferItem>,
    val pagination: PaginationInfo,
    @SerializedName("filters_applied") val filtersApplied: FiltersApplied
)

data class TransferItem(
    val id: String,
    val amount: Double,
    val currency: String,
    val status: String,
    val type: String,
    val description: String?,
    val direction: String, // "sent", "received", "other"
    val sender: UserInfo,
    val receiver: UserInfo,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class UserInfo(
    val id: String,
    val name: String,
    val phone: String
)

data class PaginationInfo(
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("total_records") val totalRecords: Int,
    @SerializedName("per_page") val perPage: Int,
    @SerializedName("has_next") val hasNext: Boolean,
    @SerializedName("has_prev") val hasPrev: Boolean
)

data class FiltersApplied(
    @SerializedName("user_id") val userId: String?,
    @SerializedName("date_range") val dateRange: DateRange?,
    val status: String?,
    @SerializedName("amount_range") val amountRange: AmountRange?,
    val type: String
)

data class DateRange(
    @SerializedName("start_date") val startDate: String?,
    @SerializedName("end_date") val endDate: String?
)

data class AmountRange(
    @SerializedName("min_amount") val minAmount: String?,
    @SerializedName("max_amount") val maxAmount: String?
)

// Transfer Summary Response
data class TransferSummaryResponse(
    val success: Boolean,
    val message: String,
    val data: TransferSummaryData,
    val timestamp: String
)

data class TransferSummaryData(
    @SerializedName("total_transfers") val totalTransfers: Int,
    val sent: TransferStats,
    val received: TransferStats,
    @SerializedName("status_breakdown") val statusBreakdown: StatusBreakdown,
    @SerializedName("date_range") val dateRange: DateRange?
)

data class TransferStats(
    val count: Int,
    @SerializedName("total_amount") val totalAmount: Double
)

data class StatusBreakdown(
    val pending: Int,
    val completed: Int,
    val failed: Int
)