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
import com.smartpay.repository.TransactionCategoryRepository
import kotlinx.coroutines.launch

class ManageTransactionCategoriesActivity : ComponentActivity() {

    private val categoryRepository = TransactionCategoryRepository()

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
        if (!TransactionCategory.hasFeatureAccess(subscriptionPlan)) {
            Toast.makeText(this, TransactionCategory.getUpgradeMessage(), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContent {
            ManageTransactionCategoriesScreen(
                onBack = { finish() },
                repository = categoryRepository
            )
        }
    }
}

@Composable
fun ManageTransactionCategoriesScreen(
    onBack: () -> Unit,
    repository: TransactionCategoryRepository
) {
    val context = LocalContext.current
    var categories by remember { mutableStateOf<List<TransactionCategory>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<TransactionCategory?>(null) }

    fun loadCategories() {
        isLoading = true
        errorMessage = null
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val response = repository.getCategories()
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.success) {
                        categories = body.categories ?: emptyList()
                    } else {
                        errorMessage = body.message ?: "فشل في تحميل التصنيفات"
                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    errorMessage = "فشل في تحميل التصنيفات"
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

    fun deleteCategory(category: TransactionCategory) {
        isLoading = true
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val response = repository.deleteCategory(category.id)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.success) {
                        Toast.makeText(context, body.message ?: "تم حذف التصنيف بنجاح", Toast.LENGTH_SHORT).show()
                        loadCategories() // Refresh list
                    } else {
                        Toast.makeText(context, body.message ?: "فشل في حذف التصنيف", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "فشل في حذف التصنيف", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadCategories()
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
                    text = "🧷 إدارة التصنيفات",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { loadCategories() },
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
                            "جاري تحميل التصنيفات...",
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
                            onClick = { loadCategories() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D632))
                        ) {
                            Text("إعادة المحاولة", color = Color.White)
                        }
                    }
                }
            } else if (categories.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🧷", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "لا توجد تصنيفات",
                            fontSize = 18.sp,
                            color = Color(0xFF666666)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "أنشئ تصنيفات مخصصة لتنظيم معاملاتك المالية",
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
                            Text("إضافة تصنيف جديد", color = Color.White, fontWeight = FontWeight.Bold)
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
                                text = "التصنيفات المتاحة",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "${categories.size} تصنيف",
                                fontSize = 14.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    }

                    // Category Items
                    items(categories) { category ->
                        TransactionCategoryCard(
                            category = category,
                            onEdit = { editingCategory = category },
                            onDelete = { deleteCategory(category) }
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
                            contentDescription = "إضافة تصنيف جديد",
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
        CreateOrEditCategoryDialog(
            category = null,
            onDismiss = { showCreateDialog = false },
            onSuccess = {
                showCreateDialog = false
                loadCategories()
            },
            repository = repository
        )
    }

    // Edit Dialog
    editingCategory?.let { category ->
        CreateOrEditCategoryDialog(
            category = category,
            onDismiss = { editingCategory = null },
            onSuccess = {
                editingCategory = null
                loadCategories()
            },
            repository = repository
        )
    }
}

@Composable
fun TransactionCategoryCard(
    category: TransactionCategory,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryColor = try {
        Color(category.color.toColorInt())
    } catch (e: Exception) {
        Color(0xFF666666)
    }
    
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color Badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(categoryColor, CircleShape)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.nameAr.firstOrNull()?.toString() ?: "؟",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Category Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.nameAr,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = category.nameEn,
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = TransactionCategory.getColorNameAr(category.color),
                    fontSize = 12.sp,
                    color = categoryColor
                )
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
    }
}