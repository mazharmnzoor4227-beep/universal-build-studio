package com.aistudio.universalbuilder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HistoryScreen(
    onBack: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07090E))
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {

        Spacer(Modifier.height(24.dp))

        Text(
            text = "BUILD HISTORY",
            color = Color.White,
            fontSize = 25.sp,
            fontWeight = FontWeight.Black
        )

        Text(
            text = "Your generated apps and games",
            color = Color(0xFF8D97A7),
            fontSize = 13.sp
        )

        Spacer(Modifier.height(22.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Color(0xFF11151D),
                    RoundedCornerShape(20.dp)
                )
                .padding(18.dp)
        ) {

            Text(
                text = "NO BUILDS YET",
                color = Color(0xFFA58BFF),
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Your successful and failed APK builds will appear here.",
                color = Color(0xFF9AA4B5),
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(20.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("BACK TO BUILDER")
        }

        Spacer(Modifier.height(30.dp))
    }
}
