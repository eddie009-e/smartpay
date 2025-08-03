package com.smartpay.repository

import com.smartpay.data.network.ApiClient
import com.smartpay.models.CreateTransactionCategoryRequest
import com.smartpay.models.TransactionCategoryResponse
import com.smartpay.models.UpdateTransactionCategoryRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class TransactionCategoryRepository {

    private val apiService = ApiClient.apiService

    suspend fun getCategories(): Response<TransactionCategoryResponse> = withContext(Dispatchers.IO) {
        apiService.getTransactionCategories()
    }

    suspend fun addCategory(nameAr: String, nameEn: String, color: String): Response<TransactionCategoryResponse> = withContext(Dispatchers.IO) {
        val request = CreateTransactionCategoryRequest(
            nameAr = nameAr,
            nameEn = nameEn,
            color = color
        )
        apiService.createTransactionCategory(request)
    }

    suspend fun updateCategory(id: String, nameAr: String, nameEn: String, color: String): Response<TransactionCategoryResponse> = withContext(Dispatchers.IO) {
        val request = UpdateTransactionCategoryRequest(
            nameAr = nameAr,
            nameEn = nameEn,
            color = color
        )
        apiService.updateTransactionCategory(id, request)
    }

    suspend fun deleteCategory(id: String): Response<TransactionCategoryResponse> = withContext(Dispatchers.IO) {
        apiService.deleteTransactionCategory(id)
    }
}