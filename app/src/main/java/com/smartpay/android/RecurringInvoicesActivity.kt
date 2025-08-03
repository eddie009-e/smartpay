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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.smartpay.models.*
import com.smartpay.repository.RecurringInvoiceRepository
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

class RecurringInvoicesActivity : ComponentActivity() {

    private val recurringInvoiceRepository = RecurringInvoiceRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get token from secure storage
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val securePrefs = EncryptedSharedPreferences.create(
            "SmartPaySecurePrefs",
            masterKey,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val token = securePrefs.getString("token", null) ?: ""

        if (token.isEmpty()) {
            Toast.makeText(this, "الجلسة غير صالحة، يرجى تسجيل الدخول", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            RecurringInvoicesScreen(
                onBack = { finish() },
                repository = recurringInvoiceRepository
            )
        }
    }
}

@Composable
fun RecurringInvoicesScreen(
    onBack: () -> Unit,
    repository: RecurringInvoiceRepository
) {
    val context = LocalContext.current
    var recurringInvoices by remember { mutableStateOf<List<RecurringInvoice>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    fun loadRecurringInvoices() {
        isLoading = true
        errorMessage = null
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val response = repository.getRecurringInvoices()
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.success) {
                        recurringInvoices = body.recurringInvoices ?: emptyList()
                    } else {
                        errorMessage = body.message ?: "فشل في تحميل الفواتير المتكررة"
                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    errorMessage = "فشل في تحميل الفواتير المتكررة"
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                errorMessage = "خطأ في الاتصال: ${e.message}"
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    fun cancelRecurringInvoice(invoice: RecurringInvoice) {
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val response = repository.cancelRecurringInvoice(invoice.id)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.success) {
                        Toast.makeText(context, body.message ?: "تم إلغاء الفاتورة المتكررة", Toast.LENGTH_SHORT).show()
                        loadRecurringInvoices() // Refresh list
                    } else {
                        Toast.makeText(context, body.message ?: "فشل في إلغاء الفاتورة", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "فشل في إلغاء الفاتورة", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        loadRecurringInvoices()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .padding(top = 40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.Black)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "🔄 الفواتير المتكررة",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { loadRecurringInvoices() },
                    enabled = !isLoading
                ) {
                    Icon(
                        Icons.Default.Refresh, 
                        contentDescription = "تحديث", 
                        tint = Color(0xFF00D632)
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF00D632))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "جاري تحميل الفواتير المتكررة...",
                            fontSize = 16.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("❌", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "خطأ في تحميل البيانات",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE53E3E)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            errorMessage!!,
                            fontSize = 14.sp,
                            color = Color(0xFF666666),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { loadRecurringInvoices() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D632))
                        ) {
                            Text("إعادة المحاولة", color = Color.White)
                        }
                    }
                }
            } else if (recurringInvoices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔄", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "لا توجد فواتير متكررة",
                            fontSize = 18.sp,
                            color = Color(0xFF666666)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "أنشئ فاتورة متكررة لتوفير الوقت والجهد",
                            fontSize = 14.sp,
                            color = Color(0xFF999999),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D632)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إنشاء فاتورة متكررة", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الفواتير النشطة",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "${recurringInvoices.size} فاتورة",
                                fontSize = 14.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    }

                    // Recurring Invoice Items
                    items(recurringInvoices) { invoice ->
                        RecurringInvoiceCard(
                            invoice = invoice,
                            onCancel = { cancelRecurringInvoice(invoice) }
                        )
                    }
                }

                // Add Button (Floating)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    FloatingActionButton(
                        onClick = { showCreateDialog = true },
                        containerColor = Color(0xFF00D632),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "إنشاء فاتورة متكررة",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }

    // Create Dialog
    if (showCreateDialog) {
        CreateRecurringInvoiceDialog(
            onDismiss = { showCreateDialog = false },
            onSuccess = {
                showCreateDialog = false
                loadRecurringInvoices()
            },
            repository = repository
        )
    }
}

@Composable
fun RecurringInvoiceCard(
    invoice: RecurringInvoice,
    onCancel: () -> Unit
) {
    val statusColor = Color(RecurringInvoice.getStatusColor(invoice.isActive, invoice.nextRunDate))
    val nextRunDays = calculateDaysUntilNextRun(invoice.nextRunDate)
    
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Status Indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(statusColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Column {
                        Text(
                            text = "${RecurringInvoice.getRecurrenceEmoji(invoice.recurrenceInterval)} ${RecurringInvoice.getRecurrenceDisplayName(invoice.recurrenceInterval)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        if (!invoice.customerName.isNullOrEmpty()) {
                            Text(
                                text = "العميل: ${invoice.customerName}",
                                fontSize = 12.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                }
                
                Text(
                    text = formatAmount(invoice.amount),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00D632)
                )
            }

            // Description
            if (!invoice.description.isNullOrEmpty()) {
                Text(
                    text = invoice.description,
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Next Run Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "التشغيل التالي",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = formatDate(invoice.nextRunDate),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                    if (nextRunDays >= 0) {
                        Text(
                            text = when (nextRunDays) {
                                0L -> "اليوم"
                                1L -> "غداً"
                                else -> "خلال $nextRunDays أيام"
                            },
                            fontSize = 12.sp,
                            color = statusColor
                        )
                    }
                }

                if (invoice.isActive) {
                    OutlinedButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE53E3E)),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إلغاء", fontSize = 12.sp)
                    }
                } else {
                    Text(
                        text = "ملغي",
                        fontSize = 12.sp,
                        color = Color(0xFF666666),
                        modifier = Modifier
                            .background(
                                Color(0xFF666666).copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

fun calculateDaysUntilNextRun(nextRunDate: String): Long {
    return try {
        val nextDate = LocalDate.parse(nextRunDate)
        val today = LocalDate.now()
        ChronoUnit.DAYS.between(today, nextDate)
    } catch (e: Exception) {
        999L
    }
}

fun formatDate(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString)
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale("ar"))
        date.format(formatter)
    } catch (e: Exception) {
        dateString
    }
}

fun formatAmount(amount: BigDecimal): String {
    val formatter = NumberFormat.getNumberInstance(Locale("ar"))
    return "${formatter.format(amount)} ل.س"
}