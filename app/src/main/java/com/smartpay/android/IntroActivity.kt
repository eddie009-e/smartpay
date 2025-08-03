package com.smartpay.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.pager.*
import kotlinx.coroutines.launch

class IntroActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("SmartPayPrefs", MODE_PRIVATE)
        val introSeen = prefs.getBoolean("introSeen", false)
        val pin = prefs.getString("pin", null)

        if (introSeen) {
            if (pin != null) {
                // ✅ عرض المقدمة سابقًا والمستخدم مسجل → الذهاب مباشرة إلى PinActivity
                startActivity(Intent(this, PinActivity::class.java))
            } else {
                // ✅ عرض المقدمة سابقًا والمستخدم جديد → الذهاب إلى UserTypeSelection
                startActivity(Intent(this, UserTypeSelectionActivity::class.java))
            }
            finish()
            return
        }

        // ⬇️ لم تُعرض المقدمة من قبل → عرضها الآن
        setContent {
            IntroPager(onFinish = {
                // 📝 حفظ حالة أن المقدمة تم عرضها
                prefs.edit().putBoolean("introSeen", true).apply()

                if (pin != null) {
                    startActivity(Intent(this, PinActivity::class.java))
                } else {
                    startActivity(Intent(this, UserTypeSelectionActivity::class.java))
                }

                finish()
            })
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun IntroPager(onFinish: () -> Unit) {
    val pages = listOf(
        IntroPage(
            title = "الحساب الشخصي",
            description = "حول المال لأصدقائك وعائلتك بسهولة.\nتتبع مدفوعاتك واحتفظ بسجل تحويلاتك.",
            imageRes = R.drawable.personal_account
        ),
        IntroPage(
            title = "الحساب التجاري",
            description = "استقبل المدفوعات من الزبائن عبر QR Code.\nاحصل على تقارير مفصّلة عن عمليات البيع.",
            imageRes = R.drawable.business_account
        ),
        IntroPage(
            title = "التحويل الذكي",
            description = "ارسل الأموال بسرعة وحدد سبب التحويل (إيجار، مشتريات...)\nنقوم بتسهيل تجربتك وحفظها للمرة القادمة.",
            imageRes = R.drawable.smart_transfer
        )
    )

    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Skip button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp, end = 24.dp)
            ) {
                TextButton(
                    onClick = onFinish,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Text(
                        text = "تخطي",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF666666),
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }

            // Page content
            HorizontalPager(
                count = pages.size,
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                IntroPageView(pages[page])
            }

            // Bottom section
            Column(
                modifier = Modifier
                    .padding(horizontal = 40.dp, vertical = 50.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 40.dp)
                ) {
                    repeat(pages.size) { index ->
                        Box(
                            modifier = Modifier
                                .size(if (pagerState.currentPage == index) 32.dp else 12.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == index)
                                        Color(0xFF00D632)
                                    else
                                        Color(0xFFE0E0E0)
                                )
                        )
                    }
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.lastIndex) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onFinish()
                        }
                    },
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(30.dp),
                            clip = false
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00D632),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (pagerState.currentPage == pages.lastIndex) "ابدأ الآن" else "التالي",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
    }
}

@Composable
fun IntroPageView(page: IntroPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .padding(bottom = 60.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(40.dp),
                    clip = false
                )
                .clip(RoundedCornerShape(40.dp))
                .background(Color(0xFFF7F8FA)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = page.imageRes),
                contentDescription = null,
                modifier = Modifier
                    .size(200.dp)
                    .padding(20.dp)
            )
        }

        Text(
            text = page.title,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            color = Color.Black,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        Text(
            text = page.description,
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            color = Color(0xFF666666),
            fontFamily = FontFamily.SansSerif,
            lineHeight = 28.sp
        )
    }
}

data class IntroPage(
    val title: String,
    val description: String,
    val imageRes: Int
)
