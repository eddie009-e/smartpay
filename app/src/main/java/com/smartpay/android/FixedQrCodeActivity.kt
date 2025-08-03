package com.smartpay.android

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

class FixedQrCodeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
            var finalData by remember { mutableStateOf("") }

            LaunchedEffect(Unit) {
                // أصل البيانات
                val merchantJson = JSONObject().apply {
                    put("type", "merchant_payment")
                    put("merchant_id", "12345")
                    put("merchant_name", "متجر الذكي")
                    put("account_number", "SYR123456789")
                }

                // توليد التوقيع الآمن
                val token = generateToken(
                    merchantJson.getString("merchant_id"),
                    merchantJson.getString("account_number")
                )

                merchantJson.put("token", token)
                finalData = merchantJson.toString()
                qrBitmap = generateQRCode(finalData)
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = androidx.compose.ui.graphics.Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // شريط التنقل
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 30.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { finish() },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(androidx.compose.ui.graphics.Color(0xFFF7F8FA))
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "رجوع",
                                tint = androidx.compose.ui.graphics.Color.Black
                            )
                        }

                        Text(
                            text = "رمز QR الثابت",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color.Black
                        )

                        Spacer(modifier = Modifier.size(48.dp))
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Card(
                        modifier = Modifier
                            .size(280.dp)
                            .border(
                                2.dp,
                                androidx.compose.ui.graphics.Color(0xFFE0E0E0),
                                RoundedCornerShape(20.dp)
                            ),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = androidx.compose.ui.graphics.Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            qrBitmap?.let { bitmap ->
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "QR Code",
                                    modifier = Modifier.size(240.dp)
                                )
                            } ?: run {
                                CircularProgressIndicator(
                                    color = androidx.compose.ui.graphics.Color(0xFF00D632)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    Text(
                        text = "اطلب من العملاء مسح هذا الرمز\nلإرسال الأموال إليك مباشرة",
                        fontSize = 16.sp,
                        color = androidx.compose.ui.graphics.Color(0xFF666666),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                copyToClipboard(context, finalData)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(width = 2.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("نسخ", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }

                        Button(
                            onClick = {
                                qrBitmap?.let { shareQRCode(context, it) }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = androidx.compose.ui.graphics.Color(0xFF00D632)
                            )
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "مشاركة", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("مشاركة", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFFF7F8FA))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("💡 نصائح", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "• يمكنك طباعة هذا الرمز ووضعه في متجرك\n" +
                                        "• الرمز ثابت ولا يتغير\n" +
                                        "• يمكن استخدامه لاستقبال المدفوعات فقط",
                                fontSize = 14.sp,
                                color = androidx.compose.ui.graphics.Color(0xFF666666),
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }

    private fun generateQRCode(data: String): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun generateToken(id: String, account: String): String {
        val secretKey = "super_secret_key_123"
        val data = "$id:$account"
        return try {
            val hmac = Mac.getInstance("HmacSHA256")
            val secretKeySpec = SecretKeySpec(secretKey.toByteArray(), "HmacSHA256")
            hmac.init(secretKeySpec)
            val hash = hmac.doFinal(data.toByteArray())
            Base64.encodeToString(hash, Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("بيانات التاجر", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "تم نسخ البيانات", Toast.LENGTH_SHORT).show()
    }

    private fun shareQRCode(context: Context, bitmap: Bitmap) {
        try {
            val file = File(context.cacheDir, "qr_code.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                ))
                putExtra(Intent.EXTRA_TEXT, "رمز QR الخاص بي للدفع")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "مشاركة رمز QR"))
        } catch (e: Exception) {
            Toast.makeText(context, "خطأ في المشاركة", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
}
