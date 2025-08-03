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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.smartpay.data.repository.MerchantRepository
import kotlinx.coroutines.launch

class BusinessDashboardActivity : ComponentActivity() {

    private val merchantRepository = MerchantRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get merchant ID from secure preferences
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val securePrefs = EncryptedSharedPreferences.create(
            "SmartPaySecurePrefs",
            masterKey,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        
        val merchantId = securePrefs.getString("user_id", null) ?: ""
        
        if (merchantId.isEmpty()) {
            Toast.makeText(this, "خطأ في تحديد هوية التاجر", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            ModernBusinessDashboard(
                merchantId = merchantId,
                merchantRepository = merchantRepository
            )
        }
    }
}

@Composable
fun ModernBusinessDashboard(
    merchantId: String,
    merchantRepository: MerchantRepository
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var balance by remember { mutableStateOf(0L) }
    var isLoading by remember { mutableStateOf(true) }

    // Load merchant balance
    LaunchedEffect(Unit) {
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val response = merchantRepository.getWallet(merchantId)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FDED))
    ) {
        // Background elements
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
                .offset(x = 50.dp, y = 350.dp)
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
            BusinessHeader(
                onSettingsClick = {
                    val intent = Intent(context, SettingsActivity::class.java)
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Business Balance Card
            BusinessBalanceCard(balance = balance, isLoading = isLoading)

            Spacer(modifier = Modifier.height(32.dp))

            // Business Actions Section
            BusinessActionsSection()

            Spacer(modifier = Modifier.height(24.dp))

            // QR Code Section
            QRCodeSection(
                onClick = {
                    val intent = Intent(context, FixedQrCodeActivity::class.java)
                    context.startActivity(intent)
                }
            )
            
            Spacer(modifier = Modifier.height(120.dp))
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = {
                val intent = Intent(context, InvoicesActivity::class.java)
                context.startActivity(intent)
            },
            containerColor = Color(0xFFD8FBA9),
            contentColor = Color.Black,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 50.dp)
                .size(64.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "إنشاء فاتورة",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun BusinessHeader(onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "مرحباً بك",
                fontSize = 16.sp,
                color = Color(0xFF6B7280)
            )
            Text(
                text = "لوحة التاجر",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D2D2D)
            )
        }
        
        Card(
            modifier = Modifier
                .size(48.dp)
                .clickable { onSettingsClick() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.8f)
            ),
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
    }
}

@Composable
fun BusinessBalanceCard(
    balance: Long,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFFD8FBA9)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "رصيد الحساب التجاري",
                    fontSize = 16.sp,
                    color = Color(0xFF6B7280),
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color(0xFFD8FBA9),
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = balance.toString(),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D2D2D)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ل.س",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFD8FBA9),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BusinessActionsSection() {
    val context = LocalContext.current
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModernBusinessActionButton(
                title = "إرسال أموال",
                icon = Icons.Default.Send,
                isPrimary = true,
                modifier = Modifier.weight(1f),
                onClick = {
                    val intent = Intent(context, MerchantSendMoneyActivity::class.java)
                    context.startActivity(intent)
                }
            )
            ModernBusinessActionButton(
                title = "طلب دفعة",
                icon = Icons.Default.CallReceived,
                isPrimary = false,
                modifier = Modifier.weight(1f),
                onClick = {
                    val intent = Intent(context, RequestPaymentActivity::class.java)
                    context.startActivity(intent)
                }
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ModernBusinessActionButton(
                title = "الفواتير",
                icon = Icons.Default.Receipt,
                isPrimary = false,
                modifier = Modifier.weight(1f),
                onClick = {
                    val intent = Intent(context, InvoicesActivity::class.java)
                    context.startActivity(intent)
                }
            )
            ModernBusinessActionButton(
                title = "الرواتب",
                icon = Icons.Default.People,
                isPrimary = false,
                modifier = Modifier.weight(1f),
                onClick = {
                    val intent = Intent(context, SalariesActivity::class.java)
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun ModernBusinessActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPrimary) {
                Color(0xFFD8FBA9)
            } else {
                Color.White.copy(alpha = 0.8f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPrimary) 16.dp else 12.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isPrimary) {
                    Color.Black
                } else {
                    Color(0xFFD8FBA9)
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPrimary) {
                    Color.Black
                } else {
                    Color(0xFF2D2D2D)
                },
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun QRCodeSection(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2D2D2D)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier.size(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFD8FBA9)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.QrCode,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color.Black
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "رمز QR الثابت",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "شارك رمزك مع العملاء لاستلام المدفوعات",
                    fontSize = 14.sp,
                    color = Color(0xFFD8FBA9)
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color(0xFFD8FBA9)
            )
        }
    }
}