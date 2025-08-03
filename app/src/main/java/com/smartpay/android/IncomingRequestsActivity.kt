package com.smartpay.android

import android.Manifest
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartpay.data.repository.UserRepository
import kotlinx.coroutines.launch

class IncomingRequestsActivity : ComponentActivity() {

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
        val token = securePrefs.getString("token", null) ?: ""

        if (token.isEmpty()) {
            Toast.makeText(this, "الجلسة غير صالحة، يرجى تسجيل الدخول", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            ModernIncomingRequestsScreen(
                onBack = { onBackPressed() },
                userRepository = userRepository
            )
        }
    }
}

@Composable
fun ModernIncomingRequestsScreen(onBack: () -> Unit, userRepository: UserRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var requests by remember { mutableStateOf<List<IncomingRequest>>(emptyList()) }
    var contactsMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isPermissionGranted by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isPermissionGranted = granted
        if (granted) {
            contactsMap = getContactsMap(context)
        } else {
            Toast.makeText(context, "تم رفض إذن جهات الاتصال", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.READ_CONTACTS)
        isLoading = true
        requests = loadRequestsFromApi(userRepository)
        isLoading = false
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
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
                    .size(180.dp)
                    .offset(x = 250.dp, y = 100.dp)
                    .background(
                        Color(0xFFD8FBA9).copy(alpha = 0.15f),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .offset(x = (-40).dp, y = 300.dp)
                    .background(
                        Color.White.copy(alpha = 0.25f),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .offset(x = 200.dp, y = 500.dp)
                    .background(
                        Color(0xFF2D2D2D).copy(alpha = 0.08f),
                        CircleShape
                    )
            )

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                // Modern Header
                ModernHeader(onBack = onBack, requestCount = requests.size)

                when {
                    isLoading -> {
                        LoadingSection()
                    }
                    
                    requests.isNotEmpty() -> {
                        RequestsList(
                            requests = requests,
                            contactsMap = contactsMap,
                            onAccept = { request ->
                                scope.launch {
                                    val res = userRepository.acceptRequest(request.id)
                                    if (res.isSuccessful) {
                                        requests = requests - request
                                        Toast.makeText(context, "تم قبول الطلب", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "فشل قبول الطلب", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onReject = { request ->
                                scope.launch {
                                    val res = userRepository.rejectRequest(request.id)
                                    if (res.isSuccessful) {
                                        requests = requests - request
                                        Toast.makeText(context, "تم رفض الطلب", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "فشل رفض الطلب", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                    
                    else -> {
                        EmptyStateSection()
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernHeader(
    onBack: () -> Unit,
    requestCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back Button
            Card(
                modifier = Modifier
                    .size(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFD8FBA9).copy(alpha = 0.2f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "رجوع",
                        tint = Color(0xFF2D2D2D),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Title Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CallReceived,
                        contentDescription = null,
                        tint = Color(0xFFD8FBA9),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "الطلبات الواردة",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D2D2D),
                        fontFamily = FontFamily.SansSerif
                    )
                }
                if (requestCount > 0) {
                    Text(
                        text = "$requestCount طلب جديد",
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                }
            }

            // Notification Badge
            Card(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (requestCount > 0) Color(0xFFD8FBA9) else Color(0xFFF0F0F0)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (requestCount > 0) {
                        Text(
                            text = requestCount.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D2D2D)
                        )
                    } else {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color(0xFF999999),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingSection() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFD8FBA9),
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "جاري تحميل الطلبات...",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2D2D2D)
                )
                Text(
                    "يرجى الانتظار",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

@Composable
private fun EmptyStateSection() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon Background
                Card(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFD8FBA9).copy(alpha = 0.2f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFFD8FBA9),
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    "لا توجد طلبات جديدة",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D2D2D),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    "ستظهر هنا طلبات الأموال من الآخرين",
                    fontSize = 16.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "عندما يطلب منك أحد الأشخاص مبلغًا ستجد الطلب هنا",
                    fontSize = 14.sp,
                    color = Color(0xFF999999),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun RequestsList(
    requests: List<IncomingRequest>,
    contactsMap: Map<String, String>,
    onAccept: (IncomingRequest) -> Unit,
    onReject: (IncomingRequest) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 24.dp)
    ) {
        // Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFD8FBA9).copy(alpha = 0.15f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFD8FBA9),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "طلبات الدفع الجديدة",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D2D2D)
                        )
                        Text(
                            text = "اختر قبول أو رفض كل طلب",
                            fontSize = 14.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
            }
        }

        // Requests
        items(requests, key = { it.id }) { request ->
            ModernRequestCard(
                nameOrPhone = contactsMap[request.phone] ?: request.phone,
                phone = request.phone,
                amount = request.amount,
                onAccept = { onAccept(request) },
                onReject = { onReject(request) }
            )
        }
    }
}

@Composable
fun ModernRequestCard(
    nameOrPhone: String,
    phone: String,
    amount: Long,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp, 
                shape = RoundedCornerShape(24.dp), 
                clip = false
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box {
            // Subtle card pattern
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .offset(x = 200.dp, y = (-50).dp)
                    .background(
                        Color(0xFFD8FBA9).copy(alpha = 0.05f),
                        CircleShape
                    )
            )

            Column(modifier = Modifier.padding(24.dp)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar
                    Card(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFD8FBA9).copy(alpha = 0.2f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = nameOrPhone.firstOrNull()?.toString()?.uppercase() ?: "؟",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD8FBA9)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // User Info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = nameOrPhone,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D2D2D),
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            "يطلب منك", 
                            fontSize = 14.sp, 
                            color = Color(0xFF666666)
                        )
                        if (nameOrPhone != phone) {
                            Text(
                                phone,
                                fontSize = 12.sp,
                                color = Color(0xFF999999)
                            )
                        }
                    }

                    // Amount Display
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFD8FBA9).copy(alpha = 0.15f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "ل.س ",
                                    fontSize = 14.sp,
                                    color = Color(0xFFD8FBA9),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                                Text(
                                    text = "$amount",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF2D2D2D),
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Reject Button
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFE53E3E)
                        ),
                        border = BorderStroke(2.dp, Color(0xFFE53E3E).copy(alpha = 0.3f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "رفض", 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 16.sp
                            )
                        }
                    }

                    // Accept Button
                    Button(
                        onClick = onAccept,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD8FBA9)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFF2D2D2D)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "قبول", 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 16.sp,
                                color = Color(0xFF2D2D2D)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Keep existing functions unchanged
suspend fun loadRequestsFromApi(userRepository: UserRepository): List<IncomingRequest> {
    val res = userRepository.getIncomingRequests()
    return if (res.isSuccessful && res.body() != null) {
        res.body()!!.map {
            IncomingRequest(
                id = it.id,
                phone = it.phone,
                amount = it.amount
            )
        }
    } else {
        emptyList()
    }
}

fun getContactsMap(context: android.content.Context): Map<String, String> {
    val map = mutableMapOf<String, String>()
    val cursor = context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        ),
        null,
        null,
        null
    )
    cursor?.use {
        while (it.moveToNext()) {
            val name = it.getString(0)
            val number = it.getString(1).replace(" ", "").replace("-", "")
            map[number] = name
        }
    }
    return map
}

data class IncomingRequest(
    val id: String,
    val phone: String,
    val amount: Long
)