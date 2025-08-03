package com.smartpay.android

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.smartpay.data.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PinActivity : ComponentActivity() {

    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("SmartPayPrefs", MODE_PRIVATE)
        val storedPin = sharedPreferences.getString("pin", null)

        setContent {
            ModernPinScreen(
                storedPin = storedPin,
                onPinSuccess = { userType ->
                    val intent = if (userType == "business") {
                        Intent(this@PinActivity, BusinessDashboardActivity::class.java)
                    } else {
                        Intent(this@PinActivity, DashboardActivity::class.java)
                    }
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                },
                onForgotPin = {
                    val intent = Intent(this@PinActivity, SetPinActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                },
                userRepository = userRepository,
                sharedPreferences = sharedPreferences
            )
        }
    }
}

@Composable
fun ModernPinScreen(
    storedPin: String?,
    onPinSuccess: (String) -> Unit,
    onForgotPin: () -> Unit,
    userRepository: UserRepository,
    sharedPreferences: android.content.SharedPreferences
) {
    var inputPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    BackHandler { }

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
                .offset(x = 280.dp, y = 100.dp)
                .background(
                    Color(0xFFD8FBA9).copy(alpha = 0.15f),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(180.dp)
                .offset(x = (-70).dp, y = 300.dp)
                .background(
                    Color.White.copy(alpha = 0.25f),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(140.dp)
                .offset(x = 200.dp, y = 600.dp)
                .background(
                    Color(0xFF2D2D2D).copy(alpha = 0.08f),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // App Logo Section
            Card(
                modifier = Modifier.size(100.dp),
                shape = RoundedCornerShape(32.dp),
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
                        Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp),
                        tint = Color(0xFF2D2D2D)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SmartPay",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF2D2D2D),
                fontFamily = FontFamily.SansSerif
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "نظام الدفع الذكي",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF666666)
            )

            Spacer(modifier = Modifier.height(80.dp))

            // Security Icon Section
            Card(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = if (isError) Color(0xFFE53E3E).copy(alpha = 0.15f)
                    else Color(0xFFD8FBA9).copy(alpha = 0.2f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isError) Icons.Default.LockReset else Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = if (isError) Color(0xFFE53E3E) else Color(0xFFD8FBA9),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "أدخل رمز PIN",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D2D2D)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "للوصول الآمن إلى حسابك",
                    fontSize = 16.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // PIN Entry Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "رمز PIN الخاص بك",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2D2D2D)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    ModernPinEntryField(
                        value = inputPin,
                        onValueChange = {
                            if (it.length <= 4 && it.all(Char::isDigit)) {
                                inputPin = it
                                isError = false
                            }
                        },
                        isError = isError,
                        isLoading = isLoading
                    )

                    if (isError) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE53E3E).copy(alpha = 0.1f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = Color(0xFFE53E3E),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "رمز PIN غير صحيح، حاول مرة أخرى",
                                    fontSize = 14.sp,
                                    color = Color(0xFFE53E3E),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Forgot PIN Button
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.8f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                TextButton(
                    onClick = onForgotPin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Help,
                            contentDescription = null,
                            tint = Color(0xFFD8FBA9),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "نسيت رمز PIN؟",
                            fontSize = 16.sp,
                            color = Color(0xFFD8FBA9),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // PIN Validation Logic
    LaunchedEffect(inputPin) {
        if (inputPin.length == 4) {
            isLoading = true
            delay(300)

            if (storedPin == null) {
                Toast.makeText(context, "لم يتم تعيين رمز PIN بعد", Toast.LENGTH_SHORT).show()
                isLoading = false
            } else if (inputPin == storedPin) {
                val token = sharedPreferences.getString("token", null)

                if (token.isNullOrBlank()) {
                    Toast.makeText(context, "لا يمكن تحديد المستخدم", Toast.LENGTH_SHORT).show()
                    isLoading = false
                    return@LaunchedEffect
                }

                try {
                    val walletResponse = userRepository.getWallet(token)
                    if (walletResponse.isSuccessful) {
                        val userType = sharedPreferences.getString("userType", "personal") ?: "personal"
                        onPinSuccess(userType)
                    } else {
                        Toast.makeText(context, "فشل في تحميل البيانات", Toast.LENGTH_SHORT).show()
                        isLoading = false
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
                    isLoading = false
                }
            } else {
                isError = true
                isLoading = false
                Toast.makeText(context, "رمز غير صحيح", Toast.LENGTH_SHORT).show()
                delay(500)
                inputPin = ""
                isError = false
            }
        }
    }
}

@Composable
fun ModernPinEntryField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    isLoading: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        // PIN Dots Display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { index ->
                ModernPinDot(
                    isFilled = index < value.length,
                    isActive = index == value.length && !isLoading,
                    isError = isError,
                    isLoading = isLoading && index == value.length - 1
                )
                if (index < 3) {
                    Spacer(modifier = Modifier.width(20.dp))
                }
            }
        }

        // Hidden TextField for input
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
fun ModernPinDot(
    isFilled: Boolean,
    isActive: Boolean,
    isError: Boolean,
    isLoading: Boolean = false
) {
    Card(
        modifier = Modifier.size(24.dp),
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = when {
                isError && isFilled -> Color(0xFFE53E3E)
                isFilled -> Color(0xFFD8FBA9)
                else -> Color.Transparent
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFilled) 8.dp else 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = if (isActive) 3.dp else 2.dp,
                    color = when {
                        isError -> Color(0xFFE53E3E)
                        isActive -> Color(0xFFD8FBA9)
                        isFilled -> Color.Transparent
                        else -> Color(0xFFE0E0E0)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp
                    )
                }
                isFilled -> {
                    Icon(
                        Icons.Default.Circle,
                        contentDescription = null,
                        tint = Color(0xFF2D2D2D),
                        modifier = Modifier.size(8.dp)
                    )
                }
                isActive -> {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                Color(0xFFD8FBA9).copy(alpha = 0.5f),
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}