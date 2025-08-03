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

class WalletActivity : ComponentActivity() {

    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val securePrefs = EncryptedSharedPreferences.create(
            "SmartPaySecurePrefs",
            masterKey,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val token = securePrefs.getString("token", null)
        if (token.isNullOrBlank()) {
            Toast.makeText(this, "مستخدم غير مصرح", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            SmartPayTheme {
                ModernWalletScreen(token, userRepository)
            }
        }
    }
}

@Composable
fun ModernWalletScreen(token: String, userRepository: UserRepository) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var balance by remember { mutableStateOf(0L) }
    var isLoading by remember { mutableStateOf(true) }
    var topUpAmount by remember { mutableStateOf("") }
    var withdrawAmount by remember { mutableStateOf("") }
    var isTopUpLoading by remember { mutableStateOf(false) }
    var isWithdrawLoading by remember { mutableStateOf(false) }

    // --- جلب الرصيد ---
    LaunchedEffect(Unit) {
        try {
            val response = userRepository.getWallet(token)
            if (response.isSuccessful && response.body() != null) {
                balance = response.body()!!.balance
            } else {
                Toast.makeText(context, "فشل في تحميل الرصيد", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
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

            // Balance Section
            BalanceSection(
                balance = balance,
                isLoading = isLoading
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Top Up Section
            TopUpSection(
                amount = topUpAmount,
                onAmountChange = { topUpAmount = it },
                isLoading = isTopUpLoading,
                onTopUp = {
                    val amount = topUpAmount.toLongOrNull()
                    if (amount != null && amount > 0 && amount <= 1_000_000) {
                        isTopUpLoading = true
                        (context as ComponentActivity).lifecycleScope.launch {
                            try {
                                val response = userRepository.topUp(token, amount)
                                if (response.isSuccessful) {
                                    balance += amount
                                    topUpAmount = ""
                                    Toast.makeText(context, "تم شحن +$amount ل.س", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "فشل في الشحن", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isTopUpLoading = false
                            }
                        }
                    } else {
                        Toast.makeText(context, "أدخل مبلغًا صالحًا", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Withdraw Section
            WithdrawSection(
                amount = withdrawAmount,
                onAmountChange = { withdrawAmount = it },
                isLoading = isWithdrawLoading,
                currentBalance = balance,
                onWithdraw = {
                    val amount = withdrawAmount.toLongOrNull()
                    if (amount != null && amount > 0 && amount <= balance) {
                        isWithdrawLoading = true
                        (context as ComponentActivity).lifecycleScope.launch {
                            try {
                                val response = userRepository.withdraw(token, amount)
                                if (response.isSuccessful) {
                                    balance -= amount
                                    withdrawAmount = ""
                                    Toast.makeText(context, "تم تحويل $amount ل.س إلى البنك", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "فشل في التحويل", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isWithdrawLoading = false
                            }
                        }
                    } else {
                        Toast.makeText(context, "تحقق من صحة المبلغ", Toast.LENGTH_SHORT).show()
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(40.dp))
        }
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
                text = "المحفظة",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.primary_dark)
            )
            
            Spacer(modifier = Modifier.width(48.dp))
        }
    }

    @Composable
    fun BalanceSection(
        balance: Long,
        isLoading: Boolean
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = colorResource(R.color.primary_green)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "الرصيد الحالي",
                    fontSize = 16.sp,
                    color = colorResource(R.color.gray_dark),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (isLoading) {
                    CircularProgressIndicator(
                        color = colorResource(R.color.primary_green),
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = balance.toString(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(R.color.primary_dark)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ل.س",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorResource(R.color.primary_green)
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun TopUpSection(
        amount: String,
        onAmountChange: (String) -> Unit,
        isLoading: Boolean,
        onTopUp: () -> Unit
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
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = colorResource(R.color.primary_green)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "شحن الرصيد",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.primary_dark)
                    )
                }
                
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
                            value = amount,
                            onValueChange = { if (it.all { char -> char.isDigit() }) onAmountChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = colorResource(R.color.primary_dark)
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            cursorBrush = SolidColor(colorResource(R.color.primary_green)),
                            decorationBox = { innerTextField ->
                                if (amount.isEmpty()) {
                                    Text(
                                        text = "أدخل مبلغ الشحن",
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
                
                Button(
                    onClick = onTopUp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading && amount.isNotEmpty(),
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
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "شحن من البنك",
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
    fun WithdrawSection(
        amount: String,
        onAmountChange: (String) -> Unit,
        isLoading: Boolean,
        currentBalance: Long,
        onWithdraw: () -> Unit
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
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = colorResource(R.color.primary_green)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "سحب إلى البنك",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.primary_dark)
                    )
                }
                
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
                            value = amount,
                            onValueChange = { if (it.all { char -> char.isDigit() }) onAmountChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = colorResource(R.color.primary_dark)
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            cursorBrush = SolidColor(colorResource(R.color.primary_green)),
                            decorationBox = { innerTextField ->
                                if (amount.isEmpty()) {
                                    Text(
                                        text = "أدخل مبلغ السحب (ماكس: $currentBalance)",
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
                
                Button(
                    onClick = onWithdraw,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading && amount.isNotEmpty(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.primary_dark),
                        disabledContainerColor = colorResource(R.color.gray_light)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CallMade,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تحويل إلى البنك",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
