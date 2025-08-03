package com.smartpay.android

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import com.smartpay.models.*
import com.smartpay.repository.TransactionCategoryRepository
import kotlinx.coroutines.launch

@Composable
fun CreateOrEditCategoryDialog(
    category: TransactionCategory? = null,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    repository: TransactionCategoryRepository
) {
    val context = LocalContext.current
    val isEditing = category != null
    
    // Form state
    var nameAr by remember { mutableStateOf(category?.nameAr ?: "") }
    var nameEn by remember { mutableStateOf(category?.nameEn ?: "") }
    var selectedColor by remember { mutableStateOf(category?.color ?: "#4CAF50") }
    
    // UI state
    var isLoading by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    
    val colorOptions = TransactionCategory.getColorOptions()

    fun validateAndSubmit() {
        when {
            nameAr.isBlank() -> {
                Toast.makeText(context, "يرجى إدخال اسم التصنيف بالعربية", Toast.LENGTH_SHORT).show()
                return
            }
            nameEn.isBlank() -> {
                Toast.makeText(context, "يرجى إدخال اسم التصنيف بالإنجليزية", Toast.LENGTH_SHORT).show()
                return
            }
            !TransactionCategory.isValidColor(selectedColor) -> {
                Toast.makeText(context, "يرجى اختيار لون صحيح", Toast.LENGTH_SHORT).show()
                return
            }
            else -> {
                // Submit form
                isLoading = true
                (context as ComponentActivity).lifecycleScope.launch {
                    try {
                        val response = if (isEditing) {
                            repository.updateCategory(category!!.id, nameAr, nameEn, selectedColor)
                        } else {
                            repository.addCategory(nameAr, nameEn, selectedColor)
                        }
                        
                        if (response.isSuccessful && response.body() != null) {
                            val body = response.body()!!
                            if (body.success) {
                                val message = if (isEditing) "تم تحديث التصنيف بنجاح" else "تم إنشاء التصنيف بنجاح"
                                Toast.makeText(context, body.message ?: message, Toast.LENGTH_SHORT).show()
                                onSuccess()
                            } else {
                                Toast.makeText(context, body.message ?: "فشل في العملية", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val message = if (isEditing) "فشل في تحديث التصنيف" else "فشل في إنشاء التصنيف"
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
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Modern Background with animated orbs
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFF8FDED),
                                Color(0xFFF0F8E8)
                            ),
                            radius = 800f
                        )
                    )
            ) {
                // Animated Background Orbs
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .offset(x = 200.dp, y = 100.dp)
                        .background(
                            Color(0xFFD8FBA9).copy(alpha = 0.15f),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .offset(x = (-30).dp, y = 300.dp)
                        .background(
                            Color.White.copy(alpha = 0.25f),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .offset(x = 150.dp, y = 450.dp)
                        .background(
                            Color(0xFF2D2D2D).copy(alpha = 0.08f),
                            CircleShape
                        )
                )
            }

            // Main Dialog Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
            ) {
                Box {
                    // Subtle card patterns
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .offset(x = 250.dp, y = (-60).dp)
                            .background(
                                Color(0xFFD8FBA9).copy(alpha = 0.08f),
                                CircleShape
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .offset(x = (-40).dp, y = 350.dp)
                            .background(
                                Color(0xFF2D2D2D).copy(alpha = 0.05f),
                                CircleShape
                            )
                    )

                    Column(
                        modifier = Modifier.padding(32.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Header Section
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Icon
                            Card(
                                modifier = Modifier.size(80.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFD8FBA9)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isEditing) Icons.Default.Edit else Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = Color(0xFF2D2D2D)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (isEditing) "تعديل التصنيف" else "إضافة تصنيف جديد",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2D2D2D),
                                textAlign = TextAlign.Center
                            )
                            
                            Text(
                                text = if (isEditing) "قم بتحديث بيانات التصنيف" else "أضف تصنيف جديد لتنظيم معاملاتك",
                                fontSize = 14.sp,
                                color = Color(0xFF666666),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        // Arabic Name Field
                        ModernInputField(
                            label = "الاسم بالعربية",
                            value = nameAr,
                            onValueChange = { nameAr = it },
                            icon = Icons.Default.Language,
                            placeholder = "مثل: طعام"
                        )

                        // English Name Field
                        ModernInputField(
                            label = "الاسم بالإنجليزية",
                            value = nameEn,
                            onValueChange = { nameEn = it },
                            icon = Icons.Default.Translate,
                            placeholder = "e.g: Food"
                        )

                        // Color Selection Section
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Palette,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFFD8FBA9)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "اللون",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF2D2D2D)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showColorPicker = !showColorPicker },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.8f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Color Preview Circle
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                try { 
                                                    Color(selectedColor.toColorInt()) 
                                                } catch (e: Exception) { 
                                                    Color(0xFFD8FBA9) 
                                                },
                                                CircleShape
                                            )
                                            .border(
                                                3.dp, 
                                                Color.White, 
                                                CircleShape
                                            )
                                    )
                                    
                                    Spacer(modifier = Modifier.width(16.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = TransactionCategory.getColorNameAr(selectedColor),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF2D2D2D)
                                        )
                                        Text(
                                            text = selectedColor,
                                            fontSize = 12.sp,
                                            color = Color(0xFF666666)
                                        )
                                    }
                                    
                                    Icon(
                                        if (showColorPicker) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = Color(0xFF666666)
                                    )
                                }
                            }
                            
                            // Color Picker Grid
                            if (showColorPicker) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.White.copy(alpha = 0.9f)
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                                ) {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(6),
                                        modifier = Modifier.padding(20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(colorOptions) { colorOption ->
                                            val isSelected = selectedColor == colorOption.hex
                                            Card(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clickable {
                                                        selectedColor = colorOption.hex
                                                        showColorPicker = false
                                                    },
                                                shape = CircleShape,
                                                colors = CardDefaults.cardColors(
                                                    containerColor = Color(colorOption.hex.toColorInt())
                                                ),
                                                elevation = CardDefaults.cardElevation(
                                                    defaultElevation = if (isSelected) 12.dp else 4.dp
                                                )
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .border(
                                                            width = if (isSelected) 4.dp else 2.dp,
                                                            color = if (isSelected) Color(0xFFD8FBA9) else Color.White,
                                                            shape = CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isSelected) {
                                                        Icon(
                                                            Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Cancel Button
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                enabled = !isLoading,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(2.dp, Color(0xFF2D2D2D).copy(alpha = 0.2f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF2D2D2D)
                                )
                            ) {
                                Text(
                                    "إلغاء",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                )
                            }
                            
                            // Submit Button
                            Button(
                                onClick = { validateAndSubmit() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                enabled = !isLoading,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD8FBA9),
                                    disabledContainerColor = Color(0xFFD8FBA9).copy(alpha = 0.5f)
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF2D2D2D),
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (isEditing) Icons.Default.Update else Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = Color(0xFF2D2D2D)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            if (isEditing) "تحديث" else "إضافة",
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
        }
    }
}

@Composable
private fun ModernInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    placeholder: String
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color(0xFFD8FBA9)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2D2D2D)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.8f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 16.sp, 
                    color = Color(0xFF2D2D2D),
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(Color(0xFFD8FBA9)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = TextStyle(
                                fontSize = 16.sp,
                                color = Color(0xFF999999)
                            )
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}