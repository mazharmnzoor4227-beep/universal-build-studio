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
    val projectName: String,
    val iconUri: Uri? = null
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
    ): BuildResult =
        withContext(Dispatchers.IO) {

            try {

                val config =
                    GitHubConfigStore(context)
                        .load()

                if (
                    config.username.isBlank() ||
                    config.repository.isBlank() ||
                    config.token.isBlank()
                ) {
                    return@withContext BuildResult(
                        false,
                        "GitHub Builder is not configured"
                    )
                }

                val connection =
                    GitHubApiClient.testConnection(
                        config.username,
                        config.repository,
                        config.token
                    )

                if (connection.isFailure) {
                    return@withContext BuildResult(
                        false,
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
                        false,
                        upload.message
                    )
                }

                val previousRunId =
                    BuildMonitor.latestRunId(
                        config.username,
                        config.repository,
                        config.token
                    )

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
                        false,
                        "Workflow trigger failed"
                    )
                }

                val monitor =
                    BuildMonitor.waitForBuild(
                        username = config.username,
                        repository = config.repository,
                        token = config.token,
                        afterRunId = previousRunId
                    )

                if (!monitor.success) {
                    return@withContext BuildResult(
                        false,
                        monitor.message
                    )
                }

                val artifactId =
                    monitor.artifactId
                        ?: return@withContext BuildResult(
                            false,
                            "APK artifact not found"
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
                        false,
                        download.message
                    )
                }

                BuildResult(
                    success = true,
                    message = "APK READY",
                    apkFile = download.apkFile
                )

            } catch (e: Exception) {

                BuildResult(
                    success = false,
                    message =
                        e.message ?: "Build failed"
                )
            }
        }
}
