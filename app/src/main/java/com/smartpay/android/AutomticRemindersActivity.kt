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
            ModernAutomaticRemindersScreen(
                onBack = { finish() },
                repository = reminderRepository
            )
        }
    }
}

@Composable
fun ModernAutomaticRemindersScreen(
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FDED))
    ) {
        // Background elements
        Box(
            modifier = Modifier
                .size(120.dp)
                .offset(x = 250.dp, y = 150.dp)
                .background(
                    Color(0xFFD8FBA9).copy(alpha = 0.1f),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(80.dp)
                .offset(x = 50.dp, y = 400.dp)
                .background(
                    Color(0xFF2D2D2D).copy(alpha = 0.05f),
                    CircleShape
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Modern Header
            ModernRemindersHeader(
                onBack = onBack,
                onRefresh = { loadReminders() },
                isLoading = isLoading
            )

            // Tab Row
            if (reminders.isNotEmpty()) {
                ModernTabRow(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }

            when {
                isLoading -> ModernLoadingState()
                errorMessage != null -> ModernErrorState(
                    errorMessage = errorMessage!!,
                    onRetry = { loadReminders() }
                )
                filteredReminders.isEmpty() -> ModernEmptyRemindersState(
                    selectedTab = selectedTab,
                    onCreateClick = { showCreateDialog = true }
                )
                else -> ModernRemindersList(
                    reminders = filteredReminders,
                    onEdit = { editingReminder = it },
                    onDelete = { deleteReminder(it) }
                )
            }
        }

        // Floating Action Button
        if (filteredReminders.isNotEmpty()) {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = Color(0xFFD8FBA9),
                contentColor = Color.Black,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
                    .size(56.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "إضافة تذكير جديد",
                    modifier = Modifier.size(24.dp)
                )
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
fun ModernRemindersHeader(
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .padding(top = 40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FDED)),
                onClick = onBack
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "رجوع",
                        tint = Color(0xFF2D2D2D),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text("⏰", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "التذكيرات التلقائية",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D2D2D)
                )
            }

            Card(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FDED)),
                onClick = onRefresh
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color(0xFFD8FBA9),
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "تحديث",
                            tint = Color(0xFFD8FBA9),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernTabRow(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ModernTabButton(
                text = "القادمة",
                isSelected = selectedTab == "upcoming",
                onClick = { onTabSelected("upcoming") }
            )
            ModernTabButton(
                text = "السابقة",
                isSelected = selectedTab == "past",
                onClick = { onTabSelected("past") }
            )
            ModernTabButton(
                text = "الكل",
                isSelected = selectedTab == "all",
                onClick = { onTabSelected("all") }
            )
        }
    }
}

@Composable
fun ModernTabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFD8FBA9) else Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 12.dp),
            textAlign = TextAlign.Center,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.Black else Color(0xFF6B7280),
            fontSize = 14.sp
        )
    }
}

@Composable
fun ModernLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(40.dp)
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFD8FBA9),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "جاري تحميل التذكيرات...",
                    fontSize = 16.sp,
                    color = Color(0xFF6B7280),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ModernErrorState(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(40.dp)
            ) {
                Text("❌", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "خطأ في تحميل البيانات",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D2D2D)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    errorMessage,
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD8FBA9)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("إعادة المحاولة", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ModernEmptyRemindersState(
    selectedTab: String,
    onCreateClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(40.dp)
            ) {
                Text("⏰", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    when (selectedTab) {
                        "upcoming" -> "لا توجد تذكيرات قادمة"
                        "past" -> "لا توجد تذكيرات سابقة"
                        else -> "لا توجد تذكيرات"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D2D2D)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "أنشئ تذكيرات ذكية للفواتير والمدفوعات",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onCreateClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD8FBA9)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إضافة تذكير جديد", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ModernRemindersList(
    reminders: List<AutomaticReminder>,
    onEdit: (AutomaticReminder) -> Unit,
    onDelete: (AutomaticReminder) -> Unit
) {
    LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    text = "التذكيرات النشطة",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D2D2D)
                )
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD8FBA9).copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "${reminders.size} تذكير",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2D2D2D),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Reminder Items
        items(reminders) { reminder ->
            ModernReminderCard(
                reminder = reminder,
                onEdit = { onEdit(reminder) },
                onDelete = { onDelete(reminder) }
            )
        }
    }
}

@Composable
fun ModernReminderCard(
    reminder: AutomaticReminder,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = Color(AutomaticReminder.getStatusColor(reminder.isSent, reminder.scheduledAt))
    
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Status Indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(statusColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = reminder.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D2D2D),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = reminder.getReminderTypeDisplay(),
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
                
                // Action Buttons
                Row {
                    Card(
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FDED)),
                        onClick = onEdit
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "تعديل",
                                tint = Color(0xFF2D2D2D),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Card(
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                        onClick = onDelete
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "حذف",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Message
            if (!reminder.message.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = reminder.message,
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                        color = Color(0xFF6B7280)
                    )
                    Text(
                        text = AutomaticReminder.formatScheduledDate(reminder.scheduledAt),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2D2D2D)
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
                        color = Color(0xFF6B7280)
                    )
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.1f))
                    ) {
                        Text(
                            text = AutomaticReminder.getStatusDisplay(reminder.isSent, reminder.scheduledAt),
                            fontSize = 12.sp,
                            color = statusColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}