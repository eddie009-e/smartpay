package com.smartpay.android

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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import com.smartpay.models.UnifiedReportSetting
import com.smartpay.repositories.UnifiedReportRepository
import kotlinx.coroutines.launch

/**
 * Report Presets Dialog - Pro Plan Only
 * 
 * Allows Pro Plan merchants to:
 * - View all saved report presets
 * - Load/apply a saved preset
 * - Delete unwanted presets
 * - See preset details and creation date
 */
@Composable
fun ReportPresetsDialog(
    repository: UnifiedReportRepository,
    onDismiss: () -> Unit,
    onPresetSelected: (UnifiedReportSetting) -> Unit
) {
    val context = LocalContext.current
    var presets by remember { mutableStateOf<List<UnifiedReportSetting>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var presetToDelete by remember { mutableStateOf<UnifiedReportSetting?>(null) }

    // Load presets
    fun loadPresets() {
        isLoading = true
        errorMessage = null
        
        (context as androidx.activity.ComponentActivity).lifecycleScope.launch {
            try {
                val result = repository.getReportSettings()
                if (result.isSuccess) {
                    presets = result.getOrNull() ?: emptyList()
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "فشل في تحميل الإعدادات المحفوظة"
                }
            } catch (e: Exception) {
                errorMessage = "خطأ في الشبكة: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Delete preset
    fun deletePreset(preset: UnifiedReportSetting) {
        (context as androidx.activity.ComponentActivity).lifecycleScope.launch {
            try {
                val result = repository.deleteReportSetting(preset.id)
                if (result.isSuccess) {
                    presets = presets.filter { it.id != preset.id }
                    presetToDelete = null
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "فشل في حذف الإعداد"
                }
            } catch (e: Exception) {
                errorMessage = "خطأ في الشبكة: ${e.message}"
            }
        }
    }

    // Load presets on first run
    LaunchedEffect(Unit) {
        loadPresets()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "🔖 الإعدادات المحفوظة",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Row {
                        IconButton(
                            onClick = { loadPresets() },
                            enabled = !isLoading
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "تحديث",
                                tint = Color(0xFF00D632)
                            )
                        }
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
                                    "جاري تحميل الإعدادات المحفوظة...",
                                    fontSize = 14.sp,
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
                                    fontSize = 12.sp,
                                    color = Color(0xFF666666),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { loadPresets() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D632))
                                ) {
                                    Text("إعادة المحاولة", color = Color.White)
                                }
                            }
                        }
                    }
                    
                    presets.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🔖", fontSize = 64.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "لا توجد إعدادات محفوظة",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF666666)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "يمكنك حفظ إعداداتك المفضلة من نافذة الفلترة",
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
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Header
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "الإعدادات المحفوظة",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Text(
                                        "${presets.size} إعداد",
                                        fontSize = 12.sp,
                                        color = Color(0xFF666666)
                                    )
                                }
                            }

                            // Presets List
                            items(presets) { preset ->
                                PresetCard(
                                    preset = preset,
                                    onSelect = { onPresetSelected(preset) },
                                    onDelete = { presetToDelete = preset }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    presetToDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = { presetToDelete = null },
            title = {
                Text(
                    "حذف الإعداد المحفوظ",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            },
            text = {
                Text(
                    "هل أنت متأكد من حذف الإعداد \"${preset.name}\"؟\nلا يمكن التراجع عن هذا الإجراء.",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            },
            confirmButton = {
                Button(
                    onClick = { deletePreset(preset) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53E3E))
                ) {
                    Text("حذف", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { presetToDelete = null }) {
                    Text("إلغاء", color = Color(0xFF666666))
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun PresetCard(
    preset: UnifiedReportSetting,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
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
                // Preset Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "تم الإنشاء: ${preset.getFormattedCreatedDate()}",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }
                
                // Actions
                Row {
                    IconButton(
                        onClick = onSelect,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "تطبيق",
                            tint = Color(0xFF00D632),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "حذف",
                            tint = Color(0xFFE53E3E),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Filter Summary
            Text(
                text = preset.filters.getDisplaySummary(),
                fontSize = 12.sp,
                color = Color(0xFF666666),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Filter Tags
            FilterTags(filters = preset.filters)
        }
    }
}

@Composable
fun FilterTags(filters: com.smartpay.models.ReportFilters) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Time Period Tag
        FilterTag(
            text = getTimePeriodDisplay(filters.timePeriod),
            color = Color(0xFF2196F3)
        )
        
        // Report Types Tag
        filters.reportTypes?.let { types ->
            if (types.isNotEmpty()) {
                FilterTag(
                    text = "${types.size} نوع",
                    color = Color(0xFF00D632)
                )
            }
        }
        
        // Amount Range Tag
        if (filters.minAmount != null || filters.maxAmount != null) {
            FilterTag(
                text = "نطاق المبلغ",
                color = Color(0xFF9C27B0)
            )
        }
        
        // Notes Filter Tag
        if (!filters.notesFilter.isNullOrBlank()) {
            FilterTag(
                text = "بحث نصي",
                color = Color(0xFFFF9800)
            )
        }
    }
}

@Composable
fun FilterTag(
    text: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

private fun getTimePeriodDisplay(timePeriod: String): String {
    return when (timePeriod) {
        "daily" -> "يومي"
        "weekly" -> "أسبوعي"
        "monthly" -> "شهري"
        "custom" -> "مخصص"
        else -> timePeriod
    }
}