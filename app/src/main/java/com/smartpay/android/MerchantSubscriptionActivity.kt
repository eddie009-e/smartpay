package com.smartpay.android

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.smartpay.models.*
import com.smartpay.repository.MerchantSubscriptionRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.util.*

class MerchantSubscriptionActivity : ComponentActivity() {

    private val subscriptionRepository = MerchantSubscriptionRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get token from secure storage
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val securePrefs = EncryptedSharedPreferences.create(
            "SmartPaySecurePrefs",
            masterKey,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val token = securePrefs.getString("token", null) ?: ""

        if (token.isEmpty()) {
            Toast.makeText(this, "الجلسة غير صالحة، يرجى تسجيل الدخول", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            MerchantSubscriptionScreen(
                onBack = { finish() },
                repository = subscriptionRepository
            )
        }
    }
}

@Composable
fun MerchantSubscriptionScreen(
    onBack: () -> Unit,
    repository: MerchantSubscriptionRepository
) {
    val context = LocalContext.current
    var currentSubscription by remember { mutableStateOf<SubscriptionModel?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isSubscribing by remember { mutableStateOf(false) }
    var selectedPlan by remember { mutableStateOf<String?>(null) }

    val availablePlans = SubscriptionPlan.getAvailablePlans()

    fun loadCurrentSubscription() {
        isLoading = true
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val response = repository.getCurrentSubscription()
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    currentSubscription = body.subscription
                    if (body.message != null && body.subscription == null) {
                        // No active subscription
                        currentSubscription = null
                    }
                } else {
                    Toast.makeText(context, "فشل في تحميل بيانات الاشتراك", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    fun subscribeToPlan(planName: String) {
        selectedPlan = planName
        isSubscribing = true
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val response = repository.subscribeToPlan(planName)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.success) {
                        currentSubscription = body.subscription
                        Toast.makeText(context, body.message ?: "تم الاشتراك بنجاح", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, body.message ?: "فشل في الاشتراك", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "فشل في الاشتراك", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isSubscribing = false
                selectedPlan = null
            }
        }
    }

    fun cancelSubscription() {
        isSubscribing = true
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val response = repository.cancelSubscription()
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.success) {
                        currentSubscription = body.subscription
                        Toast.makeText(context, body.message ?: "تم إلغاء الاشتراك بنجاح", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, body.message ?: "فشل في إلغاء الاشتراك", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "فشل في إلغاء الاشتراك", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isSubscribing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadCurrentSubscription()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .padding(top = 40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.Black)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "🔔 الاشتراكات",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.Subscriptions, contentDescription = null, tint = Color(0xFF00D632))
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF00D632))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "جاري تحميل بيانات الاشتراك...",
                            fontSize = 16.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Current Subscription Section
                    item {
                        CurrentSubscriptionSection(
                            subscription = currentSubscription,
                            onCancel = { cancelSubscription() },
                            isLoading = isSubscribing
                        )
                    }

                    // Available Plans Header
                    item {
                        Text(
                            text = "📋 الخطط المتاحة",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    // Available Plans
                    items(availablePlans) { plan ->
                        SubscriptionPlanCard(
                            plan = plan,
                            currentSubscription = currentSubscription,
                            onSubscribe = { subscribeToPlan(plan.name) },
                            isLoading = isSubscribing && selectedPlan == plan.name
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentSubscriptionSection(
    subscription: SubscriptionModel?,
    onCancel: () -> Unit,
    isLoading: Boolean
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (subscription?.isActive == true) 
                Color(0xFF00D632).copy(alpha = 0.1f) 
            else 
                Color(0xFFF7F8FA)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (subscription?.isActive == true) Color(0xFF00D632) else Color(0xFF666666),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "الاشتراك الحالي",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            if (subscription != null && subscription.isActive) {
                // Active subscription details
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SubscriptionDetailRow("الخطة:", subscription.planName)
                    SubscriptionDetailRow("الرسوم الشهرية:", "${subscription.monthlyFee} ل.س")
                    SubscriptionDetailRow("تاريخ البداية:", formatDate(subscription.startDate))
                    SubscriptionDetailRow("تاريخ النهاية:", formatDate(subscription.endDate))
                    SubscriptionDetailRow("الحالة:", 
                        when (subscription.status) {
                            "active" -> "نشط"
                            "expired" -> "منتهي الصلاحية"
                            "canceled" -> "ملغي"
                            else -> subscription.status ?: "غير معروف"
                        }
                    )
                }

                if (subscription.status == "active") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53E3E)),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Default.Cancel, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            if (isLoading) "جاري الإلغاء..." else "إلغاء الاشتراك",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Text(
                    text = "لا يوجد اشتراك نشط حالياً",
                    fontSize = 16.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun SubscriptionDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF666666)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )
    }
}

@Composable
fun SubscriptionPlanCard(
    plan: SubscriptionPlan,
    currentSubscription: SubscriptionModel?,
    onSubscribe: () -> Unit,
    isLoading: Boolean
) {
    val isCurrentPlan = currentSubscription?.planName == plan.name && currentSubscription.isActive
    val hasActiveSubscription = currentSubscription?.isActive == true

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (plan.isRecommended) {
                    Modifier.border(2.dp, Color(0xFF00D632), RoundedCornerShape(16.dp))
                } else {
                    Modifier
                }
            )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color(plan.color), RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = plan.displayName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                
                if (plan.isRecommended) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFF00D632), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "موصى به",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Price
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${plan.monthlyFee}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(plan.color)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${plan.currency} / شهرياً",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Features
            plan.features.forEach { feature ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF00D632),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = feature,
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Subscribe Button
            Button(
                onClick = onSubscribe,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCurrentPlan) Color(0xFF00D632) else Color(plan.color)
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading && !isCurrentPlan && !hasActiveSubscription,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جاري الاشتراك...")
                } else if (isCurrentPlan) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("الخطة الحالية", color = Color.White, fontWeight = FontWeight.Bold)
                } else if (hasActiveSubscription) {
                    Text("يجب إلغاء الاشتراك الحالي أولاً", color = Color.White, fontSize = 12.sp)
                } else {
                    Text("اشترك الآن", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun formatDate(dateString: String): String {
    return try {
        val dateFormatter = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        if (dateString.contains("T")) {
            val zonedDateTime = ZonedDateTime.parse(dateString)
            dateFormatter.format(Date.from(zonedDateTime.toInstant()))
        } else {
            // Already in YYYY-MM-DD format, just reformat
            val inputFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormatter.parse(dateString)
            dateFormatter.format(date!!)
        }
    } catch (e: Exception) {
        dateString
    }
}