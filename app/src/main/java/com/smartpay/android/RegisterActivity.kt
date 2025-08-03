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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.smartpay.android.R
import com.smartpay.android.security.SecureStorage
import com.smartpay.android.ui.theme.SmartPayTheme
import com.smartpay.data.models.RegisterRequest
import com.smartpay.data.repository.AuthRepository
import kotlinx.coroutines.launch

class RegisterActivity : ComponentActivity() {
    private val authRepository = AuthRepository()
    private lateinit var secureStorage: SecureStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        secureStorage = SecureStorage.getInstance(this)
        
        setContent {
            SmartPayTheme {
                ModernRegisterScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ModernRegisterScreen() {
        var fullName by remember { mutableStateOf("") }
        var phoneNumber by remember { mutableStateOf("") }
        var nationalId by remember { mutableStateOf("") }
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

                // Header with Back Button
                HeaderSection()

                Spacer(modifier = Modifier.height(32.dp))

                // Welcome Section
                WelcomeSection()

                Spacer(modifier = Modifier.height(32.dp))

                // Registration Form
                RegistrationForm(
                    fullName = fullName,
                    phoneNumber = phoneNumber,
                    nationalId = nationalId,
                    password = password,
                    confirmPassword = confirmPassword,
                    isPasswordVisible = isPasswordVisible,
                    isConfirmPasswordVisible = isConfirmPasswordVisible,
                    isLoading = isLoading,
                    agreeToTerms = agreeToTerms,
                    onFullNameChange = { fullName = it },
                    onPhoneNumberChange = { phoneNumber = it },
                    onNationalIdChange = { nationalId = it },
                    onPasswordChange = { password = it },
                    onConfirmPasswordChange = { confirmPassword = it },
                    onPasswordVisibilityToggle = { isPasswordVisible = !isPasswordVisible },
                    onConfirmPasswordVisibilityToggle = { isConfirmPasswordVisible = !isConfirmPasswordVisible },
                    onAgreeToTermsChange = { agreeToTerms = it },
                    onRegisterClick = {
                        if (validateForm(fullName, phoneNumber, nationalId, password, confirmPassword, agreeToTerms)) {
                            performRegistration(fullName, phoneNumber, nationalId, password) { success ->
                                if (success) {
                                    startActivity(Intent(this@RegisterActivity, ModernLoginActivity::class.java))
                                    finish()
                                } else {
                                    Toast.makeText(context, "خطأ في التسجيل", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "يرجى ملء جميع الحقول بشكل صحيح", Toast.LENGTH_SHORT).show()
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
                    Icons.Default.PersonAdd,
                    contentDescription = "إنشاء حساب",
                    modifier = Modifier.size(60.dp),
                    tint = colorResource(R.color.primary_dark)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "إنشاء حساب جديد",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.primary_dark)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "انضم إلى SmartPay وابدأ التحويل الفوري",
            fontSize = 16.sp,
            color = colorResource(R.color.gray_dark),
            textAlign = TextAlign.Center
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun RegistrationForm(
        fullName: String,
        phoneNumber: String,
        nationalId: String,
        password: String,
        confirmPassword: String,
        isPasswordVisible: Boolean,
        isConfirmPasswordVisible: Boolean,
        isLoading: Boolean,
        agreeToTerms: Boolean,
        onFullNameChange: (String) -> Unit,
        onPhoneNumberChange: (String) -> Unit,
        onNationalIdChange: (String) -> Unit,
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
                containerColor = colorResource(R.color.white_transparent_80)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Full Name Field
                OutlinedTextField(
                    value = fullName,
                    onValueChange = onFullNameChange,
                    label = { Text("الاسم الكامل") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = colorResource(R.color.primary_green)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorResource(R.color.primary_green),
                        focusedLabelColor = colorResource(R.color.primary_green)
                    )
                )

                // Phone Number Field
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = onPhoneNumberChange,
                    label = { Text("رقم الهاتف") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            tint = colorResource(R.color.primary_green)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorResource(R.color.primary_green),
                        focusedLabelColor = colorResource(R.color.primary_green)
                    )
                )

                // National ID Field
                OutlinedTextField(
                    value = nationalId,
                    onValueChange = onNationalIdChange,
                    label = { Text("الرقم الوطني") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Badge,
                            contentDescription = null,
                            tint = colorResource(R.color.primary_green)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorResource(R.color.primary_green),
                        focusedLabelColor = colorResource(R.color.primary_green)
                    )
                )

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("كلمة المرور") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = colorResource(R.color.primary_green)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = onPasswordVisibilityToggle) {
                            Icon(
                                if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isPasswordVisible) "إخفاء كلمة المرور" else "إظهار كلمة المرور",
                                tint = colorResource(R.color.gray_medium)
                            )
                        }
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorResource(R.color.primary_green),
                        focusedLabelColor = colorResource(R.color.primary_green)
                    )
                )

                // Confirm Password Field
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = { Text("تأكيد كلمة المرور") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.LockReset,
                            contentDescription = null,
                            tint = colorResource(R.color.primary_green)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = onConfirmPasswordVisibilityToggle) {
                            Icon(
                                if (isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isConfirmPasswordVisible) "إخفاء كلمة المرور" else "إظهار كلمة المرور",
                                tint = colorResource(R.color.gray_medium)
                            )
                        }
                    },
                    visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorResource(R.color.primary_green),
                        focusedLabelColor = colorResource(R.color.primary_green)
                    )
                )

                // Terms and Conditions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = agreeToTerms,
                        onCheckedChange = onAgreeToTermsChange,
                        colors = CheckboxDefaults.colors(
                            checkedColor = colorResource(R.color.primary_green)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "أوافق على الشروط والأحكام",
                        fontSize = 14.sp,
                        color = colorResource(R.color.gray_dark),
                        modifier = Modifier.clickable { onAgreeToTermsChange(!agreeToTerms) }
                    )
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
                        containerColor = colorResource(R.color.primary_green)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.Black
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "إنشاء الحساب",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
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
                containerColor = colorResource(R.color.white_transparent_60)
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
                    text = "لديك حساب بالفعل؟",
                    fontSize = 14.sp,
                    color = colorResource(R.color.gray_dark)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تسجيل الدخول",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.primary_green),
                    modifier = Modifier.clickable {
                        startActivity(Intent(this@RegisterActivity, ModernLoginActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }

    private fun validateForm(
        fullName: String,
        phoneNumber: String,
        nationalId: String,
        password: String,
        confirmPassword: String,
        agreeToTerms: Boolean
    ): Boolean {
        return fullName.isNotBlank() &&
                phoneNumber.isNotBlank() &&
                nationalId.isNotBlank() &&
                password.isNotBlank() &&
                password == confirmPassword &&
                agreeToTerms
    }

    private fun performRegistration(
        fullName: String,
        phoneNumber: String,
        nationalId: String,
        password: String,
        onResult: (Boolean) -> Unit
    ) {
        lifecycleScope.launch {
            try {
                // TODO: Implement actual registration logic
                kotlinx.coroutines.delay(2000)
                
                // Save registration success
                secureStorage.saveData("isRegistered", "true")
                secureStorage.saveData("phoneNumber", phoneNumber)
                
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }
}