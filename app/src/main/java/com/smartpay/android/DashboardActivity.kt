package com.smartpay.android

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.smartpay.data.repository.UserRepository
import kotlinx.coroutines.launch

class DashboardActivity : ComponentActivity() {

    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ التحقق من تسجيل الدخول
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val securePrefs = EncryptedSharedPreferences.create(
            "SmartPaySecurePrefs",
            masterKey,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val token = securePrefs.getString("token", null)

        if (token.isNullOrEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContent {
            ModernSmartPayDashboard()
        }
    }

    @Composable
    fun ModernSmartPayDashboard() {
        val context = LocalContext.current
        val scrollState = rememberScrollState()
        var balance by remember { mutableStateOf<Long>(0) }
        var showRequestDialog by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(true) }

        // ✅ جلب الرصيد من السيرفر
        LaunchedEffect(Unit) {
            lifecycleScope.launch {
                try {
                    val response = userRepository.getWalletBalance()
                    if (response.isSuccessful && response.body() != null) {
                        balance = response.body()!!.balance
                    } else {
                        Toast.makeText(context, "فشل في جلب الرصيد", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isLoading = false
                }
            }
        }

        // الخلفية مع العناصر المتحركة
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FDED))
        ) {
            // عناصر خلفية متحركة
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .offset(x = 250.dp, y = 100.dp)
                    .background(
                        Color(0xFFD8FBA9).copy(alpha = 0.1f),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .offset(x = 50.dp, y = 300.dp)
                    .background(
                        Color(0xFF2D2D2D).copy(alpha = 0.05f),
                        CircleShape
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                // Header Section
                ModernHeader()

                Spacer(modifier = Modifier.height(32.dp))

                // Bank Card Section
                ModernBankCard(balance = balance, isLoading = isLoading)

                Spacer(modifier = Modifier.height(32.dp))

                // Quick Actions Section
                ModernQuickActions(onRequestClick = { showRequestDialog = true })

                Spacer(modifier = Modifier.height(24.dp))

                // Activity Section
                ModernActivitySection()

                Spacer(modifier = Modifier.height(120.dp))
            }

            // Bottom Action Buttons
            ModernBottomActions(onRequestClick = { showRequestDialog = true })
        }

        // Request Dialog
        if (showRequestDialog) {
            ModernRequestDialog(
                onDismiss = { showRequestDialog = false },
                onSend = { toPhone, amount ->
                    lifecycleScope.launch {
                        try {
                            val response = userRepository.sendMoneyRequest(toPhone, amount)
                            if (response.isSuccessful) {
                                Toast.makeText(context, "تم إرسال الطلب", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "فشل في إرسال الطلب", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }

    @Composable
    fun ModernHeader() {
        val context = LocalContext.current
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Settings Button
            Card(
                modifier = Modifier
                    .size(48.dp)
                    .clickable {
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "الإعدادات",
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF2D2D2D)
                    )
                }
            }

            // App Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "✨",
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "سمارت باي",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D2D2D)
                )
            }

            // Notifications Button
            Card(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "الإشعارات",
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF2D2D2D)
                    )
                    // نقطة الإشعار
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .offset(x = 8.dp, y = (-8).dp)
                            .background(Color.Red, CircleShape)
                    )
                }
            }
        }
    }

    @Composable
    fun ModernBankCard(balance: Long, isLoading: Boolean) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Background Patterns
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .offset(x = 200.dp, y = (-80).dp)
                        .background(
                            Color.White.copy(alpha = 0.05f),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(128.dp)
                        .offset(x = (-64).dp, y = 150.dp)
                        .background(
                            Color(0xFFD8FBA9).copy(alpha = 0.1f),
                            CircleShape
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                ) {
                    // Balance Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFD8FBA9)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "الرصيد الكلي",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFD1D5DB)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Balance Amount
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color(0xFFD8FBA9),
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Text(
                                text = "$${balance}",
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        IconButton(
                            onClick = { /* Toggle visibility */ }
                        ) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = "إظهار/إخفاء",
                                modifier = Modifier.size(24.dp),
                                tint = Color(0xFF9CA3AF)
                            )
                        }
                    }

                    // Growth Indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        repeat(3) { index ->
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFFD8FBA9), CircleShape)
                            )
                            if (index < 2) Spacer(modifier = Modifier.width(4.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "+٢.٥% هذا الشهر",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFD8FBA9)
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // Card Details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "CARD HOLDER",
                                fontSize = 10.sp,
                                color = Color(0xFF9CA3AF)
                            )
                            Text(
                                text = "Your Name",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Card(
                                modifier = Modifier.size(width = 64.dp, height = 48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFD8FBA9)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CreditCard,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = Color.Black
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row {
                                repeat(3) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 12.dp, height = 4.dp)
                                            .background(
                                                Color.White.copy(alpha = 0.6f),
                                                RoundedCornerShape(2.dp)
                                            )
                                    )
                                    if (it < 2) Spacer(modifier = Modifier.width(2.dp))
                                }
                                Spacer(modifier = Modifier.width(2.dp))
                                Box(
                                    modifier = Modifier
                                        .size(width = 12.dp, height = 4.dp)
                                        .background(
                                            Color.White.copy(alpha = 0.9f),
                                            RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                            Text(
                                text = "••• ١٢٣٤",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Add Money Button
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { /* Add money */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D2D2D)),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
        ) {
            Icon(
                Icons.Default.Bolt,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "شحن الرصيد",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    @Composable
    fun ModernQuickActions(onRequestClick: () -> Unit) {
        val context = LocalContext.current
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.6f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✨", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "الإجراءات السريعة",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D2D2D)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // QR Code Action
                    ModernActionButton(
                        title = "الباركود",
                        icon = Icons.Default.QrCode,
                        backgroundColor = Color(0xFFD8FBA9),
                        iconColor = Color.Black,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            context.startActivity(Intent(context, QRCodeActivity::class.java))
                        }
                    )

                    // Requests Action
                    ModernActionButton(
                        title = "الطلبات",
                        icon = Icons.Default.CallReceived,
                        backgroundColor = Color(0xFF2D2D2D),
                        iconColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            context.startActivity(Intent(context, IncomingRequestsActivity::class.java))
                        }
                    )

                    // History Action
                    ModernActionButton(
                        title = "السجل",
                        icon = Icons.Default.History,
                        backgroundColor = Color(0xFF6B7280),
                        iconColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            context.startActivity(Intent(context, TransactionHistoryActivity::class.java))
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun ModernActionButton(
        title: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        backgroundColor: Color,
        iconColor: Color,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
    ) {
        Card(
            modifier = modifier
                .height(100.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = backgroundColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = iconColor
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D2D2D)
                )
            }
        }
    }

    @Composable
    fun ModernActivitySection() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD8FBA9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Timeline,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "النشاط",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D2D2D)
                )
            }

            TextButton(
                onClick = { /* View all */ },
                colors = ButtonDefaults.textButtonColors(
                    containerColor = Color.White.copy(alpha = 0.5f),
                    contentColor = Color(0xFF2D2D2D)
                )
            ) {
                Text(
                    text = "عرض الكل",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.6f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD8FBA9).copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Timeline,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Color(0xFF2D2D2D)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "لا يوجد نشاط حديث",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6B7280)
                )
                Text(
                    text = "ستظهر التحويلات هنا عند توفرها",
                    fontSize = 14.sp,
                    color = Color(0xFF9CA3AF)
                )
            }
        }
    }

    @Composable
    fun ModernBottomActions(onRequestClick: () -> Unit) {
        val context = LocalContext.current
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FDED)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Request Button
                    Button(
                        onClick = onRequestClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D2D2D)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
                    ) {
                        Icon(
                            Icons.Default.CallReceived,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "طلب",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Send Button
                    Button(
                        onClick = {
                            context.startActivity(Intent(context, SendMoneyActivity::class.java))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD8FBA9)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
                    ) {
                        Text(
                            text = "إرسال",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.Black
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun ModernRequestDialog(
        onDismiss: () -> Unit,
        onSend: (String, Long) -> Unit
    ) {
        var phone by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "طلب أموال",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D2D2D)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("المبلغ") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = {
                            val parsedAmount = amount.toLongOrNull()
                            if (parsedAmount != null && phone.isNotBlank()) {
                                onSend(phone, parsedAmount)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD8FBA9))
                    ) {
                        Text("إرسال", color = Color.Black)
                    }
                }
            }
        }
    }
}