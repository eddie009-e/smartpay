package com.smartpay.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.smartpay.data.models.SendMoneyRequest
import com.smartpay.data.repository.UserRepository
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
class QRCodeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QRCodeScreen()
        }
    }
}

@Composable
fun QRCodeScreen() {
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid ?: ""
    val phone = user?.phoneNumber ?: ""
    val qrData = "smartpay:user:$uid,phone:$phone"
    val repository = UserRepository()
    val coroutineScope = rememberCoroutineScope()

    var mode by remember { mutableStateOf("display") }
    var scannedData by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }

    // Camera permission
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .padding(top = 40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { (context as? ComponentActivity)?.finish() }
                ) {
                    Text(
                        text = "‹",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Text(
                    text = "QR Code",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontFamily = FontFamily.SansSerif
                )

                Box(modifier = Modifier.size(48.dp))
            }

            // Mode Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CashAppToggleButton(
                    text = "عرض كودي",
                    isSelected = mode == "display",
                    onClick = { mode = "display" },
                    modifier = Modifier.weight(1f)
                )
                CashAppToggleButton(
                    text = "مسح كود",
                    isSelected = mode == "scan",
                    onClick = {
                        mode = "scan"
                        if (!hasCameraPermission) {
                            launcher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Content Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                if (mode == "display") {
                    DisplayQRCode(qrData = qrData, phone = phone)
                } else {
                    if (hasCameraPermission) {
                        CameraPreviewView(
                            onQrScanned = { result ->
                                if (scannedData == null) {
                                    scannedData = result
                                    showDialog = true
                                }
                            }
                        )
                    } else {
                        NoCameraPermissionView()
                    }
                }
            }
        }
    }

    // Send Money Dialog
    if (showDialog && scannedData != null) {
        CashAppSendDialog(
            scannedData = scannedData!!,
            amount = amount,
            onAmountChange = { amount = it },
            onConfirm = {
                coroutineScope.launch {
                    try {
                        val request = SendMoneyRequest(
                            receiverPhone = scannedData ?: "",
                            amount = amount.toLongOrNull() ?: 0,
                            note = "QR Payment"
                        )
                        val response = repository.sendMoney("", request)
                        if (response.isSuccessful) {
                            Toast.makeText(context, "تم إرسال $amount ل.س", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "فشل الإرسال", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                showDialog = false
                scannedData = null
                amount = ""
            },
            onDismiss = {
                showDialog = false
                scannedData = null
                amount = ""
            }
        )
    }
}

@Composable
fun CashAppToggleButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF00D632) else Color(0xFFF7F8FA),
            contentColor = if (isSelected) Color.White else Color.Black
        )
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DisplayQRCode(qrData: String, phone: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            modifier = Modifier
                .size(280.dp)
                .shadow(12.dp, RoundedCornerShape(24.dp), clip = false),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                val qrImage = generateQrCodeBitmap(qrData)
                qrImage?.let { img ->
                    Image(bitmap = img, contentDescription = null, modifier = Modifier.fillMaxSize())
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("امسح للدفع أو الاستلام", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Text(phone, fontSize = 16.sp, color = Color(0xFF666666))
    }
}

@Composable
fun CameraPreviewView(onQrScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) { onDispose { cameraExecutor.shutdown() } }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { ctx ->
            PreviewView(ctx).apply {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(surfaceProvider) }
                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { it.setAnalyzer(cameraExecutor, QrCodeAnalyzer(onQrScanned)) }
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer)
                    } catch (exc: Exception) {
                        Log.e("CameraX", "Use case binding failed", exc)
                    }
                }, ContextCompat.getMainExecutor(ctx))
            }
        }, modifier = Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(20.dp))
                    .border(3.dp, Color(0xFF00D632), RoundedCornerShape(20.dp))
            )
        }

        Text(
            text = "ضع الرمز في الإطار",
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp),
            color = Color.White,
            fontSize = 18.sp
        )
    }
}

class QrCodeAnalyzer(private val onQrScanned: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient()

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        try {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            barcode.rawValue?.let { value ->
                                onQrScanned(value)
                                return@addOnSuccessListener
                            }
                        }
                    }
                    .addOnCompleteListener { imageProxy.close() }
            } else imageProxy.close()
        } catch (e: Exception) {
            Log.e("QrCodeAnalyzer", "Error analyzing image", e)
            imageProxy.close()
        }
    }
}

@Composable
fun NoCameraPermissionView() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📷", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("يجب السماح بالوصول للكاميرا", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Text("لمسح رموز QR، يحتاج التطبيق إلى إذن الكاميرا", fontSize = 16.sp, color = Color(0xFF666666))
    }
}

@Composable
fun CashAppSendDialog(
    scannedData: String,
    amount: String,
    onAmountChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = { Text("إرسال الأموال", fontSize = 24.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("إلى: ${scannedData.take(20)}...", fontSize = 16.sp, color = Color(0xFF666666))
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(Color(0xFFF7F8FA), RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = amount,
                        onValueChange = { input -> if (input.all { it.isDigit() }) onAmountChange(input) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color.Black),
                        singleLine = true,
                        cursorBrush = SolidColor(Color(0xFF00D632)),
                        decorationBox = { inner ->
                            if (amount.isEmpty()) Text("أدخل المبلغ", color = Color(0xFF999999))
                            inner()
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = amount.isNotEmpty()) {
                Text("إرسال", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = Color(0xFF666666)) }
        }
    )
}

fun generateQrCodeBitmap(text: String): ImageBitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
        val bmp = createBitmap(bitMatrix.width, bitMatrix.height)
        for (x in 0 until bitMatrix.width) {
            for (y in 0 until bitMatrix.height) {
                bmp[x, y] = if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }
        bmp.asImageBitmap()
    } catch (e: Exception) {
        Log.e("QR", "فشل توليد الكود", e)
        null
    }
}
