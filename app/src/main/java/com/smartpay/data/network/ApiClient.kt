package com.smartpay.data.network

import android.content.Context
import com.smartpay.android.security.CertificatePinner
import com.smartpay.android.security.SecureStorage
import com.smartpay.android.security.SecurityUtils
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "https://navapay.org/api/"
    private const val CONNECT_TIMEOUT = 30L
    private const val READ_TIMEOUT = 30L
    private const val WRITE_TIMEOUT = 30L
    private const val DEBUG_MODE = true // غير هذا إلى false في الإنتاج

    @Volatile
    private var secureStorage: SecureStorage? = null

    fun initialize(context: Context) {
        secureStorage = SecureStorage.getInstance(context)
    }

    fun setAuthToken(token: String?) {
        secureStorage?.saveAuthToken(token ?: "")
    }

    fun getAuthToken(): String? {
        return secureStorage?.getAuthToken()
    }

    fun clearAuthToken() {
        secureStorage?.clearSession()
    }

    private val securityInterceptor = Interceptor { chain ->
        val original: Request = chain.request()
        val builder = original.newBuilder()

        getAuthToken()?.let { token ->
            val formattedToken = if (!token.startsWith("Bearer ")) "Bearer $token" else token
            builder.header("Authorization", formattedToken)
        }

        builder.header("Content-Type", "application/json")
        builder.header("User-Agent", "SmartPay-Android/1.0")
        builder.header("X-App-Version", "1.0.0")

        val timestamp = System.currentTimeMillis().toString()
        builder.header("X-Timestamp", timestamp)

        val nonce = SecurityUtils.generateRandomString(16)
        builder.header("X-Nonce", nonce)

        chain.proceed(builder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        android.util.Log.d("ApiClient", SecurityUtils.sanitizeLog(message))
    }.apply {
        level = if (DEBUG_MODE) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val client = CertificatePinner.createSecureOkHttpClient()
        .newBuilder()
        .addInterceptor(securityInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}