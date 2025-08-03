package com.smartpay.android

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.smartpay.models.*
import com.smartpay.repository.ReminderRepository
import kotlinx.coroutines.launch

class AutomaticRemindersActivity : ComponentActivity() {

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

        // Check subscription plan access (Standard/Pro only)
        if (!AutomaticReminder.hasFeatureAccess(subscriptionPlan)) {
            Toast.makeText(this, AutomaticReminder.getUpgradeMessage(), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContent {
            AutomaticRemindersScreen(
                onBack = { finish() },
                repository = reminderRepository
            )
        }
    }
}

@Composable
fun AutomaticRemindersScreen(
    onBack: () -> Unit,
    repository: ReminderRepository
) {
    val context = LocalContext.current
    var reminders by remember { mutableStateOf<List<AutomaticReminder>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<AutomaticReminder?>(null) }
    var selectedTab by remember { mutableStateOf("upcoming") } // upcoming, past, all

    fun loadReminders() {
        isLoading = true
        errorMessage = null
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val response = repository.getReminders()
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
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
            } catch (e: Exception) {
                errorMessage = "خطأ في الاتصال: ${e.message}"
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    fun deleteReminder(reminder: AutomaticReminder) {
        isLoading = true
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val response = repository.deleteReminder(reminder.id)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.success) {
                        Toast.makeText(context, body.message ?: "تم حذف التذكير بنجاح", Toast.LENGTH_SHORT).show()
                        loadReminders() // Refresh list
                    } else {
                        Toast.makeText(context, body.message ?: "فشل في حذف التذكير", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "فشل في حذف التذكير", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    // Filter reminders based on selected tab
    val filteredReminders = when (selectedTab) {
        "upcoming" -> reminders.filter { !it.isSent && !AutomaticReminder.isOverdue(it.isSent, it.scheduledAt) }
        "past" -> reminders.filter { it.isSent || AutomaticReminder.isOverdue(it.isSent, it.scheduledAt) }
        else -> reminders
    }

    LaunchedEffect(Unit) {
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
                    text = "⏰ التذكيرات التلقائية",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.weight(1f))
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

            // Tab Row
            if (reminders.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = when (selectedTab) {
                        "upcoming" -> 0
                        "past" -> 1
                        else -> 2
                    },
                    containerColor = Color.White,
                    contentColor = Color(0xFF00D632),
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Tab(
                        selected = selectedTab == "upcoming",
                        onClick = { selectedTab = "upcoming" },
                        text = { Text("القادمة") }
                    )
                    Tab(
                        selected = selectedTab == "past",
                        onClick = { selectedTab = "past" },
                        text = { Text("السابقة") }
                    )
                    Tab(
                        selected = selectedTab == "all",
                        onClick = { selectedTab = "all" },
                        text = { Text("الكل") }
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
                            "جاري تحميل التذكيرات...",
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
                            onClick = { loadReminders() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D632))
                        ) {
                            Text("إعادة المحاولة", color = Color.White)
                        }
                    }
                }
            } else if (filteredReminders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⏰", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            when (selectedTab) {
                                "upcoming" -> "لا توجد تذكيرات قادمة"
                                "past" -> "لا توجد تذكيرات سابقة"
                                else -> "لا توجد تذكيرات"
                            },
                            fontSize = 18.sp,
                            color = Color(0xFF666666)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "أنشئ تذكيرات ذكية للفواتير والمدفوعات",
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
                            Text("إضافة تذكير جديد", color = Color.White, fontWeight = FontWeight.Bold)
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
                                text = when (selectedTab) {
                                    "upcoming" -> "التذكيرات القادمة"
                                    "past" -> "التذكيرات السابقة"
                                    else -> "جميع التذكيرات"
                                },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "${filteredReminders.size} تذكير",
                                fontSize = 14.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    }

                    // Reminder Items
                    items(filteredReminders) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onEdit = { editingReminder = reminder },
                            onDelete = { deleteReminder(reminder) }
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
                            contentDescription = "إضافة تذكير جديد",
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
        CreateOrEditReminderDialog(
            reminder = null,
            onDismiss = { showCreateDialog = false },
            onSuccess = {
                showCreateDialog = false
                loadReminders()
            },
            repository = repository
        )
    }

    // Edit Dialog
    editingReminder?.let { reminder ->
        CreateOrEditReminderDialog(
            reminder = reminder,
            onDismiss = { editingReminder = null },
            onSuccess = {
                editingReminder = null
                loadReminders()
            },
            repository = repository
        )
    }
}

@Composable
fun ReminderCard(
    reminder: AutomaticReminder,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = Color(AutomaticReminder.getStatusColor(reminder.isSent, reminder.scheduledAt))
    
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
                            text = reminder.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = reminder.getReminderTypeDisplay(),
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
                
                // Action Buttons
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "تعديل",
                            tint = Color(0xFF2196F3),
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

            // Message
            if (!reminder.message.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = reminder.message,
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Schedule Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "موعد التذكير",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = AutomaticReminder.formatScheduledDate(reminder.scheduledAt),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                    Text(
                        text = reminder.getTimeUntilReminder(),
                        fontSize = 12.sp,
                        color = statusColor
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = reminder.getRecurrenceDisplay(),
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = AutomaticReminder.getStatusDisplay(reminder.isSent, reminder.scheduledAt),
                        fontSize = 12.sp,
                        color = statusColor,
                        modifier = Modifier
                            .background(
                                statusColor.copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}