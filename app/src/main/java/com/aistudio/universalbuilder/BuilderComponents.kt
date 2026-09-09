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
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(20.dp)
            )
            .padding(20.dp)
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary,
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
                MaterialTheme.colorScheme.surfaceVariant,
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
                    MaterialTheme.colorScheme.primaryContainer,
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
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column {

            Text(
                text =
                    if (iconUri != null)
                        "App icon selected"
                    else
                        "Choose an app icon",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Tap to change • PNG or JPG",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun BuildStatusCard(
    status: String,
    isBuilding: Boolean = false
) {
    BuilderCard(
        title = "Build status"
    ) {

        if (isBuilding) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
        }
        Text(
            text = status,
            color =
                if (
                    status.contains("started", true) ||
                    status.contains("ready", true)
                ) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
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
        title = "01  /  Project source"
    ) {

        Button(
            onClick = onUploadClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Choose project file")
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = projectName,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = projectType,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}
