package com.smartpay.android

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.smartpay.data.models.Invoice
import com.smartpay.data.repository.UserRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SentInvoicesActivity : ComponentActivity() {

    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SentInvoicesScreen(userRepository = userRepository)
        }
    }
}

@Composable
fun SentInvoicesScreen(userRepository: UserRepository) {
    val context = LocalContext.current
    var invoices by remember { mutableStateOf<List<Invoice>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // تحميل الفواتير من API
    LaunchedEffect(Unit) {
        val activity = context as? ComponentActivity

        // الحصول على الـ token من التخزين الآمن
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val securePrefs = EncryptedSharedPreferences.create(
            "SmartPaySecurePrefs",
            masterKey,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val token = securePrefs.getString("token", null)

        if (token.isNullOrBlank()) {
            Toast.makeText(context, "فشل: لم يتم العثور على التوكن", Toast.LENGTH_SHORT).show()
            isLoading = false
            return@LaunchedEffect
        }

        activity?.lifecycleScope?.launch {
            try {
                val response = userRepository.getInvoices(token)
                if (response.isSuccessful) {
                    invoices = response.body() ?: emptyList()
                } else {
                    Toast.makeText(context, "فشل في جلب الفواتير", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = "الفواتير المرسلة",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF00D632))
                }
            } else if (invoices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد فواتير حتى الآن",
                        color = Color(0xFF666666),
                        fontFamily = FontFamily.SansSerif
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(invoices) { invoice ->
                        InvoiceCard(invoice)
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceCard(invoice: Invoice) {
    val formattedDate = remember(invoice.issuedAt) {
        try {
            SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                .format(Date(invoice.issuedAt))
        } catch (e: Exception) {
            "غير معروف"
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("إلى: ${invoice.toPhone}", fontWeight = FontWeight.Bold, color = Color.Black)
            Text("المبلغ: ${invoice.amount} ل.س", color = Color(0xFF00D632))
            if (!invoice.description.isNullOrBlank()) {
                Text("الوصف: ${invoice.description}", color = Color.Black)
            }
            Text("التاريخ: $formattedDate", fontSize = 12.sp, color = Color(0xFF999999))
        }
    }
}
