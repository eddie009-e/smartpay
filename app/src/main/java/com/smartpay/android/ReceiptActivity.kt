package com.smartpay.android

import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

class ReceiptActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val receiverName = intent.getStringExtra("receiverName") ?: "مجهول"
        val amount = intent.getLongExtra("amount", 0L)
        val note = intent.getStringExtra("note") ?: ""
        val time = DateFormat.format("yyyy/MM/dd - hh:mm a", Date()).toString()

        setContent {
            ModernReceiptScreen(
                receiverName = receiverName,
                amount = amount,
                note = note,
                time = time
            )
        }
    }
}

@Composable
fun ModernReceiptScreen(
    receiverName: String,
    amount: Long,
    note: String,
    time: String
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Handle back button
    BackHandler {
        val intent = Intent(context, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

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
                .size(200.dp)
                .offset(x = 280.dp, y = 100.dp)
                .background(
                    Color(0xFFD8FBA9).copy(alpha = 0.15f),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(150.dp)
                .offset(x = (-60).dp, y = 300.dp)
                .background(
                    Color.White.copy(alpha = 0.25f),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(120.dp)
                .offset(x = 220.dp, y = 600.dp)
                .background(
                    Color(0xFF2D2D2D).copy(alpha = 0.08f),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Success Header
            SuccessHeader()

            Spacer(modifier = Modifier.height(32.dp))

            // Receipt Card
            ReceiptCard(
                receiverName = receiverName,
                amount = amount,
                note = note,
                time = time
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            ActionButtons(
                onShare = {
                    val shareText = "تم إرسال ${amount} ل.س إلى $receiverName في $time"
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(intent, "مشاركة الإيصال عبر"))
                },
                onBackToDashboard = {
                    val intent = Intent(context, DashboardActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SuccessHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Success Icon
        Card(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFD8FBA9).copy(alpha = 0.2f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Outer ring
                Card(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFD8FBA9).copy(alpha = 0.3f)
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
                            modifier = Modifier.size(50.dp),
                            tint = Color(0xFFD8FBA9)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "تم الإرسال بنجاح!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2D2D2D),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "تمت العملية بنجاح وتم إرسال الأموال",
            fontSize = 16.sp,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ReceiptCard(
    receiverName: String,
    amount: Long,
    note: String,
    time: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
    ) {
        Box {
            // Subtle card patterns
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .offset(x = 250.dp, y = (-60).dp)
                    .background(
                        Color(0xFFD8FBA9).copy(alpha = 0.05f),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .offset(x = (-40).dp, y = 200.dp)
                    .background(
                        Color(0xFF2D2D2D).copy(alpha = 0.03f),
                        CircleShape
                    )
            )

            Column(
                modifier = Modifier.padding(32.dp)
            ) {
                // Receipt Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Receipt,
                        contentDescription = null,
                        tint = Color(0xFFD8FBA9),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "إيصال العملية",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D2D2D)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "SmartPay",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF666666)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Transaction Details
                ReceiptDetailRow(
                    icon = Icons.Default.Person,
                    label = "المرسل إليه",
                    value = receiverName,
                    isHighlighted = true
                )

                ReceiptDetailRow(
                    icon = Icons.Default.AttachMoney,
                    label = "المبلغ",
                    value = "${amount} ل.س",
                    isHighlighted = true
                )

                if (note.isNotBlank()) {
                    ReceiptDetailRow(
                        icon = Icons.Default.Note,
                        label = "الملاحظة",
                        value = note
                    )
                }

                ReceiptDetailRow(
                    icon = Icons.Default.Schedule,
                    label = "تاريخ العملية",
                    value = time
                )

                ReceiptDetailRow(
                    icon = Icons.Default.Verified,
                    label = "حالة العملية",
                    value = "مكتملة",
                    valueColor = Color(0xFFD8FBA9)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Transaction ID (Mock)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF8FDED).copy(alpha = 0.5f)
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
                            Icons.Default.Tag,
                            contentDescription = null,
                            tint = Color(0xFF666666),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "رقم العملية",
                                fontSize = 12.sp,
                                color = Color(0xFF666666)
                            )
                            Text(
                                text = "TXN${System.currentTimeMillis().toString().takeLast(8)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2D2D2D)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isHighlighted: Boolean = false,
    valueColor: Color = Color(0xFF2D2D2D)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isHighlighted) 
                    Color(0xFFD8FBA9).copy(alpha = 0.15f) 
                else 
                    Color(0xFF2D2D2D).copy(alpha = 0.08f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isHighlighted) Color(0xFFD8FBA9) else Color(0xFF666666),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                color = valueColor
            )
        }
    }
}

@Composable
private fun ActionButtons(
    onShare: () -> Unit,
    onBackToDashboard: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Share Button
        Button(
            onClick = onShare,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD8FBA9)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF2D2D2D)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "مشاركة الإيصال",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D2D2D)
                )
            }
        }

        // Save/Download Button (Mock)
        OutlinedButton(
            onClick = { /* TODO: Implement save functionality */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, Color(0xFFD8FBA9).copy(alpha = 0.3f)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFD8FBA9)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "حفظ الإيصال",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Back to Dashboard Button
        Button(
            onClick = onBackToDashboard,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2D2D2D)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Home,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "العودة إلى الرئيسية",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}