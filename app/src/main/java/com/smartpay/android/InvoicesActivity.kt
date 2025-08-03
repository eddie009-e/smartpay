package com.smartpay.android

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.smartpay.android.R
import com.smartpay.android.ui.theme.SmartPayTheme
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.smartpay.data.models.InvoiceRequest
import com.smartpay.data.repository.UserRepository
import com.smartpay.data.network.ApiService
import com.smartpay.models.*
import com.smartpay.repositories.MerchantTaxRepository
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class InvoicesActivity : ComponentActivity() {

    private val userRepository = UserRepository()
    private lateinit var taxRepository: MerchantTaxRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get secure preferences
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
        val userType = securePrefs.getString("userType", "personal") ?: "personal"

        // Initialize tax repository for Pro merchants
        if (MerchantTax.hasFeatureAccess(subscriptionPlan) && (userType == "business" || userType == "merchant")) {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.smartpay.sy/") // Replace with actual base URL
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val apiService = retrofit.create(ApiService::class.java)
            taxRepository = MerchantTaxRepository(apiService)
        }

        setContent {
            SmartPayTheme {
                ModernInvoicesScreen(
                    token = token,
                    subscriptionPlan = subscriptionPlan,
                    userType = userType,
                    taxRepository = if (MerchantTax.hasFeatureAccess(subscriptionPlan) && (userType == "business" || userType == "merchant")) taxRepository else null
                )
            }
        }
    }
}

