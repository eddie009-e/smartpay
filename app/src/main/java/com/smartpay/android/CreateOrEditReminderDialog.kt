package com.smartpay.android

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import com.smartpay.models.*
import com.smartpay.repository.ReminderRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun CreateOrEditReminderDialog(
    reminder: AutomaticReminder? = null,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    repository: ReminderRepository
) {
    val context = LocalContext.current
    val isEditing = reminder != null
    val scrollState = rememberScrollState()
    
    // Form state
    var title by remember { mutableStateOf(reminder?.title ?: "") }
    var message by remember { mutableStateOf(reminder?.message ?: "") }
    var selectedReminderType by remember { mutableStateOf(reminder?.reminderType ?: "custom") }
    var selectedDate by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
    var selectedTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var isRecurring by remember { mutableStateOf(reminder?.isRecurring ?: false) }
    var selectedRecurrenceInterval by remember { mutableStateOf(reminder?.recurrenceInterval ?: "daily") }
    
    // UI state
    var isLoading by remember { mutableStateOf(false) }
    var showReminderTypeDropdown by remember { mutableStateOf(false) }
    var showRecurrenceDropdown by remember { mutableStateOf(false) }
    
    val reminderTypes = ReminderType.getAllOptions()
    val recurrenceIntervals = RecurrenceInterval.getAllOptions()
    
    // Initialize date/time from existing reminder
    LaunchedEffect(reminder) {
        if (reminder != null) {
            try {
                val dateTime = LocalDateTime.parse(reminder.scheduledAt.substring(0, 19))
                selectedDate = dateTime.toLocalDate()
                selectedTime = dateTime.toLocalTime()
            } catch (e: Exception) {
                // Keep default values
            }
        }
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
            title.isBlank() -> {
                Toast.makeText(context, "يرجى إدخال عنوان التذكير", Toast.LENGTH_SHORT).show()
                return
            }
            title.length > 100 -> {
                Toast.makeText(context, "عنوان التذكير يجب أن يكون أقل من 100 حرف", Toast.LENGTH_SHORT).show()
                return
            }
            selectedDate.isBefore(LocalDate.now()) -> {
                Toast.makeText(context, "لا يمكن إنشاء تذكير في الماضي", Toast.LENGTH_SHORT).show()
                return
            }
            selectedDate.isEqual(LocalDate.now()) && selectedTime.isBefore(LocalTime.now()) -> {
                Toast.makeText(context, "لا يمكن إنشاء تذكير في وقت مضى من اليوم", Toast.LENGTH_SHORT).show()
                return
            }
            else -> {
                // Submit form
                isLoading = true
                val scheduledDateTime = LocalDateTime.of(selectedDate, selectedTime)
                val scheduledAtString = scheduledDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                
                (context as ComponentActivity).lifecycleScope.launch {
                    try {
                        val response = if (isEditing) {
                            repository.updateReminder(
                                id = reminder!!.id,
                                title = title,
                                message = message.takeIf { it.isNotBlank() },
                                reminderType = selectedReminderType,
                                scheduledAt = scheduledAtString,
                                isRecurring = isRecurring,
                                recurrenceInterval = if (isRecurring) selectedRecurrenceInterval else null
                            )
                        } else {
                            repository.createReminder(
                                userId = null, // For now, we don't link to specific users
                                title = title,
                                message = message.takeIf { it.isNotBlank() },
                                reminderType = selectedReminderType,
                                scheduledAt = scheduledAtString,
                                isRecurring = isRecurring,
                                recurrenceInterval = if (isRecurring) selectedRecurrenceInterval else null
                            )
                        }
                        
                        if (response.isSuccessful && response.body() != null) {
                            val body = response.body()!!
                            if (body.success) {
                                val successMessage = if (isEditing) "تم تحديث التذكير بنجاح" else "تم إنشاء التذكير بنجاح"
                                Toast.makeText(context, body.message ?: successMessage, Toast.LENGTH_SHORT).show()
                                onSuccess()
                            } else {
                                Toast.makeText(context, body.message ?: "فشل في العملية", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val errorMessage = if (isEditing) "فشل في تحديث التذكير" else "فشل في إنشاء التذكير"
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
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
                            radius = 900f
                        )
                    )
            ) {
                // Animated Background Orbs
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .offset(x = 220.dp, y = 80.dp)
                        .background(
                            Color(0xFFD8FBA9).copy(alpha = 0.12f),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .offset(x = (-40).dp, y = 250.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .offset(x = 180.dp, y = 500.dp)
                        .background(
                            Color(0xFF2D2D2D).copy(alpha = 0.06f),
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
                            .size(140.dp)
                            .offset(x = 280.dp, y = (-70).dp)
                            .background(
                                Color(0xFFD8FBA9).copy(alpha = 0.08f),
                                CircleShape
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .offset(x = (-50).dp, y = 600.dp)
                            .background(
                                Color(0xFF2D2D2D).copy(alpha = 0.04f),
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
                                        if (isEditing) Icons.Default.EditNotifications else Icons.Default.NotificationAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = Color(0xFF2D2D2D)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (isEditing) "تعديل التذكير" else "إضافة تذكير جديد",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2D2D2D),
                                textAlign = TextAlign.Center
                            )
                            
                            Text(
                                text = if (isEditing) "قم بتحديث بيانات التذكير" else "أضف تذكير جديد لتنظيم مهامك",
                                fontSize = 14.sp,
                                color = Color(0xFF666666),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        // Title Field
                        ModernInputField(
                            label = "عنوان التذكير",
                            value = title,
                            onValueChange = { title = it },
                            icon = Icons.Default.Title,
                            placeholder = "مثل: اجتماع مع العميل"
                        )

                        // Message Field
                        ModernInputField(
                            label = "الرسالة (اختياري)",
                            value = message,
                            onValueChange = { message = it },
                            icon = Icons.Default.Message,
                            placeholder = "تفاصيل إضافية...",
                            maxLines = 3
                        )

                        // Reminder Type Selection
                        ModernDropdownField(
                            label = "نوع التذكير",
                            selectedValue = selectedReminderType,
                            options = reminderTypes,
                            expanded = showReminderTypeDropdown,
                            onExpandedChange = { showReminderTypeDropdown = it },
                            onItemSelected = { selectedReminderType = it.value },
                            icon = Icons.Default.Category,
                            getDisplayText = { "${it.emoji} ${it.displayName}" },
                            placeholder = "اختر نوع التذكير"
                        )

                        // Date and Time Selection
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFFD8FBA9)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "موعد التذكير",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF2D2D2D)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Date Selection
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showDatePicker() },
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
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "التاريخ",
                                                fontSize = 12.sp,
                                                color = Color(0xFF666666)
                                            )
                                            Text(
                                                text = selectedDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF2D2D2D)
                                            )
                                        }
                                    }
                                }
                                
                                // Time Selection
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showTimePicker() },
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
                                            Icons.Default.AccessTime,
                                            contentDescription = null,
                                            tint = Color(0xFFD8FBA9),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "الوقت",
                                                fontSize = 12.sp,
                                                color = Color(0xFF666666)
                                            )
                                            Text(
                                                text = selectedTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF2D2D2D)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Recurring Option
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isRecurring) Color(0xFFD8FBA9).copy(alpha = 0.15f) 
                                               else Color.White.copy(alpha = 0.8f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isRecurring = !isRecurring }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isRecurring,
                                    onCheckedChange = { isRecurring = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFFD8FBA9),
                                        uncheckedColor = Color(0xFF666666)
                                    )
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "تذكير متكرر",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF2D2D2D)
                                    )
                                    Text(
                                        text = "سيتكرر التذكير بشكل دوري",
                                        fontSize = 12.sp,
                                        color = Color(0xFF666666)
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    Icons.Default.Repeat,
                                    contentDescription = null,
                                    tint = if (isRecurring) Color(0xFFD8FBA9) else Color(0xFF666666),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Recurrence Interval (if recurring)
                        if (isRecurring) {
                            ModernDropdownField(
                                label = "نوع التكرار",
                                selectedValue = selectedRecurrenceInterval,
                                options = recurrenceIntervals,
                                expanded = showRecurrenceDropdown,
                                onExpandedChange = { showRecurrenceDropdown = it },
                                onItemSelected = { selectedRecurrenceInterval = it.value },
                                icon = Icons.Default.Repeat,
                                getDisplayText = { "${it.emoji} ${it.displayName}" },
                                placeholder = "اختر نوع التكرار"
                            )
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
                                            if (isEditing) Icons.Default.Update else Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = Color(0xFF2D2D2D)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            if (isEditing) "تحديث" else "إضافة",
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
    maxLines: Int = 1
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
                            is ReminderType -> it.value == selectedValue
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