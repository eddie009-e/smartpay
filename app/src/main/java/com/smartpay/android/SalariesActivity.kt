package com.smartpay.android

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.smartpay.data.repository.MerchantRepository
import com.smartpay.models.SalaryPayment
import com.smartpay.models.SalaryPaymentRequest
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class SalariesActivity : ComponentActivity() {
    private val merchantRepository = MerchantRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get business ID and token from secure storage
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val securePrefs = EncryptedSharedPreferences.create(
            "SmartPaySecurePrefs",
            masterKey,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        
        val businessId = securePrefs.getString("user_id", null) ?: ""
        val token = securePrefs.getString("token", null) ?: ""
        
        if (businessId.isEmpty() || token.isEmpty()) {
            Toast.makeText(this, "خطأ في التحقق من هوية التاجر", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            ModernSalariesScreen(
                businessId = businessId,
                onBack = { finish() }
            )
        }
    }
}

@Composable
fun ModernSalariesScreen(
    businessId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val merchantRepository = MerchantRepository()
    
    var showDialog by remember { mutableStateOf(false) }
    var employeeId by remember { mutableStateOf("") }
    var employeeName by remember { mutableStateOf("") }
    var salaryAmount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var salaryPayments by remember { mutableStateOf(listOf<SalaryPayment>()) }
    var isLoading by remember { mutableStateOf(false) }

    fun refreshSalaries() {
        isLoading = true
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val response = merchantRepository.getSalaryPayments(businessId)
                if (response.isSuccessful && response.body() != null) {
                    salaryPayments = response.body()!!
                } else {
                    Toast.makeText(context, "فشل في جلب قائمة الرواتب", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(businessId) {
        refreshSalaries()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8FDED),
                        Color(0xFFF0F8E8)
                    )
                )
            )
    ) {
        // Animated Background Orbs
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = 280.dp, y = 100.dp)
                .background(
                    Color(0xFFD8FBA9).copy(alpha = 0.15f),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(150.dp)
                .offset(x = (-60).dp, y = 300.dp)
                .background(
                    Color.White.copy(alpha = 0.25f),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(120.dp)
                .offset(x = 250.dp, y = 600.dp)
                .background(
                    Color(0xFF2D2D2D).copy(alpha = 0.08f),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Modern Header
            ModernHeader(
                onBack = onBack,
                onRefresh = { refreshSalaries() },
                isLoading = isLoading,
                paymentsCount = salaryPayments.size
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Add Salary Button
            AddSalarySection(
                onAddSalary = { showDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Content
            when {
                isLoading -> {
                    LoadingSection()
                }
                
                salaryPayments.isEmpty() -> {
                    EmptyStateSection(
                        onAddSalary = { showDialog = true }
                    )
                }
                
                else -> {
                    SalariesList(
                        salaryPayments = salaryPayments
                    )
                }
            }
        }
    }

    // Add Salary Dialog
    if (showDialog) {
        ModernSalaryDialog(
            employeeId = employeeId,
            employeeName = employeeName,
            salaryAmount = salaryAmount,
            note = note,
            onEmployeeIdChange = { employeeId = it },
            onEmployeeNameChange = { employeeName = it },
            onSalaryAmountChange = { salaryAmount = it },
            onNoteChange = { note = it },
            onDismiss = { 
                showDialog = false
                employeeId = ""
                employeeName = ""
                salaryAmount = ""
                note = ""
            },
            onConfirm = {
                val amount = salaryAmount.toBigDecimalOrNull()
                if (employeeId.isNotBlank() && amount != null && amount > BigDecimal.ZERO) {
                    (context as ComponentActivity).lifecycleScope.launch {
                        try {
                            val request = SalaryPaymentRequest(
                                employeeId = employeeId,
                                businessId = businessId,
                                amount = amount,
                                note = note.ifBlank { null }
                            )
                            
                            val response = merchantRepository.createSalaryPayment(request)
                            if (response.isSuccessful) {
                                Toast.makeText(context, "تم دفع الراتب بنجاح", Toast.LENGTH_SHORT).show()
                                showDialog = false
                                employeeId = ""
                                employeeName = ""
                                salaryAmount = ""
                                note = ""
                                refreshSalaries()
                            } else {
                                Toast.makeText(context, "فشل في دفع الراتب", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "يرجى ملء جميع الحقول المطلوبة بشكل صحيح", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
private fun ModernHeader(
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    isLoading: Boolean,
    paymentsCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            Card(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFD8FBA9).copy(alpha = 0.2f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "رجوع",
                        tint = Color(0xFF2D2D2D),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Title Section
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = Color(0xFFD8FBA9),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "رواتب الموظفين",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D2D2D),
                        fontFamily = FontFamily.SansSerif
                    )
                }
                if (paymentsCount > 0) {
                    Text(
                        text = "$paymentsCount دفعة راتب",
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                }
            }

            // Refresh Button
            Card(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isLoading) Color(0xFFD8FBA9).copy(alpha = 0.3f) 
                                   else Color(0xFFD8FBA9).copy(alpha = 0.2f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                IconButton(
                    onClick = onRefresh,
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color(0xFFD8FBA9),
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "تحديث",
                            tint = Color(0xFFD8FBA9),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddSalarySection(
    onAddSalary: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFD8FBA9).copy(alpha = 0.2f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Payments,
                            contentDescription = null,
                            tint = Color(0xFFD8FBA9),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "إدارة الرواتب",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D2D2D)
                    )
                    Text(
                        text = "أضف وتتبع رواتب الموظفين",
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Button(
                onClick = onAddSalary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD8FBA9)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF2D2D2D)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "إضافة راتب جديد",
                        color = Color(0xFF2D2D2D),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingSection() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFD8FBA9),
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "جاري تحميل الرواتب...",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2D2D2D),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun EmptyStateSection(
    onAddSalary: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFD8FBA9).copy(alpha = 0.2f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = Color(0xFFD8FBA9),
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    "لا توجد رواتب مدفوعة",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D2D2D),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    "ابدأ بدفع أول راتب للموظفين",
                    fontSize = 16.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onAddSalary,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD8FBA9)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF2D2D2D),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "إضافة راتب",
                        color = Color(0xFF2D2D2D),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SalariesList(
    salaryPayments: List<SalaryPayment>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سجل المدفوعات",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D2D2D)
                )
                Text(
                    text = "${salaryPayments.size} دفعة",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
        }

        // Salary Payment Items
        items(salaryPayments) { payment ->
            ModernSalaryPaymentCard(payment = payment)
        }
    }
}

@Composable
fun ModernSalaryPaymentCard(payment: SalaryPayment) {
    val dateFormatter = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    val paymentDate = try {
        if (payment.paidAt.contains("T")) {
            // Parse ISO 8601 format
            val zonedDateTime = ZonedDateTime.parse(payment.paidAt)
            dateFormatter.format(Date.from(zonedDateTime.toInstant()))
        } else {
            payment.paidAt
        }
    } catch (e: Exception) {
        payment.paidAt
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box {
            // Subtle pattern
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .offset(x = 250.dp, y = (-40).dp)
                    .background(
                        Color(0xFFD8FBA9).copy(alpha = 0.05f),
                        CircleShape
                    )
            )

            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Employee Info
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFD8FBA9).copy(alpha = 0.2f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFFD8FBA9),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ID: ${payment.employeeId.take(8)}...", 
                                fontWeight = FontWeight.Bold, 
                                color = Color(0xFF2D2D2D),
                                fontSize = 16.sp
                            )
                            if (payment.note != null) {
                                Text(
                                    text = payment.note, 
                                    fontSize = 14.sp, 
                                    color = Color(0xFF666666),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = Color(0xFF999999),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = paymentDate, 
                                    fontSize = 12.sp, 
                                    color = Color(0xFF666666)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Amount and Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Amount
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFD8FBA9).copy(alpha = 0.15f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AttachMoney,
                                contentDescription = null,
                                tint = Color(0xFFD8FBA9),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${payment.amount} ل.س", 
                                color = Color(0xFF2D2D2D),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                    
                    // Status
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (payment.status) {
                                "completed" -> Color(0xFFD8FBA9).copy(alpha = 0.2f)
                                "pending" -> Color(0xFFFF9800).copy(alpha = 0.2f)
                                else -> Color(0xFFE53E3E).copy(alpha = 0.2f)
                            }
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                when (payment.status) {
                                    "completed" -> Icons.Default.CheckCircle
                                    "pending" -> Icons.Default.Schedule
                                    else -> Icons.Default.Error
                                },
                                contentDescription = null,
                                tint = when (payment.status) {
                                    "completed" -> Color(0xFFD8FBA9)
                                    "pending" -> Color(0xFFFF9800)
                                    else -> Color(0xFFE53E3E)
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (payment.status) {
                                    "completed" -> "مكتمل"
                                    "pending" -> "قيد الانتظار"
                                    else -> "فشل"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = when (payment.status) {
                                    "completed" -> Color(0xFFD8FBA9)
                                    "pending" -> Color(0xFFFF9800)
                                    else -> Color(0xFFE53E3E)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernSalaryDialog(
    employeeId: String,
    employeeName: String,
    salaryAmount: String,
    note: String,
    onEmployeeIdChange: (String) -> Unit,
    onEmployeeNameChange: (String) -> Unit,
    onSalaryAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFD8FBA9).copy(alpha = 0.15f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Payments,
                            contentDescription = null,
                            tint = Color(0xFFD8FBA9),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "دفع راتب",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF2D2D2D)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Employee ID
                ModernDialogTextField(
                    value = employeeId,
                    onValueChange = onEmployeeIdChange,
                    label = "معرف الموظف",
                    placeholder = "أدخل معرف الموظف",
                    icon = Icons.Default.Badge
                )
                
                // Employee Name (Optional)
                ModernDialogTextField(
                    value = employeeName,
                    onValueChange = onEmployeeNameChange,
                    label = "اسم الموظف (اختياري)",
                    placeholder = "اسم الموظف للعرض",
                    icon = Icons.Default.Person
                )

                // Salary Amount
                ModernDialogTextField(
                    value = salaryAmount,
                    onValueChange = onSalaryAmountChange,
                    label = "مبلغ الراتب",
                    placeholder = "أدخل المبلغ",
                    icon = Icons.Default.AttachMoney,
                    keyboardType = KeyboardType.Decimal
                )

                // Note
                ModernDialogTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    label = "ملاحظة (اختياري)",
                    placeholder = "راتب شهر...",
                    icon = Icons.Default.Note
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD8FBA9)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "دفع الراتب",
                    color = Color(0xFF2D2D2D),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFF666666)
                )
            ) {
                Text(
                    "إلغاء",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}

@Composable
private fun ModernDialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color(0xFFD8FBA9)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2D2D2D)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF8F9FA)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                textStyle = TextStyle(
                    fontSize = 16.sp, 
                    color = Color(0xFF2D2D2D),
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(Color(0xFFD8FBA9)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
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