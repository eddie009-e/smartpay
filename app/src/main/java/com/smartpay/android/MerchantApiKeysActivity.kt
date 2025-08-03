package com.smartpay.android

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.smartpay.repositories.MerchantApiKeyRepository
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MerchantApiKeysActivity : ComponentActivity() {

    private lateinit var repository: MerchantApiKeyRepository

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
        if (!MerchantApiKey.hasFeatureAccess(subscriptionPlan)) {
            Toast.makeText(this, MerchantApiKey.getUpgradeMessage(), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Initialize API service and repository
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.smartpay.sy/") // Replace with actual base URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        repository = MerchantApiKeyRepository(apiService)

        setContent {
            MerchantApiKeysScreen(
                repository = repository,
                onBack = { finish() }
            )
        }
    }
}

@Composable
fun MerchantApiKeysScreen(
    repository: MerchantApiKeyRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var apiKeys by remember { mutableStateOf<List<MerchantApiKey>>(emptyList()) }
    var stats by remember { mutableStateOf<ApiKeyStats?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var selectedKey by remember { mutableStateOf<MerchantApiKey?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var newlyCreatedKey by remember { mutableStateOf<MerchantApiKey?>(null) }

    // Load API keys and stats
    fun loadData() {
        isLoading = true
        errorMessage = null
        
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                // Load API keys
                val keysResult = repository.getApiKeys()
                if (keysResult.isSuccess) {
                    apiKeys = keysResult.getOrNull() ?: emptyList()
                } else {
                    errorMessage = keysResult.exceptionOrNull()?.message
                }

                // Load stats
                val statsResult = repository.getApiKeyStats()
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

    // Delete API key
    fun deleteApiKey(key: MerchantApiKey) {
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val result = repository.deleteApiKey(key.id)
                if (result.isSuccess) {
                    Toast.makeText(context, "تم حذف مفتاح API بنجاح", Toast.LENGTH_SHORT).show()
                    loadData()
                } else {
                    Toast.makeText(context, result.exceptionOrNull()?.message ?: "فشل في حذف مفتاح API", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Toggle API key status
    fun toggleKeyStatus(key: MerchantApiKey) {
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val result = repository.toggleApiKeyStatus(key.id, !key.isActive)
                if (result.isSuccess) {
                    val action = if (!key.isActive) "تفعيل" else "إلغاء تفعيل"
                    Toast.makeText(context, "تم $action مفتاح API بنجاح", Toast.LENGTH_SHORT).show()
                    loadData()
                } else {
                    Toast.makeText(context, result.exceptionOrNull()?.message ?: "فشل في تغيير حالة مفتاح API", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Load data on first run
    LaunchedEffect(Unit) {
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
                    text = "🔌 تكامل API",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.weight(1f))
                
                // Info Button
                IconButton(onClick = { showInfoDialog = true }) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "معلومات",
                        tint = Color(0xFF2196F3)
                    )
                }
                
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
            stats?.let { apiStats ->
                StatsCards(stats = apiStats)
                Spacer(modifier = Modifier.height(16.dp))
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
                                "جاري تحميل مفاتيح API...",
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
                
                apiKeys.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔌", fontSize = 64.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "لا توجد مفاتيح API",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF666666)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "أنشئ مفتاح API للتكامل مع الأنظمة الخارجية",
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
                                Text("إنشاء مفتاح API", color = Color.White, fontWeight = FontWeight.Bold)
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
                                    text = "مفاتيح API",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = "${apiKeys.size} مفتاح",
                                    fontSize = 14.sp,
                                    color = Color(0xFF666666)
                                )
                            }
                        }

                        // API Keys List
                        items(apiKeys) { apiKey ->
                            ApiKeyCard(
                                apiKey = apiKey,
                                onEdit = {
                                    selectedKey = apiKey
                                    showEditDialog = true
                                },
                                onDelete = {
                                    selectedKey = apiKey
                                    showDeleteDialog = true
                                },
                                onToggleStatus = { toggleKeyStatus(apiKey) },
                                onCopyKey = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("API Key", apiKey.maskedKey ?: "")
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "تم نسخ مفتاح API", Toast.LENGTH_SHORT).show()
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
                            onClick = { 
                                if (stats?.isAtCapacity() == true) {
                                    Toast.makeText(context, "تم الوصول للحد الأقصى من مفاتيح API (${stats?.maxKeys})", Toast.LENGTH_LONG).show()
                                } else {
                                    showCreateDialog = true
                                }
                            },
                            containerColor = Color(0xFF00D632),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "إنشاء مفتاح API",
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
        CreateApiKeyDialog(
            repository = repository,
            onDismiss = { showCreateDialog = false },
            onSuccess = { createdKey ->
                newlyCreatedKey = createdKey
                showCreateDialog = false
                loadData()
            }
        )
    }

    if (showInfoDialog) {
        ApiKeyInfoDialog(
            onDismiss = { showInfoDialog = false }
        )
    }

    if (showEditDialog && selectedKey != null) {
        EditApiKeyDialog(
            repository = repository,
            apiKey = selectedKey!!,
            onDismiss = { 
                showEditDialog = false
                selectedKey = null
            },
            onSuccess = {
                showEditDialog = false
                selectedKey = null
                loadData()
            }
        )
    }

    if (showDeleteDialog && selectedKey != null) {
        DeleteApiKeyDialog(
            apiKey = selectedKey!!,
            onDismiss = { 
                showDeleteDialog = false
                selectedKey = null
            },
            onConfirm = {
                deleteApiKey(selectedKey!!)
                showDeleteDialog = false
                selectedKey = null
            }
        )
    }

    // Show newly created key dialog
    newlyCreatedKey?.let { key ->
        ShowNewApiKeyDialog(
            apiKey = key,
            onDismiss = { newlyCreatedKey = null }
        )
    }
}

@Composable
fun StatsCards(stats: ApiKeyStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCard(
                title = "إجمالي المفاتيح",
                value = stats.totalKeys.toString(),
                subtitle = "من ${stats.maxKeys}",
                color = Color(0xFF2196F3),
                icon = "🔑"
            )
            StatCard(
                title = "المفاتيح النشطة",
                value = stats.activeKeys.toString(),
                subtitle = "${if (stats.totalKeys > 0) (stats.activeKeys * 100 / stats.totalKeys) else 0}%",
                color = Color(0xFF00D632),
                icon = "✅"
            )
            StatCard(
                title = "المُستخدمة",
                value = stats.usedKeys.toString(),
                subtitle = "${stats.getUsagePercentage().toInt()}%",
                color = Color(0xFF9C27B0),
                icon = "📊"
            )
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = icon,
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = title,
            fontSize = 10.sp,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            fontSize = 9.sp,
            color = Color(0xFF999999)
        )
    }
}

@Composable
fun ApiKeyCard(
    apiKey: MerchantApiKey,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: () -> Unit,
    onCopyKey: () -> Unit
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
                // API Key Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = apiKey.description,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = apiKey.maskedKey ?: "مفتاح مخفي",
                        fontSize = 12.sp,
                        color = Color(0xFF666666),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
                
                // Status Indicators
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    // Active Status
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(apiKey.getStatusColor()), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            apiKey.getStatusDisplay(),
                            fontSize = 11.sp,
                            color = Color(apiKey.getStatusColor())
                        )
                    }
                    
                    // Usage Status
                    Text(
                        apiKey.getUsageStatusDisplay(),
                        fontSize = 10.sp,
                        color = Color(apiKey.getUsageStatusColor())
                    )
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
                        text = "تم الإنشاء: ${apiKey.getFormattedCreatedDate()}",
                        fontSize = 10.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = "آخر استخدام: ${apiKey.getFormattedLastUsedDate()}",
                        fontSize = 10.sp,
                        color = Color(0xFF666666)
                    )
                }
                
                // Security Level
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(apiKey.getSecurityLevelColor()).copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = apiKey.getSecurityLevel(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(apiKey.getSecurityLevelColor())
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Copy Button
                OutlinedButton(
                    onClick = onCopyKey,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("نسخ", fontSize = 12.sp)
                }
                
                // Toggle Status Button
                OutlinedButton(
                    onClick = onToggleStatus,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (apiKey.isActive) Color(0xFF999999) else Color(0xFF00D632)
                    )
                ) {
                    Icon(
                        if (apiKey.isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (apiKey.isActive) "إيقاف" else "تفعيل",
                        fontSize = 12.sp
                    )
                }
                
                // Edit Button
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF2196F3)
                    )
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تعديل", fontSize = 12.sp)
                }
                
                // Delete Button
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFE53E3E)
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حذف", fontSize = 12.sp)
                }
            }
        }
    }
}