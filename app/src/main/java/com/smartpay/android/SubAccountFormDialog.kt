package com.smartpay.android

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import com.smartpay.models.*
import com.smartpay.repository.SubAccountRepository
import kotlinx.coroutines.launch

@Composable
fun SubAccountFormDialog(
    subAccount: SubAccount? = null,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    repository: SubAccountRepository
) {
    val context = LocalContext.current
    val isEditing = subAccount != null
    
    // Form state
    var fullName by remember { mutableStateOf(subAccount?.fullName ?: "") }
    var phone by remember { mutableStateOf(subAccount?.phone ?: "") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    
    // Permissions state
    var sendPermission by remember { mutableStateOf(subAccount?.permissions?.send ?: false) }
    var receivePermission by remember { mutableStateOf(subAccount?.permissions?.receive ?: true) }
    var salaryPermission by remember { mutableStateOf(subAccount?.permissions?.salary ?: false) }
    var reportsPermission by remember { mutableStateOf(subAccount?.permissions?.reports ?: false) }
    var isActive by remember { mutableStateOf(subAccount?.isActive ?: true) }
    
    // UI state
    var isLoading by remember { mutableStateOf(false) }

    fun validateAndSubmit() {
        when {
            fullName.isBlank() -> {
                Toast.makeText(context, "يرجى إدخال اسم الموظف", Toast.LENGTH_SHORT).show()
                return
            }
            phone.isBlank() -> {
                Toast.makeText(context, "يرجى إدخال رقم الهاتف", Toast.LENGTH_SHORT).show()
                return
            }
            !phone.matches(Regex("^\\d{10,15}$")) -> {
                Toast.makeText(context, "رقم الهاتف يجب أن يكون بين 10-15 رقم", Toast.LENGTH_SHORT).show()
                return
            }
            !isEditing && password.isBlank() -> {
                Toast.makeText(context, "يرجى إدخال كلمة المرور", Toast.LENGTH_SHORT).show()
                return
            }
            !isEditing && password.length < 6 -> {
                Toast.makeText(context, "كلمة المرور يجب أن تكون 6 أحرف على الأقل", Toast.LENGTH_SHORT).show()
                return
            }
            !isEditing && password != confirmPassword -> {
                Toast.makeText(context, "كلمة المرور وتأكيدها غير متطابقتان", Toast.LENGTH_SHORT).show()
                return
            }
            !sendPermission && !receivePermission && !salaryPermission && !reportsPermission -> {
                Toast.makeText(context, "يجب اختيار صلاحية واحدة على الأقل", Toast.LENGTH_SHORT).show()
                return
            }
            else -> {
                // Submit form
                isLoading = true
                val permissions = SubAccountPermissions(
                    send = sendPermission,
                    receive = receivePermission,
                    salary = salaryPermission,
                    reports = reportsPermission
                )
                
                (context as ComponentActivity).lifecycleScope.launch {
                    try {
                        val response = if (isEditing) {
                            repository.updateSubAccount(
                                id = subAccount!!.id,
                                fullName = fullName,
                                permissions = permissions,
                                isActive = isActive
                            )
                        } else {
                            repository.createSubAccount(
                                fullName = fullName,
                                phone = phone,
                                password = password,
                                permissions = permissions
                            )
                        }
                        
                        if (response.isSuccessful && response.body() != null) {
                            val body = response.body()!!
                            if (body.success) {
                                val message = if (isEditing) "تم تحديث الحساب الفرعي بنجاح" else "تم إنشاء الحساب الفرعي بنجاح"
                                Toast.makeText(context, body.message ?: message, Toast.LENGTH_SHORT).show()
                                onSuccess()
                            } else {
                                Toast.makeText(context, body.message ?: "فشل في العملية", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val message = if (isEditing) "فشل في تحديث الحساب الفرعي" else "فشل في إنشاء الحساب الفرعي"
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isLoading = false
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = if (isEditing) "تعديل الحساب الفرعي" else "إضافة حساب فرعي جديد",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Full Name
                Column {
                    Text(
                        text = "اسم الموظف *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF666666)
                    )
                    
                    BasicTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                        cursorBrush = SolidColor(Color(0xFF00D632)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF7F8FA), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    )
                }

                // Phone
                Column {
                    Text(
                        text = "رقم الهاتف *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF666666)
                    )
                    
                    BasicTextField(
                        value = phone,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() }) {
                                phone = newValue
                            }
                        },
                        enabled = !isEditing, // Can't change phone when editing
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        textStyle = TextStyle(fontSize = 16.sp, color = if (isEditing) Color(0xFF999999) else Color.Black),
                        cursorBrush = SolidColor(Color(0xFF00D632)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF7F8FA), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    )
                    if (isEditing) {
                        Text(
                            text = "لا يمكن تغيير رقم الهاتف",
                            fontSize = 12.sp,
                            color = Color(0xFF999999)
                        )
                    }
                }

                // Password (Only for new accounts)
                if (!isEditing) {
                    Column {
                        Text(
                            text = "كلمة المرور *",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF666666)
                        )
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF7F8FA), RoundedCornerShape(12.dp))
                                .padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = password,
                                onValueChange = { password = it },
                                singleLine = true,
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                                cursorBrush = SolidColor(Color(0xFF00D632)),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(16.dp)
                            )
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPassword) "إخفاء" else "إظهار",
                                    tint = Color(0xFF666666)
                                )
                            }
                        }
                    }

                    // Confirm Password
                    Column {
                        Text(
                            text = "تأكيد كلمة المرور *",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF666666)
                        )
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF7F8FA), RoundedCornerShape(12.dp))
                                .padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                singleLine = true,
                                visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                                cursorBrush = SolidColor(Color(0xFF00D632)),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(16.dp)
                            )
                            IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                Icon(
                                    if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showConfirmPassword) "إخفاء" else "إظهار",
                                    tint = Color(0xFF666666)
                                )
                            }
                        }
                    }
                }

                // Permissions Section
                Column {
                    Text(
                        text = "الصلاحيات *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF666666)
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Send Money Permission
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = sendPermission,
                                    onCheckedChange = { sendPermission = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00D632))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "إرسال الأموال",
                                    fontSize = 16.sp,
                                    color = Color.Black
                                )
                            }
                            
                            // Receive Money Permission
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = receivePermission,
                                    onCheckedChange = { receivePermission = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00D632))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "استلام الأموال",
                                    fontSize = 16.sp,
                                    color = Color.Black
                                )
                            }
                            
                            // Salary Permission
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = salaryPermission,
                                    onCheckedChange = { salaryPermission = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00D632))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "دفع الرواتب",
                                    fontSize = 16.sp,
                                    color = Color.Black
                                )
                            }
                            
                            // Reports Permission
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = reportsPermission,
                                    onCheckedChange = { reportsPermission = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00D632))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "عرض التقارير",
                                    fontSize = 16.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }

                // Account Status (Only when editing)
                if (isEditing) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isActive,
                            onCheckedChange = { isActive = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00D632))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "الحساب نشط",
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        Text("إلغاء")
                    }
                    
                    Button(
                        onClick = { validateAndSubmit() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D632)),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(if (isEditing) "تحديث" else "إضافة")
                        }
                    }
                }
            }
        }
    }
}