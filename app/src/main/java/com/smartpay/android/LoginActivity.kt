package com.smartpay.android

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.smartpay.data.models.LoginRequest
import com.smartpay.data.repository.AuthRepository
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {

    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userType = intent.getStringExtra("userType") ?: "personal"

        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val securePrefs = EncryptedSharedPreferences.create(
            "SmartPaySecurePrefs",
            masterKey,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        setContent {
            var phoneNumber by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(120.dp))

                    Text(
                        "SmartPay",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "أهلاً بك مجدداً",
                        fontSize = 20.sp,
                        color = Color(0xFF666666),
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(80.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "رقم الهاتف",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF666666)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CashAppTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            placeholder = "أدخل رقم هاتفك",
                            keyboardType = KeyboardType.Phone
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "كلمة المرور",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF666666)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CashAppTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = "أدخل كلمة المرور",
                            isPassword = true
                        )
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Button(
                        onClick = {
                            if (phoneNumber.isBlank() || password.isBlank()) {
                                Toast.makeText(
                                    applicationContext,
                                    "يرجى ملء جميع الحقول",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            lifecycleScope.launch {
                                try {
                                    val response = if (userType == "business") {
                                        authRepository.loginBusiness(
                                            LoginRequest(
                                                phone = phoneNumber,
                                                password = password
                                            )
                                        )
                                    } else {
                                        authRepository.loginUser(
                                            LoginRequest(
                                                phone = phoneNumber,
                                                password = password
                                            )
                                        )
                                    }

                                    if (response.isSuccessful && response.body() != null) {
                                        val authResponse = response.body()!!

                                        securePrefs.edit()
                                            .putBoolean("is_logged_in", true)
                                            .putString("token", authResponse.token)
                                            .putString("user_id", authResponse.user.id)
                                            .putString("phone", authResponse.user.phone)
                                            .putString("name", authResponse.user.fullname ?: authResponse.user.businessName ?: "")
                                            .putString("userType", authResponse.user.userType)
                                            .apply()

                                        val intent = if (authResponse.user.userType == "merchant") {
                                            Intent(
                                                this@LoginActivity,
                                                BusinessDashboardActivity::class.java
                                            )
                                        } else {
                                            Intent(
                                                this@LoginActivity,
                                                DashboardActivity::class.java
                                            )
                                        }

                                        startActivity(intent)
                                        finish()
                                    } else {
                                        Toast.makeText(
                                            applicationContext,
                                            "بيانات الدخول غير صحيحة",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        applicationContext,
                                        "خطأ: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .shadow(8.dp, RoundedCornerShape(30.dp)),
                        shape = RoundedCornerShape(30.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00D632),
                            contentColor = Color.White
                        ),
                        enabled = phoneNumber.isNotBlank() && password.isNotBlank()
                    ) {
                        Text(
                            "تسجيل الدخول",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    TextButton(onClick = {
                        startActivity(
                            Intent(
                                this@LoginActivity,
                                UserTypeSelectionActivity::class.java
                            )
                        )
                    }) {
                        Text(
                            "ليس لديك حساب؟ سجل الآن",
                            fontSize = 16.sp,
                            color = Color(0xFF00D632),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CashAppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(color = Color(0xFFF7F8FA), shape = RoundedCornerShape(16.dp))
            .border(
                width = 2.dp,
                color = Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                fontFamily = FontFamily.SansSerif
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            cursorBrush = SolidColor(Color(0xFF00D632)),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(placeholder, fontSize = 18.sp, color = Color(0xFF999999))
                }
                innerTextField()
            }
        )
    }
}
