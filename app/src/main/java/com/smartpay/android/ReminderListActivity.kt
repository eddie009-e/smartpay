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
import com.smartpay.repository.ReminderRepository
import com.smartpay.dialogs.CreateOrEditReminderDialog
import kotlinx.coroutines.launch

class ReminderListActivity : ComponentActivity() {

    private val reminderRepository = ReminderRepository()

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
        val subscriptionPlan = securePrefs.getString("subscriptionPlan", "Free") ?: "Free"

        if (token.isEmpty()) {
            Toast.makeText(this, "الجلسة غير صالحة، يرجى تسجيل الدخول", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Check subscription plan access (Pro only)
        if (!Reminder.hasFeatureAccess(subscriptionPlan)) {
            Toast.makeText(this, Reminder.getUpgradeMessage(), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContent {
            ReminderListScreen(
                onBack = { finish() },
                repository = reminderRepository
            )
        }
    }
}

@Composable
fun ReminderListScreen(
    onBack: () -> Unit,
    repository: ReminderRepository
) {
    val context = LocalContext.current
    var reminders by remember { mutableStateOf<List<Reminder>>(emptyList()) }
    var stats by remember { mutableStateOf<ReminderStats?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedReminder by remember { mutableStateOf<Reminder?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(ReminderFilter.ALL) }

    fun loadReminders() {
        isLoading = true
        errorMessage = null
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val status = if (selectedFilter == ReminderFilter.ALL) null else selectedFilter.value
                val remindersResponse = repository.getReminders(status = status)
                val statsResponse = repository.getReminderStats()

                if (remindersResponse.isSuccessful && remindersResponse.body() != null) {
                    val body = remindersResponse.body()!!
                    if (body.success) {
                        reminders = body.reminders ?: emptyList()
                    } else {
                        errorMessage = body.message ?: "فشل في تحميل التذكيرات"
                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    errorMessage = "فشل في تحميل التذكيرات"
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                }

                if (statsResponse.isSuccessful && statsResponse.body() != null) {
                    val statsBody = statsResponse.body()!!
                    if (statsBody.success) {
                        stats = statsBody.stats
                    }
                }

            } catch (e: Exception) {
                errorMessage = "خطأ في الاتصال: ${e.message}"
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    fun deleteReminder(reminder: Reminder) {
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val response = repository.deleteReminder(reminder.id)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.success) {
                        Toast.makeText(context, "تم حذف التذكير بنجاح", Toast.LENGTH_SHORT).show()
                        loadReminders()
                    } else {
                        Toast.makeText(context, body.message ?: "فشل في حذف التذكير", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "فشل في حذف التذكير", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(selectedFilter) {
        loadReminders()
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
                    text = "🔔 التذكيرات التلقائية",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.weight(1f))
                
                // Refresh Button
                IconButton(
                    onClick = { loadReminders() },
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
            stats?.let { statsData ->
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    items(listOf(
                        Pair("الإجمالي", statsData.totalReminders) to Color(0xFF2196F3),
                        Pair("القادمة", statsData.upcomingReminders) to Color(0xFF00D632),
                        Pair("المتأخرة", statsData.pastReminders - statsData.sentReminders) to Color(0xFFFF9800),
                        Pair("المرسلة", statsData.sentReminders) to Color(0xFF9C27B0)
                    )) { (data, color) ->
                        Card(
                            modifier = Modifier.width(100.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = data.second.toString(),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = color
                                )
                                Text(
                                    text = data.first,
                                    fontSize = 12.sp,
                                    color = Color(0xFF666666),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Filter Tabs
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(ReminderFilter.getAllOptions()) { filter ->
                    FilterChip(
                        onClick = { selectedFilter = filter },
                        label = { Text("${filter.emoji} ${filter.displayName}") },
                        selected = selectedFilter == filter,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00D632).copy(alpha = 0.2f)
                        )
                    )
                }
            }

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
                                "جاري تحميل التذكيرات...",
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
                                onClick = { loadReminders() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D632))
                            ) {
                                Text("إعادة المحاولة", color = Color.White)
                            }
                        }
                    }
                }
                
                reminders.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔔", fontSize = 64.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "لا توجد تذكيرات",
                                fontSize = 18.sp,
                                color = Color(0xFF666666)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "أنشئ تذكيراً جديداً لتتبع مهامك ومتابعاتك",
                                fontSize = 14.sp,
                                color = Color(0xFF999999),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D632)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إنشاء تذكير", color = Color.White, fontWeight = FontWeight.Bold)
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
                                    text = "التذكيرات",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = "${reminders.size} تذكير",
                                    fontSize = 14.sp,
                                    color = Color(0xFF666666)
                                )
                            }
                        }

                        // Reminder Items
                        items(reminders) { reminder ->
                            ReminderCard(
                                reminder = reminder,
                                onEdit = {
                                    selectedReminder = reminder
                                    showEditDialog = true
                                },
                                onDelete = {
                                    selectedReminder = reminder
                                    showDeleteDialog = true
                                }
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
                            onClick = { showAddDialog = true },
                            containerColor = Color(0xFF00D632),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "إضافة تذكير",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Dialog
    if (showAddDialog) {
        CreateOrEditReminderDialog(
            onDismiss = { showAddDialog = false },
            onSuccess = {
                showAddDialog = false
                loadReminders()
            },
            repository = repository
        )
    }

    // Edit Dialog
    if (showEditDialog && selectedReminder != null) {
        CreateOrEditReminderDialog(
            reminder = selectedReminder,
            onDismiss = { 
                showEditDialog = false
                selectedReminder = null
            },
            onSuccess = {
                showEditDialog = false
                selectedReminder = null
                loadReminders()
            },
            repository = repository
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog && selectedReminder != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false
                selectedReminder = null
            },
            title = { Text("حذف التذكير") },
            text = { 
                Text("هل أنت متأكد من حذف التذكير \"${selectedReminder!!.title}\"؟ لا يمكن التراجع عن هذا الإجراء.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteReminder(selectedReminder!!)
                        showDeleteDialog = false
                        selectedReminder = null
                    }
                ) {
                    Text("حذف", color = Color(0xFFE53E3E))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDeleteDialog = false
                        selectedReminder = null
                    }
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun ReminderCard(
    reminder: Reminder,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val statusInfo = reminder.getStatusDisplay()
    
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Color(statusInfo.color).copy(alpha = 0.1f),
                            CircleShape
                        )
                        .border(
                            2.dp,
                            Color(statusInfo.color).copy(alpha = 0.3f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = reminder.getReminderTypeIcon(),
                        fontSize = 18.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Reminder Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!reminder.description.isNullOrEmpty()) {
                        Text(
                            text = reminder.description,
                            fontSize = 14.sp,
                            color = Color(0xFF666666),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Actions
                Row {
                    IconButton(onClick = onEdit, enabled = !reminder.isSent) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "تعديل",
                            tint = if (reminder.isSent) Color(0xFFCCCCCC) else Color(0xFF2196F3),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "حذف",
                            tint = Color(0xFFE53E3E),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF666666),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = reminder.getFormattedScheduledDate(),
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    }
                    if (reminder.isRecurring) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = reminder.getRecurrenceDisplay(),
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = statusInfo.text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(statusInfo.color)
                    )
                    Text(
                        text = reminder.getTimeUntilReminder(),
                        fontSize = 11.sp,
                        color = Color(0xFF999999)
                    )
                }
            }
        }
    }
}