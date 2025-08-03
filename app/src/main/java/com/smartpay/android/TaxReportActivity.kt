package com.smartpay.android

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.smartpay.models.*
import com.smartpay.repositories.MerchantTaxRepository
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class TaxReportActivity : ComponentActivity() {

    private lateinit var repository: MerchantTaxRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get token and subscription plan from secure storage
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val securePrefs = EncryptedSharedPreferences.create(
            "SmartPaySecurePrefs",
            masterKey,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val token = securePrefs.getString("token", null) ?: ""
        val subscriptionPlan = securePrefs.getString("subscriptionPlan", "Free") ?: "Free"

        if (token.isEmpty()) {
            Toast.makeText(this, "الجلسة غير صالحة، يرجى تسجيل الدخول", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Check subscription plan access (Pro only)
        if (!MerchantTax.hasFeatureAccess(subscriptionPlan)) {
            Toast.makeText(this, MerchantTax.getUpgradeMessage(), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Initialize API service and repository
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.smartpay.sy/") // Replace with actual base URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        repository = MerchantTaxRepository(apiService)

        setContent {
            TaxReportScreen(
                repository = repository,
                onBack = { finish() },
                onNavigateToTaxManagement = {
                    startActivity(Intent(this, MerchantTaxesActivity::class.java))
                }
            )
        }
    }
}

@Composable
fun TaxReportScreen(
    repository: MerchantTaxRepository,
    onBack: () -> Unit,
    onNavigateToTaxManagement: () -> Unit
) {
    val context = LocalContext.current
    var report by remember { mutableStateOf<TaxReport?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var startDate by remember { mutableStateOf<String?>(null) }
    var endDate by remember { mutableStateOf<String?>(null) }

    // Date formatters
    val displayFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
    val apiFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // Generate report
    fun generateReport() {
        isLoading = true
        errorMessage = null
        
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val result = repository.generateTaxReport(startDate, endDate)
                if (result.isSuccess) {
                    report = result.getOrNull()
                } else {
                    errorMessage = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                errorMessage = "خطأ في الشبكة: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Date picker functions
    fun showStartDatePicker() {
        val calendar = Calendar.getInstance()
        val picker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                startDate = selectedDate.format(apiFormatter)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        picker.show()
    }

    fun showEndDatePicker() {
        val calendar = Calendar.getInstance()
        val picker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                endDate = selectedDate.format(apiFormatter)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        picker.show()
    }

    fun clearDates() {
        startDate = null
        endDate = null
    }

    // Load initial report on first run
    LaunchedEffect(Unit) {
        generateReport()
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
                    text = "📊 التقرير الضريبي",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.weight(1f))
                
                // Tax Management Button
                IconButton(
                    onClick = onNavigateToTaxManagement
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "إدارة الضرائب",
                        tint = Color(0xFF00D632)
                    )
                }
            }

            // Date Range Picker Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "الفترة الزمنية",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Start Date
                        OutlinedButton(
                            onClick = { showStartDatePicker() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF00D632)
                            )
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = startDate?.let { 
                                    LocalDate.parse(it).format(displayFormatter) 
                                } ?: "من تاريخ",
                                fontSize = 12.sp
                            )
                        }
                        
                        // End Date
                        OutlinedButton(
                            onClick = { showEndDatePicker() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF00D632)
                            )
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = endDate?.let { 
                                    LocalDate.parse(it).format(displayFormatter) 
                                } ?: "إلى تاريخ",
                                fontSize = 12.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Generate Report Button
                        Button(
                            onClick = { generateReport() },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00D632)
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    Icons.Default.Assessment,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إنشاء التقرير", color = Color.White, fontSize = 12.sp)
                        }
                        
                        // Clear Dates Button
                        OutlinedButton(
                            onClick = { clearDates() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مسح التواريخ", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF00D632))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "جاري إنشاء التقرير الضريبي...",
                                fontSize = 16.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                }
                
                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("❌", fontSize = 64.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "خطأ في إنشاء التقرير",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE53E3E)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                errorMessage!!,
                                fontSize = 14.sp,
                                color = Color(0xFF666666),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { generateReport() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D632))
                            ) {
                                Text("إعادة المحاولة", color = Color.White)
                            }
                        }
                    }
                }
                
                report == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📊", fontSize = 64.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "لا يوجد تقرير",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF666666)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "اضغط على 'إنشاء التقرير' لإنشاء تقرير ضريبي",
                                fontSize = 14.sp,
                                color = Color(0xFF999999),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Report Period
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = Color(0xFF1976D2),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "فترة التقرير: ${report!!.period.getFormattedPeriod()}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1976D2)
                                    )
                                }
                            }
                        }

                        // Summary Cards
                        item {
                            TaxReportSummaryCards(summary = report!!.summary)
                        }

                        // Tax Breakdown Header
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "تفصيل الضرائب",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = "${report!!.taxBreakdown.size} نوع ضريبة",
                                    fontSize = 14.sp,
                                    color = Color(0xFF666666)
                                )
                            }
                        }

                        // Tax Breakdown Items
                        if (report!!.taxBreakdown.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("📊", fontSize = 48.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "لا توجد ضرائب في هذه الفترة",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF666666)
                                        )
                                        Text(
                                            "لم يتم تطبيق أي ضرائب على الفواتير في الفترة المحددة",
                                            fontSize = 12.sp,
                                            color = Color(0xFF999999),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(report!!.taxBreakdown) { breakdown ->
                                TaxBreakdownCard(
                                    breakdown = breakdown,
                                    totalTaxCollected = report!!.summary.totalTaxCollected
                                )
                            }
                        }

                        // Compliance Information
                        item {
                            TaxComplianceCard(summary = report!!.summary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaxReportSummaryCards(summary: TaxReportSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ملخص التقرير",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // First Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TaxReportStatCard(
                    title = "إجمالي الضرائب",
                    value = summary.getFormattedTotalTaxCollected(),
                    subtitle = "مجموع محصل",
                    color = Color(0xFF00D632),
                    icon = "💰"
                )
                TaxReportStatCard(
                    title = "فواتير بضريبة",
                    value = summary.totalInvoicesWithTax.toString(),
                    subtitle = "من ${summary.getTotalInvoices()}",
                    color = Color(0xFF2196F3),
                    icon = "🧾"
                )
                TaxReportStatCard(
                    title = "متوسط الضريبة",
                    value = summary.getFormattedAverageTaxRate(),
                    subtitle = "معدل عام",
                    color = Color(0xFFFF9800),
                    icon = "📊"
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Second Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TaxReportStatCard(
                    title = "القيمة الخاضعة",
                    value = summary.getFormattedTotalTaxableAmount(),
                    subtitle = "قبل الضريبة",
                    color = Color(0xFF9C27B0),
                    icon = "💳"
                )
                TaxReportStatCard(
                    title = "أنواع الضرائب",
                    value = summary.uniqueTaxesUsed.toString(),
                    subtitle = "نوع مختلف",
                    color = Color(0xFF607D8B),
                    icon = "🏷️"
                )
                TaxReportStatCard(
                    title = "نسبة الامتثال",
                    value = summary.getFormattedTaxCompliancePercentage(),
                    subtitle = "من الفواتير",
                    color = if (summary.getTaxCompliancePercentage() >= 80) Color(0xFF00D632) else Color(0xFFE53E3E),
                    icon = "✅"
                )
            }
        }
    }
}

