package com.smartpay.dialogs

import android.graphics.Color as AndroidColor
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.smartpay.models.*
import com.smartpay.repository.FinancialReportRepository
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@Composable
fun FinancialReportsChartsDialog(
    onDismiss: () -> Unit,
    repository: FinancialReportRepository,
    startDate: String? = null,
    endDate: String? = null,
    reportType: String? = null
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var chartData by remember { mutableStateOf<List<FinancialReportGraphData>>(emptyList()) }
    var selectedGroupBy by remember { mutableStateOf("type") }
    var showExportDialog by remember { mutableStateOf(false) }

    fun loadChartData() {
        isLoading = true
        errorMessage = null
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val response = repository.getGraphData(
                    groupBy = selectedGroupBy,
                    startDate = startDate,
                    endDate = endDate,
                    reportType = reportType
                )

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.success) {
                        chartData = body.graphData ?: emptyList()
                    } else {
                        errorMessage = body.message ?: "فشل في تحميل بيانات الرسوم البيانية"
                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    errorMessage = "فشل في تحميل بيانات الرسوم البيانية"
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                errorMessage = "خطأ في الاتصال: ${e.message}"
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }


    LaunchedEffect(selectedGroupBy) {
        loadChartData()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📊 الرسوم البيانية",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    
                    Row {
                        // Export Button
                        IconButton(onClick = { showExportDialog = true }) {
                            Icon(
                                Icons.Default.FileDownload,
                                contentDescription = "تصدير",
                                tint = Color(0xFF00D632)
                            )
                        }
                        
                        // Close Button
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = Color(0xFF666666)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Group By Options
                Text(
                    text = "تجميع البيانات حسب:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF666666)
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    items(listOf(
                        Pair("type", "النوع"),
                        Pair("category", "التصنيف"),
                        Pair("date", "التاريخ")
                    )) { (value, label) ->
                        FilterChip(
                            onClick = { selectedGroupBy = value },
                            label = { Text(label) },
                            selected = selectedGroupBy == value,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00D632).copy(alpha = 0.2f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Chart Content
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
                                    "جاري تحميل البيانات...",
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
                                Text("❌", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "خطأ في تحميل البيانات",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE53E3E)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    errorMessage!!,
                                    fontSize = 14.sp,
                                    color = Color(0xFF666666),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { loadChartData() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D632))
                                ) {
                                    Text("إعادة المحاولة", color = Color.White)
                                }
                            }
                        }
                    }
                    
                    chartData.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📊", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "لا توجد بيانات للعرض",
                                    fontSize = 16.sp,
                                    color = Color(0xFF666666)
                                )
                                Text(
                                    "أضف بعض العمليات المالية لرؤية الرسوم البيانية",
                                    fontSize = 14.sp,
                                    color = Color(0xFF999999),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    
                    else -> {
                        // Chart and Legend
                        Column {
                            // Pie Chart
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AndroidView(
                                    factory = { context ->
                                        PieChart(context).apply {
                                            description.isEnabled = false
                                            isRotationEnabled = true
                                            isDragDecelerationEnabled = true
                                            setUsePercentValues(false)
                                            setDrawHoleEnabled(true)
                                            setHoleColor(AndroidColor.WHITE)
                                            holeRadius = 35f
                                            transparentCircleRadius = 40f
                                            setDrawCenterText(true)
                                            centerText = "الإجمالي"
                                            setCenterTextSize(14f)
                                            setCenterTextColor(AndroidColor.BLACK)
                                            legend.isEnabled = false
                                        }
                                    },
                                    update = { pieChart ->
                                        if (chartData.isNotEmpty()) {
                                            val entries = chartData.map { data ->
                                                PieEntry(data.value.toFloat(), data.label)
                                            }
                                            
                                            val dataSet = PieDataSet(entries, "").apply {
                                                colors = chartData.map { AndroidColor.parseColor(it.color) }
                                                sliceSpace = 2f
                                                selectionShift = 8f
                                                setDrawValues(true)
                                                valueTextSize = 12f
                                                valueTextColor = AndroidColor.BLACK
                                                valueFormatter = object : ValueFormatter() {
                                                    private val formatter = DecimalFormat("#,###")
                                                    override fun getFormattedValue(value: Float): String {
                                                        return "${formatter.format(value.toInt())} ل.س"
                                                    }
                                                }
                                            }
                                            
                                            val pieData = PieData(dataSet)
                                            pieChart.data = pieData
                                            pieChart.invalidate()
                                        }
                                    }
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Legend
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(chartData) { data ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .background(
                                                Color(0xFFF7F8FA),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(
                                                    Color(android.graphics.Color.parseColor(data.color)),
                                                    RoundedCornerShape(2.dp)
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                data.label,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.Black
                                            )
                                            Text(
                                                FinancialReport.formatAmount(data.value),
                                                fontSize = 11.sp,
                                                color = Color(0xFF666666)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Statistics
                            if (chartData.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            "إحصائيات سريعة",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        val totalAmount = chartData.sumOf { it.value }
                                        val totalCount = chartData.sumOf { it.count }
                                        val averageAmount = if (totalCount > 0) totalAmount / totalCount else 0.0
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    "إجمالي المبلغ",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF666666)
                                                )
                                                Text(
                                                    FinancialReport.formatAmount(totalAmount),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF00D632)
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    "عدد العمليات",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF666666)
                                                )
                                                Text(
                                                    totalCount.toString(),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF2196F3)
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    "متوسط المبلغ",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF666666)
                                                )
                                                Text(
                                                    FinancialReport.formatAmount(averageAmount),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF9C27B0)
                                                )
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
        
        // Export Dialog
        if (showExportDialog) {
            ExportReportDialog(
                onDismiss = { showExportDialog = false },
                repository = repository,
                startDate = startDate,
                endDate = endDate,
                reportType = reportType
            )
        }
    }
}