package com.smartpay.android

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.smartpay.models.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Advanced Report Filter Dialog - Pro Plan Only
 * 
 * Provides comprehensive filtering options for unified business reports including:
 * - Report types selection (expense, income, transfer, debt, salary, invoice)
 * - Time period selection with custom date range
 * - Amount range filtering
 * - Notes/description filtering
 * - Category selection
 */
@Composable
fun ReportFilterDialog(
    initialFilters: ReportFilters?,
    onDismiss: () -> Unit,
    onApplyFilters: (ReportFilters) -> Unit,
    onSavePreset: (ReportFilters, String) -> Unit
) {
    var selectedReportTypes by remember { 
        mutableStateOf(initialFilters?.reportTypes?.toSet() ?: emptySet()) 
    }
    var selectedTimePeriod by remember { 
        mutableStateOf(initialFilters?.timePeriod ?: "monthly") 
    }
    var startDate by remember { 
        mutableStateOf(initialFilters?.startDate ?: "") 
    }
    var endDate by remember { 
        mutableStateOf(initialFilters?.endDate ?: "") 
    }
    var minAmount by remember { 
        mutableStateOf(initialFilters?.minAmount?.toString() ?: "") 
    }
    var maxAmount by remember { 
        mutableStateOf(initialFilters?.maxAmount?.toString() ?: "") 
    }
    var notesFilter by remember { 
        mutableStateOf(initialFilters?.notesFilter ?: "") 
    }
    var showSavePresetDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
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
                        "🔍 فلترة التقارير",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Color(0xFF666666)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Content
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Report Types Section
                    item {
                        FilterSection(title = "أنواع التقارير") {
                            ReportTypesSelector(
                                selectedTypes = selectedReportTypes,
                                onTypesChanged = { selectedReportTypes = it }
                            )
                        }
                    }

                    // Time Period Section
                    item {
                        FilterSection(title = "الفترة الزمنية") {
                            TimePeriodSelector(
                                selectedPeriod = selectedTimePeriod,
                                onPeriodSelected = { selectedTimePeriod = it },
                                startDate = startDate,
                                endDate = endDate,
                                onStartDateChanged = { startDate = it },
                                onEndDateChanged = { endDate = it }
                            )
                        }
                    }

                    // Amount Range Section
                    item {
                        FilterSection(title = "نطاق المبلغ") {
                            AmountRangeSelector(
                                minAmount = minAmount,
                                maxAmount = maxAmount,
                                onMinAmountChanged = { minAmount = it },
                                onMaxAmountChanged = { maxAmount = it }
                            )
                        }
                    }

                    // Notes Filter Section
                    item {
                        FilterSection(title = "البحث في الملاحظات") {
                            NotesFilterInput(
                                notesFilter = notesFilter,
                                onNotesFilterChanged = { notesFilter = it }
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Reset Button
                    OutlinedButton(
                        onClick = {
                            selectedReportTypes = emptySet()
                            selectedTimePeriod = "monthly"
                            startDate = ""
                            endDate = ""
                            minAmount = ""
                            maxAmount = ""
                            notesFilter = ""
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إعادة تعيين", color = Color(0xFF666666))
                    }
                    
                    // Save Preset Button
                    OutlinedButton(
                        onClick = { showSavePresetDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF2196F3)
                        )
                    ) {
                        Icon(
                            Icons.Default.Bookmark,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حفظ")
                    }

                    // Apply Button
                    Button(
                        onClick = {
                            val filters = ReportFilters(
                                reportTypes = if (selectedReportTypes.isEmpty()) null else selectedReportTypes.toList(),
                                categories = null, // TODO: Add category selection
                                timePeriod = selectedTimePeriod,
                                startDate = if (startDate.isBlank()) null else startDate,
                                endDate = if (endDate.isBlank()) null else endDate,
                                minAmount = minAmount.toDoubleOrNull(),
                                maxAmount = maxAmount.toDoubleOrNull(),
                                notesFilter = if (notesFilter.isBlank()) null else notesFilter
                            )
                            onApplyFilters(filters)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D632))
                    ) {
                        Text("تطبيق", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Save Preset Dialog
    if (showSavePresetDialog) {
        SavePresetDialog(
            onDismiss = { showSavePresetDialog = false },
            onSave = { presetName ->
                val filters = ReportFilters(
                    reportTypes = if (selectedReportTypes.isEmpty()) null else selectedReportTypes.toList(),
                    categories = null,
                    timePeriod = selectedTimePeriod,
                    startDate = if (startDate.isBlank()) null else startDate,
                    endDate = if (endDate.isBlank()) null else endDate,
                    minAmount = minAmount.toDoubleOrNull(),
                    maxAmount = maxAmount.toDoubleOrNull(),
                    notesFilter = if (notesFilter.isBlank()) null else notesFilter
                )
                onSavePreset(filters, presetName)
                showSavePresetDialog = false
            }
        )
    }
}

@Composable
fun FilterSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
fun ReportTypesSelector(
    selectedTypes: Set<String>,
    onTypesChanged: (Set<String>) -> Unit
) {
    val reportTypes = listOf(
        "expense" to ("💸" to "مصروفات"),
        "income" to ("💰" to "دخل"),
        "transfer" to ("🔄" to "تحويلات"),
        "debt" to ("🧾" to "ديون"),
        "salary" to ("👥" to "رواتب"),
        "invoice" to ("💳" to "فواتير")
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(reportTypes) { (type, displayInfo) ->
            val (emoji, displayName) = displayInfo
            val isSelected = selectedTypes.contains(type)
            
            ReportTypeChip(
                emoji = emoji,
                text = displayName,
                isSelected = isSelected,
                onClick = {
                    val newTypes = if (isSelected) {
                        selectedTypes - type
                    } else {
                        selectedTypes + type
                    }
                    onTypesChanged(newTypes)
                }
            )
        }
    }
}

@Composable
fun ReportTypeChip(
    emoji: String,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) Color(0xFF00D632).copy(alpha = 0.1f) else Color(0xFFF7F8FA)
            )
            .border(
                1.dp,
                if (isSelected) Color(0xFF00D632) else Color(0xFFE0E0E0),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = emoji,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color(0xFF00D632) else Color(0xFF666666)
            )
        }
    }
}

@Composable
fun TimePeriodSelector(
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit,
    startDate: String,
    endDate: String,
    onStartDateChanged: (String) -> Unit,
    onEndDateChanged: (String) -> Unit
) {
    val periods = TimePeriod.getAllOptions()
    
    Column {
        // Period Selection
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(periods) { period ->
                PeriodChip(
                    emoji = period.emoji,
                    text = period.displayName,
                    isSelected = selectedPeriod == period.value,
                    onClick = { onPeriodSelected(period.value) }
                )
            }
        }
        
        // Custom Date Range (only show if custom is selected)
        if (selectedPeriod == "custom") {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DateInputField(
                    label = "من تاريخ",
                    value = startDate,
                    onValueChange = onStartDateChanged,
                    modifier = Modifier.weight(1f)
                )
                DateInputField(
                    label = "إلى تاريخ",
                    value = endDate,
                    onValueChange = onEndDateChanged,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun PeriodChip(
    emoji: String,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) Color(0xFF2196F3) else Color(0xFFF7F8FA)
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = emoji,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFF666666)
            )
        }
    }
}

