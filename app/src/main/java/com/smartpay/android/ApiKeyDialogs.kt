package com.smartpay.android

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.smartpay.repositories.MerchantApiKeyRepository
import kotlinx.coroutines.launch

/**
 * Create API Key Dialog - Pro Plan Only
 * 
 * Allows Pro Plan merchants to create new API keys with:
 * - Description input with validation
 * - Integration type selection
 * - Usage instructions display
 * - Secure key generation
 */
@Composable
fun CreateApiKeyDialog(
    repository: MerchantApiKeyRepository,
    onDismiss: () -> Unit,
    onSuccess: (MerchantApiKey) -> Unit
) {
    val context = LocalContext.current
    var description by remember { mutableStateOf("") }
    var selectedIntegrationType by remember { mutableStateOf<ApiKeyIntegrationType?>(null) }
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun createApiKey() {
        if (description.isBlank()) {
            errorMessage = "يرجى إدخال وصف للمفتاح"
            return
        }

        isCreating = true
        errorMessage = null

        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val request = CreateApiKeyRequest(description.trim())
                val result = repository.createApiKey(request)
                if (result.isSuccess) {
                    val createdKey = result.getOrNull()!!
                    Toast.makeText(context, "تم إنشاء مفتاح API بنجاح", Toast.LENGTH_SHORT).show()
                    onSuccess(createdKey)
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "فشل في إنشاء مفتاح API"
                }
            } catch (e: Exception) {
                errorMessage = "خطأ: ${e.message}"
            } finally {
                isCreating = false
            }
        }
    }

    Dialog(onDismissRequest = { if (!isCreating) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "🔑 إنشاء مفتاح API جديد",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    if (!isCreating) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = Color(0xFF666666)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))

                if (isCreating) {
                    // Creating Progress
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF00D632))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "جاري إنشاء مفتاح API آمن...",
                                fontSize = 16.sp,
                                color = Color(0xFF666666),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "يرجى الانتظار",
                                fontSize = 12.sp,
                                color = Color(0xFF999999)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Description Input
                        item {
                            Column {
                                Text(
                                    text = "وصف المفتاح *",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                ApiKeyTextField(
                                    value = description,
                                    onValueChange = { 
                                        description = it
                                        errorMessage = null
                                    },
                                    placeholder = "مثال: تكامل نظام ERP الخاص بالمحاسبة",
                                    maxLength = 200
                                )
                                Text(
                                    text = "اختر وصفاً واضحاً يساعدك على تذكر الغرض من هذا المفتاح",
                                    fontSize = 12.sp,
                                    color = Color(0xFF666666),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        // Integration Type Selection
                        item {
                            Column {
                                Text(
                                    text = "نوع التكامل (اختياري)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                IntegrationTypeSelector(
                                    selectedType = selectedIntegrationType,
                                    onTypeSelected = { selectedIntegrationType = it }
                                )
                            }
                        }

                        // Security Information
                        item {
                            SecurityInfoCard()
                        }

                        // Usage Instructions Preview
                        item {
                            UsageInstructionsCard()
                        }
                    }

                    // Error Message
                    errorMessage?.let { error ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
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
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = error,
                                    fontSize = 14.sp,
                                    color = Color(0xFFE53E3E)
                                )
                            }
                        }
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء", color = Color(0xFF666666))
                        }

                        Button(
                            onClick = { createApiKey() },
                            enabled = description.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00D632),
                                disabledContainerColor = Color(0xFFE0E0E0)
                            )
                        ) {
                            Icon(
                                Icons.Default.VpnKey,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (description.isNotBlank()) Color.White else Color(0xFF999999)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "إنشاء المفتاح",
                                color = if (description.isNotBlank()) Color.White else Color(0xFF999999),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Show New API Key Dialog
 * 
 * Displays the newly created API key with copy functionality
 * This is the ONLY time the actual key is shown to the user
 */
@Composable
fun ShowNewApiKeyDialog(
    apiKey: MerchantApiKey,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    fun copyApiKey() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("SmartPay API Key", apiKey.apiKey ?: "")
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "تم نسخ مفتاح API بنجاح", Toast.LENGTH_SHORT).show()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header with Warning
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "🔑 مفتاح API الجديد",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Warning Message
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "تحذير أمني مهم",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9800)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "هذا المفتاح سيظهر مرة واحدة فقط. احفظه في مكان آمن قبل إغلاق هذه النافذة.",
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // API Key Display
                Column {
                    Text(
                        "مفتاح API:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                    ) {
                        SelectionContainer {
                            Text(
                                text = apiKey.apiKey ?: "خطأ في عرض المفتاح",
                                fontSize = 12.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = Color.Black,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Copy Button
                    Button(
                        onClick = { copyApiKey() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("نسخ مفتاح API", fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Usage Instructions
                Text(
                    "كيفية الاستخدام:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "أضف المفتاح في رأس طلبات HTTP:",
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "X-API-Key: ${apiKey.apiKey?.take(20)}...",
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = Color(0xFF333333)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "أو",
                            fontSize = 11.sp,
                            color = Color(0xFF666666)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Authorization: Bearer ${apiKey.apiKey?.take(20)}...",
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = Color(0xFF333333)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D632))
                ) {
                    Text("فهمت، أغلق النافذة", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Edit API Key Dialog
 * 
 * Allows editing API key description and status
 */
@Composable
fun EditApiKeyDialog(
    repository: MerchantApiKeyRepository,
    apiKey: MerchantApiKey,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    var description by remember { mutableStateOf(apiKey.description) }
    var isActive by remember { mutableStateOf(apiKey.isActive) }
    var isUpdating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun updateApiKey() {
        if (description.isBlank()) {
            errorMessage = "يرجى إدخال وصف للمفتاح"
            return
        }

        isUpdating = true
        errorMessage = null

        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val request = UpdateApiKeyRequest(
                    description = description.trim(),
                    isActive = isActive
                )
                val result = repository.updateApiKey(apiKey.id, request)
                if (result.isSuccess) {
                    Toast.makeText(context, "تم تحديث مفتاح API بنجاح", Toast.LENGTH_SHORT).show()
                    onSuccess()
                } else {
                    errorMessage = result.exceptionOrNull()?.message ?: "فشل في تحديث مفتاح API"
                }
            } catch (e: Exception) {
                errorMessage = "خطأ: ${e.message}"
            } finally {
                isUpdating = false
            }
        }
    }

    Dialog(onDismissRequest = { if (!isUpdating) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "✏️ تعديل مفتاح API",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    if (!isUpdating) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = Color(0xFF666666)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))

                if (isUpdating) {
                    // Updating Progress
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF00D632))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "جاري تحديث مفتاح API...",
                                fontSize = 14.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // API Key Info
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "المفتاح: ${apiKey.maskedKey}",
                                    fontSize = 12.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = Color(0xFF666666)
                                )
                                Text(
                                    "تم الإنشاء: ${apiKey.getFormattedCreatedDate()}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF999999)
                                )
                            }
                        }

                        // Description Input
                        Column {
                            Text(
                                text = "وصف المفتاح *",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            ApiKeyTextField(
                                value = description,
                                onValueChange = { 
                                    description = it
                                    errorMessage = null
                                },
                                placeholder = "وصف المفتاح",
                                maxLength = 200
                            )
                        }

                        // Status Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "حالة المفتاح",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = if (isActive) "نشط - يمكن استخدامه" else "غير نشط - معطل",
                                    fontSize = 12.sp,
                                    color = Color(0xFF666666)
                                )
                            }
                            Switch(
                                checked = isActive,
                                onCheckedChange = { isActive = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF00D632),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFF999999)
                                )
                            )
                        }
                    }

                    // Error Message
                    errorMessage?.let { error ->
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
                                    text = error,
                                    fontSize = 12.sp,
                                    color = Color(0xFFE53E3E)
                                )
                            }
                        }
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء", color = Color(0xFF666666))
                        }

                        Button(
                            onClick = { updateApiKey() },
                            enabled = description.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00D632),
                                disabledContainerColor = Color(0xFFE0E0E0)
                            )
                        ) {
                            Text(
                                "حفظ التغييرات",
                                color = if (description.isNotBlank()) Color.White else Color(0xFF999999),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Delete API Key Dialog
 * 
 * Confirmation dialog for API key deletion
 */
@Composable
fun DeleteApiKeyDialog(
    apiKey: MerchantApiKey,
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
                    text = "حذف مفتاح API",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        },
        text = {
            Column {
                Text(
                    "هل أنت متأكد من حذف مفتاح API التالي؟",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = apiKey.description,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = apiKey.maskedKey ?: "",
                            fontSize = 12.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = Color(0xFF666666)
                        )
                        Text(
                            text = "تم الإنشاء: ${apiKey.getFormattedCreatedDate()}",
                            fontSize = 11.sp,
                            color = Color(0xFF999999)
                        )
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
                            "لا يمكن التراجع عن هذا الإجراء. ستتوقف جميع التطبيقات التي تستخدم هذا المفتاح عن العمل.",
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
                Text("حذف نهائي", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = Color(0xFF666666))
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White
    )
}

/**
 * API Key Info Dialog
 * 
 * Shows general information about API keys and usage
 */
@Composable
fun ApiKeyInfoDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "ℹ️ معلومات تكامل API",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Color(0xFF666666)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // What is API Integration
                    item {
                        InfoSection(
                            title = "ما هو تكامل API؟",
                            content = "تكامل API يسمح للأنظمة الخارجية بالتواصل مع SmartPay لإجراء العمليات التجارية مثل إنشاء الفواتير، تتبع المدفوعات، وإدارة العملاء بشكل آلي."
                        )
                    }

                    // Common Use Cases
                    item {
                        InfoSection(
                            title = "الاستخدامات الشائعة",
                            content = listOf(
                                "🏢 أنظمة ERP للمحاسبة والمالية",
                                "👥 أنظمة CRM لإدارة العملاء",
                                "🛒 متاجر إلكترونية ومنصات البيع",
                                "📊 أدوات التحليل والتقارير",
                                "📱 تطبيقات الهاتف المخصصة"
                            )
                        )
                    }

                    // Security Best Practices
                    item {
                        InfoSection(
                            title = "الممارسات الأمنية",
                            content = listOf(
                                "🔒 احفظ مفاتيح API في مكان آمن",
                                "🚫 لا تشارك مفاتيح API مع أطراف خارجية",
                                "🔄 قم بتدوير المفاتيح بانتظام",
                                "📍 استخدم HTTPS فقط في الطلبات",
                                "⏸️ عطّل المفاتيح غير المستخدمة"
                            )
                        )
                    }

                    // API Limits
                    item {
                        InfoSection(
                            title = "حدود الاستخدام",
                            content = listOf(
                                "📊 الحد الأقصى: 10 مفاتيح API",
                                "⚡ معدل الطلبات: 1000 طلب/ساعة",
                                "📁 حجم البيانات: 1MB لكل طلب",
                                "🔄 مهلة الاستجابة: 30 ثانية"
                            )
                        )
                    }

                    // Support Information
                    item {
                        InfoSection(
                            title = "الدعم والتوثيق",
                            content = "للحصول على التوثيق الكامل لـ API وأمثلة البرمجة، يرجى زيارة مركز المطورين أو التواصل مع فريق الدعم الفني."
                        )
                    }
                }

                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D632))
                ) {
                    Text("فهمت", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Helper Composables

@Composable
fun ApiKeyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    maxLength: Int = 200
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFFF7F8FA), shape = RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = { if (it.length <= maxLength) onValueChange(it) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(
                fontSize = 14.sp,
                color = Color.Black
            ),
            singleLine = true,
            cursorBrush = SolidColor(Color(0xFF00D632)),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = Color(0xFF999999)
                        )
                    )
                }
                innerTextField()
            }
        )
    }
}

