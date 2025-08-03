package com.smartpay.dialogs

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import com.smartpay.repository.FinancialReportRepository
import kotlinx.coroutines.launch

@Composable
fun ExportReportDialog(
    onDismiss: () -> Unit,
    repository: FinancialReportRepository,
    startDate: String? = null,
    endDate: String? = null,
    reportType: String? = null
) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf<String?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    fun exportReport(format: String) {
        selectedFormat = format
        isExporting = true
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val response = repository.exportReports(
                    format = format,
                    startDate = startDate,
                    endDate = endDate,
                    reportType = reportType
                )

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.success) {
                        showSuccess = true
                        Toast.makeText(
                            context, 
                            "تم تصدير التقرير بنجاح بصيغة ${if (format == "pdf") "PDF" else "Excel"}", 
                            Toast.LENGTH_LONG
                        ).show()
                        
                        // Auto-dismiss after success
                        kotlinx.coroutines.delay(2000)
                        onDismiss()
                    } else {
                        Toast.makeText(
                            context, 
                            body.message ?: "فشل في تصدير التقرير", 
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(context, "فشل في تصدير التقرير", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ في التصدير: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isExporting = false
                selectedFormat = null
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تصدير التقرير",
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

                if (showSuccess) {
                    // Success State
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✅", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "تم التصدير بنجاح!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00D632)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "سيتم إغلاق النافذة تلقائياً...",
                            fontSize = 14.sp,
                            color = Color(0xFF666666)
                        )
                    }
                } else if (isExporting) {
                    // Loading State
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = Color(0xFF00D632),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "جاري تصدير التقرير...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "بصيغة ${if (selectedFormat == "pdf") "PDF" else "Excel"}",
                            fontSize = 14.sp,
                            color = Color(0xFF666666)
                        )
                    }
                } else {
                    // Selection State
                    Column {
                        Text(
                            "اختر صيغة التصدير:",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Export Options
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // PDF Option
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { exportReport("pdf") },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(Color(0xFFFF5722).copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.PictureAsPdf,
                                            contentDescription = null,
                                            tint = Color(0xFFFF5722),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "ملف PDF",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                        Text(
                                            "مناسب للعرض والطباعة",
                                            fontSize = 14.sp,
                                            color = Color(0xFF666666)
                                        )
                                    }
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = Color(0xFF999999)
                                    )
                                }
                            }

                            // Excel Option
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { exportReport("excel") },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(Color(0xFF4CAF50).copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.TableChart,
                                            contentDescription = null,
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "ملف Excel",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                        Text(
                                            "مناسب للتحليل والتعديل",
                                            fontSize = 14.sp,
                                            color = Color(0xFF666666)
                                        )
                                    }
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = Color(0xFF999999)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Info
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF2196F3),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "سيتم تصدير البيانات حسب الفلاتر المطبقة حالياً",
                                    fontSize = 12.sp,
                                    color = Color(0xFF666666)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Cancel Button
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إلغاء")
                        }
                    }
                }
            }
        }
    }
}