package com.smartpay.android

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.smartpay.models.*
import kotlinx.coroutines.launch

class ManageEmployeesActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get token and subscription plan from secure storage
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val securePrefs = EncryptedSharedPreferences.create(
            "SmartPaySecurePrefs",
            masterKey,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val token = securePrefs.getString("token", null) ?: ""
        val subscriptionPlan = securePrefs.getString("subscriptionPlan", "Free") ?: "Free"

        if (token.isEmpty()) {
            Toast.makeText(this, "الجلسة غير صالحة، يرجى تسجيل الدخول", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Check subscription plan access (Pro only)
        if (!Employee.hasFeatureAccess(subscriptionPlan)) {
            Toast.makeText(this, Employee.getUpgradeMessage(), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContent {
            ModernManageEmployeesScreen(
                onBack = { finish() }
            )
        }
    }
}

@Composable
fun ModernManageEmployeesScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var employees by remember { mutableStateOf<List<Employee>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedEmployee by remember { mutableStateOf<Employee?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Mock data for demonstration (replace with actual API calls)
    LaunchedEffect(Unit) {
        loadEmployees { loadedEmployees, error ->
            employees = loadedEmployees
            errorMessage = error
            isLoading = false
        }
    }

    fun loadEmployees(onResult: (List<Employee>, String?) -> Unit) {
        isLoading = true
        errorMessage = null
        
        // Mock implementation - replace with actual API call
        try {
            // Simulate loading
            employees = listOf(
                Employee(
                    id = "1",
                    merchantId = "merchant_1",
                    name = "أحمد محمد",
                    email = "ahmed@example.com",
                    phone = "+963123456789",
                    position = "مدير مبيعات",
                    department = "sales",
                    hireDate = "2023-01-15",
                    salary = 50000.0,
                    isActive = true,
                    permissions = listOf("manage_sales", "view_reports"),
                    createdAt = "2023-01-15T10:00:00Z",
                    updatedAt = "2023-01-15T10:00:00Z"
                ),
                Employee(
                    id = "2",
                    merchantId = "merchant_1",
                    name = "فاطمة أحمد",
                    email = "fatima@example.com",
                    phone = "+963123456788",
                    position = "محاسبة",
                    department = "finance",
                    hireDate = "2023-02-01",
                    salary = 45000.0,
                    isActive = true,
                    permissions = listOf("view_reports", "handle_payments"),
                    createdAt = "2023-02-01T10:00:00Z",
                    updatedAt = "2023-02-01T10:00:00Z"
                ),
                Employee(
                    id = "3",
                    merchantId = "merchant_1",
                    name = "محمد خالد",
                    email = "mohammed@example.com",
                    phone = "+963123456777",
                    position = "مطور",
                    department = "tech",
                    hireDate = "2023-03-01",
                    salary = 60000.0,
                    isActive = false,
                    permissions = listOf("manage_tech"),
                    createdAt = "2023-03-01T10:00:00Z",
                    updatedAt = "2023-03-01T10:00:00Z"
                )
            )
            onResult(employees, null)
        } catch (e: Exception) {
            onResult(emptyList(), "خطأ في تحميل بيانات الموظفين: ${e.message}")
        }
    }

    fun deleteEmployee(employee: Employee) {
        // Mock implementation - replace with actual API call
        employees = employees.filter { it.id != employee.id }
        Toast.makeText(context, "تم حذف الموظف بنجاح", Toast.LENGTH_SHORT).show()
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
                .offset(x = 280.dp, y = 80.dp)
                .background(
                    Color(0xFFD8FBA9).copy(alpha = 0.12f),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(150.dp)
                .offset(x = (-60).dp, y = 300.dp)
                .background(
                    Color.White.copy(alpha = 0.2f),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(120.dp)
                .offset(x = 250.dp, y = 600.dp)
                .background(
                    Color(0xFF2D2D2D).copy(alpha = 0.06f),
                    CircleShape
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(40.dp))

            // Modern Header
            ModernHeader(
                onBack = onBack,
                onRefresh = { 
                    loadEmployees { loadedEmployees, error ->
                        employees = loadedEmployees
                        errorMessage = error
                        isLoading = false
                    }
                },
                isLoading = isLoading,
                employeeCount = employees.size
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Summary Card
            EmployeeSummaryCard(employees = employees)

            Spacer(modifier = Modifier.height(24.dp))

            // Content
            when {
                isLoading -> {
                    LoadingSection()
                }
                
                errorMessage != null -> {
                    ErrorSection(
                        errorMessage = errorMessage!!,
                        onRetry = { 
                            loadEmployees { loadedEmployees, error ->
                                employees = loadedEmployees
                                errorMessage = error
                                isLoading = false
                            }
                        }
                    )
                }
                
                employees.isEmpty() -> {
                    EmptyStateSection(
                        onAddEmployee = { showAddDialog = true }
                    )
                }
                
                else -> {
                    EmployeesList(
                        employees = employees,
                        onEdit = { employee ->
                            selectedEmployee = employee
                            showEditDialog = true
                        },
                        onDelete = { employee ->
                            selectedEmployee = employee
                            showDeleteDialog = true
                        },
                        onAddEmployee = { showAddDialog = true }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showAddDialog) {
        ModernDialog(
            title = "إضافة موظف جديد",
            message = "سيتم إضافة نموذج إضافة الموظفين هنا",
            onDismiss = { showAddDialog = false },
            onConfirm = { showAddDialog = false }
        )
    }

    if (showEditDialog && selectedEmployee != null) {
        ModernDialog(
            title = "تعديل بيانات الموظف",
            message = "سيتم إضافة نموذج تعديل الموظف هنا\nالموظف: ${selectedEmployee!!.name}",
            onDismiss = { 
                showEditDialog = false
                selectedEmployee = null
            },
            onConfirm = { 
                showEditDialog = false
                selectedEmployee = null
            }
        )
    }

    if (showDeleteDialog && selectedEmployee != null) {
        ModernDialog(
            title = "حذف الموظف",
            message = "هل أنت متأكد من حذف الموظف \"${selectedEmployee!!.name}\"؟ لا يمكن التراجع عن هذا الإجراء.",
            onDismiss = { 
                showDeleteDialog = false
                selectedEmployee = null
            },
            onConfirm = {
                deleteEmployee(selectedEmployee!!)
                showDeleteDialog = false
                selectedEmployee = null
            },
            isDestructive = true
        )
    }
}

@Composable
private fun ModernHeader(
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    isLoading: Boolean,
    employeeCount: Int
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            Card(
                modifier = Modifier.size(48.dp),
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

            Spacer(modifier = Modifier.width(16.dp))

            // Title Section
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Groups,
                        contentDescription = null,
                        tint = Color(0xFFD8FBA9),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إدارة الموظفين",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D2D2D)
                    )
                }
                if (employeeCount > 0) {
                    Text(
                        text = "$employeeCount موظف",
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                }
            }

            // Refresh Button
            Card(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isLoading) Color(0xFFD8FBA9).copy(alpha = 0.3f) 
                                   else Color(0xFFD8FBA9).copy(alpha = 0.2f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                IconButton(
                    onClick = onRefresh,
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color(0xFFD8FBA9),
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "تحديث",
                            tint = Color(0xFFD8FBA9),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployeeSummaryCard(employees: List<Employee>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryItem(
                title = "إجمالي الموظفين",
                value = employees.size.toString(),
                color = Color(0xFF2196F3),
                icon = Icons.Default.Groups
            )
            SummaryItem(
                title = "الموظفون النشطون",
                value = employees.count { it.isActive }.toString(),
                color = Color(0xFFD8FBA9),
                icon = Icons.Default.CheckCircle
            )
            SummaryItem(
                title = "غير النشطين",
                value = employees.count { !it.isActive }.toString(),
                color = Color(0xFF999999),
                icon = Icons.Default.PauseCircle
            )
        }
    }
}

@Composable
private fun SummaryItem(
    title: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = color.copy(alpha = 0.15f)
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
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            title,
            fontSize = 12.sp,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center
        )
        Text(
            value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
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
                    "جاري تحميل بيانات الموظفين...",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2D2D2D),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ErrorSection(
    errorMessage: String,
    onRetry: () -> Unit
) {
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
                Card(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE53E3E).copy(alpha = 0.15f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = Color(0xFFE53E3E),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    "خطأ في تحميل البيانات",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE53E3E),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    errorMessage,
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD8FBA9)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF2D2D2D)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "إعادة المحاولة",
                        color = Color(0xFF2D2D2D),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateSection(
    onAddEmployee: () -> Unit
) {
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
                            Icons.Default.GroupAdd,
                            contentDescription = null,
                            tint = Color(0xFFD8FBA9),
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    "لا يوجد موظفون",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D2D2D),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    "ابدأ بإضافة موظفين لإدارة فريق العمل",
                    fontSize = 16.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onAddEmployee,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD8FBA9)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF2D2D2D),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "إضافة موظف",
                        color = Color(0xFF2D2D2D),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun EmployeesList(
    employees: List<Employee>,
    onEdit: (Employee) -> Unit,
    onDelete: (Employee) -> Unit,
    onAddEmployee: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "قائمة الموظفين",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D2D2D)
                )
                Text(
                    text = "${employees.size} موظف",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
        }

        // Employee Items
        items(employees) { employee ->
            ModernEmployeeCard(
                employee = employee,
                onEdit = { onEdit(employee) },
                onDelete = { onDelete(employee) }
            )
        }

        // Add Button
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAddEmployee() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFD8FBA9).copy(alpha = 0.15f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFFD8FBA9),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "إضافة موظف جديد",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D2D2D)
                    )
                }
            }
        }
    }
}