@Composable
fun ModernInvoicesScreen(
    token: String,
    subscriptionPlan: String,
    userType: String,
    taxRepository: MerchantTaxRepository?
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val userRepository = UserRepository()
    
    var showInvoiceDialog by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedTax by remember { mutableStateOf<MerchantTax?>(null) }
    var taxes by remember { mutableStateOf<List<MerchantTax>>(emptyList()) }
    var showTaxDropdown by remember { mutableStateOf(false) }

    // Load taxes for Pro merchants
    LaunchedEffect(Unit) {
        if (taxRepository != null) {
            try {
                val result = taxRepository.getMerchantTaxes()
                if (result.isSuccess) {
                    taxes = result.getOrNull() ?: emptyList()
                    // Auto-select default tax if exists
                    selectedTax = taxes.firstOrNull { it.isDefault }
                }
            } catch (e: Exception) {
                // Silently handle error - taxes will remain empty
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colorResource(R.color.background_light),
                        colorResource(R.color.background_secondary)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Bar Spacer
            Spacer(modifier = Modifier.height(32.dp))

            // Header Section
            HeaderSection()

            Spacer(modifier = Modifier.height(32.dp))

            // Welcome Section
            WelcomeSection()

            Spacer(modifier = Modifier.height(32.dp))

            // Create Invoice Button
            CreateInvoiceButton(
                onClick = { showInvoiceDialog = true }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Invoice Actions Section
            InvoiceActionsSection()
            
            Spacer(modifier = Modifier.height(40.dp))
        }

    }

    if (showInvoiceDialog) {
        ModernInvoiceCreationDialog(
            phone = phone,
            onPhoneChange = { phone = it },
            amount = amount,
            onAmountChange = { amount = it },
            description = description,
            onDescriptionChange = { description = it },
            taxes = taxes,
            selectedTax = selectedTax,
            onTaxSelected = { selectedTax = it },
            showTaxDropdown = showTaxDropdown,
            onShowTaxDropdownChange = { showTaxDropdown = it },
            subscriptionPlan = subscriptionPlan,
            userType = userType,
            onDismiss = { showInvoiceDialog = false },
            onConfirm = {
                val parsedAmount = amount.toDoubleOrNull()
                if (parsedAmount == null || phone.isBlank()) {
                    Toast.makeText(context, "يرجى إدخال بيانات صحيحة", Toast.LENGTH_SHORT).show()
                    return@ModernInvoiceCreationDialog
                }

                // Calculate tax amount
                val taxAmount = selectedTax?.calculateTaxFor(parsedAmount) ?: 0.0
                val totalAmount = parsedAmount + taxAmount

                val invoiceRequest = InvoiceRequest(
                    toPhone = phone,
                    amount = totalAmount.toLong(),
                    description = description
                    // Note: Add tax_id and tax_amount fields to InvoiceRequest model
                )

                (context as ComponentActivity).lifecycleScope.launch {
                    try {
                        val response = userRepository.createInvoice(token, invoiceRequest)
                        if (response.isSuccessful) {
                            Toast.makeText(context, "تم إرسال الفاتورة بنجاح", Toast.LENGTH_SHORT).show()
                            showInvoiceDialog = false
                            phone = ""
                            amount = ""
                            description = ""
                            selectedTax = taxes.firstOrNull { it.isDefault }
                        } else {
                            Toast.makeText(context, "فشل إرسال الفاتورة", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
    
    // New Modern Components
    @Composable
    fun HeaderSection() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Card(
                modifier = Modifier
                    .size(48.dp)
                    .clickable { (context as? ComponentActivity)?.finish() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(R.color.white_transparent_80)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "رجوع",
                        modifier = Modifier.size(24.dp),
                        tint = colorResource(R.color.primary_dark)
                    )
                }
            }
            
            Text(
                text = "الفواتير",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.primary_dark)
            )
            
            Spacer(modifier = Modifier.width(48.dp))
        }
    }

    @Composable
    fun WelcomeSection() {
        Card(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorResource(R.color.primary_green)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Receipt,
                    contentDescription = "الفواتير",
                    modifier = Modifier.size(60.dp),
                    tint = colorResource(R.color.primary_dark)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "إدارة الفواتير",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.primary_dark),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "أنشئ وارسل فواتيرك بسهولة",
            fontSize = 16.sp,
            color = colorResource(R.color.gray_dark),
            textAlign = TextAlign.Center
        )
    }

    @Composable
    fun CreateInvoiceButton(onClick: () -> Unit) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.primary_green)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "إصدار فاتورة جديدة",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }

    @Composable
    fun InvoiceActionsSection() {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sent Invoices
            InvoiceActionCard(
                icon = Icons.Default.CallMade,
                title = "الفواتير المرسلة",
                subtitle = "عرض الفواتير التي أرسلتها",
                onClick = {
                    Toast.makeText(context, "قيد التطوير", Toast.LENGTH_SHORT).show()
                }
            )
            
            // Received Invoices
            InvoiceActionCard(
                icon = Icons.Default.CallReceived,
                title = "الفواتير المستلمة",
                subtitle = "عرض الفواتير التي استلمتها",
                onClick = {
                    Toast.makeText(context, "قيد التطوير", Toast.LENGTH_SHORT).show()
                }
            )
            
            // Recurring Invoices (if Pro merchant)
            if (MerchantTax.hasFeatureAccess(subscriptionPlan) && (userType == "business" || userType == "merchant")) {
                InvoiceActionCard(
                    icon = Icons.Default.Repeat,
                    title = "الفواتير المتكررة",
                    subtitle = "إعداد فواتير تلقائية",
                    onClick = {
                        Toast.makeText(context, "قيد التطوير", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    @Composable
    fun InvoiceActionCard(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        title: String,
        subtitle: String,
        onClick: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorResource(R.color.white_transparent_80)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorResource(R.color.primary_green).copy(alpha = 0.1f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = colorResource(R.color.primary_green)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.primary_dark)
                    )
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = colorResource(R.color.gray_dark)
                    )
                }
                
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colorResource(R.color.gray_medium)
                )
            }
        }
    }
}

@Composable
fun ModernInvoiceCreationDialog(
    phone: String,
    onPhoneChange: (String) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    taxes: List<MerchantTax>,
    selectedTax: MerchantTax?,
    onTaxSelected: (MerchantTax?) -> Unit,
    showTaxDropdown: Boolean,
    onShowTaxDropdownChange: (Boolean) -> Unit,
    subscriptionPlan: String,
    userType: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val isProMerchant = MerchantTax.hasFeatureAccess(subscriptionPlan) && (userType == "business" || userType == "merchant")
    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
    val taxAmount = selectedTax?.calculateTaxFor(parsedAmount) ?: 0.0
    val totalAmount = parsedAmount + taxAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Card(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorResource(R.color.primary_green).copy(alpha = 0.1f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Receipt,
                            contentDescription = null,
                            tint = colorResource(R.color.primary_green),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "إصدار فاتورة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = colorResource(R.color.primary_dark)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Phone Number
                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = { Text("رقم الهاتف") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorResource(R.color.primary_green),
                        focusedLabelColor = colorResource(R.color.primary_green)
                    ),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            tint = colorResource(R.color.primary_green)
                        )
                    }
                )
                
                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    label = { Text("المبلغ (ل.س)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorResource(R.color.primary_green),
                        focusedLabelColor = colorResource(R.color.primary_green)
                    ),
                    leadingIcon = {
                        Icon(
                            Icons.Default.AttachMoney,
                            contentDescription = null,
                            tint = colorResource(R.color.primary_green)
                        )
                    }
                )
                
                // Tax Selection (Pro merchants only)
                if (isProMerchant) {
                    if (taxes.isNotEmpty()) {
                        // Tax Dropdown
                        ExposedDropdownMenuBox(
                            expanded = showTaxDropdown,
                            onExpandedChange = onShowTaxDropdownChange
                        ) {
                            OutlinedTextField(
                                value = selectedTax?.let { "${it.name} (${it.getFormattedRate()})" } ?: "لا توجد ضريبة",
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("الضريبة المطبقة") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00D632),
                                    focusedLabelColor = Color(0xFF00D632)
                                ),
                                leadingIcon = {
                                    Icon(Icons.Default.Receipt, contentDescription = null)
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTaxDropdown)
                                }
                            )
                            
                            ExposedDropdownMenu(
                                expanded = showTaxDropdown,
                                onDismissRequest = { onShowTaxDropdownChange(false) }
                            ) {
                                // No Tax Option
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = Color(0xFF666666)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("بدون ضريبة")
                                        }
                                    },
                                    onClick = {
                                        onTaxSelected(null)
                                        onShowTaxDropdownChange(false)
                                    }
                                )
                                
                                // Tax Options
                                taxes.forEach { tax ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(12.dp)
                                                            .background(
                                                                Color(tax.getTaxCategoryColor()),
                                                                RoundedCornerShape(6.dp)
                                                            )
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = tax.name,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        Text(
                                                            text = tax.getFormattedRate(),
                                                            fontSize = 12.sp,
                                                            color = Color(tax.getTaxCategoryColor())
                                                        )
                                                    }
                                                }
                                                if (tax.isDefault) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color(0xFF00D632), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "افتراضي",
                                                            fontSize = 8.sp,
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        onClick = {
                                            onTaxSelected(tax)
                                            onShowTaxDropdownChange(false)
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // No taxes available
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "لا توجد ضرائب مضافة بعد",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFF9800)
                                )
                            }
                        }
                    }
                }
                
                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("الوصف (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorResource(R.color.primary_green),
                        focusedLabelColor = colorResource(R.color.primary_green)
                    ),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = colorResource(R.color.primary_green)
                        )
                    }
                )
                
                // Tax Calculation Summary (if tax is selected)
                if (selectedTax != null && parsedAmount > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "ملخص الفاتورة",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("المبلغ الأساسي:", fontSize = 11.sp, color = Color(0xFF666666))
                                Text(
                                    MerchantTax.formatAmount(parsedAmount),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${selectedTax.name}:", fontSize = 11.sp, color = Color(0xFF666666))
                                Text(
                                    MerchantTax.formatAmount(taxAmount),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(selectedTax.getTaxCategoryColor())
                                )
                            }
                            
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "المجموع الكلي:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    MerchantTax.formatAmount(totalAmount),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00D632)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.primary_green)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "إرسال الفاتورة",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "إلغاء",
                    color = colorResource(R.color.gray_medium),
                    fontWeight = FontWeight.Medium
                )
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = colorResource(R.color.white_transparent_80)
    )
}
