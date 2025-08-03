package com.smartpay.android

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.smartpay.repository.InvoiceRepository
import com.smartpay.models.InvoiceModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.util.*

class ReceivedInvoicesActivity : ComponentActivity() {
    private val invoiceRepository = InvoiceRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get user data from secure storage
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
            val context = LocalContext.current
            var invoices by remember { mutableStateOf(listOf<InvoiceModel>()) }
            var isLoading by remember { mutableStateOf(false) }

            fun loadInvoices() {
                isLoading = true
                lifecycleScope.launch {
                    try {
                        val response = invoiceRepository.getUserInvoices()
                        if (response.isSuccessful && response.body() != null) {
                            invoices = response.body()!!
                        } else {
                            Toast.makeText(context, "فشل في جلب الفواتير", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isLoading = false
                    }
                }
            }

            LaunchedEffect(Unit) {
                loadInvoices()
            }

            Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    Text(
                        text = "الفواتير المستلمة",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF00D632))
                        }
                    } else if (invoices.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("لا توجد فواتير حتى الآن", color = Color(0xFF666666))
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(invoices) { invoice ->
                                InvoiceCard(invoice) {
                                    // Pay invoice functionality
                                    lifecycleScope.launch {
                                        try {
                                            val response = invoiceRepository.payInvoice(invoice.id)
                                            if (response.isSuccessful) {
                                                Toast.makeText(context, "تم دفع الفاتورة بنجاح", Toast.LENGTH_SHORT).show()
                                                loadInvoices() // Refresh list
                                            } else {
                                                Toast.makeText(context, "فشل في دفع الفاتورة", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun InvoiceCard(invoice: InvoiceModel, onPayClick: () -> Unit) {
        val dateFormatter = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        val createdDate = try {
            if (invoice.createdAt.contains("T")) {
                val zonedDateTime = ZonedDateTime.parse(invoice.createdAt)
                dateFormatter.format(Date.from(zonedDateTime.toInstant()))
            } else {
                invoice.createdAt
            }
        } catch (e: Exception) {
            invoice.createdAt
        }

        val paidDate = invoice.paidAt?.let { paidAt ->
            try {
                if (paidAt.contains("T")) {
                    val zonedDateTime = ZonedDateTime.parse(paidAt)
                    dateFormatter.format(Date.from(zonedDateTime.toInstant()))
                } else {
                    paidAt
                }
            } catch (e: Exception) {
                paidAt
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "من التاجر: ${invoice.businessId?.take(8) ?: "غير معروف"}...",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "الوصف: ${invoice.description}",
                            color = Color.DarkGray,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = "تاريخ الإنشاء: $createdDate",
                            fontSize = 12.sp,
                            color = Color(0xFF666666),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        if (paidDate != null) {
                            Text(
                                text = "تاريخ الدفع: $paidDate",
                                fontSize = 12.sp,
                                color = Color(0xFF666666),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${invoice.amount} ل.س",
                            color = Color(0xFF00D632),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        
                        Text(
                            text = when (invoice.status) {
                                "paid" -> "مدفوعة"
                                "pending" -> "معلقة"
                                "cancelled" -> "ملغية"
                                else -> invoice.status
                            },
                            fontSize = 12.sp,
                            color = when (invoice.status) {
                                "paid" -> Color(0xFF00A000)
                                "pending" -> Color(0xFFFF9800)
                                "cancelled" -> Color.Red
                                else -> Color.Gray
                            },
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        
                        if (invoice.status == "pending") {
                            Button(
                                onClick = onPayClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D632)),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text("دفع", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
