package com.smartpay.android

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.smartpay.android.R
import com.smartpay.android.ui.theme.SmartPayTheme
import com.smartpay.data.models.PinRequest
import com.smartpay.data.repository.AuthRepository
import kotlinx.coroutines.launch

class SetPinActivity : ComponentActivity() {

    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // EncryptedSharedPreferences لتخزين البيانات بشكل آمن
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPreferences = EncryptedSharedPreferences.create(
            "SmartPaySecurePrefs",
            masterKeyAlias,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val token = sharedPreferences.getString("token", null)

        setContent {
            SmartPayTheme {
                ModernSetPinScreen(token)
            }
        }
    }

    @Composable
    fun ModernSetPinScreen(token: String?) {
        var pin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var showPin by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(false) }
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

                // Header Section
                HeaderSection()

                Spacer(modifier = Modifier.height(32.dp))

                // Welcome Section
                WelcomeSection()

                Spacer(modifier = Modifier.height(32.dp))

                // PIN Input Section
                PinInputSection(
                    pin = pin,
                    confirmPin = confirmPin,
                    showPin = showPin,
                    onPinChange = { newPin ->
                        if (newPin.length <= 4 && newPin.all(Char::isDigit)) pin = newPin
                    },
                    onConfirmPinChange = { newConfirm ->
                        if (newConfirm.length <= 4 && newConfirm.all(Char::isDigit)) confirmPin = newConfirm
                    },
                    onShowPinChange = { showPin = it }
                )

                Spacer(modifier = Modifier.height(32.dp))


                // Continue Button
                ContinueButton(
                    pin = pin,
                    confirmPin = confirmPin,
                    isLoading = isLoading,
                    onContinueClick = {
                        handlePinSubmission(
                            pin = pin,
                            confirmPin = confirmPin,
                            token = token,
                            onLoadingChange = { isLoading = it }
                        )
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    @Composable
    fun HeaderSection() {
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
                    Icons.Default.Lock,
                    contentDescription = "أنشئ رمز PIN",
                    modifier = Modifier.size(60.dp),
                    tint = colorResource(R.color.primary_dark)
                )
            }
        }
    }

    @Composable
    fun WelcomeSection() {
        Text(
            text = "أنشئ رمز PIN",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.primary_dark),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "سيحمي هذا الرمز حسابك ومعاملاتك المالية",
            fontSize = 16.sp,
            color = colorResource(R.color.gray_dark),
            textAlign = TextAlign.Center
        )
    }

    @Composable
    fun PinInputSection(
        pin: String,
        confirmPin: String,
        showPin: Boolean,
        onPinChange: (String) -> Unit,
        onConfirmPinChange: (String) -> Unit,
        onShowPinChange: (Boolean) -> Unit
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
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // PIN Input
                Column {
                    Text(
                        text = "رمز PIN (4 أرقام)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorResource(R.color.gray_dark)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ModernPinInputField(
                        value = pin,
                        onValueChange = onPinChange,
                        showValue = showPin
                    )
                }

                // Confirm PIN Input
                Column {
                    Text(
                        text = "تأكيد رمز PIN",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorResource(R.color.gray_dark)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ModernPinInputField(
                        value = confirmPin,
                        onValueChange = onConfirmPinChange,
                        showValue = showPin
                    )
                }

                // Show PIN Toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorResource(R.color.white_transparent_60)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (showPin) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = colorResource(R.color.primary_green)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "إظهار رمز PIN",
                                fontSize = 14.sp,
                                color = colorResource(R.color.gray_dark)
                            )
                        }
                        Switch(
                            checked = showPin,
                            onCheckedChange = onShowPinChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = colorResource(R.color.primary_green),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = colorResource(R.color.gray_light)
                            )
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun ContinueButton(
        pin: String,
        confirmPin: String,
        isLoading: Boolean,
        onContinueClick: () -> Unit
    ) {
        Button(
            onClick = onContinueClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = pin.length == 4 && confirmPin.length == 4 && !isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(R.color.primary_green),
                disabledContainerColor = colorResource(R.color.gray_light)
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
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "متابعة",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }

    private fun handlePinSubmission(
        pin: String,
        confirmPin: String,
        token: String?,
        onLoadingChange: (Boolean) -> Unit
    ) {
        if (pin.length != 4 || confirmPin.length != 4) {
            Toast.makeText(
                this,
                "يجب أن يكون الرمز 4 أرقام",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        if (pin != confirmPin) {
            Toast.makeText(
                this,
                "الرمزان غير متطابقان",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        if (token.isNullOrBlank()) {
            Toast.makeText(
                this,
                "المستخدم غير مسجل الدخول",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        onLoadingChange(true)
        
        lifecycleScope.launch {
            try {
                val bearerToken = "Bearer $token"
                val response = authRepository.setPin(bearerToken, PinRequest(pin))

                if (response.isSuccessful) {
                    val normalPrefs = getSharedPreferences("SmartPayPrefs", MODE_PRIVATE)
                    normalPrefs.edit()
                        .putString("pin", pin)
                        .putBoolean("introSeen", true)
                        .apply()

                    Toast.makeText(
                        this@SetPinActivity,
                        "تم حفظ رمز PIN بنجاح",
                        Toast.LENGTH_SHORT
                    ).show()

                    val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
                    val sharedPreferences = EncryptedSharedPreferences.create(
                        "SmartPaySecurePrefs",
                        masterKeyAlias,
                        this@SetPinActivity,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )
                    
                    val userType = sharedPreferences.getString("userType", "individual")
                    val nextIntent = if (userType == "merchant") {
                        Intent(this@SetPinActivity, BusinessDashboardActivity::class.java)
                    } else {
                        Intent(this@SetPinActivity, ModernDashboardActivity::class.java)
                    }

                    nextIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(nextIntent)
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(
                        this@SetPinActivity,
                        "فشل حفظ PIN: ${response.code()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@SetPinActivity,
                    "خطأ في الشبكة: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                onLoadingChange(false)
            }
        }
    }
}

/* ==============
   مكونات PIN UI
   ============== */

@Composable
fun ModernPinInputField(
    value: String,
    onValueChange: (String) -> Unit,
    showValue: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(4) { index ->
                ModernPinDigitBox(
                    digit = value.getOrNull(index),
                    isFocused = value.length == index,
                    showValue = showValue
                )
            }
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            textStyle = TextStyle(color = Color.Transparent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            cursorBrush = SolidColor(Color.Transparent)
        )
    }
}

@Composable
fun ModernPinDigitBox(
    digit: Char?,
    isFocused: Boolean,
    showValue: Boolean
) {
    Card(
        modifier = Modifier.size(65.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(R.color.white_transparent_80)
        ),
        border = if (isFocused) {
            androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = colorResource(R.color.primary_green)
            )
        } else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (digit != null) {
                if (showValue) {
                    Text(
                        text = digit.toString(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.primary_dark)
                    )
                } else {
                    Card(
                        modifier = Modifier.size(16.dp),
                        shape = RoundedCornerShape(50),
                        colors = CardDefaults.cardColors(
                            containerColor = colorResource(R.color.primary_dark)
                        )
                    ) {}
                }
            }
        }
    }
}
