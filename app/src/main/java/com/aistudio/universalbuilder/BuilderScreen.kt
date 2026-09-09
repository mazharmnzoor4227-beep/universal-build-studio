package com.aistudio.universalbuilder

import android.content.Intent
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
import kotlinx.coroutines.delay
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

    val sessionStore = remember {
        BuilderSessionStore(context)
    }

    val savedSession = remember {
        sessionStore.load()
    }

    var projectUri by remember {
        mutableStateOf(
            savedSession.projectUri
                .takeIf { it.isNotBlank() }
                ?.let(Uri::parse)
        )
    }

    var iconUri by remember {
        mutableStateOf(
            savedSession.iconUri
                .takeIf { it.isNotBlank() }
                ?.let(Uri::parse)
        )
    }

    var projectType by remember {
        mutableStateOf(
            if (projectName == "No project selected") {
                "No project selected"
            } else {
                detectProjectType(projectName)
            }
        )
    }

    var htmlCode by remember {
        mutableStateOf("")
    }

    var previewVisible by remember {
        mutableStateOf(false)
    }

    var previewUrl by remember {
        mutableStateOf<String?>(null)
    }

    var previewStatus by remember {
        mutableStateOf(
            if (projectUri != null) {
                "✓ PROJECT RESTORED"
            } else {
                "NO PROJECT"
            }
        )
    }

    var previewMessage by remember {
        mutableStateOf(
            if (projectUri != null) {
                "Project is ready."
            } else {
                "Upload ZIP or paste HTML code."
            }
        )
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

    fun saveSession(
        newAppName: String = appName,
        newPackageName: String = packageName,
        newProjectName: String = projectName
    ) {
        sessionStore.save(
            appName = newAppName,
            packageName = newPackageName,
            projectName = newProjectName,
            projectUri = projectUri?.toString(),
            iconUri = iconUri?.toString()
        )
    }

    LaunchedEffect(Unit) {

        while (true) {

            val state =
                withContext(Dispatchers.IO) {
                    BuildStatusHelper
                        .getCurrentState(context)
                }

            isBuilding = state.isBuilding
            buildStatus = state.status

            if (state.apkFile != null) {
                generatedApk = state.apkFile
            }

            delay(
                if (state.isBuilding) {
                    2000L
                } else {
                    5000L
                }
            )
        }
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
                                ?: error(
                                    "Unable to save APK"
                                )
                        }

                        buildStatus =
                            "APK SAVED SUCCESSFULLY ✓"

                    } catch (e: Exception) {

                        buildStatus =
                            "Save failed: ${
                                e.message
                                    ?: "Unknown error"
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

                try {

                    context.contentResolver
                        .takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )

                } catch (_: Exception) {
                }

                projectUri = uri

                val realName =
                    readProjectFileName(
                        context,
                        uri
                    )

                projectType =
                    detectProjectType(realName)

                onProjectSelected(realName)

                htmlCode = ""

                sessionStore.save(
                    appName = appName,
                    packageName = packageName,
                    projectName = realName,
                    projectUri = uri.toString(),
                    iconUri = iconUri?.toString()
                )

                generatedApk = null
                previewUrl = null
                previewVisible = false

                buildStatus =
                    "Project selected ✓"

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
            }
        }

    val iconPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri != null) {

                try {

                    context.contentResolver
                        .takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )

                } catch (_: Exception) {
                }

                iconUri = uri

                onIconSelected()

                sessionStore.save(
                    appName = appName,
                    packageName = packageName,
                    projectName = projectName,
                    projectUri = projectUri?.toString(),
                    iconUri = uri.toString()
                )
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
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
            text = "HTML • React/Vite • Android • Flutter ZIP",
            color = Color(0xFF8E98A8),
            fontSize = 13.sp
        )

        Text("Upload a complete project. Native Android/Flutter projects use their own name, ID and icon. Public GitHub repositories expose uploaded source ZIPs.", color = Color(0xFF8E98A8), fontSize = 12.sp)
        Spacer(Modifier.height(22.dp))

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

        CodePasteCard(
            htmlCode = htmlCode,

            onCodeChange = {
                htmlCode = it
            },

            onUseCode = {
                if (isBuilding) return@CodePasteCard

                val result =
                    CodeProjectCreator.create(
                        context = context,
                        html = htmlCode
                    )

                if (
                    result.success &&
                    result.uri != null
                ) {

                    val codeProjectName =
                        "pasted-code-project.zip"

                    projectUri =
                        result.uri

                    projectType =
                        "Web / HTML Project"

                    onProjectSelected(
                        codeProjectName
                    )

                    generatedApk = null

                    previewUrl = null

                    previewVisible = false

                    previewStatus =
                        "✓ HTML CODE READY"

                    previewMessage =
                        "Pasted code is ready for APK build."

                    buildStatus =
                        "HTML code selected ✓"

                    sessionStore.save(
                        appName = appName,
                        packageName = packageName,
                        projectName = codeProjectName,
                        projectUri =
                            result.uri.toString(),
                        iconUri =
                            iconUri?.toString()
                    )

                } else {

                    buildStatus =
                        result.message
                }
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

                onValueChange = {

                    onAppNameChange(it)

                    sessionStore.save(
                        appName = it,
                        packageName = packageName,
                        projectName = projectName,
                        projectUri =
                            projectUri?.toString(),
                        iconUri =
                            iconUri?.toString()
                    )
                },

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

                onValueChange = {

                    onPackageChange(it)

                    sessionStore.save(
                        appName = appName,
                        packageName = it,
                        projectName = projectName,
                        projectUri =
                            projectUri?.toString(),
                        iconUri =
                            iconUri?.toString()
                    )
                },

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

        val options = remember { WebOptions(context) }
        BuilderCard(title = "WEB APK OPTIONS") {
            Text("Applies to HTML / React / Vite APKs. Enable only features your code uses.")
            listOf("camera" to "Camera", "microphone" to "Microphone", "library" to "Media library", "media" to "Media controls", "landscape" to "Landscape", "fullscreen" to "Fullscreen").forEach { (key, label) ->
                var checked by remember { mutableStateOf(options.enabled(key)) }
                Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = checked, onCheckedChange = { checked = it; options.set(key, it) }, enabled = !isBuilding)
                    Text(label)
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        OutlinedButton(
            onClick = {

                val uri = projectUri

                if (uri == null) {

                    previewVisible = true

                    previewStatus =
                        "✕ NO PROJECT"

                    previewMessage =
                        "Upload ZIP or use pasted HTML first."

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
                                    projectName =
                                        projectName
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

                            previewStatus =
                                if (
                                    projectType ==
                                    "ZIP Project"
                                ) {
                                    "✓ BUILD ROUTE AVAILABLE"
                                } else {
                                    "✕ PREVIEW FAILED"
                                }

                            previewMessage =
                                result.message
                        }
                    }
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
                if (preparingPreview) {
                    "PREPARING..."
                } else {
                    "PREVIEW UPLOADED PROJECT"
                }
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
                previewMessage =
                    previewMessage,
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

                when {

                    uri == null -> {

                        buildStatus =
                            "Upload ZIP or use pasted HTML first"
                    }

                    appName.isBlank() -> {

                        buildStatus =
                            "Enter app name"
                    }

                    packageName.isBlank() -> {

                        buildStatus =
                            "Enter package ID"
                    }

                    else -> {

                        saveSession()

                        generatedApk = null

                        isBuilding = true

                        buildStatus =
                            "Build queued..."

                        BackgroundBuildManager
                            .startBuild(
                                context = context,
                                appName = appName,
                                packageName =
                                    packageName,
                                projectName =
                                    projectName,
                                projectUri =
                                    uri.toString(),
                                iconUri =
                                    iconUri?.toString()
                            )
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
                text =
                    if (isBuilding) {
                        "BUILDING APK..."
                    } else {
                        "BUILD APK"
                    },

                fontSize = 17.sp,

                fontWeight =
                    FontWeight.Black
            )
        }

        if (generatedApk != null) {
            OutlinedButton(onClick = {
                generatedApk?.let { file ->
                    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "application/vnd.android.package-archive"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(share, "Share APK"))
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("SHARE APK") }


            Spacer(
                Modifier.height(12.dp)
            )

            Button(
                onClick = {

                    val safeName =
                        appName
                            .trim()
                            .replace(
                                Regex(
                                    "[^A-Za-z0-9._-]"
                                ),
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
                    text =
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
                    text = "INSTALL APK",
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
