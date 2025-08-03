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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.smartpay.data.models.RegisterBusinessRequest
import com.smartpay.data.repository.AuthRepository
import kotlinx.coroutines.launch

class RegisterBusinessActivity : ComponentActivity() {

    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ModernRegisterBusinessScreen()
        }
    }

    @Composable
    fun ModernRegisterBusinessScreen() {
        var companyName by remember { mutableStateOf("") }
        var ownerName by remember { mutableStateOf("") }
        var companyAddress by remember { mutableStateOf("") }
        var licenseNumber by remember { mutableStateOf("") }
        var phoneNumber by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var isPasswordVisible by remember { mutableStateOf(false) }
        var isConfirmPasswordVisible by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(false) }
        var agreeToTerms by remember { mutableStateOf(false) }
        
        val context = LocalContext.current
        val scrollState = rememberScrollState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF8FDED),
                            Color(0xFFF0F8E8)
                        )
                    )
                )
        ) {
            // Animated Background Orbs
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .offset(x = 280.dp, y = 80.dp)
                    .background(
                        Color(0xFFD8FBA9).copy(alpha = 0.12f),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .offset(x = (-60).dp, y = 300.dp)
                    .background(
                        Color.White.copy(alpha = 0.2f),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .offset(x = 250.dp, y = 600.dp)
                    .background(
                        Color(0xFF2D2D2D).copy(alpha = 0.06f),
                        CircleShape
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status Bar Spacer
                Spacer(modifier = Modifier.height(32.dp))

                // Header with Back Button
                HeaderSection()

                Spacer(modifier = Modifier.height(32.dp))

                // Welcome Section
                WelcomeSection()

                Spacer(modifier = Modifier.height(32.dp))

                // Business Registration Form
                BusinessRegistrationForm(
                    companyName = companyName,
                    ownerName = ownerName,
                    companyAddress = companyAddress,
                    licenseNumber = licenseNumber,
                    phoneNumber = phoneNumber,
                    password = password,
                    confirmPassword = confirmPassword,
                    isPasswordVisible = isPasswordVisible,
                    isConfirmPasswordVisible = isConfirmPasswordVisible,
                    isLoading = isLoading,
                    agreeToTerms = agreeToTerms,
                    onCompanyNameChange = { companyName = it },
                    onOwnerNameChange = { ownerName = it },
                    onCompanyAddressChange = { companyAddress = it },
                    onLicenseNumberChange = { licenseNumber = it },
                    onPhoneNumberChange = { phoneNumber = it },
                    onPasswordChange = { password = it },
                    onConfirmPasswordChange = { confirmPassword = it },
                    onPasswordVisibilityToggle = { isPasswordVisible = !isPasswordVisible },
                    onConfirmPasswordVisibilityToggle = { isConfirmPasswordVisible = !isConfirmPasswordVisible },
                    onAgreeToTermsChange = { agreeToTerms = it },
                    onRegisterClick = {
                        if (validateBusinessForm(companyName, ownerName, phoneNumber, password, confirmPassword, agreeToTerms)) {
                            performBusinessRegistration(
                                companyName, ownerName, companyAddress, licenseNumber, phoneNumber, password
                            ) { success ->
                                if (success) {
                                    startActivity(Intent(this@RegisterBusinessActivity, SetPinActivity::class.java))
                                    finish()
                                } else {
                                    Toast.makeText(context, "خطأ في التسجيل", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "يرجى ملء جميع الحقول المطلوبة بشكل صحيح", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Login Section
                LoginSection()

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    @Composable
    fun HeaderSection() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Card(
                modifier = Modifier
                    .size(48.dp)
                    .clickable { finish() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.8f)
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
                        tint = Color(0xFF2D2D2D)
                    )
                }
            }
        }
    }

    @Composable
    fun WelcomeSection() {
        Card(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFD8FBA9)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Business,
                    contentDescription = "تسجيل حساب تجاري",
                    modifier = Modifier.size(60.dp),
                    tint = Color(0xFF2D2D2D)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "إنشاء حساب تجاري",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2D2D2D)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "ابدأ رحلتك التجارية مع SmartPay",
            fontSize = 16.sp,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center
        )
    }

    @Composable
    fun BusinessRegistrationForm(
        companyName: String,
        ownerName: String,
        companyAddress: String,
        licenseNumber: String,
        phoneNumber: String,
        password: String,
        confirmPassword: String,
        isPasswordVisible: Boolean,
        isConfirmPasswordVisible: Boolean,
        isLoading: Boolean,
        agreeToTerms: Boolean,
        onCompanyNameChange: (String) -> Unit,
        onOwnerNameChange: (String) -> Unit,
        onCompanyAddressChange: (String) -> Unit,
        onLicenseNumberChange: (String) -> Unit,
        onPhoneNumberChange: (String) -> Unit,
        onPasswordChange: (String) -> Unit,
        onConfirmPasswordChange: (String) -> Unit,
        onPasswordVisibilityToggle: () -> Unit,
        onConfirmPasswordVisibilityToggle: () -> Unit,
        onAgreeToTermsChange: (Boolean) -> Unit,
        onRegisterClick: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Box {
                // Subtle card patterns
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .offset(x = 250.dp, y = (-50).dp)
                        .background(
                            Color(0xFFD8FBA9).copy(alpha = 0.05f),
                            CircleShape
                        )
                )

                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Company Name Field
                    ModernInputField(
                        label = "اسم الشركة",
                        value = companyName,
                        onValueChange = onCompanyNameChange,
                        icon = Icons.Default.Business,
                        placeholder = "شركة التقنية المتقدمة"
                    )

                    // Owner Name Field
                    ModernInputField(
                        label = "اسم المالك",
                        value = ownerName,
                        onValueChange = onOwnerNameChange,
                        icon = Icons.Default.Person,
                        placeholder = "أحمد محمد"
                    )

                    // Company Address Field
                    ModernInputField(
                        label = "عنوان الشركة",
                        value = companyAddress,
                        onValueChange = onCompanyAddressChange,
                        icon = Icons.Default.LocationOn,
                        placeholder = "الشارع، الحي، المدينة"
                    )

                    // License Number Field
                    ModernInputField(
                        label = "رقم الترخيص / الرقم الوطني",
                        value = licenseNumber,
                        onValueChange = onLicenseNumberChange,
                        icon = Icons.Default.Badge,
                        placeholder = "رقم الترخيص التجاري",
                        keyboardType = KeyboardType.Number
                    )

                    // Phone Number Field
                    ModernInputField(
                        label = "رقم الهاتف",
                        value = phoneNumber,
                        onValueChange = onPhoneNumberChange,
                        icon = Icons.Default.Phone,
                        placeholder = "+963 9XX XXX XXX",
                        keyboardType = KeyboardType.Phone
                    )

                    // Password Field
                    ModernPasswordField(
                        label = "كلمة المرور",
                        value = password,
                        onValueChange = onPasswordChange,
                        isVisible = isPasswordVisible,
                        onVisibilityToggle = onPasswordVisibilityToggle,
                        placeholder = "كلمة مرور قوية"
                    )

                    // Confirm Password Field
                    ModernPasswordField(
                        label = "تأكيد كلمة المرور",
                        value = confirmPassword,
                        onValueChange = onConfirmPasswordChange,
                        isVisible = isConfirmPasswordVisible,
                        onVisibilityToggle = onConfirmPasswordVisibilityToggle,
                        placeholder = "أعد إدخال كلمة المرور"
                    )

                    // Terms and Conditions
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (agreeToTerms) Color(0xFFD8FBA9).copy(alpha = 0.1f) 
                                           else Color.White.copy(alpha = 0.8f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAgreeToTermsChange(!agreeToTerms) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = agreeToTerms,
                                onCheckedChange = onAgreeToTermsChange,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFFD8FBA9)
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "أوافق على الشروط والأحكام التجارية",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF2D2D2D)
                                )
                                Text(
                                    text = "أتعهد بصحة البيانات المدخلة",
                                    fontSize = 12.sp,
                                    color = Color(0xFF666666)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Register Button
                    Button(
                        onClick = onRegisterClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD8FBA9)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color(0xFF2D2D2D),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Business,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFF2D2D2D)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "إنشاء الحساب التجاري",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2D2D2D)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun LoginSection() {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.8f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "لديك حساب تجاري بالفعل؟",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تسجيل الدخول",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD8FBA9),
                    modifier = Modifier.clickable {
                        startActivity(Intent(this@RegisterBusinessActivity, ModernLoginActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }

    @Composable
    private fun ModernInputField(
        label: String,
        value: String,
        onValueChange: (String) -> Unit,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        placeholder: String,
        keyboardType: KeyboardType = KeyboardType.Text
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFFD8FBA9)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2D2D2D)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.8f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    textStyle = TextStyle(
                        fontSize = 16.sp, 
                        color = Color(0xFF2D2D2D),
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(Color(0xFFD8FBA9)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    decorationBox = { innerTextField ->
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    color = Color(0xFF999999)
                                )
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }
    }

    @Composable
    private fun ModernPasswordField(
        label: String,
        value: String,
        onValueChange: (String) -> Unit,
        isVisible: Boolean,
        onVisibilityToggle: () -> Unit,
        placeholder: String
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFFD8FBA9)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2D2D2D)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.8f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        textStyle = TextStyle(
                            fontSize = 16.sp, 
                            color = Color(0xFF2D2D2D),
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(Color(0xFFD8FBA9)),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (value.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        color = Color(0xFF999999)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                    
                    IconButton(onClick = onVisibilityToggle) {
                        Icon(
                            if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (isVisible) "إخفاء كلمة المرور" else "إظهار كلمة المرور",
                            tint = Color(0xFF666666),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    private fun validateBusinessForm(
        companyName: String,
        ownerName: String,
        phoneNumber: String,
        password: String,
        confirmPassword: String,
        agreeToTerms: Boolean
    ): Boolean {
        return companyName.isNotBlank() &&
                ownerName.isNotBlank() &&
                phoneNumber.isNotBlank() &&
                password.isNotBlank() &&
                password == confirmPassword &&
                agreeToTerms
    }

    private fun performBusinessRegistration(
        companyName: String,
        ownerName: String,
        companyAddress: String,
        licenseNumber: String,
        phoneNumber: String,
        password: String,
        onResult: (Boolean) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                val request = RegisterBusinessRequest(
                    phone = phoneNumber,
                    businessName = companyName,
                    ownerName = ownerName,
                    password = password,
                    userType = "merchant"
                )

                val response = authRepository.registerBusiness(request)
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    saveBusinessSecurely(
                        authResponse.token,
                        authResponse.user.phone,
                        authResponse.user.businessName ?: companyName
                    )
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    private fun saveBusinessSecurely(token: String, phone: String, businessName: String) {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val securePrefs = EncryptedSharedPreferences.create(
            "SmartPaySecurePrefs",
            masterKeyAlias,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        securePrefs.edit().apply {
            putString("token", token)
            putString("phone", phone)
            putString("businessName", businessName)
            putString("userType", "merchant")
            putBoolean("isRegistered", true)
            apply()
        }
    }
}