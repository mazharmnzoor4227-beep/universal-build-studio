package com.aistudio.universalbuilder

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BuildRequest(
    val appName: String,
    val packageName: String,
    val projectUri: android.net.Uri,
    val projectName: String
)

data class BuildResult(
    val success: Boolean,
    val message: String
)

class BuildController(
    private val context: Context
) {

    suspend fun startBuild(
        request: BuildRequest
    ): BuildResult = withContext(Dispatchers.IO) {

        try {

            val store = GitHubConfigStore(context)
            val config = store.load()

            if (
                config.username.isBlank() ||
                config.repository.isBlank() ||
                config.token.isBlank()
            ) {
                return@withContext BuildResult(
                    success = false,
                    message = "GitHub Builder is not configured"
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
                    message = "GitHub connection failed: ${
                        connection.exceptionOrNull()?.message
                            ?: "Unknown error"
                    }"
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
                    workflowFile = "build-generated-app.yml"
                )

            if (trigger.isFailure) {
                return@withContext BuildResult(
                    success = false,
                    message = "Workflow trigger failed: ${
                        trigger.exceptionOrNull()?.message
                            ?: "Unknown error"
                    }"
                )
            }

            BuildResult(
                success = true,
                message = "Project uploaded. GitHub build started."
            )

        } catch (e: Exception) {

            BuildResult(
                success = false,
                message = e.message ?: "Build failed"
            )
        }
    }
}
