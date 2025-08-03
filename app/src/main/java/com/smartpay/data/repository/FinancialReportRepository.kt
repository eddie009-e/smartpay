package com.smartpay.repository

import com.smartpay.data.network.ApiClient
import com.smartpay.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.math.BigDecimal

class FinancialReportRepository {

    private val apiService = ApiClient.apiService

    suspend fun getReports(filters: ReportFilters? = null): Response<FinancialReportResponse> = withContext(Dispatchers.IO) {
        apiService.getFinancialReports(
            reportType = filters?.reportType,
            categoryId = filters?.categoryId,
            startDate = filters?.startDate,
            endDate = filters?.endDate,
            minAmount = filters?.minAmount?.toString(),
            maxAmount = filters?.maxAmount?.toString()
        )
    }

    suspend fun addReport(
        reportType: String,
        amount: BigDecimal,
        note: String?,
        categoryId: String?,
        occurredAt: String
    ): Response<FinancialReportResponse> = withContext(Dispatchers.IO) {
        val request = CreateFinancialReportRequest(
            reportType = reportType,
            amount = amount,
            note = note,
            categoryId = categoryId,
            occurredAt = occurredAt
        )
        apiService.createFinancialReport(request)
    }

    suspend fun getSummary(
        startDate: String? = null,
        endDate: String? = null,
        reportType: String? = null
    ): Response<FinancialReportSummaryResponse> = withContext(Dispatchers.IO) {
        apiService.getFinancialReportSummary(
            startDate = startDate,
            endDate = endDate,
            reportType = reportType
        )
    }

    suspend fun getGraphData(
        groupBy: String = "type", // type, category, date
        startDate: String? = null,
        endDate: String? = null,
        reportType: String? = null
    ): Response<FinancialReportGraphResponse> = withContext(Dispatchers.IO) {
        apiService.getFinancialReportGraphData(
            groupBy = groupBy,
            startDate = startDate,
            endDate = endDate,
            reportType = reportType
        )
    }

    suspend fun exportReports(
        format: String, // pdf, excel
        startDate: String? = null,
        endDate: String? = null,
        reportType: String? = null
    ): Response<BasicResponse> = withContext(Dispatchers.IO) {
        apiService.exportFinancialReports(
            format = format,
            startDate = startDate,
            endDate = endDate,
            reportType = reportType
        )
    }
}