package com.smartpay.android

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
fun CreateScheduledOperationDialog(
    repository: ScheduledOperationRepository,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var operationType by remember { mutableStateOf<OperationType?>(null) }
    var amount by remember { mutableStateOf("") }
    var targetId by remember { mutableStateOf("") }
    var recurrenceType by remember { mutableStateOf<RecurrenceType?>(null) }
    var startDate by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentStep by remember { mutableStateOf(0) }

    // Initialize start date to current time + 1 hour
    LaunchedEffect(Unit) {
        val now = LocalDateTime.now().plusHours(1)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        startDate = now.format(formatter)
    }

    val isValid = operationType != null && 
                  amount.isNotEmpty() && 
                  targetId.isNotEmpty() && 
                  recurrenceType != null && 
                  startDate.isNotEmpty()

    fun createOperation() {
        if (!isValid) return
        
        isCreating = true
        errorMessage = null
        
        lifecycleOwner.lifecycleScope.launch {
            try {
                val request = CreateScheduledOperationRequest(
                    operationType = operationType!!.value,
                    amount = amount.toDouble(),
                    targetId = targetId,
                    recurrence = recurrenceType!!.value,
                    startDate = startDate,
                    description = description.takeIf { it.isNotBlank() }
                )
                
                val result = repository.createScheduledOperation(request)
                if (result.isSuccess) {
                    onSuccess()
                } else {
                    errorMessage = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                errorMessage = "خطأ: ${e.message}"
            } finally {
                isCreating = false
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
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFF00D632),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "إنشاء عملية مجدولة",
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

                // Progress indicator
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        repeat(4) { index ->
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (index <= currentStep) Color(0xFF00D632) else Color(0xFFE0E0E0),
                                        RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                    }
                }

                // Step 1: Operation Type
                if (currentStep >= 0) {
                    item {
                        Text(
                            text = "1. نوع العملية",
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
                                    onClick = { 
                                        operationType = type
                                        if (currentStep == 0) currentStep = 1
                                    }
                                )
                            }
                        }
                    }
                }

                // Step 2: Amount and Target
                if (currentStep >= 1) {
                    item {
                        Text(
                            text = "2. المبلغ والهدف",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                    
                    item {
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { 
                                amount = it
                                if (currentStep == 1 && amount.isNotEmpty() && targetId.isNotEmpty()) {
                                    currentStep = 2
                                }
                            },
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
                            onValueChange = { 
                                targetId = it
                                if (currentStep == 1 && amount.isNotEmpty() && targetId.isNotEmpty()) {
                                    currentStep = 2
                                }
                            },
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
                }

                // Step 3: Recurrence
                if (currentStep >= 2) {
                    item {
                        Text(
                            text = "3. التكرار",
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
                                    onClick = { 
                                        recurrenceType = type
                                        if (currentStep == 2) currentStep = 3
                                    }
                                )
                            }
                        }
                    }
                }

                // Step 4: Schedule and Description
                if (currentStep >= 3) {
                    item {
                        Text(
                            text = "4. الجدولة والوصف",
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
                            onClick = { createOperation() },
                            modifier = Modifier.weight(1f),
                            enabled = isValid && !isCreating,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00D632)
                            )
                        ) {
                            if (isCreating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White
                                )
                            } else {
                                Text("إنشاء العملية", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OperationTypeCard(
    operationType: OperationType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF00D632).copy(alpha = 0.1f) else Color.White
        ),
        border = if (isSelected) BorderStroke(2.dp, Color(0xFF00D632)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = operationType.emoji,
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = operationType.displayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color(0xFF00D632) else Color.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = operationType.description,
                fontSize = 10.sp,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
fun RecurrenceTypeCard(
    recurrenceType: RecurrenceType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF00D632).copy(alpha = 0.1f) else Color.White
        ),
        border = if (isSelected) BorderStroke(2.dp, Color(0xFF00D632)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = recurrenceType.emoji,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = recurrenceType.displayName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color(0xFF00D632) else Color.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = recurrenceType.description,
                fontSize = 9.sp,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}