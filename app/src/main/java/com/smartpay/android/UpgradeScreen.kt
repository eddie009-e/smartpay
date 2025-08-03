package com.smartpay.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.smartpay.data.network.ApiService
import com.smartpay.models.FeatureAccessMap
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Arabic RTL Subscription Upgrade Screen for SmartPay
 * 
 * Features:
 * - Display current subscription status
 * - Show all available plans with Arabic marketing copy
 * - Google IAP integration for automatic activation
 * - Manual SmartPay payment fallback
 * - RTL Arabic UI with proper styling
 */
class UpgradeScreen : ComponentActivity() {
    
    private lateinit var apiService: ApiService
    private lateinit var billingManager: BillingManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize API service
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.smartpay.sy/") // Replace with actual base URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)
        
        // Initialize billing manager
        billingManager = BillingManager(this)
        
        setContent {
            SubscriptionUpgradeScreen(
                apiService = apiService,
                billingManager = billingManager,
                onBack = { finish() }
            )
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        billingManager.endConnection()
    }
}

@Composable
fun SubscriptionUpgradeScreen(
    apiService: ApiService,
    billingManager: BillingManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var currentSubscription by remember { mutableStateOf<CurrentSubscription?>(null) }
    var availablePlans by remember { mutableStateOf<List<SubscriptionPlan>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isProcessingPayment by remember { mutableStateOf(false) }
    
    // Get secure preferences
    val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    val securePrefs = EncryptedSharedPreferences.create(
        "SmartPaySecurePrefs",
        masterKey,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    val token = securePrefs.getString("token", "") ?: ""
    val merchantId = securePrefs.getString("merchantId", "") ?: ""
    
    // Load data
    LaunchedEffect(Unit) {
        loadSubscriptionData(apiService, token, merchantId) { subscription, plans, error ->
            currentSubscription = subscription
            availablePlans = plans
            errorMessage = error
            isLoading = false
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF8F9FA)
    ) {
        if (isLoading) {
            LoadingScreen()
        } else if (errorMessage != null) {
            ErrorScreen(errorMessage!!) {
                isLoading = true
                errorMessage = null
                (context as ComponentActivity).lifecycleScope.launch {
                    loadSubscriptionData(apiService, token, merchantId) { subscription, plans, error ->
                        currentSubscription = subscription
                        availablePlans = plans
                        errorMessage = error
                        isLoading = false
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    UpgradeHeader(onBack = onBack)
                }
                
                // Current Subscription Status
                item {
                    currentSubscription?.let { subscription ->
                        CurrentSubscriptionCard(subscription)
                    }
                }
                
                // Available Plans
                items(availablePlans) { plan ->
                    SubscriptionPlanCard(
                        plan = plan,
                        isCurrentPlan = currentSubscription?.planCode == plan.planCode,
                        isProcessing = isProcessingPayment,
                        onSelectPlan = { selectedPlan ->
                            if (selectedPlan.planCode != "Free") {
                                isProcessingPayment = true
                                handlePlanSelection(
                                    context = context,
                                    billingManager = billingManager,
                                    apiService = apiService,
                                    plan = selectedPlan,
                                    token = token,
                                    merchantId = merchantId,
                                    onSuccess = {
                                        Toast.makeText(context, "تم تفعيل الاشتراك بنجاح!", Toast.LENGTH_LONG).show()
                                        isProcessingPayment = false
                                        // Reload data
                                        (context as ComponentActivity).lifecycleScope.launch {
                                            loadSubscriptionData(apiService, token, merchantId) { subscription, plans, error ->
                                                currentSubscription = subscription
                                                availablePlans = plans
                                                errorMessage = error
                                            }
                                        }
                                    },
                                    onError = { error ->
                                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                        isProcessingPayment = false
                                    }
                                )
                            }
                        }
                    )
                }
                
                // Footer with support info
                item {
                    SupportFooter()
                }
            }
        }
    }
}

@Composable
fun UpgradeHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "رجوع",
                tint = Color(0xFF2C3E50)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "اختر خطة اشتراكك",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
            Text(
                text = "ارتقِ بتجربتك مع SmartPay",
                fontSize = 14.sp,
                color = Color(0xFF7F8C8D)
            )
        }
    }
}

