package com.smartpay.android

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.smartpay.android.security.SecureStorage
import com.smartpay.android.ui.theme.SmartPayTheme
import com.smartpay.models.*
import com.smartpay.repository.TransferHistoryRepository
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class EnhancedTransactionHistoryActivity : ComponentActivity() {
    private lateinit var transferRepository: TransferHistoryRepository
    private lateinit var secureStorage: SecureStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize dependencies
        transferRepository = TransferHistoryRepository(/* inject ApiService */)
        secureStorage = SecureStorage.getInstance(this)

        setContent {
            SmartPayTheme {
                TransactionHistoryScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TransactionHistoryScreen() {
        var transfers by remember { mutableStateOf<List<TransferItem>>(emptyList()) }
        var summary by remember { mutableStateOf<TransferSummaryData?>(null) }
        var isLoading by remember { mutableStateOf(false) }
        var currentPage by remember { mutableStateOf(1) }
        var hasMorePages by remember { mutableStateOf(true) }
        var filterType by remember { mutableStateOf("all") } // all, sent, received
        var filterStatus by remember { mutableStateOf<String?>(null) }
        
        val context = LocalContext.current

        // Load initial data
        LaunchedEffect(Unit) {
            loadTransfers()
            loadSummary()
        }

        // Load transfers function
        fun loadTransfers(page: Int = 1, reset: Boolean = true) {
            lifecycleScope.launch {
                isLoading = true
                try {
                    val response = transferRepository.getAllTransfers(
                        type = if (filterType == "all") null else filterType,
                        status = filterStatus,
                        page = page,
                        limit = 20
                    )
                    
                    if (response.isSuccessful) {
                        response.body()?.let { apiResponse ->
                            if (reset) {
                                transfers = apiResponse.data.transfers
                            } else {
                                transfers = transfers + apiResponse.data.transfers
                            }
                            hasMorePages = apiResponse.data.pagination.hasNext
                            currentPage = page
                        }
                    } else {
                        Toast.makeText(context, "خطأ في تحميل التحويلات", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "خطأ في الاتصال", Toast.LENGTH_SHORT).show()
                } finally {
                    isLoading = false
                }
            }
        }

        // Load summary function
        fun loadSummary() {
            lifecycleScope.launch {
                try {
                    val response = transferRepository.getTransferSummary()
                    if (response.isSuccessful) {
                        summary = response.body()?.data
                    }
                } catch (e: Exception) {
                    // Handle error silently for summary
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "سجل التحويلات", 
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                        }
                    },
                    actions = {
                        IconButton(onClick = { loadTransfers(1, true); loadSummary() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // Summary Card
                summary?.let { summaryData ->
                    SummaryCard(summaryData)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Filter Row
                FilterRow(
                    selectedType = filterType,
                    selectedStatus = filterStatus,
                    onTypeChanged = { newType ->
                        filterType = newType
                        loadTransfers(1, true)
                    },
                    onStatusChanged = { newStatus ->
                        filterStatus = newStatus
                        loadTransfers(1, true)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Transactions List
                if (isLoading && transfers.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (transfers.isEmpty()) {
                    EmptyState()
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(transfers) { transfer ->
                            TransferCard(transfer = transfer)
                        }

                        // Load more button
                        if (hasMorePages) {
                            item {
                                Button(
                                    onClick = { loadTransfers(currentPage + 1, false) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isLoading
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White
                                        )
                                    } else {
                                        Text("تحميل المزيد")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun SummaryCard(summary: TransferSummaryData) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "ملخص التحويلات",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("إجمالي التحويلات", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${summary.totalTransfers}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text("المرسل", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${formatCurrency(summary.sent.totalAmount)} (${summary.sent.count})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }
                    Column {
                        Text("المستلم", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${formatCurrency(summary.received.totalAmount)} (${summary.received.count})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Green
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun FilterRow(
        selectedType: String,
        selectedStatus: String?,
        onTypeChanged: (String) -> Unit,
        onStatusChanged: (String?) -> Unit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Type Filter
            FilterChip(
                onClick = { onTypeChanged("all") },
                label = { Text("الكل") },
                selected = selectedType == "all"
            )
            FilterChip(
                onClick = { onTypeChanged("sent") },
                label = { Text("مُرسل") },
                selected = selectedType == "sent"
            )
            FilterChip(
                onClick = { onTypeChanged("received") },
                label = { Text("مُستلم") },
                selected = selectedType == "received"
            )
            
            // Status Filter
            FilterChip(
                onClick = { 
                    onStatusChanged(if (selectedStatus == "completed") null else "completed") 
                },
                label = { Text("مكتمل") },
                selected = selectedStatus == "completed"
            )
        }
    }

    @Composable
    fun TransferCard(transfer: TransferItem) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* Handle click for details */ },
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (transfer.direction == "sent") 
                                "إلى: ${transfer.receiver.name}" 
                            else 
                                "من: ${transfer.sender.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatDateTime(transfer.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!transfer.description.isNullOrEmpty()) {
                            Text(
                                text = transfer.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${if (transfer.direction == "sent") "-" else "+"}${formatCurrency(transfer.amount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (transfer.direction == "sent") Color.Red else Color.Green
                        )
                        StatusChip(status = transfer.status)
                    }
                }
            }
        }
    }

    @Composable
    fun StatusChip(status: String) {
        val (backgroundColor, textColor, text) = when (status.lowercase()) {
            "completed" -> Triple(Color.Green.copy(alpha = 0.1f), Color.Green, "مكتمل")
            "pending" -> Triple(Color.Orange.copy(alpha = 0.1f), Color.Orange, "معلق")
            "failed" -> Triple(Color.Red.copy(alpha = 0.1f), Color.Red, "فشل")
            else -> Triple(Color.Gray.copy(alpha = 0.1f), Color.Gray, status)
        }
        
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = backgroundColor
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.Medium
            )
        }
    }

    @Composable
    fun EmptyState() {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "لا توجد تحويلات",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "ستظهر تحويلاتك هنا",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    private fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("ar", "SY"))
        return format.format(amount).replace("SYP", "ل.س")
    }

    private fun formatDateTime(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("ar"))
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString
        }
    }
}