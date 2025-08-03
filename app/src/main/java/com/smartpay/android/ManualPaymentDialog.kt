package com.smartpay.android

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Manual Payment Dialog for SmartPay Subscription Upgrades
 * 
 * Shows payment instructions when Google IAP is not available:
 * - SmartPay admin phone number
 * - Payment amount and plan details
 * - Copy phone number functionality
 * - WhatsApp integration
 * - Payment confirmation button
 */
object ManualPaymentDialog {
    
    const val SMARTPAY_ADMIN_PHONE = "+963999999999"
    const val WHATSAPP_MESSAGE_TEMPLATE = "مرحباً، أريد ترقية اشتراك SmartPay إلى %s بقيمة $%.2f"
    
    @Composable
    fun ManualPaymentDialog(
        plan: SubscriptionPlan,
        onDismiss: () -> Unit,
        onConfirmPayment: () -> Unit
    ) {
        val context = LocalContext.current
        var showConfirmation by remember { mutableStateOf(false) }
        
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Header
                    PaymentDialogHeader(plan = plan)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Payment instructions
                    PaymentInstructions(
                        plan = plan,
                        onCopyPhone = { copyPhoneToClipboard(context) },
                        onOpenWhatsApp = { openWhatsApp(context, plan) }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Warning message
                    WarningMessage()
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Action buttons
                    PaymentDialogActions(
                        showConfirmation = showConfirmation,
                        onConfirmPayment = {
                            showConfirmation = true
                        },
                        onFinalConfirm = {
                            onConfirmPayment()
                            onDismiss()
                        },
                        onCancel = onDismiss
                    )
                }
            }
        }
    }
    
    @Composable
    private fun PaymentDialogHeader(plan: SubscriptionPlan) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Plan icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        brush = when (plan.planCode) {
                            "Standard" -> Brush.radialGradient(
                                colors = listOf(Color(0xFF2196F3), Color(0xFF1976D2))
                            )
                            "Pro" -> Brush.radialGradient(
                                colors = listOf(Color(0xFF9C27B0), Color(0xFF7B1FA2))
                            )
                            else -> Brush.radialGradient(
                                colors = listOf(Color(0xFF4CAF50), Color(0xFF388E3C))
                            )
                        },
                        shape = RoundedCornerShape(50)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (plan.planCode) {
                        "Standard" -> Icons.Default.Star
                        "Pro" -> Icons.Default.Stars
                        else -> Icons.Default.StarBorder
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "دفع يدوي عبر SmartPay",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = plan.name,
                fontSize = 16.sp,
                color = Color(0xFF666666)
            )
            
            Text(
                text = "$${plan.monthlyPrice}/شهر",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = when (plan.planCode) {
                    "Standard" -> Color(0xFF2196F3)
                    "Pro" -> Color(0xFF9C27B0)
                    else -> Color(0xFF4CAF50)
                }
            )
        }
    }
    
    @Composable
    private fun PaymentInstructions(
        plan: SubscriptionPlan,
        onCopyPhone: () -> Unit,
        onOpenWhatsApp: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "تعليمات الدفع",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Step 1: Transfer money
                PaymentStep(
                    stepNumber = 1,
                    title = "قم بتحويل المبلغ",
                    description = "احول مبلغ $${plan.monthlyPrice} إلى رقم SmartPay التالي:"
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Phone number card
                PhoneNumberCard(onCopyPhone = onCopyPhone, onOpenWhatsApp = onOpenWhatsApp)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Step 2: Confirm payment
                PaymentStep(
                    stepNumber = 2,
                    title = "أكد عملية الدفع",
                    description = "بعد التحويل، اضغط على زر \"تم الدفع\" أدناه"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Step 3: Wait for activation
                PaymentStep(
                    stepNumber = 3,
                    title = "انتظر التفعيل",
                    description = "سيتم مراجعة الدفع وتفعيل الاشتراك خلال 24 ساعة"
                )
            }
        }
    }
    
    @Composable
    private fun PhoneNumberCard(
        onCopyPhone: () -> Unit,
        onOpenWhatsApp: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFF00D632))
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        tint = Color(0xFF00D632),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = SMARTPAY_ADMIN_PHONE,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Copy button
                    OutlinedButton(
                        onClick = onCopyPhone,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF00D632)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF00D632))
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نسخ", fontSize = 12.sp)
                    }
                    
                    // WhatsApp button
                    Button(
                        onClick = onOpenWhatsApp,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF25D366)
                        )
                    ) {
                        Icon(
                            Icons.Default.Message,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("واتساب", fontSize = 12.sp)
                    }
                }
            }
        }
    }
    
    @Composable
    private fun PaymentStep(
        stepNumber: Int,
        title: String,
        description: String
    ) {
        Row(
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color(0xFF00D632), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNumber.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color(0xFF666666),
                    lineHeight = 16.sp
                )
            }
        }
    }
    
    @Composable
    private fun WarningMessage() {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
            border = BorderStroke(1.dp, Color(0xFFFF9800))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "لا تضغط على \"تم الدفع\" إلا بعد إتمام التحويل فعلياً. الضغط على الزر قبل الدفع قد يؤدي إلى رفض طلب الترقية.",
                    fontSize = 12.sp,
                    color = Color(0xFFE65100),
                    lineHeight = 16.sp
                )
            }
        }
    }
    
    @Composable
    private fun PaymentDialogActions(
        showConfirmation: Boolean,
        onConfirmPayment: () -> Unit,
        onFinalConfirm: () -> Unit,
        onCancel: () -> Unit
    ) {
        if (showConfirmation) {
            // Final confirmation
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
                border = BorderStroke(1.dp, Color(0xFF4CAF50))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "هل قمت بتحويل المبلغ فعلياً؟",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "سيتم تقديم طلب الترقية للمراجعة",
                        fontSize = 12.sp,
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء")
                        }
                        Button(
                            onClick = onFinalConfirm,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Text("نعم، تم الدفع")
                        }
                    }
                }
            }
        } else {
            // Initial buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("إلغاء", fontSize = 14.sp)
                }
                
                Button(
                    onClick = onConfirmPayment,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00D632)
                    )
                ) {
                    Icon(
                        Icons.Default.Payment,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تم الدفع", fontSize = 14.sp)
                }
            }
        }
    }
    
    /**
     * Show manual payment dialog
     */
    fun show(
        context: Context,
        plan: SubscriptionPlan,
        onConfirmPayment: () -> Unit
    ) {
        // This would typically be called from a Compose context
        // For now, we'll show a simple toast with instructions
        val message = """
            تعليمات الدفع اليدوي:
            
            1. احول مبلغ $${plan.monthlyPrice} إلى:
               $SMARTPAY_ADMIN_PHONE
            
            2. بعد التحويل، اضغط على "تم الدفع"
            
            3. سيتم المراجعة خلال 24 ساعة
        """.trimIndent()
        
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        
        // Copy phone number to clipboard for convenience
        copyPhoneToClipboard(context)
        
        // After showing instructions, allow user to confirm
        onConfirmPayment()
    }
    
    private fun copyPhoneToClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("SmartPay Admin Phone", SMARTPAY_ADMIN_PHONE)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "تم نسخ رقم الهاتف", Toast.LENGTH_SHORT).show()
    }
    
    private fun openWhatsApp(context: Context, plan: SubscriptionPlan) {
        try {
            val message = String.format(WHATSAPP_MESSAGE_TEMPLATE, plan.name, plan.monthlyPrice)
            val encodedMessage = Uri.encode(message)
            val whatsappIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://wa.me/$SMARTPAY_ADMIN_PHONE?text=$encodedMessage")
            }
            context.startActivity(whatsappIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح واتساب", Toast.LENGTH_SHORT).show()
        }
    }
}