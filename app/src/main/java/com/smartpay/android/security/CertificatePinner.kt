package com.smartpay.android.security

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.*

object CertificatePinner {
    
    private const val DOMAIN = "10.0.2.2"
    
    fun createSecureOkHttpClient(): OkHttpClient {
        return try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            
            val sslSocketFactory = sslContext.socketFactory
            
            OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .addInterceptor { chain ->
                    val request = chain.request()
                    
                    if (!request.url.isHttps && request.url.host != DOMAIN) {
                        throw SecurityException("Only HTTPS connections are allowed for external hosts")
                    }
                    
                    chain.proceed(request)
                }
                .build()
        } catch (e: Exception) {
            throw RuntimeException("Failed to create secure HTTP client", e)
        }
    }
    
    fun createProductionOkHttpClient(certificateHashes: List<String>): OkHttpClient {
        val certificatePinner = CertificatePinner.Builder().apply {
            certificateHashes.forEach { hash ->
                add("*.yourdomain.com", "sha256/$hash")
            }
        }.build()
        
        return OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
            .addInterceptor { chain ->
                val request = chain.request()
                
                if (!request.url.isHttps) {
                    throw SecurityException("Only HTTPS connections are allowed")
                }
                
                chain.proceed(request)
            }
            .build()
    }
    
    fun validateCertificateChain(chain: Array<X509Certificate>?): Boolean {
        if (chain.isNullOrEmpty()) return false
        
        return try {
            val leafCertificate = chain[0]
            
            leafCertificate.checkValidity()
            
            val expectedSubject = "CN=yourdomain.com"
            leafCertificate.subjectDN.name.contains(expectedSubject, ignoreCase = true)
        } catch (e: CertificateException) {
            false
        }
    }
}