@Composable
fun CurrentSubscriptionCard(subscription: CurrentSubscription) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (subscription.planCode) {
                "Free" -> Color(0xFFE8F5E8)
                "Standard" -> Color(0xFFE3F2FD)
                "Pro" -> Color(0xFFE8EAF6)
                else -> Color(0xFFF5F5F5)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    when (subscription.planCode) {
                        "Free" -> Icons.Default.StarBorder
                        "Standard" -> Icons.Default.Star
                        "Pro" -> Icons.Default.Stars
                        else -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = when (subscription.planCode) {
                        "Free" -> Color(0xFF4CAF50)
                        "Standard" -> Color(0xFF2196F3)
                        "Pro" -> Color(0xFF9C27B0)
                        else -> Color(0xFF757575)
                    },
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "خطتك الحالية",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = subscription.planDisplayName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                }
                if (subscription.planCode != "Free") {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$${subscription.monthlyPrice}/شهر",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF27AE60)
                        )
                        if (subscription.isActive && subscription.endDate != null) {
                            Text(
                                text = "تنتهي: ${subscription.endDate}",
                                fontSize = 10.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionPlanCard(
    plan: SubscriptionPlan,
    isCurrentPlan: Boolean,
    isProcessing: Boolean,
    onSelectPlan: (SubscriptionPlan) -> Unit
) {
    val cardColor = when (plan.planCode) {
        "Free" -> Color.White
        "Standard" -> if (plan.isRecommended) Color(0xFFFFF3E0) else Color.White
        "Pro" -> Color(0xFFF3E5F5)
        else -> Color.White
    }
    
    val borderColor = when {
        isCurrentPlan -> Color(0xFF00D632)
        plan.isRecommended -> Color(0xFFFF9800)
        else -> Color(0xFFE0E0E0)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isCurrentPlan || plan.isRecommended) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (plan.isRecommended) 8.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // Plan header with badge
            Box(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = plan.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = if (plan.monthlyPrice == 0.0) "مجاني" else "$${plan.monthlyPrice}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (plan.planCode) {
                                "Free" -> Color(0xFF4CAF50)
                                "Standard" -> Color(0xFF2196F3)
                                "Pro" -> Color(0xFF9C27B0)
                                else -> Color(0xFF2C3E50)
                            }
                        )
                        if (plan.monthlyPrice > 0) {
                            Text(
                                text = "/شهر",
                                fontSize = 16.sp,
                                color = Color(0xFF666666),
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                        }
                    }
                }
                
                // Recommended badge
                if (plan.isRecommended) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFFF9800), Color(0xFFFF5722))
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "الأكثر شعبية",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                
                // Current plan badge
                if (isCurrentPlan) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(y = if (plan.isRecommended) 36.dp else 0.dp)
                            .background(
                                Color(0xFF00D632),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "خطتك الحالية",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Features list
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                plan.features.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF00D632),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = feature,
                            fontSize = 14.sp,
                            color = Color(0xFF2C3E50),
                            lineHeight = 20.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action button
            if (isCurrentPlan) {
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF95A5A6),
                        disabledContainerColor = Color(0xFF95A5A6)
                    ),
                    enabled = false,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "الخطة المفعلة حالياً",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            } else {
                Button(
                    onClick = { onSelectPlan(plan) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (plan.planCode) {
                            "Standard" -> Color(0xFF2196F3)
                            "Pro" -> Color(0xFF9C27B0)
                            else -> Color(0xFF00D632)
                        }
                    ),
                    enabled = !isProcessing,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isProcessing) "جاري المعالجة..." else "اختر هذه الباقة",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = Color(0xFF00D632),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "جاري تحميل خطط الاشتراك...",
                fontSize = 16.sp,
                color = Color(0xFF666666)
            )
        }
    }
}

@Composable
fun ErrorScreen(error: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = Color(0xFFE53E3E),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "خطأ في التحميل",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                fontSize = 14.sp,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D632))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("إعادة المحاولة")
            }
        }
    }
}

@Composable
fun SupportFooter() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.SupportAgent,
                contentDescription = null,
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "تحتاج مساعدة؟",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "تواصل معنا على +963999999999 أو عبر البريد الإلكتروني support@smartpay.sy",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )
        }
    }
}

// Data classes for API responses
data class CurrentSubscription(
    val id: String?,
    val planName: String,
    val planCode: String,
    val planDisplayName: String,
    val monthlyPrice: Double,
    val startDate: String?,
    val endDate: String?,
    val isActive: Boolean,
    val features: List<String>
)

