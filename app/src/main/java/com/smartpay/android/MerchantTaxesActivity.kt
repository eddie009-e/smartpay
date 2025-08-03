package com.smartpay.android

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.smartpay.data.network.ApiService
import com.smartpay.models.*
import com.smartpay.repositories.MerchantTaxRepository
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MerchantTaxesActivity : ComponentActivity() {

    private lateinit var repository: MerchantTaxRepository

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
        if (!MerchantTax.hasFeatureAccess(subscriptionPlan)) {
            Toast.makeText(this, MerchantTax.getUpgradeMessage(), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Initialize API service and repository
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.smartpay.sy/") // Replace with actual base URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)
        repository = MerchantTaxRepository(apiService)

        setContent {
            MerchantTaxesScreen(
                repository = repository,
                onBack = { finish() }
            )
        }
    }
}

@Composable
fun MerchantTaxesScreen(
    repository: MerchantTaxRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var taxes by remember { mutableStateOf<List<MerchantTax>>(emptyList()) }
    var stats by remember { mutableStateOf<TaxStats?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedTax by remember { mutableStateOf<MerchantTax?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Load taxes and stats
    fun loadData() {
        isLoading = true
        errorMessage = null
        
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                // Load taxes
                val taxesResult = repository.getMerchantTaxes()
                if (taxesResult.isSuccess) {
                    taxes = taxesResult.getOrNull() ?: emptyList()
                } else {
                    errorMessage = taxesResult.exceptionOrNull()?.message
                }

                // Load stats
                val statsResult = repository.getTaxStats()
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

    // Delete tax
    fun deleteTax(tax: MerchantTax) {
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val result = repository.deleteMerchantTax(tax.id)
                if (result.isSuccess) {
                    Toast.makeText(context, "تم حذف الضريبة بنجاح", Toast.LENGTH_SHORT).show()
                    loadData()
                } else {
                    Toast.makeText(context, result.exceptionOrNull()?.message ?: "فشل في حذف الضريبة", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Set as default tax
    fun setAsDefault(tax: MerchantTax) {
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val result = repository.setAsDefaultTax(tax.id)
                if (result.isSuccess) {
                    Toast.makeText(context, "تم تعيين الضريبة كافتراضية بنجاح", Toast.LENGTH_SHORT).show()
                    loadData()
                } else {
                    Toast.makeText(context, result.exceptionOrNull()?.message ?: "فشل في تعيين الضريبة الافتراضية", Toast.LENGTH_SHORT).show()
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
                    text = "🧾 إدارة الضرائب",
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
            stats?.let { taxStats ->
                TaxStatsCards(stats = taxStats)
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
                                "جاري تحميل إعدادات الضرائب...",
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
                
                taxes.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🧾", fontSize = 64.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "لا توجد ضرائب",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF666666)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "أنشئ ضرائب مخصصة لفواتيرك وخدماتك",
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
                                Text("إنشاء ضريبة جديدة", color = Color.White, fontWeight = FontWeight.Bold)
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
                                    text = "الضرائب المسجلة",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = "${taxes.size} ضريبة",
                                    fontSize = 14.sp,
                                    color = Color(0xFF666666)
                                )
                            }
                        }

                        // Tax Templates (for new users)
                        if (taxes.isEmpty()) {
                            item {
                                TaxTemplatesSection(
                                    onTemplateSelected = { template ->
                                        showCreateDialog = true
                                        // Pre-fill with template values
                                    }
                                )
                            }
                        }

                        // Taxes List
                        items(taxes) { tax ->
                            TaxCard(
                                tax = tax,
                                onEdit = {
                                    selectedTax = tax
                                    showEditDialog = true
                                },
                                onDelete = {
                                    selectedTax = tax
                                    showDeleteDialog = true
                                },
                                onSetAsDefault = { setAsDefault(tax) }
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
                            onClick = { showCreateDialog = true },
                            containerColor = Color(0xFF00D632),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "إنشاء ضريبة جديدة",
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
        CreateTaxDialog(
            repository = repository,
            onDismiss = { showCreateDialog = false },
            onSuccess = {
                showCreateDialog = false
                loadData()
            }
        )
    }

    if (showEditDialog && selectedTax != null) {
        EditTaxDialog(
            repository = repository,
            tax = selectedTax!!,
            onDismiss = { 
                showEditDialog = false
                selectedTax = null
            },
            onSuccess = {
                showEditDialog = false
                selectedTax = null
                loadData()
            }
        )
    }

    if (showDeleteDialog && selectedTax != null) {
        DeleteTaxDialog(
            tax = selectedTax!!,
            onDismiss = { 
                showDeleteDialog = false
                selectedTax = null
            },
            onConfirm = {
                deleteTax(selectedTax!!)
                showDeleteDialog = false
                selectedTax = null
            }
        )
    }
}

@Composable
fun TaxStatsCards(stats: TaxStats) {
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
                TaxStatCard(
                    title = "إجمالي الضرائب",
                    value = stats.totalTaxes.toString(),
                    subtitle = if (stats.hasDefaultTax()) "يوجد افتراضي" else "لا يوجد افتراضي",
                    color = Color(0xFF2196F3),
                    icon = "🧾"
                )
                TaxStatCard(
                    title = "المتوسط",
                    value = stats.getFormattedAverageTaxRate(),
                    subtitle = "معدل الضريبة",
                    color = Color(0xFF00D632),
                    icon = "📊"
                )
                TaxStatCard(
                    title = "هذا الشهر",
                    value = stats.invoicesWithTaxThisMonth.toString(),
                    subtitle = "فاتورة بضريبة",
                    color = Color(0xFFFF9800),
                    icon = "📅"
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Second Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TaxStatCard(
                    title = "الأعلى",
                    value = stats.getFormattedHighestTaxRate(),
                    subtitle = "أعلى معدل",
                    color = Color(0xFFE53E3E),
                    icon = "⬆️"
                )
                TaxStatCard(
                    title = "الأقل",
                    value = stats.getFormattedLowestTaxRate(),
                    subtitle = "أقل معدل",
                    color = Color(0xFF9C27B0),
                    icon = "⬇️"
                )
                TaxStatCard(
                    title = "المجموع",
                    value = stats.getFormattedTotalTaxCollectedThisMonth(),
                    subtitle = "هذا الشهر",
                    color = Color(0xFF607D8B),
                    icon = "💰"
                )
            }
        }
    }
}

@Composable
fun TaxStatCard(
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
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
fun TaxTemplatesSection(
    onTemplateSelected: (SyrianTaxTemplate) -> Unit
) {
    Column {
        Text(
            text = "قوالب الضرائب الشائعة",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SyrianTaxTemplate.getAllTemplates()) { template ->
                TaxTemplateCard(
                    template = template,
                    onClick = { onTemplateSelected(template) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun TaxTemplateCard(
    template: SyrianTaxTemplate,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = template.getFormattedRate(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(TaxCategory.fromRate(template.rate).color)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = template.displayName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = template.description,
                fontSize = 10.sp,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TaxCard(
    tax: MerchantTax,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetAsDefault: () -> Unit
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
                // Tax Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(tax.getTaxCategoryColor()).copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🧾",
                            fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = tax.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            if (tax.isDefault) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF00D632), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "افتراضي",
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Text(
                            text = tax.getFormattedRate(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(tax.getTaxCategoryColor())
                        )
                    }
                }
                
                // Usage Info
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = tax.getUsageStatus(),
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = tax.getTaxCategory(),
                        fontSize = 10.sp,
                        color = Color(tax.getTaxCategoryColor())
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "تاريخ الإنشاء: ${tax.getFormattedCreatedDate()}",
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
                
                if (tax.invoiceCount > 0) {
                    Text(
                        text = "مستخدم في ${tax.invoiceCount} فاتورة",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF00D632)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Set as Default Button (only if not already default)
                if (!tax.isDefault) {
                    OutlinedButton(
                        onClick = onSetAsDefault,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF00D632)
                        )
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("افتراضي", fontSize = 10.sp)
                    }
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
                
                // Delete Button (only if not used in invoices)
                if (tax.canBeDeleted()) {
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
                } else {
                    OutlinedButton(
                        onClick = { /* Disabled */ },
                        modifier = Modifier.weight(1f),
                        enabled = false,
                        contentPadding = PaddingValues(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("مقفل", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CreateTaxDialog(
    repository: MerchantTaxRepository,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    
    var name by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isValid = name.trim().isNotEmpty() && 
                  rate.isNotEmpty() && 
                  rate.toDoubleOrNull()?.let { it >= 0 && it <= 100 } == true

    fun createTax() {
        if (!isValid) return
        
        isCreating = true
        errorMessage = null
        
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val request = CreateMerchantTaxRequest(
                    name = name.trim(),
                    rate = rate.toDouble(),
                    isDefault = isDefault
                )
                
                val result = repository.createMerchantTax(request)
                if (result.isSuccess) {
                    Toast.makeText(context, "تم إنشاء الضريبة بنجاح", Toast.LENGTH_SHORT).show()
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = Color(0xFF00D632),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "إنشاء ضريبة جديدة",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Tax Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        errorMessage = null
                    },
                    label = { Text("اسم الضريبة") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00D632),
                        focusedLabelColor = Color(0xFF00D632)
                    ),
                    placeholder = { Text("مثال: ضريبة القيمة المضافة") }
                )
                
                // Tax Rate
                OutlinedTextField(
                    value = rate,
                    onValueChange = { 
                        if (it.isEmpty() || (it.toDoubleOrNull()?.let { rate -> rate >= 0 && rate <= 100 } == true)) {
                            rate = it
                            errorMessage = null
                        }
                    },
                    label = { Text("نسبة الضريبة (%)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00D632),
                        focusedLabelColor = Color(0xFF00D632)
                    ),
                    placeholder = { Text("مثال: 11.00") },
                    suffix = { Text("%") }
                )
                
                // Default Tax Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isDefault = !isDefault }
                ) {
                    Checkbox(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF00D632)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تعيين كضريبة افتراضية",
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }
                
                // Error message
                if (errorMessage != null) {
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
        },
        confirmButton = {
            Button(
                onClick = { createTax() },
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
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إنشاء الضريبة", color = Color.White, fontWeight = FontWeight.Bold)
                }
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

@Composable
fun EditTaxDialog(
    repository: MerchantTaxRepository,
    tax: MerchantTax,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    
    var name by remember { mutableStateOf(tax.name) }
    var rate by remember { mutableStateOf(tax.rate.toString()) }
    var isDefault by remember { mutableStateOf(tax.isDefault) }
    var isUpdating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isValid = name.trim().isNotEmpty() && 
                  rate.isNotEmpty() && 
                  rate.toDoubleOrNull()?.let { it >= 0 && it <= 100 } == true

    fun updateTax() {
        if (!isValid) return
        
        isUpdating = true
        errorMessage = null
        
        (context as ComponentActivity).lifecycleScope.launch {
            try {
                val request = UpdateMerchantTaxRequest(
                    name = name.trim(),
                    rate = rate.toDouble(),
                    isDefault = isDefault
                )
                
                val result = repository.updateMerchantTax(tax.id, request)
                if (result.isSuccess) {
                    Toast.makeText(context, "تم تحديث الضريبة بنجاح", Toast.LENGTH_SHORT).show()
                    onSuccess()
                } else {
                    errorMessage = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                errorMessage = "خطأ: ${e.message}"
            } finally {
                isUpdating = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color(0xFF00D632),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تعديل الضريبة",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Current Tax Info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "الضريبة الحالية",
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                        Text(
                            text = "${tax.name} - ${tax.getFormattedRate()}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = tax.getUsageStatus(),
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
                
                // Tax Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        errorMessage = null
                    },
                    label = { Text("اسم الضريبة") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00D632),
                        focusedLabelColor = Color(0xFF00D632)
                    )
                )
                
                // Tax Rate
                OutlinedTextField(
                    value = rate,
                    onValueChange = { 
                        if (it.isEmpty() || (it.toDoubleOrNull()?.let { rate -> rate >= 0 && rate <= 100 } == true)) {
                            rate = it
                            errorMessage = null
                        }
                    },
                    label = { Text("نسبة الضريبة (%)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00D632),
                        focusedLabelColor = Color(0xFF00D632)
                    ),
                    suffix = { Text("%") }
                )
                
                // Default Tax Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isDefault = !isDefault }
                ) {
                    Checkbox(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF00D632)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تعيين كضريبة افتراضية",
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }
                
                // Error message
                if (errorMessage != null) {
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
        },
        confirmButton = {
            Button(
                onClick = { updateTax() },
                enabled = isValid && !isUpdating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00D632)
                )
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White
                    )
                } else {
                    Text("حفظ التغييرات", color = Color.White, fontWeight = FontWeight.Bold)
                }
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

@Composable
fun DeleteTaxDialog(
    tax: MerchantTax,
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
                    text = "حذف الضريبة",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        },
        text = {
            Column {
                Text(
                    "هل أنت متأكد من حذف الضريبة التالية؟",
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
                            text = tax.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = tax.getFormattedRate(),
                            fontSize = 14.sp,
                            color = Color(tax.getTaxCategoryColor())
                        )
                        Text(
                            text = tax.getUsageStatus(),
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
                
                if (!tax.canBeDeleted()) {
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
                                "لا يمكن حذف هذه الضريبة لأنها مستخدمة في ${tax.invoiceCount} فاتورة.",
                                fontSize = 12.sp,
                                color = Color(0xFFE53E3E)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = tax.canBeDeleted(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53E3E))
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("حذف الضريبة", color = Color.White, fontWeight = FontWeight.Bold)
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

// Extension function for SyrianTaxTemplate
fun SyrianTaxTemplate.getFormattedRate(): String {
    return MerchantTax.formatTaxRate(rate)
}