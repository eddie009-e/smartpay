package com.smartpay.android

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
import com.smartpay.data.repository.UserRepository
import kotlinx.coroutines.launch

class RequestPaymentActivity : ComponentActivity() {

    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // تحميل التوكن من EncryptedSharedPreferences
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val securePrefs = EncryptedSharedPreferences.create(
            "SmartPaySecurePrefs",
            masterKey,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val token = securePrefs.getString("token", null)

        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "الجلسة غير صالحة، يرجى تسجيل الدخول", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            SmartPayTheme {
                ModernRequestPaymentScreen(
                    token = token,
                    onBack = { finish() },
                    userRepository = userRepository
                )
            }
        }
    }
}

@Composable
fun ModernRequestPaymentScreen(
    token: String,
    onBack: () -> Unit, 
    userRepository: UserRepository
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var receiverPhone by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

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
            HeaderSection(onBack = onBack)

            Spacer(modifier = Modifier.height(32.dp))

            // Welcome Section
            WelcomeSection()

            Spacer(modifier = Modifier.height(32.dp))

            // Request Form Section
            RequestFormSection(
                receiverPhone = receiverPhone,
                onReceiverPhoneChange = { receiverPhone = it },
                amount = amount,
                onAmountChange = { amount = it },
                note = note,
                onNoteChange = { note = it }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Send Request Button
            SendRequestButton(
                isLoading = isLoading,
                onClick = {
                    if (receiverPhone.isBlank() || amount.isBlank()) {
                        Toast.makeText(context, "يرجى إدخال الرقم والمبلغ", Toast.LENGTH_SHORT).show()
                        return@SendRequestButton
                    }

                    if (!receiverPhone.matches(Regex("^(\\+963|0)9[0-9]{8}\$"))) {
                        Toast.makeText(context, "أدخل رقم هاتف صحيح", Toast.LENGTH_SHORT).show()
                        return@SendRequestButton
                    }

                    val amountLong = amount.toLongOrNull()
                    if (amountLong == null || amountLong <= 0) {
                        Toast.makeText(context, "أدخل مبلغًا صحيحًا", Toast.LENGTH_SHORT).show()
                        return@SendRequestButton
                    }

                    isLoading = true
                    (context as ComponentActivity).lifecycleScope.launch {
                        try {
                            val response = userRepository.sendMoneyRequest(receiverPhone, amountLong)
                            if (response.isSuccessful) {
                                Toast.makeText(context, "تم إرسال الطلب بنجاح", Toast.LENGTH_SHORT).show()
                                onBack()
                            } else {
                                Toast.makeText(context, "فشل في إرسال الطلب", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isLoading = false
                        }
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
    
    // New Modern Components
    @Composable
    fun HeaderSection(onBack: () -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Card(
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onBack() },
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
                text = "طلب دفعة",
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
                    Icons.Default.CallReceived,
                    contentDescription = "طلب دفعة",
                    modifier = Modifier.size(60.dp),
                    tint = colorResource(R.color.primary_dark)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "طلب دفعة",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.primary_dark),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "اطلب مبلغًا من عميل أو صديق",
            fontSize = 16.sp,
            color = colorResource(R.color.gray_dark),
            textAlign = TextAlign.Center
        )
    }

    @Composable
    fun RequestFormSection(
        receiverPhone: String,
        onReceiverPhoneChange: (String) -> Unit,
        amount: String,
        onAmountChange: (String) -> Unit,
        note: String,
        onNoteChange: (String) -> Unit
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
                // Phone Number Field
                Column {
                    Text(
                        text = "رقم هاتف العميل",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorResource(R.color.gray_dark)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ModernTextField(
                        value = receiverPhone,
                        onValueChange = onReceiverPhoneChange,
                        placeholder = "+963 9XX XXX XXX",
                        leadingIcon = Icons.Default.Phone,
                        keyboardType = KeyboardType.Phone
                    )
                }

                // Amount Field
                Column {
                    Text(
                        text = "المبلغ المطلوب (ل.س)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorResource(R.color.gray_dark)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ModernTextField(
                        value = amount,
                        onValueChange = { if (it.all { char -> char.isDigit() }) onAmountChange(it) },
                        placeholder = "0",
                        leadingIcon = Icons.Default.AttachMoney,
                        keyboardType = KeyboardType.Number
                    )
                }

                // Note Field
                Column {
                    Text(
                        text = "ملاحظة (اختياري)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorResource(R.color.gray_dark)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ModernTextField(
                        value = note,
                        onValueChange = onNoteChange,
                        placeholder = "سبب طلب الدفعة...",
                        leadingIcon = Icons.Default.Description,
                        keyboardType = KeyboardType.Text
                    )
                }
            }
        }
    }

    @Composable
    fun SendRequestButton(
        isLoading: Boolean,
        onClick: () -> Unit
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading,
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
                        Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إرسال طلب الدفعة",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }

    @Composable
    fun ModernTextField(
        value: String,
        onValueChange: (String) -> Unit,
        placeholder: String,
        leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
        keyboardType: KeyboardType
    ) {
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
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = colorResource(R.color.primary_green)
                )
                Spacer(modifier = Modifier.width(12.dp))
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
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
}

