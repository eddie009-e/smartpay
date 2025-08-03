package com.smartpay.android

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.smartpay.repository.FinancialReportRepository
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun AddReportDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    repository: FinancialReportRepository
) {
    val context = LocalContext.current
    
    // Form state
    var selectedReportType by remember { mutableStateOf<ReportType?>(null) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTime by remember { mutableStateOf(LocalTime.now()) }
    
    // UI state
    var isLoading by remember { mutableStateOf(false) }
    var showReportTypeDropdown by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    
    val reportTypes = ReportType.getAllOptions()
    
    // Sample categories
    val categories = remember {
        listOf(
            TransactionCategory("1", "", "طعام وشراب", "Food & Drink", "#D8FBA9", ""),
            TransactionCategory("2", "", "مواصلات", "Transportation", "#2D2D2D", ""),
            TransactionCategory("3", "", "فواتير", "Bills", "#6B7280", ""),
            TransactionCategory("4", "", "راتب", "Salary", "#D8FBA9", ""),
            TransactionCategory("5", "", "أخرى", "Other", "#9E9E9E", "")
        )
    }

    fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        )
        datePickerDialog.show()
    }

    fun showTimePicker() {
        val timePickerDialog = TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                selectedTime = LocalTime.of(hourOfDay, minute)
            },
            selectedTime.hour,
            selectedTime.minute,
            false
        )
        timePickerDialog.show()
    }

    fun validateAndSubmit() {
        when {
            selectedReportType == null -> {
                Toast.makeText(context, "يرجى اختيار نوع التقرير", Toast.LENGTH_SHORT).show()
                return
            }
            amount.isBlank() || amount.toBigDecimalOrNull() == null -> {
                Toast.makeText(context, "يرجى إدخال مبلغ صحيح", Toast.LENGTH_SHORT).show()
                return
            }
            amount.toBigDecimal() <= BigDecimal.ZERO -> {
                Toast.makeText(context, "المبلغ يجب أن يكون أكبر من صفر", Toast.LENGTH_SHORT).show()
                return
            }
            amount.toBigDecimal() > BigDecimal("999999999.99") -> {
                Toast.makeText(context, "المبلغ كبير جداً", Toast.LENGTH_SHORT).show()
                return
            }
            selectedDate.isAfter(LocalDate.now()) -> {
                Toast.makeText(context, "لا يمكن تسجيل عمليات مالية في المستقبل", Toast.LENGTH_SHORT).show()
                return
            }
            selectedDate.isEqual(LocalDate.now()) && selectedTime.isAfter(LocalTime.now()) -> {
                Toast.makeText(context, "لا يمكن تسجيل عملية في وقت مستقبلي اليوم", Toast.LENGTH_SHORT).show()
                return
            }
            else -> {
                // Submit form
                isLoading = true
                val occurredDateTime = LocalDateTime.of(selectedDate, selectedTime)
                val occurredAtString = occurredDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                
                (context as ComponentActivity).lifecycleScope.launch {
                    try {
                        val response = repository.addReport(
                            reportType = selectedReportType!!.value,
                            amount = amount.toBigDecimal(),
                            note = note.takeIf { it.isNotBlank() },
                            categoryId = selectedCategoryId,
                            occurredAt = occurredAtString
                        )
                        
                        if (response.isSuccessful && response.body() != null) {
                            val body = response.body()!!
                            if (body.success) {
                                Toast.makeText(context, body.message ?: "تم إضافة التقرير المالي بنجاح", Toast.LENGTH_SHORT).show()
                                onSuccess()
                            } else {
                                Toast.makeText(context, body.message ?: "فشل في إضافة التقرير", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "فشل في إضافة التقرير المالي", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isLoading = false
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Title with icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📊", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إضافة عملية مالية",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D2D2D),
                        textAlign = TextAlign.Center
                    )
                }

                // Report Type
                Column {
                    Text(
                        text = "نوع العملية *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF6B7280)
                    )
                    
                    Box {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showReportTypeDropdown = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FDED)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedReportType?.emoji ?: "📊",
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = selectedReportType?.displayName ?: "اختر نوع العملية",
                                    fontSize = 16.sp,
                                    color = if (selectedReportType != null) Color(0xFF2D2D2D) else Color(0xFF6B7280),
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF6B7280))
                            }
                        }
                        
                        DropdownMenu(
                            expanded = showReportTypeDropdown,
                            onDismissRequest = { showReportTypeDropdown = false }
                        ) {
                            reportTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(type.emoji, fontSize = 18.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(type.displayName)
                                        }
                                    },
                                    onClick = {
                                        selectedReportType = type
                                        showReportTypeDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Amount
                Column {
                    Text(
                        text = "المبلغ (ل.س) *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF6B7280)
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FDED)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        BasicTextField(
                            value = amount,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    amount = newValue
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 16.sp, color = Color(0xFF2D2D2D)),
                            cursorBrush = SolidColor(Color(0xFFD8FBA9)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }

                // Note
                Column {
                    Text(
                        text = "ملاحظة (اختياري)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF6B7280)
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FDED)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        BasicTextField(
                            value = note,
                            onValueChange = { note = it },
                            singleLine = false,
                            maxLines = 3,
                            textStyle = TextStyle(fontSize = 16.sp, color = Color(0xFF2D2D2D)),
                            cursorBrush = SolidColor(Color(0xFFD8FBA9)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }

                // Category
                Column {
                    Text(
                        text = "التصنيف (اختياري)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF6B7280)
                    )
                    
                    Box {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCategoryDropdown = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FDED)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Category, contentDescription = null, tint = Color(0xFF6B7280))
                                Spacer(modifier = Modifier.width(12.dp))
                                val selectedCategory = categories.find { it.id == selectedCategoryId }
                                Text(
                                    text = selectedCategory?.nameAr ?: "اختر التصنيف",
                                    fontSize = 16.sp,
                                    color = if (selectedCategory != null) Color(0xFF2D2D2D) else Color(0xFF6B7280),
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF6B7280))
                            }
                        }
                        
                        DropdownMenu(
                            expanded = showCategoryDropdown,
                            onDismissRequest = { showCategoryDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("بدون تصنيف") },
                                onClick = {
                                    selectedCategoryId = null
                                    showCategoryDropdown = false
                                }
                            )
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.nameAr) },
                                    onClick = {
                                        selectedCategoryId = category.id
                                        showCategoryDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Date and Time Selection
                Column {
                    Text(
                        text = "تاريخ ووقت العملية *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF6B7280)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Date
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showDatePicker() },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FDED)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF6B7280))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),
                                    fontSize = 14.sp,
                                    color = Color(0xFF2D2D2D)
                                )
                            }
                        }
                        
                        // Time
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showTimePicker() },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FDED)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF6B7280))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                    fontSize = 14.sp,
                                    color = Color(0xFF2D2D2D)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF2D2D2D)
                        )
                    ) {
                        Text("إلغاء", fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = { validateAndSubmit() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD8FBA9)),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text("إضافة", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}