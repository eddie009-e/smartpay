package com.smartpay.android

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import com.android.billingclient.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Google Play Billing Manager for SmartPay Subscriptions
 * 
 * Handles Google In-App Purchases (IAP) for subscription upgrades:
 * - Standard Plan: smartpay_standard_monthly
 * - Pro Plan: smartpay_pro_monthly
 * 
 * Features:
 * - Automatic purchase flow
 * - Purchase validation
 * - Error handling with fallback to manual payment
 * - Arabic error messages
 */
class BillingManager(
    private val context: Context
) : PurchasesUpdatedListener, BillingClientStateListener {
    
    private var billingClient: BillingClient
    private var isServiceConnected = false
    private var currentPurchaseCallback: ((String) -> Unit)? = null
    private var currentErrorCallback: ((String) -> Unit)? = null
    
    companion object {
        private const val TAG = "BillingManager"
        
        // Product IDs for Google Play Console
        private const val STANDARD_PRODUCT_ID = "smartpay_standard_monthly"
        private const val PRO_PRODUCT_ID = "smartpay_pro_monthly"
        
        // Plan code to product ID mapping
        private val PLAN_TO_PRODUCT_ID = mapOf(
            "Standard" to STANDARD_PRODUCT_ID,
            "Pro" to PRO_PRODUCT_ID
        )
    }
    
    init {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()
        
        startConnection()
    }
    
    private fun startConnection() {
        billingClient.startConnection(this)
    }
    
    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            isServiceConnected = true
            Log.d(TAG, "Billing service connected successfully")
        } else {
            Log.e(TAG, "Failed to connect to billing service: ${billingResult.debugMessage}")
            isServiceConnected = false
        }
    }
    
    override fun onBillingServiceDisconnected() {
        isServiceConnected = false
        Log.w(TAG, "Billing service disconnected")
    }
    
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "User canceled the purchase")
            currentErrorCallback?.invoke("تم إلغاء عملية الشراء من قبل المستخدم")
        } else {
            Log.e(TAG, "Purchase failed: ${billingResult.debugMessage}")
            currentErrorCallback?.invoke("فشل في إتمام عملية الشراء: ${getArabicErrorMessage(billingResult.responseCode)}")
        }
    }
    
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Verify purchase and acknowledge
            GlobalScope.launch {
                try {
                    if (!purchase.isAcknowledged) {
                        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        
                        val ackResult = billingClient.acknowledgePurchase(acknowledgePurchaseParams)
                        if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            Log.d(TAG, "Purchase acknowledged successfully")
                        }
                    }
                    
                    // Return purchase token for server validation
                    withContext(Dispatchers.Main) {
                        currentPurchaseCallback?.invoke(purchase.purchaseToken)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling purchase", e)
                    withContext(Dispatchers.Main) {
                        currentErrorCallback?.invoke("خطأ في معالجة عملية الشراء")
                    }
                }
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Log.d(TAG, "Purchase is pending")
            currentErrorCallback?.invoke("عملية الشراء معلقة. يرجى المحاولة لاحقاً")
        }
    }
    
    /**
     * Check if Google Play Billing is available and connected
     */
    fun isIAPAvailable(): Boolean {
        return isServiceConnected && billingClient.isReady
    }
    
    /**
     * Launch billing flow for a specific plan
     */
    fun launchBillingFlow(
        activity: ComponentActivity,
        planCode: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isIAPAvailable()) {
            onError("خدمة Google Play Billing غير متوفرة")
            return
        }
        
        val productId = PLAN_TO_PRODUCT_ID[planCode]
        if (productId == null) {
            onError("خطة الاشتراك غير مدعومة")
            return
        }
        
        currentPurchaseCallback = onSuccess
        currentErrorCallback = onError
        
        // Query product details
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        
        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (productDetailsList.isNotEmpty()) {
                    val productDetails = productDetailsList[0]
                    val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
                    
                    if (offerToken != null) {
                        launchBillingFlowWithProduct(activity, productDetails, offerToken)
                    } else {
                        onError("تفاصيل المنتج غير متوفرة")
                    }
                } else {
                    onError("المنتج غير موجود في متجر Google Play")
                }
            } else {
                onError("فشل في تحميل تفاصيل المنتج: ${getArabicErrorMessage(billingResult.responseCode)}")
            }
        }
    }
    
    private fun launchBillingFlowWithProduct(
        activity: ComponentActivity,
        productDetails: ProductDetails,
        offerToken: String
    ) {
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()
        
        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            currentErrorCallback?.invoke("فشل في بدء عملية الشراء: ${getArabicErrorMessage(billingResult.responseCode)}")
        }
    }
    
    /**
     * Query current purchases to check subscription status
     */
    fun queryActivePurchases(onResult: (List<Purchase>) -> Unit) {
        if (!isIAPAvailable()) {
            onResult(emptyList())
            return
        }
        
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val activePurchases = purchases.filter { 
                    it.purchaseState == Purchase.PurchaseState.PURCHASED 
                }
                onResult(activePurchases)
            } else {
                Log.e(TAG, "Failed to query purchases: ${billingResult.debugMessage}")
                onResult(emptyList())
            }
        }
    }
    
    /**
     * Get subscription details for display
     */
    fun getSubscriptionDetails(
        planCodes: List<String>,
        onResult: (Map<String, ProductDetails>) -> Unit
    ) {
        if (!isIAPAvailable()) {
            onResult(emptyMap())
            return
        }
        
        val productList = planCodes.mapNotNull { planCode ->
            PLAN_TO_PRODUCT_ID[planCode]?.let { productId ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            }
        }
        
        if (productList.isEmpty()) {
            onResult(emptyMap())
            return
        }
        
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        
        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val detailsMap = mutableMapOf<String, ProductDetails>()
                
                productDetailsList.forEach { productDetails ->
                    // Find the plan code for this product ID
                    val planCode = PLAN_TO_PRODUCT_ID.entries.find { 
                        it.value == productDetails.productId 
                    }?.key
                    
                    if (planCode != null) {
                        detailsMap[planCode] = productDetails
                    }
                }
                
                onResult(detailsMap)
            } else {
                Log.e(TAG, "Failed to query product details: ${billingResult.debugMessage}")
                onResult(emptyMap())
            }
        }
    }
    
    /**
     * End billing client connection
     */
    fun endConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }
    
    /**
     * Convert billing response codes to Arabic error messages
     */
    private fun getArabicErrorMessage(responseCode: Int): String {
        return when (responseCode) {
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> 
                "خدمة الفوترة غير متوفرة على هذا الجهاز"
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> 
                "خطأ في إعداد التطبيق"
            BillingClient.BillingResponseCode.ERROR -> 
                "خطأ غير متوقع"
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> 
                "هذه الميزة غير مدعومة"
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> 
                "لديك اشتراك نشط بالفعل"
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> 
                "لا تملك هذا الاشتراك"
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> 
                "هذا الاشتراك غير متوفر"
            BillingClient.BillingResponseCode.NETWORK_ERROR -> 
                "خطأ في الشبكة، يرجى المحاولة لاحقاً"
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> 
                "انقطع الاتصال بخدمة Google Play"
            BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> 
                "انتهت مهلة الاتصال بخدمة Google Play"
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> 
                "خدمة Google Play غير متوفرة"
            BillingClient.BillingResponseCode.USER_CANCELED -> 
                "تم إلغاء العملية من قبل المستخدم"
            else -> "خطأ غير معروف ($responseCode)"
        }
    }
}

/**
 * Helper class to manage subscription purchase information
 */
data class SubscriptionPurchase(
    val productId: String,
    val purchaseToken: String,
    val planCode: String,
    val isActive: Boolean,
    val purchaseTime: Long
) {
    companion object {
        fun fromPurchase(purchase: Purchase): SubscriptionPurchase? {
            val planCode = BillingManager.PLAN_TO_PRODUCT_ID.entries
                .find { it.value in purchase.products }?.key
                ?: return null
            
            return SubscriptionPurchase(
                productId = purchase.products.first(),
                purchaseToken = purchase.purchaseToken,
                planCode = planCode,
                isActive = purchase.purchaseState == Purchase.PurchaseState.PURCHASED,
                purchaseTime = purchase.purchaseTime
            )
        }
    }
}