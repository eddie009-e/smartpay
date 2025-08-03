package com.smartpay.android
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.smartpay.data.models.RegisterRequest
import com.smartpay.data.repository.AuthRepository
import kotlinx.coroutines.launch

class UserInfoFormActivity : ComponentActivity() {

    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userType = intent.getStringExtra("userType") ?: "personal"
        val sharedPreferences = getSharedPreferences("SmartPayPrefs", MODE_PRIVATE)

        setContent {
            UserInfoForm(userType, sharedPreferences)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInfoForm(userType: String, sharedPreferences: android.content.SharedPreferences) {
    val context = LocalContext.current
    val activity = (context as? ComponentActivity)
    var name by remember { mutableStateOf("") }
    var nationalId by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { activity?.onBackPressed() }
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Title Section
            Column(
                modifier = Modifier.padding(bottom = 50.dp)
            ) {
                Text(
                    text = "معلومات الحساب",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (userType == "personal")
                        "أدخل معلوماتك الشخصية"
                    else
                        "أدخل معلومات نشاطك التجاري",
                    fontSize = 18.sp,
                    color = Color(0xFF666666),
                    fontFamily = FontFamily.SansSerif
                )
            }

            // Input Fields
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column {
                    Text(
                        text = "الاسم الكامل",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF666666),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    CashAppTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = "أدخل اسمك الكامل"
                    )
                }

                Column {
                    Text(
                        text = "الرقم الوطني",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF666666),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    CashAppTextField(
                        value = nationalId,
                        onValueChange = { nationalId = it },
                        placeholder = "أدخل الرقم الوطني",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            // Continue Button
            Button(
                onClick = {
                    val phone = sharedPreferences.getString("phone", null)
                    val password = sharedPreferences.getString("password", "") ?: ""

                    if (phone.isNullOrBlank()) {
                        Toast.makeText(context, "رقم الهاتف غير متوفر", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (name.isNotBlank() && nationalId.isNotBlank() && password.isNotBlank()) {
                        (context as ComponentActivity).lifecycleScope.launch {
                            try {
                                val request = RegisterRequest(
                                    phone = phone,
                                    name = name,
                                    nationalId = nationalId,
                                    password = password,
                                    userType = userType
                                )

                                val response = AuthRepository().registerUser(request)
                                if (response.isSuccessful) {
                                    Toast.makeText(context, "تم حفظ البيانات", Toast.LENGTH_SHORT).show()
                                    context.startActivity(Intent(context, DashboardActivity::class.java))
                                } else {
                                    Toast.makeText(context, "فشل في حفظ البيانات", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "يرجى ملء كل الحقول", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 50.dp)
                    .height(64.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(30.dp),
                        clip = false
                    ),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00D632),
                    contentColor = Color.White
                ),
                enabled = name.isNotBlank() && nationalId.isNotBlank()
            ) {
                Text(
                    text = "متابعة",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
    }
}

@Composable
fun CashAppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(
                color = Color(0xFFF7F8FA),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 2.dp,
                color = if (isFocused) Color(0xFF00D632) else Color.Transparent,
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
            keyboardOptions = keyboardOptions,
            singleLine = true,
            cursorBrush = SolidColor(Color(0xFF00D632)),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = TextStyle(
                            fontSize = 18.sp,
                            color = Color(0xFF999999),
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
                innerTextField()
            }
        )
    }
}