@Composable
fun IntegrationTypeSelector(
    selectedType: ApiKeyIntegrationType?,
    onTypeSelected: (ApiKeyIntegrationType?) -> Unit
) {
    val types = ApiKeyIntegrationType.getAllTypes()
    
    LazyColumn(
        modifier = Modifier.height(200.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(types) { type ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .selectable(
                        selected = selectedType == type,
                        onClick = { 
                            onTypeSelected(if (selectedType == type) null else type)
                        }
                    )
                    .background(
                        if (selectedType == type) Color(0xFF00D632).copy(alpha = 0.1f) 
                        else Color.Transparent
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedType == type,
                    onClick = { 
                        onTypeSelected(if (selectedType == type) null else type)
                    },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Color(0xFF00D632)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(type.emoji, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = type.displayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                    Text(
                        text = type.description,
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }
            }
        }
    }
}

@Composable
fun SecurityInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFF00D632),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "معلومات الأمان",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00D632)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "سيتم إنشاء مفتاح API آمن بطول 256 بت. احفظه في مكان آمن فلن يظهر مرة أخرى.",
                fontSize = 12.sp,
                color = Color(0xFF666666)
            )
        }
    }
}

@Composable
fun UsageInstructionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Code,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "مثال الاستخدام",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "curl -H \"X-API-Key: your_key_here\" \\\n  https://api.smartpay.sy/v1/invoices",
                fontSize = 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = Color(0xFF333333)
            )
        }
    }
}

@Composable
fun InfoSection(
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                fontSize = 12.sp,
                color = Color(0xFF666666),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun InfoSection(
    title: String,
    content: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            content.forEach { item ->
                Text(
                    text = item,
                    fontSize = 12.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}