package com.aistudio.universalbuilder

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            UniversalBuilderApp()
        }
    }
}

@Composable
fun UniversalBuilderApp() {

    val context = LocalContext.current

    var projectUri by remember { mutableStateOf<Uri?>(null) }
    var projectName by remember { mutableStateOf("No project selected") }

    var appName by remember { mutableStateOf("My App") }
    var packageName by remember {
        mutableStateOf("com.myapp.generated")
    }

    var buildStatus by remember {
        mutableStateOf("READY")
    }

    val projectPicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri != null) {
                projectUri = uri

                projectName =
                    uri.lastPathSegment
                        ?.substringAfterLast("/")
                        ?: "Imported Project"

                buildStatus = "PROJECT DETECTED"
            }
        }

    val apkPicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri != null) {

                if (
                    android.os.Build.VERSION.SDK_INT >=
                    android.os.Build.VERSION_CODES.O &&
                    !context.packageManager.canRequestPackageInstalls()
                ) {

                    val settingsIntent =
                        Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:${context.packageName}")
                        )

                    context.startActivity(settingsIntent)

                } else {

                    val installIntent =
                        Intent(Intent.ACTION_VIEW).apply {

                            setDataAndType(
                                uri,
                                "application/vnd.android.package-archive"
                            )

                            addFlags(
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )

                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                            )
                        }

                    context.startActivity(installIntent)
                }
            }
        }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF8B5CF6),
            secondary = Color(0xFF22D3EE),
            background = Color(0xFF07090D),
            surface = Color(0xFF10131A)
        )
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF07090D),
                            Color(0xFF0B1018),
                            Color(0xFF07090D)
                        )
                    )
                )
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(20.dp)
            ) {

                Spacer(
                    modifier = Modifier.height(24.dp)
                )

                Text(
                    text = "UNIVERSAL",
                    color = Color(0xFF9B87F5),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "BUILD STUDIO",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = "Apps • Games • Web • Native",
                    color = Color(0xFF8E98A8),
                    fontSize = 14.sp
                )

                Spacer(
                    modifier = Modifier.height(26.dp)
                )

                ProjectUploadCard(
                    projectName = projectName
                ) {

                    projectPicker.launch(
                        arrayOf(
                            "application/zip",
                            "application/octet-stream",
                            "text/html",
                            "text/plain",
                            "*/*"
                        )
                    )
                }

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                SectionTitle(
                    "PROJECT ANALYSIS"
                )

                InfoCard {

                    InfoRow(
                        "Status",
                        buildStatus
                    )

                    HorizontalDivider(
                        color = Color(0xFF242936)
                    )

                    InfoRow(
                        "Engine",
                        if (projectUri == null)
                            "Waiting..."
                        else
                            "Auto Detect"
                    )

                    HorizontalDivider(
                        color = Color(0xFF242936)
                    )

                    InfoRow(
                        "Build Target",
                        "Android APK"
                    )

                    HorizontalDivider(
                        color = Color(0xFF242936)
                    )

                    InfoRow(
                        "Max Project",
                        "250 MB Target"
                    )
                }

                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                SectionTitle(
                    "APP IDENTITY"
                )

                InfoCard {

                    OutlinedTextField(
                        value = appName,
                        onValueChange = {
                            appName = it
                        },
                        label = {
                            Text("App Name")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    OutlinedTextField(
                        value = packageName,
                        onValueChange = {
                            packageName = it
                        },
                        label = {
                            Text("Package ID")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                SectionTitle(
                    "CAPABILITIES"
                )

                InfoCard {

                    CapabilityRow(
                        "Internet",
                        true
                    )

                    CapabilityRow(
                        "Media / Files",
                        true
                    )

                    CapabilityRow(
                        "Fullscreen",
                        true
                    )

                    CapabilityRow(
                        "Audio",
                        true
                    )

                    CapabilityRow(
                        "Notifications",
                        true
                    )

                    CapabilityRow(
                        "Camera",
                        false
                    )

                    CapabilityRow(
                        "Location",
                        false
                    )
                }

                Spacer(
                    modifier = Modifier.height(24.dp)
                )

                Button(
                    onClick = {

                        if (projectUri == null) {

                            buildStatus =
                                "SELECT PROJECT FIRST"

                        } else {

                            buildStatus =
                                "READY FOR BUILD ENGINE"
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape =
                        RoundedCornerShape(18.dp)
                ) {

                    Text(
                        text = "BUILD APK",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                OutlinedButton(
                    onClick = {

                        apkPicker.launch(
                            arrayOf(
                                "application/vnd.android.package-archive",
                                "application/octet-stream"
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape =
                        RoundedCornerShape(18.dp)
                ) {

                    Text(
                        text = "INSTALL BUILT APK",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Text(
                    text =
                        "Generated APKs will open using Android's native installer.",
                    color = Color(0xFF727D8E),
                    fontSize = 12.sp
                )

                Spacer(
                    modifier = Modifier.height(40.dp)
                )
            }
        }
    }
}

@Composable
fun ProjectUploadCard(
    projectName: String,
    onClick: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color(0xFF3C4352),
                shape = RoundedCornerShape(22.dp)
            )
            .background(
                Color(0xFF10141C),
                RoundedCornerShape(22.dp)
            )
            .clickable {
                onClick()
            }
            .padding(
                horizontal = 20.dp,
                vertical = 30.dp
            ),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text = "＋",
                fontSize = 40.sp,
                color = Color(0xFF9B87F5)
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "IMPORT PROJECT",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            Text(
                text = projectName,
                color = Color(0xFF9AA4B2),
                fontSize = 13.sp
            )

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text =
                    "ZIP • Android • Web • Game Project",
                color = Color(0xFF626D7C),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun SectionTitle(
    title: String
) {

    Text(
        text = title,
        color = Color(0xFF8994A5),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(
            bottom = 10.dp
        )
    )
}

@Composable
fun InfoCard(
    content: @Composable ColumnScope.() -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFF10141C),
                RoundedCornerShape(20.dp)
            )
            .border(
                1.dp,
                Color(0xFF202633),
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp),
        content = content
    )
}

@Composable
fun InfoRow(
    title: String,
    value: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 12.dp
            ),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {

        Text(
            text = title,
            color = Color(0xFF8994A5),
            fontSize = 14.sp
        )

        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun CapabilityRow(
    name: String,
    enabled: Boolean
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 9.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text =
                if (enabled) "✓" else "○",
            color =
                if (enabled)
                    Color(0xFF22D3A6)
                else
                    Color(0xFF697384),
            fontSize = 18.sp
        )

        Spacer(
            modifier = Modifier.width(12.dp)
        )

        Text(
            text = name,
            color =
                if (enabled)
                    Color.White
                else
                    Color(0xFF778191),
            fontSize = 14.sp
        )
    }
}
