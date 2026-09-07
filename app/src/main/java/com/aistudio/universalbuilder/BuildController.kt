package com.aistudio.universalbuilder

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class BuildRequest(
    val appName: String,
    val packageName: String,
    val projectUri: Uri,
    val projectName: String
)

data class BuildResult(
    val success: Boolean,
    val message: String,
    val apkFile: File? = null
)

class BuildController(
    private val context: Context
) {

    suspend fun startBuild(
        request: BuildRequest
    ): BuildResult = withContext(Dispatchers.IO) {

        try {

            val store =
                GitHubConfigStore(context)

            val config =
                store.load()

            if (
                config.username.isBlank() ||
                config.repository.isBlank() ||
                config.token.isBlank()
            ) {
                return@withContext BuildResult(
                    success = false,
                    message =
                        "GitHub Builder is not configured"
                )
            }

            val connection =
                GitHubApiClient.testConnection(
                    username = config.username,
                    repository = config.repository,
                    token = config.token
                )

            if (connection.isFailure) {

                return@withContext BuildResult(
                    success = false,
                    message =
                        "GitHub connection failed"
                )
            }

            val upload =
                ProjectTransport.uploadProject(
                    context = context,
                    projectUri = request.projectUri,
                    username = config.username,
                    repository = config.repository,
                    token = config.token
                )

            if (!upload.success) {

                return@withContext BuildResult(
                    success = false,
                    message = upload.message
                )
            }

            val trigger =
                GitHubApiClient.triggerWorkflow(
                    username = config.username,
                    repository = config.repository,
                    token = config.token,
                    workflowFile =
                        "build-generated-app.yml"
                )

            if (trigger.isFailure) {

                return@withContext BuildResult(
                    success = false,
                    message =
                        "Workflow trigger failed"
                )
            }

            val monitor =
                BuildMonitor.waitForBuild(
                    username = config.username,
                    repository = config.repository,
                    token = config.token
                )

            if (!monitor.success) {

                return@withContext BuildResult(
                    success = false,
                    message = monitor.message
                )
            }

            val artifactId =
                monitor.artifactId
                    ?: return@withContext BuildResult(
                        success = false,
                        message =
                            "APK artifact ID not found"
                    )

            val download =
                ApkDownloader.downloadArtifact(
                    context = context,
                    username = config.username,
                    repository = config.repository,
                    token = config.token,
                    artifactId = artifactId
                )

            if (
                !download.success ||
                download.apkFile == null
            ) {

                return@withContext BuildResult(
                    success = false,
                    message = download.message
                )
            }

            BuildResult(
                success = true,
                message =
                    "APK READY",
                apkFile =
                    download.apkFile
            )

        } catch (e: Exception) {

            BuildResult(
                success = false,
                message =
                    e.message
                        ?: "Build failed"
            )
        }
    }
}
