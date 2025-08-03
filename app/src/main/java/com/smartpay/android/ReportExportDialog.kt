package com.smartpay.android

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import com.smartpay.models.*
import com.smartpay.repositories.UnifiedReportRepository
import kotlinx.coroutines.launch

/**
 * Report Export Dialog - Pro Plan Only
 * 
 * Allows Pro Plan merchants to export unified business reports with:
 * - PDF format with Arabic support and professional layout
 * - Excel format with data sheets and charts
 * - Optional snapshot saving with custom notes
 * - Export progress tracking
 * - Download link generation
 */
@Composable
fun ReportExportDialog(
    repository: UnifiedReportRepository,
    currentFilters: ReportFilters?,
    onDismiss: () -> Unit,
    onExportComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf<ExportFormat?>(null) }
    var snapshotNotes by remember { mutableStateOf("") }
    var saveSnapshot by remember { mutableStateOf(true) }
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableStateOf("") }

    // Export function
    fun startExport() {
        if (selectedFormat == null || currentFilters == null) return
        
        isExporting = true
        exportProgress = "جاري تحضير التقرير..."
        
        (context as androidx.activity.ComponentActivity).lifecycleScope.launch {
            try {
                exportProgress = "جاري إنشاء التقرير..."
                
                val generateRequest = GenerateReportRequest(
                    reportTypes = currentFilters.reportTypes ?: emptyList(),
                    categories = currentFilters.categories,
                    timePeriod = currentFilters.timePeriod,
                    startDate = currentFilters.startDate,
                    endDate = currentFilters.endDate,
                    minAmount = currentFilters.minAmount,
                    maxAmount = currentFilters.maxAmount,
                    notesFilter = currentFilters.notesFilter,
                    exportFormat = selectedFormat!!.value,
                    snapshotNotes = if (saveSnapshot && snapshotNotes.isNotBlank()) snapshotNotes else null
                )
                
                exportProgress = "جاري تصدير الملف..."
                
                val result = repository.generateReport(generateRequest)
                if (result.isSuccess) {
                    val response = result.getOrNull()
                    if (response?.success == true) {
                        exportProgress = "تم التصدير بنجاح!"
                        
                        Toast.makeText(
                            context,
                            "تم تصدير التقرير بنجاح",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        onExportComplete(response.generatedFileUrl)
                    } else {
                        throw Exception(response?.message ?: "فشل في تصدير التقرير")
                    }
                } else {
                    throw Exception(result.exceptionOrNull()?.message ?: "فشل في تصدير التقرير")
                }
            } catch (e: Exception) {
                exportProgress = "خطأ في التصدير: ${e.message}"
                Toast.makeText(
                    context,
                    "فشل في تصدير التقرير: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                // Keep dialog open for a moment to show success/error message
                kotlinx.coroutines.delay(2000)
                isExporting = false
                if (exportProgress.contains("نجاح")) {
                    onDismiss()
                }
            }
        }
    }

    Dialog(onDismissRequest = { if (!isExporting) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "📤 تصدير التقرير",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    if (!isExporting) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = Color(0xFF666666)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))

                if (isExporting) {
                    // Export Progress
                    ExportProgressView(
                        progress = exportProgress,
                        format = selectedFormat
                    )
                } else {
                    // Export Configuration
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        // Format Selection
                        ExportFormatSelection(
                            selectedFormat = selectedFormat,
                            onFormatSelected = { selectedFormat = it }
                        )

                        // Snapshot Options
                        SnapshotOptions(
                            saveSnapshot = saveSnapshot,
                            snapshotNotes = snapshotNotes,
                            onSaveSnapshotChanged = { saveSnapshot = it },
                            onNotesChanged = { snapshotNotes = it }
                        )

                        // Filter Summary
                        currentFilters?.let { filters ->
                            FilterSummaryCard(filters = filters)
                        }

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("إلغاء", color = Color(0xFF666666))
                            }

                            Button(
                                onClick = { startExport() },
                                enabled = selectedFormat != null,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00D632),
                                    disabledContainerColor = Color(0xFFE0E0E0)
                                )
                            ) {
                                Icon(
                                    Icons.Default.FileDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedFormat != null) Color.White else Color(0xFF999999)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "تصدير",
                                    color = if (selectedFormat != null) Color.White else Color(0xFF999999),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExportFormatSelection(
    selectedFormat: ExportFormat?,
    onFormatSelected: (ExportFormat) -> Unit
) {
    Column {
        Text(
            text = "اختر صيغة التصدير",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExportFormat.getAllOptions().forEach { format ->
                FormatCard(
                    format = format,
                    isSelected = selectedFormat == format,
                    onSelect = { onFormatSelected(format) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun FormatCard(
    format: ExportFormat,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val description = when (format) {
        ExportFormat.PDF -> "ملف PDF مع تخطيط احترافي وإحصائيات مرئية"
        ExportFormat.EXCEL -> "جدول Excel مع بيانات تفصيلية وإمكانية التحليل"
    }
    
    val backgroundColor = if (isSelected) Color(0xFF00D632).copy(alpha = 0.1f) else Color(0xFFF7F8FA)
    val borderColor = if (isSelected) Color(0xFF00D632) else Color(0xFFE0E0E0)
    val textColor = if (isSelected) Color(0xFF00D632) else Color(0xFF666666)

    Card(
        modifier = modifier
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = format.emoji,
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = format.displayName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.Black else Color(0xFF666666)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = textColor,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun SnapshotOptions(
    saveSnapshot: Boolean,
    snapshotNotes: String,
    onSaveSnapshotChanged: (Boolean) -> Unit,
    onNotesChanged: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = saveSnapshot,
                onCheckedChange = onSaveSnapshotChanged,
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF00D632),
                    uncheckedColor = Color(0xFF666666)
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "حفظ لقطة من التقرير",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Text(
                    text = "سيتم حفظ التقرير في سجل اللقطات للمراجعة لاحقاً",
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
            }
        }
        
        if (saveSnapshot) {
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "ملاحظات اللقطة (اختياري)",
                fontSize = 12.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color(0xFFF7F8FA), shape = RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.TopStart
            ) {
                BasicTextField(
                    value = snapshotNotes,
                    onValueChange = onNotesChanged,
                    modifier = Modifier.fillMaxSize(),
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = Color.Black,
                        lineHeight = 18.sp
                    ),
                    cursorBrush = SolidColor(Color(0xFF00D632)),
                    decorationBox = { innerTextField ->
                        if (snapshotNotes.isEmpty()) {
                            Text(
                                "أدخل ملاحظات حول هذا التقرير (مثل: تقرير المبيعات للربع الأول)",
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    color = Color(0xFF999999),
                                    lineHeight = 18.sp
                                )
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }
    }
}

@Composable
fun FilterSummaryCard(filters: ReportFilters) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "ملخص الفلاتر المطبقة",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF666666)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = filters.getDisplaySummary(),
                fontSize = 11.sp,
                color = Color(0xFF666666),
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun ExportProgressView(
    progress: String,
    format: ExportFormat?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Format Icon
        format?.let {
            Text(
                text = it.emoji,
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Progress Indicator
        if (progress.contains("خطأ")) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = Color(0xFFE53E3E),
                modifier = Modifier.size(32.dp)
            )
        } else if (progress.contains("نجاح")) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF00D632),
                modifier = Modifier.size(32.dp)
            )
        } else {
            CircularProgressIndicator(
                color = Color(0xFF00D632),
                modifier = Modifier.size(32.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress Text
        Text(
            text = progress,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = when {
                progress.contains("خطأ") -> Color(0xFFE53E3E)
                progress.contains("نجاح") -> Color(0xFF00D632)
                else -> Color(0xFF666666)
            },
            textAlign = TextAlign.Center
        )
        
        if (!progress.contains("خطأ") && !progress.contains("نجاح")) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "يرجى الانتظار...",
                fontSize = 12.sp,
                color = Color(0xFF999999)
            )
        }
    }
}