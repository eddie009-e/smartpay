package com.smartpay.android

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Refresh
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
import com.smartpay.models.Transaction
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

class TransactionHistoryActivity : ComponentActivity() {
    private val userRepository = UserRepository()

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
            ModernTransactionHistoryScreen()
        }
    }

    @Composable
    fun ModernTransactionHistoryScreen() {
        val context = LocalContext.current
        var transactions by remember { mutableStateOf(listOf<Transaction>()) }
        var isLoading by remember { mutableStateOf(false) }

        fun loadTransactions() {
            isLoading = true
            lifecycleScope.launch {
                try {
                    val response = userRepository.getTransactionHistory()
                    if (response.isSuccessful && response.body() != null) {
                        transactions = response.body()!!
                    } else {
                        Toast.makeText(context, "فشل في جلب تاريخ المعاملات", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isLoading = false
                }
            }
        }

        LaunchedEffect(Unit) {
            loadTransactions()
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

            Column(modifier = Modifier.fillMaxSize()) {
                // Modern Header
                ModernTransactionHeader(
                    onBackClick = { finish() },
                    onRefreshClick = { loadTransactions() },
                    isLoading = isLoading
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFD8FBA9),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "جاري تحميل المعاملات...",
                                fontSize = 16.sp,
                                color = Color(0xFF6B7280),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else if (transactions.isEmpty()) {
                    ModernEmptyState()
                } else {
                    ModernTransactionsList(transactions = transactions)
                }
            }
        }
    }

    @Composable
    fun ModernTransactionHeader(
        onBackClick: () -> Unit,
        onRefreshClick: () -> Unit,
        isLoading: Boolean
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .padding(top = 40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FDED)),
                    onClick = onBackClick
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color(0xFF2D2D2D),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Card(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
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
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "تاريخ المعاملات",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D2D2D)
                    )
                }

                Card(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FDED)),
                    onClick = onRefreshClick
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color(0xFFD8FBA9),
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "تحديث",
                                tint = Color(0xFFD8FBA9),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ModernEmptyState() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(40.dp),
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
                            Text("📊", fontSize = 40.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "لا توجد معاملات حتى الآن",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D2D2D)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ستظهر جميع معاملاتك هنا",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }
        }
    }

    @Composable
    fun ModernTransactionsList(transactions: List<Transaction>) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "المعاملات الحديثة",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D2D2D)
                    )
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFD8FBA9).copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "${transactions.size} معاملة",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2D2D2D),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            items(transactions) { transaction ->
                ModernTransactionCard(transaction)
            }
        }
    }

    @Composable
    fun ModernTransactionCard(transaction: Transaction) {
        val dateFormatter = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        val transactionDate = try {
            val instant = Instant.ofEpochMilli(transaction.timestamp)
            dateFormatter.format(Date.from(instant))
        } catch (e: Exception) {
            transaction.timestamp.toString()
        }

        val (typeIcon, typeColor) = when (transaction.type) {
            "transfer" -> "💸" to Color(0xFFD8FBA9)
            "payment" -> "💳" to Color(0xFF2D2D2D)
            "salary" -> "💼" to Color(0xFF6B7280)
            "invoice" -> "🧾" to Color(0xFFD8FBA9)
            else -> "💰" to Color(0xFF6B7280)
        }

        val statusColor = when (transaction.status) {
            "completed" -> Color(0xFFD8FBA9)
            "pending" -> Color(0xFF6B7280)
            "failed" -> Color(0xFFEF4444)
            else -> Color(0xFF6B7280)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Icon
                Card(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = typeColor.copy(alpha = 0.1f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(typeIcon, fontSize = 20.sp)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Transaction Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (transaction.type) {
                            "transfer" -> "تحويل أموال"
                            "payment" -> "دفعة"
                            "salary" -> "راتب"
                            "invoice" -> "فاتورة"
                            else -> transaction.type
                        },
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D2D2D),
                        fontSize = 16.sp
                    )
                    
                    Text(
                        text = transaction.description,
                        color = Color(0xFF6B7280),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    Text(
                        text = transactionDate,
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    // From/To info
                    if (transaction.fromUser != null) {
                        Text(
                            text = "من: ${transaction.fromUser}",
                            fontSize = 12.sp,
                            color = Color(0xFF9CA3AF),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    
                    if (transaction.toUser != null) {
                        Text(
                            text = "إلى: ${transaction.toUser}",
                            fontSize = 12.sp,
                            color = Color(0xFF9CA3AF),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Amount and Status
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${transaction.amount} ل.س",
                        color = Color(0xFF2D2D2D),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.1f))
                    ) {
                        Text(
                            text = when (transaction.status) {
                                "completed" -> "مكتملة"
                                "pending" -> "معلقة"
                                "failed" -> "فشلت"
                                else -> transaction.status
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}