@Composable
fun TaxReportStatCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    icon: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp)
    ) {
        Text(
            text = icon,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = title,
            fontSize = 9.sp,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            fontSize = 8.sp,
            color = Color(0xFF999999),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TaxBreakdownCard(
    breakdown: TaxBreakdownItem,
    totalTaxCollected: Double
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Tax Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Color(TaxCategory.fromRate(breakdown.taxRate).color).copy(alpha = 0.1f), 
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🧾",
                            fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = breakdown.taxName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = breakdown.getFormattedTaxRate(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(TaxCategory.fromRate(breakdown.taxRate).color)
                        )
                    }
                }
                
                // Category Badge
                Box(
                    modifier = Modifier
                        .background(
                            Color(TaxCategory.fromRate(breakdown.taxRate).color).copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = TaxCategory.fromRate(breakdown.taxRate).displayName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(TaxCategory.fromRate(breakdown.taxRate).color)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Statistics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "الضرائب المحصلة",
                        fontSize = 10.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = breakdown.getFormattedTotalTaxCollected(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00D632)
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "عدد الفواتير",
                        fontSize = 10.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = breakdown.invoiceCount.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "نسبة المساهمة",
                        fontSize = 10.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = breakdown.getFormattedContributionPercentage(totalTaxCollected),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress Bar for Contribution
            if (totalTaxCollected > 0) {
                val contribution = breakdown.getContributionPercentage(totalTaxCollected) / 100f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color(0xFFE0E0E0), RoundedCornerShape(2.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(contribution)
                            .height(4.dp)
                            .background(Color(0xFF00D632), RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun TaxComplianceCard(summary: TaxReportSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (summary.getTaxCompliancePercentage() >= 80) 
                Color(0xFFE8F5E8) else Color(0xFFFFEBEE)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (summary.getTaxCompliancePercentage() >= 80) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (summary.getTaxCompliancePercentage() >= 80) Color(0xFF00D632) else Color(0xFFE53E3E),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "معلومات الامتثال الضريبي",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = when {
                    summary.getTaxCompliancePercentage() >= 90 -> "ممتاز: ${summary.getFormattedTaxCompliancePercentage()} من الفواتير تحتوي على ضرائب"
                    summary.getTaxCompliancePercentage() >= 70 -> "جيد: ${summary.getFormattedTaxCompliancePercentage()} من الفواتير تحتوي على ضرائب"
                    summary.getTaxCompliancePercentage() >= 50 -> "متوسط: ${summary.getFormattedTaxCompliancePercentage()} من الفواتير تحتوي على ضرائب"
                    else -> "ضعيف: ${summary.getFormattedTaxCompliancePercentage()} فقط من الفواتير تحتوي على ضرائب"
                },
                fontSize = 12.sp,
                color = if (summary.getTaxCompliancePercentage() >= 80) Color(0xFF00D632) else Color(0xFFE53E3E)
            )
            
            if (summary.invoicesWithoutTax > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "يوجد ${summary.invoicesWithoutTax} فاتورة بدون ضرائب في هذه الفترة",
                    fontSize = 11.sp,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}