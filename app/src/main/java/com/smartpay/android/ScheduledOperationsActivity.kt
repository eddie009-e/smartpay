package com.smartpay.android

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.smartpay.data.network.ApiService
import com.smartpay.models.*
import com.smartpay.repositories.ScheduledOperationRepository
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ScheduledOperationsActivity : ComponentActivity() {

    private lateinit var repository: ScheduledOperationRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get token and subscription plan from secure storage
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val securePrefs = EncryptedSharedPreferences.create(
            "SmartPaySecurePrefs",
            masterKey,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val token = securePrefs.getString("token", null) ?: ""
        val subscriptionPlan = securePrefs.getString("subscriptionPlan", "Free") ?: "Free"

        if (token.isEmpty()) {
            Toast.makeText(this, "الجلسة غير صالحة، يرجى تسجيل الدخول", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Check subscription plan access (Pro only)
        if (!ScheduledOperation.hasFeatureAccess(subscriptionPlan)) {
            Toast.makeText(this, ScheduledOperation.getUpgradeMessage(), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Initialize API service and repository
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.smartpay.sy/") // Replace with actual base URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        repository = ScheduledOperationRepository(apiService)

        setContent {
            ScheduledOperationsScreen(
                repository = repository,
                onBack = { finish() }
            )
        }
    }
}

@Composable
fun ScheduledOperationsScreen(
    repository: ScheduledOperationRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var operations by remember { mutableStateOf<List<ScheduledOperation>>(emptyList()) }
    var stats by remember { mutableStateOf<ScheduledOperationStats?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedOperation by remember { mutableStateOf<ScheduledOperation?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Load operations and stats
    fun loadData() {
        isLoading = true
        errorMessage = null
        
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                // Load operations
                val operationsResult = repository.getScheduledOperations(
                    status = selectedFilter,
                    limit = 100
                )
                if (operationsResult.isSuccess) {
                    operations = operationsResult.getOrNull()?.scheduledOperations ?: emptyList()
                } else {
                    errorMessage = operationsResult.exceptionOrNull()?.message
                }

                // Load stats
                val statsResult = repository.getScheduledOperationStats()
                if (statsResult.isSuccess) {
                    stats = statsResult.getOrNull()
                }
            } catch (e: Exception) {
                errorMessage = "خطأ في الشبكة: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Delete operation
    fun cancelOperation(operation: ScheduledOperation) {
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val result = repository.cancelScheduledOperation(operation.id)
                if (result.isSuccess) {
                    Toast.makeText(context, "تم إلغاء العملية المجدولة بنجاح", Toast.LENGTH_SHORT).show()
                    loadData()
                } else {
                    Toast.makeText(context, result.exceptionOrNull()?.message ?: "فشل في إلغاء العملية", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Toggle operation status
    fun toggleOperationStatus(operation: ScheduledOperation) {
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val newStatus = if (operation.status == "active") "paused" else "active"
                val result = repository.toggleOperationStatus(operation.id, newStatus == "active")
                if (result.isSuccess) {
                    val action = if (newStatus == "active") "تفعيل" else "إيقاف"
                    Toast.makeText(context, "تم $action العملية المجدولة بنجاح", Toast.LENGTH_SHORT).show()
                    loadData()
                } else {
                    Toast.makeText(context, result.exceptionOrNull()?.message ?: "فشل في تغيير حالة العملية", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Execute operation manually
    fun executeOperation(operation: ScheduledOperation) {
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val result = repository.executeScheduledOperation(operation.id)
                if (result.isSuccess) {
                    Toast.makeText(context, "تم تنفيذ العملية بنجاح", Toast.LENGTH_SHORT).show()
                    loadData()
                } else {
                    Toast.makeText(context, result.exceptionOrNull()?.message ?: "فشل في تنفيذ العملية", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Load data on first run
    LaunchedEffect(selectedFilter) {
        loadData()
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
                    text = "📅 الجدولة التلقائية",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.weight(1f))
                
                // Refresh Button
                IconButton(
                    onClick = { loadData() },
                    enabled = !isLoading
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "تحديث",
                        tint = Color(0xFF00D632)
                    )
                }
            }

            // Stats Cards
            stats?.let { operationStats ->
                StatsCards(stats = operationStats)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Filter Row
            FilterRow(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )

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
                                "جاري تحميل العمليات المجدولة...",
                                fontSize = 16.sp,
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
                                onClick = { loadData() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D632))
                            ) {
                                Text("إعادة المحاولة", color = Color.White)
                            }
                        }
                    }
                }
                
                operations.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📅", fontSize = 64.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "لا توجد عمليات مجدولة",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF666666)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (selectedFilter != null) "لا توجد عمليات للفلتر المحدد"
                                else "أنشئ عمليات تلقائية للرواتب والفواتير والتحويلات",
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
                                Text("إنشاء عملية مجدولة", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                else -> {
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
                                    text = "العمليات المجدولة",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = "${operations.size} عملية",
                                    fontSize = 14.sp,
                                    color = Color(0xFF666666)
                                )
                            }
                        }

                        // Operations List
                        items(operations) { operation ->
                            OperationCard(
                                operation = operation,
                                onEdit = {
                                    selectedOperation = operation
                                    showEditDialog = true
                                },
                                onDelete = {
                                    selectedOperation = operation
                                    showDeleteDialog = true
                                },
                                onToggleStatus = { toggleOperationStatus(operation) },
                                onExecute = { executeOperation(operation) }
                            )
                        }
                    }

                    // FAB
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FloatingActionButton(
                            onClick = { 
                                if (stats?.isAtCapacity() == true) {
                                    Toast.makeText(context, "تم الوصول للحد الأقصى من العمليات المجدولة (${stats?.maxOperations})", Toast.LENGTH_LONG).show()
                                } else {
                                    showCreateDialog = true
                                }
                            },
                            containerColor = Color(0xFF00D632),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "إنشاء عملية مجدولة",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCreateDialog) {
        CreateScheduledOperationDialog(
            repository = repository,
            onDismiss = { showCreateDialog = false },
            onSuccess = {
                showCreateDialog = false
                loadData()
            }
        )
    }

    if (showEditDialog && selectedOperation != null) {
        EditScheduledOperationDialog(
            repository = repository,
            operation = selectedOperation!!,
            onDismiss = { 
                showEditDialog = false
                selectedOperation = null
            },
            onSuccess = {
                showEditDialog = false
                selectedOperation = null
                loadData()
            }
        )
    }

    if (showDeleteDialog && selectedOperation != null) {
        DeleteScheduledOperationDialog(
            operation = selectedOperation!!,
            onDismiss = { 
                showDeleteDialog = false
                selectedOperation = null
            },
            onConfirm = {
                cancelOperation(selectedOperation!!)
                showDeleteDialog = false
                selectedOperation = null
            }
        )
    }
}

@Composable
fun StatsCards(stats: ScheduledOperationStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // First Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard(
                    title = "إجمالي العمليات",
                    value = stats.totalOperations.toString(),
                    subtitle = "من ${stats.maxOperations}",
                    color = Color(0xFF2196F3),
                    icon = "📅"
                )
                StatCard(
                    title = "العمليات النشطة",
                    value = stats.activeOperations.toString(),
                    subtitle = "${stats.getActivePercentage().toInt()}%",
                    color = Color(0xFF00D632),
                    icon = "✅"
                )
                StatCard(
                    title = "المستحقة قريباً",
                    value = stats.dueSoon.toString(),
                    subtitle = "خلال 24 ساعة",
                    color = Color(0xFFFF9800),
                    icon = "⏰"
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Second Row - Operation Types
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard(
                    title = "فواتير",
                    value = stats.invoiceOperations.toString(),
                    subtitle = "عملية",
                    color = Color(0xFF2196F3),
                    icon = "💳"
                )
                StatCard(
                    title = "رواتب",
                    value = stats.salaryOperations.toString(),
                    subtitle = "عملية",
                    color = Color(0xFF00D632),
                    icon = "💰"
                )
                StatCard(
                    title = "تحويلات",
                    value = stats.transferOperations.toString(),
                    subtitle = "عملية",
                    color = Color(0xFF9C27B0),
                    icon = "🔄"
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    icon: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Text(
            text = icon,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = title,
            fontSize = 9.sp,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            fontSize = 8.sp,
            color = Color(0xFF999999),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun FilterRow(
    selectedFilter: String?,
    onFilterSelected: (String?) -> Unit
) {
    LazyRow(
        modifier = Modifier.padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                text = "الكل",
                isSelected = selectedFilter == null,
                onClick = { onFilterSelected(null) }
            )
        }
        item {
            FilterChip(
                text = "نشط",
                isSelected = selectedFilter == "active",
                onClick = { onFilterSelected("active") }
            )
        }
        item {
            FilterChip(
                text = "متوقف",
                isSelected = selectedFilter == "paused",
                onClick = { onFilterSelected("paused") }
            )
        }
        item {
            FilterChip(
                text = "ملغي",
                isSelected = selectedFilter == "cancelled",
                onClick = { onFilterSelected("cancelled") }
            )
        }
    }
}

@Composable
fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) Color(0xFF00D632) else Color(0xFFF7F8FA)
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
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFF666666)
        )
    }
}

@Composable
fun OperationCard(
    operation: ScheduledOperation,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: () -> Unit,
    onExecute: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
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
                // Operation Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = operation.getOperationTypeEmoji(),
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = operation.getOperationTypeDisplay(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = operation.getFormattedAmount(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(operation.getOperationTypeColor())
                        )
                    }
                }
                
                // Status and Priority
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    // Status
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(operation.getStatusColor()), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            operation.getStatusDisplay(),
                            fontSize = 11.sp,
                            color = Color(operation.getStatusColor())
                        )
                    }
                    
                    // Priority
                    if (operation.getPriorityLevel() != "منخفض") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(operation.getPriorityColor()).copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = operation.getPriorityLevel(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(operation.getPriorityColor())
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${operation.getRecurrenceEmoji()} ${operation.getRecurrenceDisplay()}",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = "التنفيذ التالي: ${operation.getFormattedNextRun()}",
                        fontSize = 11.sp,
                        color = Color(0xFF666666)
                    )
                }
                
                Text(
                    text = operation.getTimeUntilNextRun(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (operation.isDueSoon()) Color(0xFFE53E3E) else Color(0xFF666666)
                )
            }
            
            // Description if available
            if (!operation.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = operation.description,
                    fontSize = 12.sp,
                    color = Color(0xFF666666),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Execute Button (only for active operations)
                if (operation.isActive()) {
                    OutlinedButton(
                        onClick = onExecute,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF2196F3)
                        )
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("تنفيذ", fontSize = 10.sp)
                    }
                }
                
                // Toggle Status Button
                OutlinedButton(
                    onClick = onToggleStatus,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (operation.isActive()) Color(0xFFFF9800) else Color(0xFF00D632)
                    )
                ) {
                    Icon(
                        if (operation.isActive()) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        if (operation.isActive()) "إيقاف" else "تفعيل",
                        fontSize = 10.sp
                    )
                }
                
                // Edit Button
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF666666)
                    )
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("تعديل", fontSize = 10.sp)
                }
                
                // Delete Button
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFE53E3E)
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("حذف", fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun DeleteScheduledOperationDialog(
    operation: ScheduledOperation,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = Color(0xFFE53E3E),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "إلغاء العملية المجدولة",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        },
        text = {
            Column {
                Text(
                    "هل أنت متأكد من إلغاء العملية المجدولة التالية؟",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(operation.getOperationTypeEmoji(), fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = operation.getOperationTypeDisplay(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = operation.getFormattedAmount(),
                                    fontSize = 12.sp,
                                    color = Color(0xFF666666)
                                )
                                Text(
                                    text = operation.getRecurrenceDisplay(),
                                    fontSize = 11.sp,
                                    color = Color(0xFF999999)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFE53E3E),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "سيتم إلغاء جميع التنفيذات المستقبلية لهذه العملية.",
                            fontSize = 12.sp,
                            color = Color(0xFFE53E3E)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53E3E))
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("إلغاء العملية", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("تراجع", color = Color(0xFF666666))
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White
    )
}