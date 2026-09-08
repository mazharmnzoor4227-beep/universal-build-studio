package com.aistudio.universalbuilder

import android.content.Context
import android.net.Uri
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
    ): BuildResult {

        return try {

            val config =
                GitHubConfigStore(context).load()

            if (
                config.username.isBlank() ||
                config.repository.isBlank() ||
                config.token.isBlank()
            ) {
                return BuildResult(
                    false,
                    "GitHub Builder is not configured"
                )
            }

            val projectUpload =
                ProjectTransport.uploadProject(
                    context = context,
                    projectUri = request.projectUri,
                    username = config.username,
                    repository = config.repository,
                    token = config.token
                )

            if (!projectUpload.success) {
                return BuildResult(
                    false,
                    projectUpload.message
                )
            }

            if (request.iconUri != null) {

                val iconUpload =
                    IconTransport.uploadIcon(
                        context = context,
                        iconUri = request.iconUri,
                        username = config.username,
                        repository = config.repository,
                        token = config.token
                    )

                if (!iconUpload.success) {
                    return BuildResult(
                        false,
                        iconUpload.message
                    )
                }
            }

            val metaUpload =
                BuildMetaTransport.uploadMeta(
                    appName = request.appName,
                    packageName = request.packageName,
                    username = config.username,
                    repository = config.repository,
                    token = config.token
                )

            if (!metaUpload.success) {
                return BuildResult(
                    false,
                    metaUpload.message
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
                return BuildResult(
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
                return BuildResult(
                    false,
                    monitor.message
                )
            }

            val artifactId =
                monitor.artifactId
                    ?: return BuildResult(
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
                return BuildResult(
                    false,
                    download.message
                )
            }

            BuildResult(
                true,
                "APK READY",
                download.apkFile
            )

        } catch (e: Exception) {

            BuildResult(
                false,
                e.message ?: "Build failed"
            )
        }
    }
}
