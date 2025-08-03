package com.smartpay.android

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.smartpay.models.TransactionCategory
import com.smartpay.models.SubAccount
import com.smartpay.models.Reminder
import com.smartpay.models.FinancialReport
import com.smartpay.models.Employee
import com.smartpay.models.UnifiedReportDashboard
import com.smartpay.models.MerchantApiKey
import com.smartpay.models.ScheduledOperation
import com.smartpay.models.MerchantTax

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SettingsScreen()
        }
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current

    // Shared Preferences
    val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    val sharedPrefs = EncryptedSharedPreferences.create(
        "SmartPaySecurePrefs",
        masterKey,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var userName by remember {
        mutableStateOf(sharedPrefs.getString("userName", "مستخدم SmartPay") ?: "مستخدم SmartPay")
    }
    var showNameDialog by remember { mutableStateOf(false) }
    var showCardDialog by remember { mutableStateOf(false) }
    var showBankDialog by remember { mutableStateOf(false) }
    var cardNumber by remember { mutableStateOf("") }
    var bankAccount by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .padding(top = 40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                }
                Text("الإعدادات", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.width(40.dp))
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                // User Info Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(Color(0xFF00D632), shape = RoundedCornerShape(50)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                userName.firstOrNull()?.toString() ?: "م",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(userName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("+963xxx", fontSize = 14.sp, color = Color.Gray)
                        }
                        TextButton(onClick = { showNameDialog = true }) {
                            Text("تعديل", color = Color(0xFF00D632))
                        }
                    }
                }

                // Security Section
                SectionTitle("الأمن")
                SettingsCard {
                    CashAppSettingsItem(Icons.Default.Lock, "تغيير رمز PIN") {
                        context.startActivity(Intent(context, SetPinActivity::class.java))
                    }
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    CashAppSettingsItem(
                        Icons.Default.ExitToApp,
                        "تسجيل الخروج",
                        titleColor = Color(0xFFFF3737)
                    ) {
                        sharedPrefs.edit().clear().apply()
                        val i = Intent(context, LoginActivity::class.java)
                        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(i)
                    }
                }

                // Banking Section
                SectionTitle("الربط البنكي")
                SettingsCard {
                    CashAppSettingsItem(Icons.Default.CreditCard, "إضافة بطاقة") { showCardDialog = true }
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    CashAppSettingsItem(Icons.Default.AccountBalance, "إضافة حساب بنكي") { showBankDialog = true }
                }

                // Merchant Services Section (Only for Merchants)
                val userType = sharedPrefs.getString("userType", "personal") ?: "personal"
                val subscriptionPlan = sharedPrefs.getString("subscriptionPlan", "Free") ?: "Free"
                if (userType == "business" || userType == "merchant") {
                    SectionTitle("خدمات التاجر")
                    SettingsCard {
                        // Subscription Management - Always visible for merchants
                        CashAppSettingsItem(Icons.Default.Subscriptions, "إدارة الاشتراكات") {
                            context.startActivity(Intent(context, UpgradeScreen::class.java))
                        }
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        CashAppSettingsItem(Icons.Default.History, "سجل العمليات") {
                            context.startActivity(Intent(context, AuditLogActivity::class.java))
                        }
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        CashAppSettingsItem(Icons.Default.Repeat, "الفواتير المتكررة") {
                            context.startActivity(Intent(context, RecurringInvoicesActivity::class.java))
                        }
                        
                        // Scheduled Operations (Pro plan only)
                        if (ScheduledOperation.hasFeatureAccess(subscriptionPlan)) {
                            Divider(modifier = Modifier.padding(horizontal = 16.dp))
                            CashAppSettingsItem(Icons.Default.Schedule, "📅 الجدولة التلقائية") {
                                context.startActivity(Intent(context, ScheduledOperationsActivity::class.java))
                            }
                        } else {
                            // Show locked item for Free/Standard plans to encourage upgrade
                            Divider(modifier = Modifier.padding(horizontal = 16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        Toast.makeText(context, ScheduledOperation.getUpgradeMessage(), Toast.LENGTH_LONG).show()
                                        context.startActivity(Intent(context, MerchantSubscriptionActivity::class.java))
                                    }
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = Color(0xFF999999),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "📅 الجدولة التلقائية",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF999999)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = "مغلق",
                                            tint = Color(0xFFFF9800),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        "إنشاء عمليات تلقائية - الخطة الاحترافية فقط",
                                        fontSize = 12.sp,
                                        color = Color(0xFF999999)
                                    )
                                }
                                Text(
                                    "Pro",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier
                                        .background(Color(0xFFFF9800), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        
                        // Transaction Categories (Standard/Pro plans only)
                        if (TransactionCategory.hasFeatureAccess(subscriptionPlan)) {
                            Divider(modifier = Modifier.padding(horizontal = 16.dp))
                            CashAppSettingsItem(Icons.Default.Category, "إدارة التصنيفات") {
                                context.startActivity(Intent(context, ManageTransactionCategoriesActivity::class.java))
                            }
                        }
                        
                        // Employee Management (Pro plan only)
                        if (Employee.hasFeatureAccess(subscriptionPlan)) {
                            Divider(modifier = Modifier.padding(horizontal = 16.dp))
                            CashAppSettingsItem(Icons.Default.People, "👥 إدارة الموظفين") {
                                context.startActivity(Intent(context, ManageEmployeesActivity::class.java))
                            }
                        }
                        
                        // Sub-Accounts (Standard/Pro plans only)
                        if (SubAccount.hasFeatureAccess(subscriptionPlan)) {
                            Divider(modifier = Modifier.padding(horizontal = 16.dp))
                            CashAppSettingsItem(Icons.Default.Group, "الحسابات الفرعية") {
                                context.startActivity(Intent(context, ManageSubAccountsActivity::class.java))
                            }
                        }
                        
                        // Reminders (Pro plan only)
                        if (Reminder.hasFeatureAccess(subscriptionPlan)) {
                            Divider(modifier = Modifier.padding(horizontal = 16.dp))
                            CashAppSettingsItem(Icons.Default.Alarm, "🔔 التذكيرات") {
                                context.startActivity(Intent(context, ReminderListActivity::class.java))
                            }
                        }
                        
                        // Financial Reports (Pro plan only)
                        if (FinancialReport.hasFeatureAccess(subscriptionPlan)) {
                            Divider(modifier = Modifier.padding(horizontal = 16.dp))
                            CashAppSettingsItem(Icons.Default.BarChart, "التقارير المالية المتقدمة") {
                                context.startActivity(Intent(context, FinancialReportsActivity::class.java))
                            }
                        }
                        
                        // Unified Business Reports (Pro plan only)
                        if (UnifiedReportDashboard.hasFeatureAccess(subscriptionPlan)) {
                            Divider(modifier = Modifier.padding(horizontal = 16.dp))
                            CashAppSettingsItem(Icons.Default.Assessment, "📊 التقارير الموحدة") {
                                context.startActivity(Intent(context, ReportDashboardActivity::class.java))
                            }
                        }
                        
                        // Merchant API Integration (Pro plan only)
                        if (MerchantApiKey.hasFeatureAccess(subscriptionPlan)) {
                            Divider(modifier = Modifier.padding(horizontal = 16.dp))
                            CashAppSettingsItem(Icons.Default.Api, "🔌 تكامل API (للتوصيل مع البرامج الخارجية)") {
                                context.startActivity(Intent(context, MerchantApiKeysActivity::class.java))
                            }
                        }
                        
                        // Tax Management (Pro plan only)
                        if (MerchantTax.hasFeatureAccess(subscriptionPlan)) {
                            Divider(modifier = Modifier.padding(horizontal = 16.dp))
                            CashAppSettingsItem(Icons.Default.Receipt, "🧾 إعدادات الضرائب") {
                                context.startActivity(Intent(context, MerchantTaxesActivity::class.java))
                            }
                        } else {
                            // Show locked item for Free/Standard plans to encourage upgrade
                            Divider(modifier = Modifier.padding(horizontal = 16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        Toast.makeText(context, MerchantTax.getUpgradeMessage(), Toast.LENGTH_LONG).show()
                                        context.startActivity(Intent(context, MerchantSubscriptionActivity::class.java))
                                    }
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = Color(0xFF999999),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "🧾 إعدادات الضرائب",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF999999)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = "مغلق",
                                            tint = Color(0xFFFF9800),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        "إدارة الضرائب والتقارير الضريبية - الخطة الاحترافية فقط",
                                        fontSize = 12.sp,
                                        color = Color(0xFF999999)
                                    )
                                }
                                Text(
                                    "Pro",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier
                                        .background(Color(0xFFFF9800), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Support Section
                SectionTitle("الدعم")
                SettingsCard {
                    CashAppSettingsItem(Icons.Default.HelpOutline, "الدعم والمساعدة") {
                        Toast.makeText(context, "يرجى التواصل معنا لاحقًا", Toast.LENGTH_SHORT).show()
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // New Modern Components
    @Composable
    fun HeaderSection(onBackClick: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Card(
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onBackClick() },
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
                text = "الإعدادات",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.primary_dark)
            )
            
            Spacer(modifier = Modifier.width(48.dp))
        }
    }

    @Composable
    fun UserInfoSection(
        userName: String,
        onEditClick: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorResource(R.color.white_transparent_80)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier.size(60.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorResource(R.color.primary_green)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.firstOrNull()?.toString() ?: "م",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(R.color.primary_dark)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.primary_dark)
                    )
                    Text(
                        text = "+963xxx",
                        fontSize = 14.sp,
                        color = colorResource(R.color.gray_medium)
                    )
                }
                
                Button(
                    onClick = onEditClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.primary_green)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Text(
                        text = "تعديل",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }

    @Composable
    fun ModernSectionTitle(title: String) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.gray_dark),
            modifier = Modifier.padding(vertical = 12.dp)
        )
    }

    @Composable
    fun ModernSettingsCard(content: @Composable ColumnScope.() -> Unit) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorResource(R.color.white_transparent_80)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column { content() }
        }
    }

    @Composable
    fun ModernSettingsItem(
        icon: ImageVector,
        title: String,
        subtitle: String? = null,
        titleColor: Color = colorResource(R.color.primary_dark),
        onClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (titleColor == colorResource(R.color.error_red)) {
                        titleColor.copy(alpha = 0.1f)
                    } else {
                        colorResource(R.color.primary_green).copy(alpha = 0.1f)
                    }
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
                        tint = if (titleColor == colorResource(R.color.error_red)) {
                            titleColor
                        } else {
                            colorResource(R.color.primary_green)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = titleColor
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = colorResource(R.color.gray_medium)
                    )
                }
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = colorResource(R.color.gray_medium),
                modifier = Modifier.size(20.dp)
            )
        }
    }

    @Composable
    fun ModernDivider() {
        Divider(
            modifier = Modifier.padding(horizontal = 20.dp),
            color = colorResource(R.color.gray_light)
        )
    }

        // Dialogs
        if (showNameDialog) {
            var newName by remember { mutableStateOf(userName) }
            ModernDialog(
                title = "تعديل اسم المستخدم",
                onDismiss = { showNameDialog = false },
                onConfirm = {
                    if (newName.isNotBlank()) {
                        sharedPrefs.edit().putString("userName", newName).apply()
                        userName = newName
                        Toast.makeText(context, "تم تحديث الاسم", Toast.LENGTH_SHORT).show()
                        showNameDialog = false
                    }
                }
            ) {
                ModernDialogTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = "الاسم الكامل"
                )
            }
        }

        if (showCardDialog) {
            ModernDialog(
                title = "إضافة بطاقة",
                onDismiss = { showCardDialog = false },
                onConfirm = {
                    if (cardNumber.isNotBlank()) {
                        sharedPrefs.edit().putString("cardNumber", cardNumber).apply()
                        Toast.makeText(context, "تم حفظ البطاقة", Toast.LENGTH_SHORT).show()
                        showCardDialog = false
                        cardNumber = ""
                    }
                }
            ) {
                ModernDialogTextField(
                    value = cardNumber,
                    onValueChange = { if (it.length <= 16 && it.all(Char::isDigit)) cardNumber = it },
                    placeholder = "رقم البطاقة (16 رقم)",
                    keyboardType = KeyboardType.Number
                )
            }
        }

        if (showBankDialog) {
            ModernDialog(
                title = "إضافة حساب بنكي",
                onDismiss = { showBankDialog = false },
                onConfirm = {
                    if (bankAccount.isNotBlank()) {
                        sharedPrefs.edit().putString("bankAccount", bankAccount).apply()
                        Toast.makeText(context, "تم حفظ الحساب البنكي", Toast.LENGTH_SHORT).show()
                        showBankDialog = false
                        bankAccount = ""
                    }
                }
            ) {
                ModernDialogTextField(
                    value = bankAccount,
                    onValueChange = { bankAccount = it },
                    placeholder = "رقم الحساب أو IBAN"
                )
            }
        }
    }
}


@Composable
fun ModernDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.primary_dark),
                textAlign = TextAlign.Center
            )
        },
        text = { 
            Column(modifier = Modifier.padding(vertical = 8.dp)) { 
                content() 
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
                Text(
                    text = "حفظ",
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

@Composable
fun ModernDialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.white_transparent_60)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorResource(R.color.primary_dark)
                ),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                singleLine = true,
                cursorBrush = SolidColor(colorResource(R.color.primary_green)),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = TextStyle(
                                fontSize = 16.sp,
                                color = colorResource(R.color.gray_medium)
                            )
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}
