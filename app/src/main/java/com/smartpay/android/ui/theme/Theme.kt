package com.smartpay.android.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.smartpay.android.R

private val BeirutiFont = FontFamily(
    Font(R.font.beiruti_regular, FontWeight.Normal)
)

private val SmartPayColors = lightColorScheme(
    primary = Color(0xFF00D632),
    onPrimary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color(0xFFF7F8FA),
    onSurface = Color.Black,
    secondary = Color(0xFF00C2A8),
    onSecondary = Color.White,
    error = Color(0xFFFF3737),
    onError = Color.White
)

@Composable
fun SmartPayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SmartPayColors,
        typography = Typography(
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = BeirutiFont),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = BeirutiFont),
            displayLarge = MaterialTheme.typography.displayLarge.copy(fontFamily = BeirutiFont),
        ),
        shapes = Shapes(),
        content = content
    )
}
