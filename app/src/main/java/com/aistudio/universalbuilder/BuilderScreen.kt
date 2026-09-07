package com.aistudio.universalbuilder

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var projectUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var iconUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var projectType by remember {
        mutableStateOf("No project selected")
    }

    var previewVisible by remember {
        mutableStateOf(false)
    }

    var previewUrl by remember {
        mutableStateOf<String?>(null)
    }

    var previewStatus by remember {
        mutableStateOf("NO PROJECT")
    }

    var previewMessage by remember {
        mutableStateOf("Upload a project first")
    }

    var preparingPreview by remember {
        mutableStateOf(false)
    }

    var buildStatus by remember {
        mutableStateOf("Ready")
    }

    var isBuilding by remember {
        mutableStateOf(false)
    }

    var generatedApk by remember {
        mutableStateOf<File?>(null)
    }

    val saveApkLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(
                "application/vnd.android.package-archive"
            )
        ) { uri ->

            val apk = generatedApk

            if (uri != null && apk != null) {

                scope.launch {

                    try {

                        withContext(Dispatchers.IO) {

                            context.contentResolver
                                .openOutputStream(uri)
                                ?.use { output ->

                                    apk.inputStream()
                                        .use { input ->
                                            input.copyTo(output)
                                        }
                                }
                                ?: throw Exception(
                                    "Unable to save APK"
                                )
                        }

                        buildStatus =
                            "APK SAVED SUCCESSFULLY ✓"

                    } catch (e: Exception) {

                        buildStatus =
                            "Save failed: ${
                                e.message ?: "Unknown error"
                            }"
                    }
                }
            }
        }

    val projectPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri != null) {

                projectUri = uri

                val realName =
                    readProjectFileName(
                        context,
                        uri
                    )

                onProjectSelected(realName)

                projectType =
                    detectProjectType(realName)

                generatedApk = null
                previewUrl = null
                previewVisible = false

                when (projectType) {

                    "Web / HTML Project" -> {

                        previewStatus =
                            "✓ LIVE PREVIEW AVAILABLE"

                        previewMessage =
                            "Tap Preview App."
                    }

                    "Android / Gradle Project" -> {

                        previewStatus =
                            "✓ BUILD SUPPORTED"

                        previewMessage =
                            "Native preview unavailable. APK build supported."
                    }

                    "Godot Project" -> {

                        previewStatus =
                            "⚠ ADAPTER REQUIRED"

                        previewMessage =
                            "Godot adapter is not connected yet."
                    }

                    "ZIP Project" -> {

                        previewStatus =
                            "✓ BUILD ROUTE AVAILABLE"

                        previewMessage =
                            "ZIP project selected."
                    }

                    else -> {

                        previewStatus =
                            "✕ UNKNOWN PROJECT"

                        previewMessage =
                            "Project type could not be confirmed."
                    }
                }

                buildStatus =
                    "Project selected"
            }
        }

    val iconPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri != null) {

                iconUri = uri

                onIconSelected()
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color(0xFF07090E)
            )
            .verticalScroll(
                rememberScrollState()
            )
            .padding(18.dp)
    ) {

        Spacer(
            Modifier.height(22.dp)
        )

        Text(
            text = "UNIVERSAL BUILD STUDIO",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )

        Text(
            text = "Apps • Games • Web • Native",
            color = Color(0xFF8E98A8),
            fontSize = 13.sp
        )

        Spacer(
            Modifier.height(22.dp)
        )

        ProjectInfoCard(
            projectName = projectName,
            projectType = projectType,
            onUploadClick = {

                projectPicker.launch(
                    arrayOf("*/*")
                )
            }
        )

        Spacer(
            Modifier.height(14.dp)
        )

        BuilderCard(
            title = "APP ICON"
        ) {

            AppIconPicker(
                iconUri = iconUri,
                onClick = {

                    iconPicker.launch(
                        arrayOf("image/*")
                    )
                }
            )
        }

        Spacer(
            Modifier.height(14.dp)
        )

        BuilderCard(
            title = "APP DETAILS"
        ) {

            OutlinedTextField(
                value = appName,
                onValueChange =
                    onAppNameChange,
                label = {
                    Text("App Name")
                },
                modifier =
                    Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(
                Modifier.height(10.dp)
            )

            OutlinedTextField(
                value = packageName,
                onValueChange =
                    onPackageChange,
                label = {
                    Text("Package ID")
                },
                modifier =
                    Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        Spacer(
            Modifier.height(14.dp)
        )

        OutlinedButton(
            onClick = {

                val uri = projectUri

                if (uri == null) {

                    previewVisible = true

                    previewStatus =
                        "✕ NO PROJECT"

                    previewMessage =
                        "Upload a project first."

                    return@OutlinedButton
                }

                previewVisible = true

                if (
                    projectType ==
                        "Web / HTML Project" ||
                    projectType ==
                        "ZIP Project"
                ) {

                    preparingPreview = true

                    previewStatus =
                        "PREPARING PREVIEW..."

                    previewMessage =
                        "Scanning project..."

                    scope.launch {

                        val result =
                            PreviewManager
                                .preparePreview(
                                    context = context,
                                    projectUri = uri,
                                    projectName = projectName
                                )

                        preparingPreview = false

                        if (
                            result.success &&
                            result.previewUrl != null
                        ) {

                            previewUrl =
                                result.previewUrl

                            previewStatus =
                                "✓ LIVE PREVIEW"

                            previewMessage =
                                "Interactive preview loaded."

                        } else {

                            previewUrl = null

                            if (
                                projectType ==
                                    "ZIP Project"
                            ) {

                                previewStatus =
                                    "✓ BUILD ROUTE AVAILABLE"

                                previewMessage =
                                    "No live web preview. Build can still be attempted."

                            } else {

                                previewStatus =
                                    "✕ PREVIEW FAILED"

                                previewMessage =
                                    result.message
                            }
                        }
                    }

                } else {

                    previewUrl = null
                }
            },
            enabled = !preparingPreview,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {

            if (preparingPreview) {

                CircularProgressIndicator(
                    modifier =
                        Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )

                Spacer(
                    Modifier.width(8.dp)
                )
            }

            Text(
                if (preparingPreview)
                    "PREPARING..."
                else
                    "PREVIEW APP"
            )
        }

        if (previewVisible) {

            Spacer(
                Modifier.height(14.dp)
            )

            PreviewPanel(
                appName = appName,
                projectType = projectType,
                previewStatus = previewStatus,
                previewMessage = previewMessage,
                previewUrl = previewUrl,
                iconUri = iconUri
            )
        }

        Spacer(
            Modifier.height(14.dp)
        )

        BuildStatusCard(
            status = buildStatus
        )

        Spacer(
            Modifier.height(18.dp)
        )

        Button(
            onClick = {

                val uri = projectUri

                if (uri == null) {

                    buildStatus =
                        "Select project first"

                    return@Button
                }

                if (appName.isBlank()) {

                    buildStatus =
                        "Enter app name"

                    return@Button
                }

                if (packageName.isBlank()) {

                    buildStatus =
                        "Enter package ID"

                    return@Button
                }

                generatedApk = null
                isBuilding = true

                buildStatus =
                    "Uploading project and starting build..."

                scope.launch {

                    val controller =
                        BuildController(context)

                    val result =
                        controller.startBuild(
                            BuildRequest(
                                appName = appName,
                                packageName = packageName,
                                projectUri = uri,
                                projectName = projectName
                            )
                        )

                    isBuilding = false

                    if (
                        result.success &&
                        result.apkFile != null
                    ) {

                        generatedApk =
                            result.apkFile

                        buildStatus =
                            "APK READY ✓"

                    } else {

                        generatedApk = null

                        buildStatus =
                            result.message
                    }
                }
            },
            enabled = !isBuilding,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {

            if (isBuilding) {

                CircularProgressIndicator(
                    modifier =
                        Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )

                Spacer(
                    Modifier.width(10.dp)
                )
            }

            Text(
                if (isBuilding)
                    "BUILDING APK..."
                else
                    "BUILD APK",
                fontSize = 17.sp,
                fontWeight =
                    FontWeight.Black
            )
        }

        if (generatedApk != null) {

            Spacer(
                Modifier.height(12.dp)
            )

            Button(
                onClick = {

                    val safeName =
                        appName
                            .trim()
                            .replace(
                                Regex("[^A-Za-z0-9._-]"),
                                "-"
                            )
                            .ifBlank {
                                "generated-app"
                            }

                    saveApkLauncher.launch(
                        "$safeName.apk"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {

                Text(
                    "DOWNLOAD / SAVE APK",
                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                Modifier.height(8.dp)
            )

            OutlinedButton(
                onClick = {

                    val apk =
                        generatedApk
                            ?: return@OutlinedButton

                    try {

                        ApkInstaller.install(
                            context,
                            apk
                        )

                    } catch (e: Exception) {

                        buildStatus =
                            e.message
                                ?: "Unable to install APK"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {

                Text(
                    "INSTALL APK",
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }

        Spacer(
            Modifier.height(12.dp)
        )

        OutlinedButton(
            onClick = onOpenGitHub,
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                "GITHUB BUILDER SETTINGS"
            )
        }

        Spacer(
            Modifier.height(8.dp)
        )

        OutlinedButton(
            onClick = onOpenHistory,
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                "BUILD HISTORY"
            )
        }

        Spacer(
            Modifier.height(35.dp)
        )
    }
}
