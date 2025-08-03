package com.smartpay.android

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
import com.smartpay.models.*
import com.smartpay.repository.FinancialReportRepository
import com.smartpay.dialogs.FinancialReportsChartsDialog
import com.smartpay.dialogs.ExportReportDialog
import kotlinx.coroutines.launch
import java.math.BigDecimal

class FinancialReportsActivity : ComponentActivity() {

    private val financialReportRepository = FinancialReportRepository()

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
        val subscriptionPlan = securePrefs.getString("subscriptionPlan", "Free") ?: "Free"

        if (token.isEmpty()) {
            Toast.makeText(this, "الجلسة غير صالحة، يرجى تسجيل الدخول", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Check subscription plan access (Pro only)
        if (!FinancialReport.hasFeatureAccess(subscriptionPlan)) {
            Toast.makeText(this, FinancialReport.getUpgradeMessage(), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContent {
            FinancialReportsScreen(
                onBack = { finish() },
                repository = financialReportRepository
            )
        }
    }
}

@Composable
fun FinancialReportsScreen(
    onBack: () -> Unit,
    repository: FinancialReportRepository
) {
    val context = LocalContext.current
    var reports by remember { mutableStateOf<List<FinancialReport>>(emptyList()) }
    var summary by remember { mutableStateOf<FinancialReportSummary?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var showChartsDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    
    // Filter states
    var selectedReportType by remember { mutableStateOf<String?>(null) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var startDate by remember { mutableStateOf<String?>(null) }
    var endDate by remember { mutableStateOf<String?>(null) }

    fun loadReports() {
        isLoading = true
        errorMessage = null
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val filters = ReportFilters(
                    reportType = selectedReportType,
                    categoryId = selectedCategoryId,
                    startDate = startDate,
                    endDate = endDate
                )
                
                val reportsResponse = repository.getReports(filters)
                val summaryResponse = repository.getSummary(startDate, endDate, selectedReportType)
                
                if (reportsResponse.isSuccessful && reportsResponse.body() != null) {
                    val body = reportsResponse.body()!!
                    if (body.success) {
                        reports = body.reports ?: emptyList()
                    } else {
                        errorMessage = body.message ?: "فشل في تحميل التقارير المالية"
                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    errorMessage = "فشل في تحميل التقارير المالية"
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                }
                
                if (summaryResponse.isSuccessful && summaryResponse.body() != null) {
                    val summaryBody = summaryResponse.body()!!
                    if (summaryBody.success) {
                        summary = summaryBody.summary
                    }
                }
                
            } catch (e: Exception) {
                errorMessage = "خطأ في الاتصال: ${e.message}"
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    fun clearFilters() {
        selectedReportType = null
        selectedCategoryId = null
        startDate = null
        endDate = null
        loadReports()
    }

    LaunchedEffect(selectedReportType, selectedCategoryId, startDate, endDate) {
        loadReports()
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
                    text = "📊 التقارير المالية",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.weight(1f))
                
                // Filter Toggle
                IconButton(onClick = { showFilters = !showFilters }) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "فلترة",
                        tint = if (selectedReportType != null || selectedCategoryId != null || startDate != null) Color(0xFF00D632) else Color(0xFF666666)
                    )
                }
                
                // Charts Button
                IconButton(onClick = { showChartsDialog = true }) {
                    Icon(
                        Icons.Default.PieChart,
                        contentDescription = "الرسوم البيانية",
                        tint = Color(0xFF00D632)
                    )
                }
                
                // Export Button
                IconButton(onClick = { showExportDialog = true }) {
                    Icon(
                        Icons.Default.FileDownload,
                        contentDescription = "تصدير",
                        tint = Color(0xFF2196F3)
                    )
                }
                
                // Refresh
                IconButton(
                    onClick = { loadReports() },
                    enabled = !isLoading
                ) {
                    Icon(
                        Icons.Default.Refresh, 
                        contentDescription = "تحديث", 
                        tint = Color(0xFF00D632)
                    )
                }
            }

            // Summary Card
            summary?.let { summaryData ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ملخص مالي",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("الدخل الإجمالي", fontSize = 12.sp, color = Color(0xFF666666))
                                Text(
                                    FinancialReport.formatAmount(summaryData.totalIncome),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00D632)
                                )
                            }
                            Column {
                                Text("المصروفات", fontSize = 12.sp, color = Color(0xFF666666))
                                Text(
                                    FinancialReport.formatAmount(summaryData.totalExpenses),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE53E3E)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("الصافي", fontSize = 12.sp, color = Color(0xFF666666))
                                Text(
                                    summaryData.getFormattedNetIncome(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(summaryData.getNetIncomeColor())
                                )
                            }
                        }
                        
                        if (summaryData.reportCount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${summaryData.reportCount} عملية مالية",
                                fontSize = 12.sp,
                                color = Color(0xFF999999)
                            )
                        }
                    }
                }
            }

            // Filters Section
            if (showFilters) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الفلاتر",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            TextButton(onClick = { clearFilters() }) {
                                Text("مسح الكل", color = Color(0xFF2196F3))
                            }
                        }
                        
                        // Report Type Filter
                        Text("نوع التقرير", fontSize = 12.sp, color = Color(0xFF666666))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            items(ReportType.getAllOptions()) { type ->
                                FilterChip(
                                    onClick = { 
                                        selectedReportType = if (selectedReportType == type.value) null else type.value
                                    },
                                    label = { Text("${type.emoji} ${type.displayName}") },
                                    selected = selectedReportType == type.value,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(type.color).copy(alpha = 0.2f)
                                    )
                                )
                            }
                        }
                    }
                }
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
                            "جاري تحميل التقارير المالية...",
                            fontSize = 16.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("❌", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "خطأ في تحميل البيانات",
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
                            onClick = { loadReports() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D632))
                        ) {
                            Text("إعادة المحاولة", color = Color.White)
                        }
                    }
                }
            } else if (reports.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📊", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "لا توجد تقارير مالية",
                            fontSize = 18.sp,
                            color = Color(0xFF666666)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "ابدأ بتسجيل العمليات المالية لتتبع أداء عملك",
                            fontSize = 14.sp,
                            color = Color(0xFF999999),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D632)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إضافة عملية مالية", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "العمليات المالية",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "${reports.size} عملية",
                                fontSize = 14.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    }

                    // Report Items
                    items(reports) { report ->
                        FinancialReportCard(report = report)
                    }
                }

                // Add Button (Floating)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        containerColor = Color(0xFF00D632),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "إضافة عملية مالية",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }

    // Add Dialog
    if (showAddDialog) {
        AddReportDialog(
            onDismiss = { showAddDialog = false },
            onSuccess = {
                showAddDialog = false
                loadReports()
            },
            repository = repository
        )
    }

    // Charts Dialog
    if (showChartsDialog) {
        FinancialReportsChartsDialog(
            onDismiss = { showChartsDialog = false },
            repository = repository,
            startDate = startDate,
            endDate = endDate,
            reportType = selectedReportType
        )
    }

    // Export Dialog
    if (showExportDialog) {
        ExportReportDialog(
            onDismiss = { showExportDialog = false },
            repository = repository,
            startDate = startDate,
            endDate = endDate,
            reportType = selectedReportType
        )
    }
}

@Composable
fun FinancialReportCard(report: FinancialReport) {
    val typeColor = Color(report.getTypeColor())
    
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type Indicator
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(typeColor.copy(alpha = 0.1f), CircleShape)
                    .border(2.dp, typeColor.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ReportType.fromValue(report.reportType)?.emoji ?: "📊",
                    fontSize = 20.sp
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Report Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = report.getReportTypeDisplay(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                if (!report.note.isNullOrEmpty()) {
                    Text(
                        text = report.note,
                        fontSize = 14.sp,
                        color = Color(0xFF666666),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!report.categoryName.isNullOrEmpty()) {
                    Text(
                        text = "التصنيف: ${report.categoryName}",
                        fontSize = 12.sp,
                        color = Color(0xFF999999)
                    )
                }
                Text(
                    text = report.getFormattedDate(),
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
            }
            
            // Amount
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = report.getFormattedAmount(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = typeColor
                )
            }
        }
    }
}