@Composable
fun ModernEmployeeCard(
    employee: Employee,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box {
            // Subtle pattern
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .offset(x = 250.dp, y = (-40).dp)
                    .background(
                        Color(0xFFD8FBA9).copy(alpha = 0.05f),
                        CircleShape
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar
                    Card(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2196F3).copy(alpha = 0.15f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = employee.getInitials(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2196F3)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Employee Info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = employee.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D2D2D),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!employee.position.isNullOrEmpty()) {
                            Text(
                                text = employee.position,
                                fontSize = 14.sp,
                                color = Color(0xFF666666)
                            )
                        }
                        
                        // Status Badge
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (employee.isActive) 
                                    Color(0xFFD8FBA9).copy(alpha = 0.2f) 
                                else 
                                    Color(0xFF999999).copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = employee.getStatusDisplay(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (employee.isActive) Color(0xFFD8FBA9) else Color(0xFF999999),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    
                    // Actions
                    Row {
                        Card(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF2196F3).copy(alpha = 0.1f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            IconButton(onClick = onEdit) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "تعديل",
                                    tint = Color(0xFF2196F3),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Card(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE53E3E).copy(alpha = 0.1f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            IconButton(onClick = onDelete) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "حذف",
                                    tint = Color(0xFFE53E3E),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                
                // Contact Info
                if (!employee.email.isNullOrEmpty() || !employee.phone.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (!employee.email.isNullOrEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Email,
                                    contentDescription = null,
                                    tint = Color(0xFF666666),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = employee.email,
                                    fontSize = 12.sp,
                                    color = Color(0xFF666666)
                                )
                            }
                        }
                        if (!employee.phone.isNullOrEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = Color(0xFF666666),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = employee.phone,
                                    fontSize = 12.sp,
                                    color = Color(0xFF666666)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isDestructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDestructive) 
                            Color(0xFFE53E3E).copy(alpha = 0.15f) 
                        else 
                            Color(0xFFD8FBA9).copy(alpha = 0.15f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isDestructive) Icons.Default.Delete else Icons.Default.Person,
                            contentDescription = null,
                            tint = if (isDestructive) Color(0xFFE53E3E) else Color(0xFFD8FBA9),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF2D2D2D)
                )
            }
        },
        text = {
            Text(
                text = message,
                fontSize = 16.sp,
                color = Color(0xFF666666)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) Color(0xFFE53E3E) else Color(0xFFD8FBA9)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (isDestructive) "حذف" else "موافق",
                    color = if (isDestructive) Color.White else Color(0xFF2D2D2D),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFF666666)
                )
            ) {
                Text(
                    "إلغاء",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}