package com.smartpay.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.smartpay.data.models.SendMoneyRequest
import com.smartpay.data.repository.MerchantRepository
import kotlinx.coroutines.launch

class MerchantSendMoneyActivity : ComponentActivity() {

    private val merchantRepository = MerchantRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("SmartPayPrefs", Context.MODE_PRIVATE)
        val uid = prefs.getString("uid", null)

        if (uid.isNullOrEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContent {
            MerchantSendMoneyScreen(onSendMoney = { receiverPhone, amount, note, type ->
                sendMoneyToMerchant(receiverPhone, amount, note, type)
            })
        }
    }

    private fun sendMoneyToMerchant(receiverPhone: String, amount: Long, note: String, type: String) {
        lifecycleScope.launch {
            try {
                val response = merchantRepository.sendMoney(
                    SendMoneyRequest(
                        receiverPhone = receiverPhone,
                        amount = amount,
                        note = "$type - $note"
                    )
                )

                if (response.isSuccessful) {
                    Toast.makeText(this@MerchantSendMoneyActivity, "تم الإرسال بنجاح", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@MerchantSendMoneyActivity, ReceiptActivity::class.java).apply {
                        putExtra("receiverName", receiverPhone)
                        putExtra("amount", amount)
                        putExtra("note", "$type - $note")
                    })
                    finish()
                } else {
                    Toast.makeText(this@MerchantSendMoneyActivity, "فشل الإرسال: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MerchantSendMoneyActivity, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun MerchantSendMoneyScreen(onSendMoney: (String, Long, String, String) -> Unit) {
    val context = LocalContext.current
    var receiverPhone by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val options = listOf("مشتريات", "ديون", "رواتب", "خدمات", "أخرى")
    var transactionType by remember { mutableStateOf(options.first()) }
    var expanded by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 60.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { (context as? ComponentActivity)?.onBackPressedDispatcher?.onBackPressed() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.Black)
                }
                Text("إرسال الأموال", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.size(48.dp))
            }

            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("ل.س", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00D632))
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = amount,
                        onValueChange = { if (it.all { char -> char.isDigit() }) amount = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Black),
                        singleLine = true,
                        cursorBrush = SolidColor(Color(0xFF00D632)),
                        decorationBox = { inner ->
                            if (amount.isEmpty()) {
                                Text("0", fontSize = 56.sp, color = Color.LightGray)
                            }
                            inner()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
                SimpleTextField(receiverPhone, { receiverPhone = it }, "رقم الهاتف", KeyboardType.Number)

                Text("نوع العملية", fontSize = 16.sp, color = Color(0xFF666666))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color(0xFFF7F8FA), RoundedCornerShape(12.dp))
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(transactionType, fontSize = 16.sp)
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }
                }

                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                transactionType = option
                                expanded = false
                            }
                        )
                    }
                }

                SimpleTextField(note, { note = it }, "ملاحظة (اختياري)")
            }

            Button(
                onClick = {
                    val amountLong = amount.toLongOrNull()
                    if (receiverPhone.isBlank() || amountLong == null || amountLong <= 0) {
                        Toast.makeText(context, "يرجى إدخال البيانات بشكل صحيح", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onSendMoney(receiverPhone, amountLong, note, transactionType)
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).height(64.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D632))
            ) {
                Text("إرسال الآن", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SimpleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFFF7F8FA), RoundedCornerShape(12.dp))
            .border(2.dp, Color.Transparent, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = { newValue ->
                if (keyboardType == KeyboardType.Number) {
                    if (newValue.all { it.isDigit() }) onValueChange(newValue)
                } else {
                    onValueChange(newValue)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
            cursorBrush = SolidColor(Color(0xFF00D632)),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, color = Color.Gray)
                }
                inner()
            }
        )
    }
}
