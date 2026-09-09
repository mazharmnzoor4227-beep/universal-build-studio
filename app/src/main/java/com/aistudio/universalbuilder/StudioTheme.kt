package com.aistudio.universalbuilder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFF4F4F5), onPrimary = Color(0xFF121212),
            primaryContainer = Color(0xFF303033), onPrimaryContainer = Color(0xFFFFFFFF),
            secondary = Color(0xFFC8C8CC), background = Color(0xFF0D0D0F),
            surface = Color(0xFF19191C), surfaceVariant = Color(0xFF26262A),
            onBackground = Color(0xFFF5F5F7), onSurface = Color(0xFFF5F5F7),
            onSurfaceVariant = Color(0xFFB6B6BF), outline = Color(0xFF72727C),
            outlineVariant = Color(0xFF36363D)
        ),
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp)),
        typography = Typography(
            headlineLarge = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.8).sp),
            titleMedium = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
            bodyMedium = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, lineHeight = 21.sp)
        ), content = content
    )
}

@Composable
fun StudioHeader(title: String, subtitle: String) {
    Text("UNIVERSAL / STUDIO", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
    Spacer(Modifier.height(12.dp))
    Text(title, style = MaterialTheme.typography.headlineLarge)
    Spacer(Modifier.height(6.dp))
    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(24.dp))
}
