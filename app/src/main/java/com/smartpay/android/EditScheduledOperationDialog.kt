package com.smartpay.android

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.smartpay.models.*
import com.smartpay.repositories.ScheduledOperationRepository
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun EditScheduledOperationDialog(
    repository: ScheduledOperationRepository,
    operation: ScheduledOperation,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var operationType by remember { mutableStateOf(OperationType.fromValue(operation.operationType)) }
    var amount by remember { mutableStateOf(operation.amount.toString()) }
    var targetId by remember { mutableStateOf(operation.targetId) }
    var recurrenceType by remember { mutableStateOf(RecurrenceType.fromValue(operation.recurrence)) }
    var startDate by remember { mutableStateOf(operation.startDate) }
    var description by remember { mutableStateOf(operation.description ?: "") }
    var status by remember { mutableStateOf(OperationStatus.fromValue(operation.status)) }
    var isUpdating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isValid = operationType != null && 
                  amount.isNotEmpty() && 
                  targetId.isNotEmpty() && 
                  recurrenceType != null && 
                  startDate.isNotEmpty()

    fun updateOperation() {
        if (!isValid) return
        
        isUpdating = true
        errorMessage = null
        
        lifecycleOwner.lifecycleScope.launch {
            try {
                val request = UpdateScheduledOperationRequest(
                    operationType = operationType?.value,
                    amount = amount.toDoubleOrNull(),
                    targetId = targetId,
                    recurrence = recurrenceType?.value,
                    startDate = startDate,
                    status = status?.value,
                    description = description.takeIf { it.isNotBlank() }
                )
                
                val result = repository.updateScheduledOperation(operation.id, request)
                if (result.isSuccess) {
                    onSuccess()
                } else {
                    errorMessage = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                errorMessage = "خطأ: ${e.message}"
            } finally {
                isUpdating = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            LazyColumn(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color(0xFF00D632),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تعديل العملية المجدولة",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
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

                // Current Operation Info
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(operation.getOperationTypeEmoji(), fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "العملية الحالية",
                                        fontSize = 12.sp,
                                        color = Color(0xFF666666)
                                    )
                                    Text(
                                        text = "${operation.getOperationTypeDisplay()} - ${operation.getFormattedAmount()}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "${operation.getRecurrenceDisplay()} - ${operation.getStatusDisplay()}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF666666)
                                    )
                                }
                            }
                        }
                    }
                }

                // Operation Type
                item {
                    Text(
                        text = "نوع العملية",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(OperationType.getAllTypes()) { type ->
                            OperationTypeCard(
                                operationType = type,
                                isSelected = operationType == type,
                                onClick = { operationType = type }
                            )
                        }
                    }
                }

                // Amount and Target
                item {
                    Text(
                        text = "المبلغ والهدف",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("المبلغ (ل.س)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00D632),
                            focusedLabelColor = Color(0xFF00D632)
                        )
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = targetId,
                        onValueChange = { targetId = it },
                        label = { 
                            Text(when (operationType) {
                                OperationType.INVOICE -> "معرف العميل"
                                OperationType.SALARY -> "معرف الموظف"
                                OperationType.TRANSFER -> "معرف المستقبل"
                                else -> "معرف الهدف"
                            })
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00D632),
                            focusedLabelColor = Color(0xFF00D632)
                        )
                    )
                }

                // Recurrence
                item {
                    Text(
                        text = "التكرار",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(RecurrenceType.getAllTypes()) { type ->
                            RecurrenceTypeCard(
                                recurrenceType = type,
                                isSelected = recurrenceType == type,
                                onClick = { recurrenceType = type }
                            )
                        }
                    }
                }

                // Status
                item {
                    Text(
                        text = "الحالة",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(OperationStatus.getAllStatuses()) { statusOption ->
                            StatusCard(
                                status = statusOption,
                                isSelected = status == statusOption,
                                onClick = { status = statusOption }
                            )
                        }
                    }
                }

                // Start Date and Description
                item {
                    Text(
                        text = "الجدولة والوصف",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("تاريخ البداية") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00D632),
                            focusedLabelColor = Color(0xFF00D632)
                        )
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("وصف (اختياري)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00D632),
                            focusedLabelColor = Color(0xFF00D632)
                        )
                    )
                }

                // Error message
                if (errorMessage != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = Color(0xFFE53E3E),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage!!,
                                    fontSize = 12.sp,
                                    color = Color(0xFFE53E3E)
                                )
                            }
                        }
                    }
                }

                // Action buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF666666)
                            )
                        ) {
                            Text("إلغاء")
                        }
                        
                        Button(
                            onClick = { updateOperation() },
                            modifier = Modifier.weight(1f),
                            enabled = isValid && !isUpdating,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00D632)
                            )
                        ) {
                            if (isUpdating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White
                                )
                            } else {
                                Text("حفظ التغييرات", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusCard(
    status: OperationStatus,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(status.color).copy(alpha = 0.1f) else Color.White
        ),
        border = if (isSelected) BorderStroke(2.dp, Color(status.color)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Color(status.color), RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = status.displayName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color(status.color) else Color.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = status.description,
                fontSize = 9.sp,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}