@Composable
fun AmountRangeSelector(
    minAmount: String,
    maxAmount: String,
    onMinAmountChanged: (String) -> Unit,
    onMaxAmountChanged: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AmountInputField(
            label = "الحد الأدنى",
            value = minAmount,
            onValueChange = onMinAmountChanged,
            modifier = Modifier.weight(1f)
        )
        AmountInputField(
            label = "الحد الأعلى",
            value = maxAmount,
            onValueChange = onMaxAmountChanged,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun NotesFilterInput(
    notesFilter: String,
    onNotesFilterChanged: (String) -> Unit
) {
    FilterInputField(
        label = "البحث في الملاحظات",
        value = notesFilter,
        onValueChange = onNotesFilterChanged,
        placeholder = "ادخل كلمة للبحث في ملاحظات التقارير"
    )
}

@Composable
fun DateInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF666666),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(0xFFF7F8FA), shape = RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = { newValue ->
                    // Simple date format validation (YYYY-MM-DD)
                    if (newValue.matches(Regex("^\\d{0,4}-?\\d{0,2}-?\\d{0,2}$"))) {
                        onValueChange(newValue)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = Color.Black
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                cursorBrush = SolidColor(Color(0xFF00D632)),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            "YYYY-MM-DD",
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = Color(0xFF999999)
                            )
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
fun AmountInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF666666),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(0xFFF7F8FA), shape = RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = { newValue ->
                    // Only allow numbers and decimal point
                    if (newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                        onValueChange(newValue)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = Color.Black
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                cursorBrush = SolidColor(Color(0xFF00D632)),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            "0.00",
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = Color(0xFF999999)
                            )
                        )
                    }
                    innerTextField()
                }
            )
            if (value.isNotEmpty()) {
                Text(
                    "ل.س",
                    fontSize = 12.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
    }
}

@Composable
fun FilterInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF666666),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(0xFFF7F8FA), shape = RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = Color.Black
                ),
                singleLine = true,
                cursorBrush = SolidColor(Color(0xFF00D632)),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = Color(0xFF999999)
                            )
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
fun SavePresetDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var presetName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "حفظ إعداد التقرير",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        },
        text = {
            Column {
                Text(
                    "ادخل اسم الإعداد لحفظه واستخدامه لاحقاً",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
                Spacer(modifier = Modifier.height(12.dp))
                FilterInputField(
                    label = "اسم الإعداد",
                    value = presetName,
                    onValueChange = { presetName = it },
                    placeholder = "مثال: تقرير شهري للمصروفات"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (presetName.isNotBlank()) {
                        onSave(presetName.trim())
                    }
                },
                enabled = presetName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D632))
            ) {
                Text("حفظ", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color(0xFF666666))
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White
    )
}