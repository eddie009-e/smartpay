package com.smartpay.android

import android.Manifest
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.smartpay.data.models.SendMoneyRequest
import com.smartpay.models.TransferRequest
import com.smartpay.data.repository.UserRepository
import kotlinx.coroutines.launch

class SendMoneyActivity : ComponentActivity() {

    private val userRepository = UserRepository()
    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // احصل على التوكن الآمن
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val securePrefs = EncryptedSharedPreferences.create(
            "SmartPaySecurePrefs",
            masterKey,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        token = securePrefs.getString("token", null)

        if (token.isNullOrEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContent {
            SendMoneyScreen { receiverPhone, amount, note, type ->
                sendMoney(receiverPhone, amount, note, type)
            }
        }
    }

    private fun sendMoney(
        receiverPhone: String,
        amount: Long,
        note: String,
        type: String
    ) {
        lifecycleScope.launch {
            try {
                val response = userRepository.transferMoney(
                    TransferRequest(
                        receiverPhone = receiverPhone,
                        amount = amount,
                        note = "$type - $note"
                    )
                )
                if (response.isSuccessful) {
                    Toast.makeText(this@SendMoneyActivity, "تم إرسال الأموال بنجاح", Toast.LENGTH_SHORT).show()
                    startActivity(
                        Intent(this@SendMoneyActivity, ReceiptActivity::class.java).apply {
                            putExtra("receiverName", receiverPhone)
                            putExtra("amount", amount)
                            putExtra("note", "$type - $note")
                        }
                    )
                    finish()
                } else {
                    Toast.makeText(
                        this@SendMoneyActivity,
                        "فشل الإرسال: ${response.message()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SendMoneyActivity, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendMoneyScreen(onSendMoney: (String, Long, String, String) -> Unit) {
    val context = LocalContext.current

    var receiverPhone by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val options = listOf("شخصي", "تجاري", "إيجار", "مشتريات")
    var transactionType by remember { mutableStateOf(options.first()) }
    var expanded by remember { mutableStateOf(false) }

    val contacts = remember { mutableStateListOf<Pair<String, String>>() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            loadContacts(context.contentResolver, contacts) { }
        } else {
            Toast.makeText(context, "تم رفض إذن الأسماء", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { (context as? ComponentActivity)?.onBackPressed() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.Black)
                }
                Text(
                    "إرسال الأموال",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.size(48.dp))
            }

            // Amount Field
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "ل.س",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00D632),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    BasicTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { ch -> ch.isDigit() } },
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

            // Input Fields
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
                SimpleTextField(
                    value = receiverPhone,
                    onValueChange = { receiverPhone = it },
                    placeholder = "رقم الهاتف"
                )

                // Contact List
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    LazyColumn(modifier = Modifier.padding(8.dp)) {
                        items(contacts) { (name, number) ->
                            ContactItem(
                                name = name,
                                number = number,
                                isRegistered = true,
                                onClick = { receiverPhone = number }
                            )
                        }
                    }
                }

                // Transaction Type Dropdown
                Column {
                    Text("نوع العملية", fontSize = 16.sp, color = Color(0xFF666666))
                    SimpleDropdown(value = transactionType, expanded = expanded, onClick = { expanded = !expanded })
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
                }

                // Note
                SimpleTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = "ملاحظة (اختياري)"
                )
            }

            // Send Button
            Button(
                onClick = {
                    val amountLong = amount.toLongOrNull()
                    if (receiverPhone.isBlank() || amountLong == null || amountLong <= 0) {
                        Toast.makeText(context, "يرجى إدخال البيانات بشكل صحيح", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onSendMoney(receiverPhone, amountLong, note, transactionType)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
                    .height(64.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D632))
            ) {
                Text("إرسال الآن", fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif)
            }
        }
    }
}

// --- Reusable Components ---
@Composable
fun SimpleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
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
            onValueChange = onValueChange,
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

@Composable
fun SimpleDropdown(value: String, expanded: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable { onClick() }
            .background(Color(0xFFF7F8FA), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(text = value, fontSize = 16.sp)
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null
            )
        }
    }
}

@Composable
fun ContactItem(name: String, number: String, isRegistered: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isRegistered) Color(0xFF00D632).copy(alpha = 0.1f) else Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isRegistered) Icons.Default.CheckCircle else Icons.Default.Person,
                contentDescription = null,
                tint = if (isRegistered) Color(0xFF00D632) else Color.Gray
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Text(number, color = Color.Gray, fontSize = 14.sp)
        }

        if (isRegistered) {
            Text("SmartPay", color = Color(0xFF00D632), fontSize = 12.sp)
        }
    }
}

// --- Load Contacts ---
fun loadContacts(
    contentResolver: android.content.ContentResolver,
    contacts: MutableList<Pair<String, String>>,
    onComplete: () -> Unit
) {
    contacts.clear()
    val cursor = contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        ),
        null, null,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
    )
    cursor?.use {
        while (it.moveToNext()) {
            val name = it.getString(0)
            val number = it.getString(1).replace(" ", "").replace("-", "")
            contacts.add(Pair(name, number))
        }
    }
    onComplete()
}
