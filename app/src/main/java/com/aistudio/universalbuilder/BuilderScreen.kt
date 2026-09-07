package com.aistudio.universalbuilder

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BuilderScreen(
    appName: String,
    packageName: String,
    projectName: String,
    iconSelected: Boolean,
    onAppNameChange: (String) -> Unit,
    onPackageChange: (String) -> Unit,
    onProjectSelected: (String) -> Unit,
    onIconSelected: () -> Unit,
    onOpenGitHub: () -> Unit,
    onOpenHistory: () -> Unit
) {

    var preview by remember { mutableStateOf(false) }

    val projectPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val name = uri.lastPathSegment
                ?.substringAfterLast("/")
                ?: "Imported Project"

            onProjectSelected(name)
        }
    }

    val iconPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onIconSelected()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07090E))
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {

        Spacer(Modifier.height(22.dp))

        Text(
            "UNIVERSAL BUILD STUDIO",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )

        Text(
            "Apps • Games • Web • Native",
            color = Color(0xFF8E98A8),
            fontSize = 13.sp
        )

        Spacer(Modifier.height(22.dp))

        PremiumCard {

            Text(
                "PROJECT",
                color = Color(0xFFA58BFF),
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    projectPicker.launch(arrayOf("*/*"))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("UPLOAD PROJECT / ZIP")
            }

            Spacer(Modifier.height(8.dp))

            Text(
                projectName,
                color = Color(0xFF9AA4B5),
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(14.dp))

        PremiumCard {

            Text(
                "APP ICON",
                color = Color(0xFFA58BFF),
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .background(
                        Color(0xFF171C25),
                        RoundedCornerShape(15.dp)
                    )
                    .clickable {
                        iconPicker.launch(arrayOf("image/*"))
                    },
                contentAlignment = Alignment.Center
            ) {

                Text(
                    if (iconSelected)
                        "✓ ICON SELECTED"
                    else
                        "SELECT LOGO / ICON",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        PremiumCard {

            Text(
                "APP DETAILS",
                color = Color(0xFFA58BFF),
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = appName,
                onValueChange = onAppNameChange,
                label = { Text("App Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = packageName,
                onValueChange = onPackageChange,
                label = { Text("Package ID") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(14.dp))

        OutlinedButton(
            onClick = {
                preview = !preview
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text(
                if (preview)
                    "HIDE PREVIEW"
                else
                    "PREVIEW APP"
            )
        }

        if (preview) {

            Spacer(Modifier.height(12.dp))

            PremiumCard {

                Text(
                    "APP PREVIEW",
                    color = Color(0xFFA58BFF),
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            Color(0xFF0A0D13),
                            RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    Color(0xFF28213E),
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (iconSelected) "✓" else "APP",
                                color = Color.White,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Text(
                            appName,
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            projectName,
                            color = Color(0xFF7F8999),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = onOpenGitHub,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(18.dp)
        ) {

            Text(
                "BUILD APK",
                fontSize = 17.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = onOpenGitHub,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("GITHUB BUILDER SETTINGS")
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onOpenHistory,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("BUILD HISTORY")
        }

        Spacer(Modifier.height(35.dp))
    }
}

@Composable
private fun PremiumCard(
    content: @Composable ColumnScope.() -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFF11151D),
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp),
        content = content
    )
}
