package com.smartpay.android

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
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
import com.smartpay.repository.RecurringInvoiceRepository
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CreateRecurringInvoiceDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    repository: RecurringInvoiceRepository
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    // Form state
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedInterval by remember { mutableStateOf<RecurrenceInterval?>(null) }
    var startDate by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var hasEndDate by remember { mutableStateOf(false) }
    
    // UI state
    var isLoading by remember { mutableStateOf(false) }
    var showCustomerDropdown by remember { mutableStateOf(false) }
    var showIntervalDropdown by remember { mutableStateOf(false) }
    
    // Sample customers (In real app, fetch from API)
    val customers = remember {
        listOf(
            Customer("1", "أحمد محمد", "0991234567", "ahmad@example.com"),
            Customer("2", "فاطمة علي", "0987654321", "fatima@example.com"),
            Customer("3", "محمد خالد", "0976543210", "mohammed@example.com"),
            Customer("4", "سارة أحمد", "0965432109", "sara@example.com")
        )
    }
    
    val intervals = RecurrenceInterval.getAllOptions()

    fun showStartDatePicker() {
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                startDate = LocalDate.of(year, month + 1, dayOfMonth)
            },
            startDate.year,
            startDate.monthValue - 1,
            startDate.dayOfMonth
        )
        datePickerDialog.show()
    }

    fun showEndDatePicker() {
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                endDate = LocalDate.of(year, month + 1, dayOfMonth)
            },
            endDate?.year ?: LocalDate.now().year,
            (endDate?.monthValue ?: LocalDate.now().monthValue) - 1,
            endDate?.dayOfMonth ?: LocalDate.now().dayOfMonth
        )
        datePickerDialog.show()
    }

    fun validateAndSubmit() {
        when {
            selectedCustomer == null -> {
                Toast.makeText(context, "يرجى اختيار العميل", Toast.LENGTH_SHORT).show()
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
            selectedInterval == null -> {
                Toast.makeText(context, "يرجى اختيار نوع التكرار", Toast.LENGTH_SHORT).show()
                return
            }
            hasEndDate && endDate != null && endDate!! <= startDate -> {
                Toast.makeText(context, "تاريخ الانتهاء يجب أن يكون بعد تاريخ البداية", Toast.LENGTH_SHORT).show()
                return
            }
            else -> {
                // Submit form
                isLoading = true
                (context as ComponentActivity).lifecycleScope.launch {
                    try {
                        val request = CreateRecurringInvoiceRequest(
                            customerId = selectedCustomer!!.id,
                            amount = amount.toBigDecimal(),
                            description = description.takeIf { it.isNotBlank() },
                            recurrenceInterval = selectedInterval!!.value,
                            nextRunDate = startDate.toString(),
                            endDate = if (hasEndDate) endDate?.toString() else null
                        )
                        
                        val response = repository.createRecurringInvoice(request)
                        if (response.isSuccessful && response.body() != null) {
                            val body = response.body()!!
                            if (body.success) {
                                Toast.makeText(context, body.message ?: "تم إنشاء الفاتورة المتكررة بنجاح", Toast.LENGTH_SHORT).show()
                                onSuccess()
                            } else {
                                Toast.makeText(context, body.message ?: "فشل في إنشاء الفاتورة", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "فشل في إنشاء الفاتورة المتكررة", Toast.LENGTH_SHORT).show()
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
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Modern Background with animated orbs
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFF8FDED),
                                Color(0xFFF0F8E8)
                            ),
                            radius = 950f
                        )
                    )
            ) {
                // Animated Background Orbs
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .offset(x = 250.dp, y = 60.dp)
                        .background(
                            Color(0xFFD8FBA9).copy(alpha = 0.1f),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .offset(x = (-50).dp, y = 280.dp)
                        .background(
                            Color.White.copy(alpha = 0.25f),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .offset(x = 200.dp, y = 520.dp)
                        .background(
                            Color(0xFF2D2D2D).copy(alpha = 0.05f),
                            CircleShape
                        )
                )
            }

            // Main Dialog Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.95f)
                    .padding(16.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
            ) {
                Box {
                    // Subtle card patterns
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .offset(x = 300.dp, y = (-80).dp)
                            .background(
                                Color(0xFFD8FBA9).copy(alpha = 0.06f),
                                CircleShape
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .offset(x = (-60).dp, y = 650.dp)
                            .background(
                                Color(0xFF2D2D2D).copy(alpha = 0.03f),
                                CircleShape
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(32.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Header Section
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Icon
                            Card(
                                modifier = Modifier.size(80.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFD8FBA9)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.RepeatOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = Color(0xFF2D2D2D)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "إنشاء فاتورة متكررة",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2D2D2D),
                                textAlign = TextAlign.Center
                            )
                            
                            Text(
                                text = "أنشئ فاتورة تلقائية تتكرر بشكل دوري",
                                fontSize = 14.sp,
                                color = Color(0xFF666666),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        // Customer Selection
                        ModernDropdownField(
                            label = "العميل",
                            selectedValue = selectedCustomer?.id ?: "",
                            options = customers,
                            expanded = showCustomerDropdown,
                            onExpandedChange = { showCustomerDropdown = it },
                            onItemSelected = { selectedCustomer = it },
                            icon = Icons.Default.Person,
                            getDisplayText = { it.getDisplayName() },
                            placeholder = "اختر العميل"
                        )

                        // Amount Field
                        ModernInputField(
                            label = "المبلغ (ل.س)",
                            value = amount,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    amount = newValue
                                }
                            },
                            icon = Icons.Default.AttachMoney,
                            placeholder = "أدخل المبلغ",
                            keyboardType = KeyboardType.Decimal
                        )

                        // Description Field
                        ModernInputField(
                            label = "الوصف (اختياري)",
                            value = description,
                            onValueChange = { description = it },
                            icon = Icons.Default.Description,
                            placeholder = "تفاصيل الفاتورة...",
                            maxLines = 3
                        )

                        // Recurrence Interval
                        ModernDropdownField(
                            label = "نوع التكرار",
                            selectedValue = selectedInterval?.value ?: "",
                            options = intervals,
                            expanded = showIntervalDropdown,
                            onExpandedChange = { showIntervalDropdown = it },
                            onItemSelected = { selectedInterval = it },
                            icon = Icons.Default.Repeat,
                            getDisplayText = { "${it.emoji} ${it.displayName}" },
                            placeholder = "اختر نوع التكرار"
                        )

                        // Start Date
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFFD8FBA9)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تاريخ البداية",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF2D2D2D)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showStartDatePicker() },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.8f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = Color(0xFFD8FBA9),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "تاريخ بداية التكرار",
                                            fontSize = 12.sp,
                                            color = Color(0xFF666666)
                                        )
                                        Text(
                                            text = startDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF2D2D2D)
                                        )
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = Color(0xFF666666),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // End Date Toggle
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (hasEndDate) Color(0xFFD8FBA9).copy(alpha = 0.15f) 
                                               else Color.White.copy(alpha = 0.8f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { hasEndDate = !hasEndDate }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = hasEndDate,
                                    onCheckedChange = { hasEndDate = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFFD8FBA9),
                                        uncheckedColor = Color(0xFF666666)
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "تحديد تاريخ انتهاء",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF2D2D2D)
                                    )
                                    Text(
                                        text = "ستتوقف الفاتورة عند هذا التاريخ",
                                        fontSize = 12.sp,
                                        color = Color(0xFF666666)
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    Icons.Default.Stop,
                                    contentDescription = null,
                                    tint = if (hasEndDate) Color(0xFFD8FBA9) else Color(0xFF666666),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // End Date (if enabled)
                        if (hasEndDate) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Stop,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = Color(0xFFD8FBA9)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "تاريخ الانتهاء",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF2D2D2D)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showEndDatePicker() },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.White.copy(alpha = 0.8f)
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = Color(0xFFD8FBA9),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "تاريخ إيقاف التكرار",
                                                fontSize = 12.sp,
                                                color = Color(0xFF666666)
                                            )
                                            Text(
                                                text = endDate?.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")) 
                                                      ?: "اختر التاريخ",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (endDate != null) Color(0xFF2D2D2D) else Color(0xFF999999)
                                            )
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        Icon(
                                            Icons.Default.DateRange,
                                            contentDescription = null,
                                            tint = Color(0xFF666666),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Cancel Button
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                enabled = !isLoading,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(2.dp, Color(0xFF2D2D2D).copy(alpha = 0.2f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF2D2D2D)
                                )
                            ) {
                                Text(
                                    "إلغاء",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                )
                            }
                            
                            // Submit Button
                            Button(
                                onClick = { validateAndSubmit() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                enabled = !isLoading,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD8FBA9),
                                    disabledContainerColor = Color(0xFFD8FBA9).copy(alpha = 0.5f)
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF2D2D2D),
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = Color(0xFF2D2D2D)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "إنشاء",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color(0xFF2D2D2D)
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

@Composable
private fun ModernInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    placeholder: String,
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color(0xFFD8FBA9)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2D2D2D)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.8f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = maxLines == 1,
                maxLines = maxLines,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                textStyle = TextStyle(
                    fontSize = 16.sp, 
                    color = Color(0xFF2D2D2D),
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(Color(0xFFD8FBA9)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = TextStyle(
                                fontSize = 16.sp,
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
private fun <T> ModernDropdownField(
    label: String,
    selectedValue: String,
    options: List<T>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onItemSelected: (T) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    getDisplayText: (T) -> String,
    placeholder: String
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color(0xFFD8FBA9)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2D2D2D)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Box {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!expanded) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.8f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val selectedOption = options.find { 
                        when (it) {
                            is Customer -> it.id == selectedValue
                            is RecurrenceInterval -> it.value == selectedValue
                            else -> false
                        }
                    }
                    
                    Text(
                        text = selectedOption?.let { getDisplayText(it) } ?: placeholder,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selectedOption != null) Color(0xFF2D2D2D) else Color(0xFF999999),
                        modifier = Modifier.weight(1f)
                    )
                    
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Color(0xFF666666)
                    )
                }
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier.background(
                    Color.White,
                    RoundedCornerShape(16.dp)
                )
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                getDisplayText(option),
                                fontSize = 16.sp,
                                color = Color(0xFF2D2D2D)
                            )
                        },
                        onClick = {
                            onItemSelected(option)
                            onExpandedChange(false)
                        }
                    )
                }
            }
        }
    }
}