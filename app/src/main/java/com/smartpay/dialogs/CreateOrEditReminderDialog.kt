package com.smartpay.dialogs

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
import androidx.compose.material.icons.filled.*
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
import com.smartpay.repository.ReminderRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun CreateOrEditReminderDialog(
    reminder: Reminder? = null,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    repository: ReminderRepository
) {
    val context = LocalContext.current
    val isEditing = reminder != null
    
    // Form state
    var title by remember { mutableStateOf(reminder?.title ?: "") }
    var description by remember { mutableStateOf(reminder?.description ?: "") }
    var selectedDate by remember { 
        mutableStateOf(
            if (reminder != null) {
                try {
                    LocalDateTime.parse(reminder.scheduledAt.substring(0, 19)).toLocalDate()
                } catch (e: Exception) {
                    LocalDate.now().plusDays(1)
                }
            } else {
                LocalDate.now().plusDays(1)
            }
        )
    }
    var selectedTime by remember { 
        mutableStateOf(
            if (reminder != null) {
                try {
                    LocalDateTime.parse(reminder.scheduledAt.substring(0, 19)).toLocalTime()
                } catch (e: Exception) {
                    LocalTime.of(9, 0)
                }
            } else {
                LocalTime.of(9, 0)
            }
        )
    }
    var isRecurring by remember { mutableStateOf(reminder?.isRecurring ?: false) }
    var selectedRecurrence by remember { 
        mutableStateOf(
            Reminder.getRecurrenceOptions().find { 
                it.value == (reminder?.recurrenceInterval ?: "none") 
            } ?: Reminder.getRecurrenceOptions().first()
        )
    }
    
    // UI state
    var isLoading by remember { mutableStateOf(false) }
    var showRecurrenceDropdown by remember { mutableStateOf(false) }
    
    val recurrenceOptions = Reminder.getRecurrenceOptions()

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
        // Don't allow past dates
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
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
                Toast.makeText(context, "عنوان التذكير طويل جداً (الحد الأقصى 100 حرف)", Toast.LENGTH_SHORT).show()
                return
            }
            description.length > 500 -> {
                Toast.makeText(context, "الوصف طويل جداً (الحد الأقصى 500 حرف)", Toast.LENGTH_SHORT).show()
                return
            }
            selectedDate.isBefore(LocalDate.now()) -> {
                Toast.makeText(context, "لا يمكن جدولة تذكير في الماضي", Toast.LENGTH_SHORT).show()
                return
            }
            selectedDate.isEqual(LocalDate.now()) && selectedTime.isBefore(LocalTime.now()) -> {
                Toast.makeText(context, "لا يمكن جدولة تذكير في وقت سابق اليوم", Toast.LENGTH_SHORT).show()
                return
            }
            isRecurring && selectedRecurrence.value == "none" -> {
                Toast.makeText(context, "يرجى اختيار فترة التكرار", Toast.LENGTH_SHORT).show()
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
                                title = if (title != reminder.title) title else null,
                                description = if (description != reminder.description) description.takeIf { it.isNotBlank() } else null,
                                scheduledAt = scheduledAtString.takeIf { it != reminder.scheduledAt },
                                isRecurring = if (isRecurring != reminder.isRecurring) isRecurring else null,
                                recurrenceInterval = if (isRecurring) {
                                    selectedRecurrence.value.takeIf { it != "none" }
                                } else null
                            )
                        } else {
                            repository.createReminder(
                                title = title,
                                description = description.takeIf { it.isNotBlank() },
                                scheduledAt = scheduledAtString,
                                isRecurring = isRecurring,
                                recurrenceInterval = if (isRecurring) {
                                    selectedRecurrence.value.takeIf { it != "none" }
                                } else null
                            )
                        }
                        
                        if (response.isSuccessful && response.body() != null) {
                            val body = response.body()!!
                            if (body.success) {
                                Toast.makeText(
                                    context, 
                                    if (isEditing) "تم تحديث التذكير بنجاح" else "تم إنشاء التذكير بنجاح", 
                                    Toast.LENGTH_SHORT
                                ).show()
                                onSuccess()
                            } else {
                                Toast.makeText(context, body.message ?: "فشل في حفظ التذكير", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "فشل في حفظ التذكير", Toast.LENGTH_SHORT).show()
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
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "تعديل التذكير" else "إنشاء تذكير جديد",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    if (!isLoading) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = Color(0xFF666666)
                            )
                        }
                    }
                }

                // Title Input
                Column {
                    Text(
                        text = "عنوان التذكير *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF666666)
                    )
                    
                    BasicTextField(
                        value = title,
                        onValueChange = { if (it.length <= 100) title = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                        cursorBrush = SolidColor(Color(0xFF00D632)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF7F8FA), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        decorationBox = { innerTextField ->
                            if (title.isEmpty()) {
                                Text(
                                    "مثال: متابعة العميل أحمد",
                                    style = TextStyle(fontSize = 16.sp, color = Color(0xFF999999))
                                )
                            }
                            innerTextField()
                        }
                    )
                    Text(
                        text = "${title.length}/100",
                        fontSize = 12.sp,
                        color = if (title.length > 100) Color(0xFFE53E3E) else Color(0xFF999999),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Description Input
                Column {
                    Text(
                        text = "الوصف (اختياري)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF666666)
                    )
                    
                    BasicTextField(
                        value = description,
                        onValueChange = { if (it.length <= 500) description = it },
                        singleLine = false,
                        maxLines = 3,
                        textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                        cursorBrush = SolidColor(Color(0xFF00D632)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF7F8FA), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        decorationBox = { innerTextField ->
                            if (description.isEmpty()) {
                                Text(
                                    "تفاصيل إضافية عن التذكير...",
                                    style = TextStyle(fontSize = 16.sp, color = Color(0xFF999999))
                                )
                            }
                            innerTextField()
                        }
                    )
                    Text(
                        text = "${description.length}/500",
                        fontSize = 12.sp,
                        color = if (description.length > 500) Color(0xFFE53E3E) else Color(0xFF999999),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Date and Time Selection
                Column {
                    Text(
                        text = "تاريخ ووقت التذكير *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF666666)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Date
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showDatePicker() },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF666666))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                            }
                        }
                        
                        // Time
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showTimePicker() },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF666666))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }

                // Recurring Toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = null,
                            tint = Color(0xFF666666)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تذكير متكرر",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black
                            )
                            Text(
                                text = "يتكرر التذكير حسب الفترة المحددة",
                                fontSize = 12.sp,
                                color = Color(0xFF666666)
                            )
                        }
                        Switch(
                            checked = isRecurring,
                            onCheckedChange = { isRecurring = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF00D632),
                                checkedTrackColor = Color(0xFF00D632).copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                // Recurrence Selection
                if (isRecurring) {
                    Column {
                        Text(
                            text = "فترة التكرار *",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF666666)
                        )
                        
                        Box {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showRecurrenceDropdown = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedRecurrence.emoji,
                                        fontSize = 20.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = selectedRecurrence.displayName,
                                        fontSize = 16.sp,
                                        color = Color.Black,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF666666))
                                }
                            }
                            
                            DropdownMenu(
                                expanded = showRecurrenceDropdown,
                                onDismissRequest = { showRecurrenceDropdown = false }
                            ) {
                                recurrenceOptions.filter { it.value != "none" }.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(option.emoji, fontSize = 18.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(option.displayName)
                                            }
                                        },
                                        onClick = {
                                            selectedRecurrence = option
                                            showRecurrenceDropdown = false
                                        }
                                    )
                                }
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
                        enabled = !isLoading
                    ) {
                        Text("إلغاء")
                    }
                    
                    Button(
                        onClick = { validateAndSubmit() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D632)),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(if (isEditing) "تحديث" else "إنشاء")
                        }
                    }
                }
            }
        }
    }
}