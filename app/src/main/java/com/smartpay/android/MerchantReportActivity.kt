package com.smartpay.android

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.FileDownload
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
import com.smartpay.repository.MerchantReportRepository
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

class MerchantReportActivity : ComponentActivity() {

    private val merchantReportRepository = MerchantReportRepository()

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
            MerchantReportScreen(
                onBack = { finish() },
                repository = merchantReportRepository
            )
        }
    }
}

@Composable
fun MerchantReportScreen(
    onBack: () -> Unit,
    repository: MerchantReportRepository
) {
    val context = LocalContext.current
    var report by remember { mutableStateOf<MerchantReport?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    fun loadReport() {
        isLoading = true
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val response = repository.getMerchantReport()
                if (response.isSuccessful && response.body() != null) {
                    report = response.body()!!
                } else {
                    Toast.makeText(context, "فشل في تحميل التقرير", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    fun exportToPdf() {
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val response = repository.exportReportToPdf()
                if (response.isSuccessful) {
                    Toast.makeText(context, "📄 تم تصدير التقرير بصيغة PDF", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "فشل في تصدير PDF", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun exportToExcel() {
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val response = repository.exportReportToExcel()
                if (response.isSuccessful) {
                    Toast.makeText(context, "📊 تم تصدير التقرير بصيغة Excel", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "فشل في تصدير Excel", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        loadReport()
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
                    text = "📊 تقرير التاجر",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.Assessment, contentDescription = null, tint = Color(0xFF00D632))
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
                            "جاري تحميل التقرير...",
                            fontSize = 16.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
            } else if (report != null) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Main Summary Cards
                    item {
                        MerchantSummarySection(report!!)
                    }
                    
                    // Invoice Summary Section
                    item {
                        InvoiceSummarySection(report!!.invoiceSummary)
                    }
                }

                // Export Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { exportToPdf() },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53E3E)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("📄 تصدير PDF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Button(
                        onClick = { exportToExcel() },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38A169)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("📊 تصدير Excel", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📊", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "لا توجد بيانات للتقرير حتى الآن",
                            fontSize = 18.sp,
                            color = Color(0xFF666666)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { loadReport() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D632))
                        ) {
                            Text("إعادة تحميل", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MerchantSummarySection(report: MerchantReport) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "📊 ملخص التقرير المالي",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Row 1: إجمالي الإرسال والاستلام
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = "💸 إجمالي الإرسال",
                amount = report.sentTotal,
                color = Color(0xFFE53E3E),
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "💰 إجمالي الاستلام",
                amount = report.receivedTotal,
                color = Color(0xFF00D632),
                modifier = Modifier.weight(1f)
            )
        }
        
        // Row 2: الرواتب والمصاريف
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = "👥 إجمالي الرواتب",
                amount = report.salariesTotal,
                color = Color(0xFF3182CE),
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "📉 إجمالي المصاريف",
                amount = report.expensesTotal,
                color = Color(0xFFFF9800),
                modifier = Modifier.weight(1f)
            )
        }
        
        // Pending Requests Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⏳",
                    fontSize = 32.sp,
                    modifier = Modifier.padding(end = 16.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "الطلبات المعلقة",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = "طلبات في انتظار الموافقة",
                        fontSize = 12.sp,
                        color = Color(0xFF999999)
                    )
                }
                Text(
                    text = "${report.pendingRequests}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    amount: BigDecimal,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = formatAmount(amount),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun InvoiceSummarySection(invoiceSummary: InvoiceSummary) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = "🧾",
                    fontSize = 24.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Text(
                    text = "ملخص الفواتير",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InvoiceStatItem("📋 إجمالي الفواتير", "${invoiceSummary.total}")
                InvoiceStatItem("✅ مدفوعة", "${invoiceSummary.paid}")
                InvoiceStatItem("⏰ معلقة", "${invoiceSummary.unpaid}")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "إجمالي قيمة الفواتير",
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = formatAmount(invoiceSummary.totalAmount),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00D632)
                    )
                }
            }
        }
    }
}

@Composable
fun InvoiceStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00D632)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center
        )
    }
}

fun formatAmount(amount: BigDecimal): String {
    val formatter = NumberFormat.getNumberInstance(Locale("ar"))
    return "${formatter.format(amount)} ل.س"
}