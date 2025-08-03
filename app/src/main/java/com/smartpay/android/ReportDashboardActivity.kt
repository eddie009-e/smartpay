package com.smartpay.android

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import com.smartpay.data.network.ApiService
import com.smartpay.models.*
import com.smartpay.repositories.UnifiedReportRepository
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ReportDashboardActivity : ComponentActivity() {

    private lateinit var repository: UnifiedReportRepository

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
        if (!UnifiedReportDashboard.hasFeatureAccess(subscriptionPlan)) {
            Toast.makeText(this, UnifiedReportDashboard.getUpgradeMessage(), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Initialize API service and repository
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.smartpay.sy/") // Replace with actual base URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        repository = UnifiedReportRepository(apiService)

        setContent {
            ReportDashboardScreen(
                repository = repository,
                onBack = { finish() }
            )
        }
    }
}

@Composable
fun ReportDashboardScreen(
    repository: UnifiedReportRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var dashboardData by remember { mutableStateOf<UnifiedReportDashboard?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTimePeriod by remember { mutableStateOf("monthly") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showPresetsDialog by remember { mutableStateOf(false) }
    var currentFilters by remember { mutableStateOf<ReportFilters?>(null) }

    // Load dashboard data
    fun loadDashboard() {
        isLoading = true
        errorMessage = null
        
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val result = repository.getDashboard(selectedTimePeriod)
                if (result.isSuccess) {
                    dashboardData = result.getOrNull()
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "فشل في تحميل البيانات"
                }
            } catch (e: Exception) {
                errorMessage = "خطأ في الشبكة: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Load data on first run
    LaunchedEffect(selectedTimePeriod) {
        loadDashboard()
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
                    text = "📊 التقارير الموحدة",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.weight(1f))
                
                // Presets Button
                IconButton(onClick = { showPresetsDialog = true }) {
                    Icon(
                        Icons.Default.Bookmark,
                        contentDescription = "الإعدادات المحفوظة",
                        tint = Color(0xFF9C27B0)
                    )
                }
                
                // Filter Button
                IconButton(onClick = { showFilterDialog = true }) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "فلترة",
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
                
                // Refresh Button
                IconButton(
                    onClick = { loadDashboard() },
                    enabled = !isLoading
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "تحديث",
                        tint = Color(0xFF00D632)
                    )
                }
            }

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
                                "جاري تحميل لوحة التقارير...",
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
                                onClick = { loadDashboard() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D632))
                            ) {
                                Text("إعادة المحاولة", color = Color.White)
                            }
                        }
                    }
                }
                
                dashboardData != null -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Time Period Selector
                        item {
                            TimePeriodSelector(
                                selectedPeriod = selectedTimePeriod,
                                onPeriodSelected = { selectedTimePeriod = it }
                            )
                        }
                        
                        // Summary Cards
                        item {
                            SummaryCards(dashboardData!!.summary)
                        }
                        
                        // Report Types Breakdown Chart
                        item {
                            ReportTypesChart(dashboardData!!.reportTypeBreakdown)
                        }
                        
                        // Time Series Chart
                        item {
                            TimeSeriesChart(dashboardData!!.timeSeries)
                        }
                        
                        // Category Breakdown
                        item {
                            CategoryBreakdown(dashboardData!!.categoryBreakdown)
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showFilterDialog) {
        ReportFilterDialog(
            initialFilters = currentFilters,
            onDismiss = { showFilterDialog = false },
            onApplyFilters = { filters ->
                currentFilters = filters
                showFilterDialog = false
                // TODO: Apply filters to dashboard data
                loadDashboard()
            },
            onSavePreset = { filters, name ->
                (context as ComponentActivity).lifecycleScope.launch {
                    try {
                        val request = CreateReportSettingRequest(name, filters)
                        val result = repository.saveReportSetting(request)
                        if (result.isSuccess) {
                            Toast.makeText(context, "تم حفظ الإعداد بنجاح", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "فشل في حفظ الإعداد", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showPresetsDialog) {
        ReportPresetsDialog(
            repository = repository,
            onDismiss = { showPresetsDialog = false },
            onPresetSelected = { preset ->
                currentFilters = preset.filters
                showPresetsDialog = false
                loadDashboard()
                Toast.makeText(context, "تم تطبيق الإعداد: ${preset.name}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showExportDialog) {
        ReportExportDialog(
            repository = repository,
            currentFilters = currentFilters,
            onDismiss = { showExportDialog = false },
            onExportComplete = { fileUrl ->
                showExportDialog = false
                if (fileUrl != null) {
                    Toast.makeText(context, "تم إنشاء الملف: $fileUrl", Toast.LENGTH_LONG).show()
                }
            }
        )
    }
}

@Composable
fun TimePeriodSelector(
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit
) {
    val periods = listOf(
        "daily" to "يومي",
        "weekly" to "أسبوعي", 
        "monthly" to "شهري",
        "custom" to "مخصص"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "الفترة الزمنية",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(periods) { (value, displayName) ->
                    PeriodChip(
                        text = displayName,
                        isSelected = selectedPeriod == value,
                        onClick = { onPeriodSelected(value) }
                    )
                }
            }
        }
    }
}

@Composable
fun PeriodChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) Color(0xFF00D632) else Color.White
            )
            .border(
                1.dp,
                if (isSelected) Color.Transparent else Color(0xFFE0E0E0),
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFF666666)
        )
    }
}

@Composable
fun SummaryCards(summary: ReportSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "الملخص المالي",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = "إجمالي الدخل",
                amount = UnifiedReportDashboard.formatAmount(summary.totalIncome),
                color = Color(0xFF00D632),
                icon = "💰",
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "إجمالي المصروفات",
                amount = UnifiedReportDashboard.formatAmount(summary.totalExpenses),
                color = Color(0xFFE53E3E),
                icon = "💸",
                modifier = Modifier.weight(1f)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = "صافي الدخل",
                amount = UnifiedReportDashboard.formatAmount(summary.netIncome),
                color = Color(if (summary.netIncome >= 0) 0xFF00D632 else 0xFFE53E3E),
                icon = if (summary.netIncome >= 0) "📈" else "📉",
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "عدد العمليات",
                amount = summary.transactionCount.toString(),
                color = Color(0xFF2196F3),
                icon = "📊",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    amount: String,
    color: Color,
    icon: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                icon,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                title,
                fontSize = 12.sp,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                amount,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ReportTypesChart(reportTypes: List<ReportTypeData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "توزيع أنواع التقارير",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (reportTypes.isNotEmpty()) {
                AndroidView(
                    factory = { context ->
                        PieChart(context).apply {
                            description.isEnabled = false
                            setUsePercentValues(true)
                            setDrawHoleEnabled(true)
                            setHoleColor(AndroidColor.WHITE)
                            setTransparentCircleColor(AndroidColor.WHITE)
                            setHoleRadius(58f)
                            setTransparentCircleRadius(61f)
                            setDrawCenterText(true)
                            centerText = "أنواع التقارير"
                            setRotationAngle(0f)
                            isRotationEnabled = true
                            isHighlightPerTapEnabled = true
                            legend.isEnabled = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    update = { pieChart ->
                        val entries = reportTypes.map { reportType ->
                            PieEntry(reportType.totalAmount.toFloat(), reportType.getTypeDisplayName())
                        }
                        
                        val dataSet = PieDataSet(entries, "").apply {
                            setDrawIcons(false)
                            sliceSpace = 3f
                            iconsOffset = MPPointF(0f, 40f)
                            selectionShift = 5f
                            colors = reportTypes.map { it.getTypeColor().toInt() }
                        }
                        
                        val data = PieData(dataSet).apply {
                            setValueTextSize(11f)
                            setValueTextColor(AndroidColor.WHITE)
                        }
                        
                        pieChart.data = data
                        pieChart.invalidate()
                    }
                )
                
                // Legend
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    items(reportTypes) { reportType ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        Color(reportType.getTypeColor()),
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${reportType.getTypeEmoji()} ${reportType.getTypeDisplayName()}",
                                fontSize = 10.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "لا توجد بيانات للعرض",
                        fontSize = 14.sp,
                        color = Color(0xFF999999)
                    )
                }
            }
        }
    }
}

@Composable
fun TimeSeriesChart(timeSeries: List<TimeSeriesData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "الاتجاه الزمني",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (timeSeries.isNotEmpty()) {
                AndroidView(
                    factory = { context ->
                        LineChart(context).apply {
                            description.isEnabled = false
                            setTouchEnabled(true)
                            isDragEnabled = true
                            setScaleEnabled(true)
                            setPinchZoom(true)
                            xAxis.isEnabled = true
                            axisLeft.isEnabled = true
                            axisRight.isEnabled = false
                            legend.isEnabled = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    update = { lineChart ->
                        val entries = timeSeries.mapIndexed { index, data ->
                            Entry(index.toFloat(), data.dailyTotal.toFloat())
                        }
                        
                        val dataSet = LineDataSet(entries, "").apply {
                            color = Color(0xFF00D632).toArgb()
                            setCircleColor(Color(0xFF00D632).toArgb())
                            lineWidth = 2f
                            circleRadius = 4f
                            setDrawCircleHole(false)
                            valueTextSize = 9f
                            setDrawFilled(true)
                            fillColor = Color(0xFF00D632).copy(alpha = 0.3f).toArgb()
                        }
                        
                        val data = LineData(dataSet)
                        lineChart.data = data
                        lineChart.invalidate()
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "لا توجد بيانات للعرض",
                        fontSize = 14.sp,
                        color = Color(0xFF999999)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryBreakdown(categories: List<CategoryData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "التوزيع حسب التصنيف",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (categories.isNotEmpty()) {
                categories.take(5).forEach { category ->
                    CategoryItem(category)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "لا توجد تصنيفات للعرض",
                        fontSize = 14.sp,
                        color = Color(0xFF999999)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryItem(category: CategoryData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.category,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Text(
                text = "${category.count} عملية",
                fontSize = 12.sp,
                color = Color(0xFF666666)
            )
        }
        Text(
            text = UnifiedReportDashboard.formatAmount(category.totalAmount),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00D632)
        )
    }
}