data class SubscriptionPlan(
    val id: String,
    val name: String,
    val planCode: String,
    val monthlyPrice: Double,
    val features: List<String>,
    val isRecommended: Boolean
)

// Helper functions
private suspend fun loadSubscriptionData(
    apiService: ApiService,
    token: String,
    merchantId: String,
    onResult: (CurrentSubscription?, List<SubscriptionPlan>, String?) -> Unit
) {
    try {
        // Load current subscription and available plans in parallel
        val currentSubscriptionResponse = apiService.getCurrentSubscription("Bearer $token", merchantId)
        val plansResponse = apiService.getSubscriptionPlans()
        
        if (currentSubscriptionResponse.isSuccessful && plansResponse.isSuccessful) {
            val subscription = currentSubscriptionResponse.body()?.subscription?.let { sub ->
                CurrentSubscription(
                    id = sub.id,
                    planName = sub.plan_name ?: "Free",
                    planCode = sub.plan_name ?: "Free",
                    planDisplayName = sub.plan_display_name ?: "الخطة المجانية",
                    monthlyPrice = sub.monthly_price ?: 0.0,
                    startDate = sub.start_date,
                    endDate = sub.end_date,
                    isActive = sub.is_active ?: true,
                    features = sub.features ?: emptyList()
                )
            }
            
            val plans = plansResponse.body()?.plans?.map { plan ->
                SubscriptionPlan(
                    id = plan.id,
                    name = plan.name,
                    planCode = plan.plan_code,
                    monthlyPrice = plan.monthly_price,
                    features = plan.features,
                    isRecommended = plan.is_recommended
                )
            } ?: emptyList()
            
            onResult(subscription, plans, null)
        } else {
            onResult(null, emptyList(), "فشل في تحميل بيانات الاشتراك")
        }
    } catch (e: Exception) {
        onResult(null, emptyList(), "خطأ في الشبكة: ${e.message}")
    }
}

private fun handlePlanSelection(
    context: Context,
    billingManager: BillingManager,
    apiService: ApiService,
    plan: SubscriptionPlan,
    token: String,
    merchantId: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    // First try Google IAP if available
    if (billingManager.isIAPAvailable()) {
        billingManager.launchBillingFlow(
            context as ComponentActivity,
            plan.planCode,
            onSuccess = { purchaseToken ->
                // Activate subscription via API
                (context as ComponentActivity).lifecycleScope.launch {
                    try {
                        val response = apiService.activateIAPSubscription(
                            "Bearer $token",
                            mapOf(
                                "merchant_id" to merchantId,
                                "purchase_token" to purchaseToken,
                                "plan_code" to plan.planCode
                            )
                        )
                        
                        if (response.isSuccessful) {
                            onSuccess()
                        } else {
                            onError("فشل في تفعيل الاشتراك")
                        }
                    } catch (e: Exception) {
                        onError("خطأ في تفعيل الاشتراك: ${e.message}")
                    }
                }
            },
            onError = { error ->
                // Fall back to manual payment
                showManualPaymentDialog(context, apiService, plan, token, merchantId, onSuccess, onError)
            }
        )
    } else {
        // Show manual payment dialog
        showManualPaymentDialog(context, apiService, plan, token, merchantId, onSuccess, onError)
    }
}

private fun showManualPaymentDialog(
    context: Context,
    apiService: ApiService,
    plan: SubscriptionPlan,
    token: String,
    merchantId: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    // Create and show manual payment dialog
    ManualPaymentDialog.show(
        context = context,
        plan = plan,
        onConfirmPayment = {
            // Submit upgrade request
            (context as ComponentActivity).lifecycleScope.launch {
                try {
                    val response = apiService.submitUpgradeRequest(
                        "Bearer $token",
                        mapOf(
                            "from_merchant_id" to merchantId,
                            "target_plan" to plan.planCode,
                            "amount" to plan.monthlyPrice
                        )
                    )
                    
                    if (response.isSuccessful) {
                        Toast.makeText(context, 
                            "تم تقديم طلب الترقية بنجاح. سيتم المراجعة خلال 24 ساعة.",
                            Toast.LENGTH_LONG
                        ).show()
                        onSuccess()
                    } else {
                        onError("فشل في تقديم طلب الترقية")
                    }
                } catch (e: Exception) {
                    onError("خطأ في تقديم الطلب: ${e.message}")
                }
            }
        }
    )
}