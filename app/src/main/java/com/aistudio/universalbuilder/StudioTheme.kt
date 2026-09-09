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
            primary = Color(0xFFB6E8CE), onPrimary = Color(0xFF123526),
            primaryContainer = Color(0xFF234537), onPrimaryContainer = Color(0xFFD4F5E4),
            secondary = Color(0xFFB4C6BE), background = Color(0xFF101513),
            surface = Color(0xFF171E1B), surfaceVariant = Color(0xFF222C27),
            onBackground = Color(0xFFF1F5F2), onSurface = Color(0xFFF1F5F2),
            onSurfaceVariant = Color(0xFFACBAB2), outline = Color(0xFF526158),
            outlineVariant = Color(0xFF303D35)
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
