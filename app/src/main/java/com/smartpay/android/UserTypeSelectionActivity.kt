package com.smartpay.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.smartpay.android.R
import com.smartpay.android.ui.theme.SmartPayTheme

class UserTypeSelectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SmartPayTheme {
                ModernUserTypeSelectionScreen()
            }
        }
    }

    @Composable
    fun ModernUserTypeSelectionScreen() {
        var selectedType by remember { mutableStateOf<String?>(null) }
        var showBottomSheet by remember { mutableStateOf(false) }
        val context = LocalContext.current
        val scrollState = rememberScrollState()

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

                // Welcome Section
                WelcomeSection()

                Spacer(modifier = Modifier.height(32.dp))

                // User Type Selection Cards
                UserTypeSelectionSection(
                    selectedType = selectedType,
                    onTypeSelected = { selectedType = it }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Action Buttons Section
                ActionButtonsSection(
                    selectedType = selectedType,
                    onRegisterClick = {
                        selectedType?.let {
                            val intent = if (it == "personal") {
                                Intent(this@UserTypeSelectionActivity, RegisterActivity::class.java)
                            } else {
                                Intent(this@UserTypeSelectionActivity, RegisterBusinessActivity::class.java)
                            }
                            intent.putExtra("userType", it)
                            startActivity(intent)
                            finish()
                        }
                    },
                    onLoginClick = {
                        selectedType?.let {
                            val intent = Intent(this@UserTypeSelectionActivity, ModernLoginActivity::class.java)
                            intent.putExtra("userType", it)
                            startActivity(intent)
                            finish()
                        }
                    },
                    onInfoClick = { showBottomSheet = true }
                )

                Spacer(modifier = Modifier.height(24.dp))

            }
            
            // Info Dialog
            if (showBottomSheet) {
                InfoDialog(onDismiss = { showBottomSheet = false })
            }
        }
    }

    @Composable
    fun HeaderSection() {
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
                    Icons.Default.PersonAdd,
                    contentDescription = "اختيار نوع الحساب",
                    modifier = Modifier.size(60.dp),
                    tint = colorResource(R.color.primary_dark)
                )
            }
        }
    }

    @Composable
    fun WelcomeSection() {
        Text(
            text = "اختر نوع حسابك",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.primary_dark),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "يمكنك تغيير نوع الحساب لاحقاً",
            fontSize = 16.sp,
            color = colorResource(R.color.gray_dark),
            textAlign = TextAlign.Center
        )
    }

    @Composable
    fun UserTypeSelectionSection(
        selectedType: String?,
        onTypeSelected: (String) -> Unit
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ModernUserTypeCard(
                title = "حساب شخصي",
                description = "حول الأموال لأصدقائك وعائلتك\nبدون رسوم إضافية",
                icon = Icons.Default.Person,
                isSelected = selectedType == "personal",
                onClick = { onTypeSelected("personal") }
            )

            ModernUserTypeCard(
                title = "حساب تجاري",
                description = "استقبل المدفوعات من العملاء\nواحصل على تقارير مفصلة",
                icon = Icons.Default.Business,
                isSelected = selectedType == "business",
                onClick = { onTypeSelected("business") }
            )
        }
    }

    @Composable
    fun ActionButtonsSection(
        selectedType: String?,
        onRegisterClick: () -> Unit,
        onLoginClick: () -> Unit,
        onInfoClick: () -> Unit
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Register Button
            Button(
                onClick = onRegisterClick,
                enabled = selectedType != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.primary_green),
                    disabledContainerColor = colorResource(R.color.gray_light)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "متابعة للتسجيل",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }

            // Login Button
            OutlinedButton(
                onClick = onLoginClick,
                enabled = selectedType != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (selectedType != null) colorResource(R.color.primary_green) else colorResource(R.color.gray_medium)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Login,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تسجيل الدخول",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Info Button
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(R.color.white_transparent_60)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                TextButton(
                    onClick = onInfoClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = colorResource(R.color.primary_green)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ما الفرق بين الحساب الشخصي والتجاري؟",
                            fontSize = 14.sp,
                            color = colorResource(R.color.primary_green),
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun InfoDialog(onDismiss: () -> Unit) {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(R.color.white_transparent_80)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Help,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = colorResource(R.color.primary_green)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "الفرق بين الحساب الشخصي والتجاري",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.primary_dark),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = colorResource(R.color.primary_green),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "الحساب الشخصي مناسب للتحويلات بين الأصدقاء والعائلة بدون رسوم، مجاني وسهل الاستخدام.",
                            fontSize = 14.sp,
                            color = colorResource(R.color.gray_dark)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Business,
                            contentDescription = null,
                            tint = colorResource(R.color.primary_green),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "الحساب التجاري مخصص لأصحاب الأعمال، يتيح استلام وارسال المدفوعات وعرض التقارير والرواتب والمزيد.",
                            fontSize = 14.sp,
                            color = colorResource(R.color.gray_dark)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.primary_green)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
                    ) {
                        Text(
                            text = "فهمت",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernUserTypeCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                colorResource(R.color.primary_green).copy(alpha = 0.1f)
            } else {
                colorResource(R.color.white_transparent_80)
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = colorResource(R.color.primary_green)
            )
        } else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        colorResource(R.color.primary_green)
                    } else {
                        colorResource(R.color.gray_light)
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (isSelected) {
                            colorResource(R.color.primary_dark)
                        } else {
                            colorResource(R.color.gray_medium)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(R.color.primary_dark)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = colorResource(R.color.gray_dark),
                    lineHeight = 18.sp
                )
            }

            if (isSelected) {
                Card(
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(50),
                    colors = CardDefaults.cardColors(
                        containerColor = colorResource(R.color.primary_green)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "تم الاختيار",
                            modifier = Modifier.size(20.dp),
                            tint = Color.Black
                        )
                    }
                }
            }
        }
    }
}
