package com.aistudio.universalbuilder

import android.net.Uri
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun BuilderCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFF11151D),
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = title,
            color = Color(0xFFA58BFF),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(12.dp))

        content()
    }
}

@Composable
fun AppIconPicker(
    iconUri: Uri?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFF171C25),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(70.dp)
                .background(
                    Color(0xFF29233E),
                    RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {

            if (iconUri != null) {

                AndroidView(
                    factory = { context ->
                        ImageView(context).apply {
                            scaleType =
                                ImageView.ScaleType.CENTER_CROP
                        }
                    },
                    update = {
                        it.setImageURI(iconUri)
                    },
                    modifier = Modifier.fillMaxSize()
                )

            } else {

                Text(
                    text = "ICON",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column {

            Text(
                text =
                    if (iconUri != null)
                        "ICON SELECTED"
                    else
                        "SELECT LOGO / ICON",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Tap to choose image",
                color = Color(0xFF8E98A8),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun BuildStatusCard(
    status: String
) {
    BuilderCard(
        title = "BUILD STATUS"
    ) {

        Text(
            text = status,
            color =
                if (
                    status.contains("started", true) ||
                    status.contains("ready", true)
                ) {
                    Color(0xFF54DFA8)
                } else {
                    Color.White
                },
            fontSize = 13.sp
        )
    }
}

@Composable
fun ProjectInfoCard(
    projectName: String,
    projectType: String,
    onUploadClick: () -> Unit
) {
    BuilderCard(
        title = "PROJECT"
    ) {

        Button(
            onClick = onUploadClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("UPLOAD PROJECT / ZIP")
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = projectName,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = projectType,
            color = Color(0xFF929CAF),
            fontSize = 12.sp
        )
    }
}
