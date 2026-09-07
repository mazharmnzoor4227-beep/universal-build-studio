package com.aistudio.universalbuilder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class BuildMonitorResult(
    val success: Boolean,
    val status: String,
    val runId: Long? = null,
    val artifactId: Long? = null,
    val message: String
)

private data class RunInfo(
    val id: Long,
    val status: String,
    val conclusion: String?
)

object BuildMonitor {

    private val client = OkHttpClient()

    suspend fun latestRunId(
        username: String,
        repository: String,
        token: String
    ): Long = withContext(Dispatchers.IO) {

        getLatestRun(
            username,
            repository,
            token
        )?.id ?: 0L
    }

    suspend fun waitForBuild(
        username: String,
        repository: String,
        token: String,
        afterRunId: Long,
        maxChecks: Int = 60
    ): BuildMonitorResult = withContext(Dispatchers.IO) {

        repeat(maxChecks) {

            val run =
                getLatestRun(
                    username,
                    repository,
                    token
                )

            if (
                run == null ||
                run.id <= afterRunId
            ) {
                delay(5000)
                return@repeat
            }

            if (run.status == "completed") {

                if (run.conclusion == "success") {

                    val artifactId =
                        getArtifactId(
                            username,
                            repository,
                            token,
                            run.id
                        )

                    if (artifactId != null) {

                        return@withContext BuildMonitorResult(
                            success = true,
                            status = "completed",
                            runId = run.id,
                            artifactId = artifactId,
                            message = "APK ready"
                        )
                    }

                    return@withContext BuildMonitorResult(
                        success = false,
                        status = "completed",
                        runId = run.id,
                        message =
                            "Build completed but APK was not found"
                    )
                }

                return@withContext BuildMonitorResult(
                    success = false,
                    status = "failed",
                    runId = run.id,
                    message =
                        "GitHub build failed"
                )
            }

            delay(5000)
        }

        BuildMonitorResult(
            success = false,
            status = "timeout",
            message =
                "Build is taking too long"
        )
    }

    private fun getLatestRun(
        username: String,
        repository: String,
        token: String
    ): RunInfo? {

        val request =
            Request.Builder()
                .url(
                    "https://api.github.com/repos/$username/$repository/actions/workflows/build-generated-app.yml/runs?per_page=1"
                )
                .header(
                    "Authorization",
                    "Bearer $token"
                )
                .header(
                    "Accept",
                    "application/vnd.github+json"
                )
                .header(
                    "X-GitHub-Api-Version",
                    "2022-11-28"
                )
                .build()

        client.newCall(request)
            .execute()
            .use { response ->

                if (!response.isSuccessful) {
                    return null
                }

                val body =
                    response.body?.string()
                        ?: return null

                val json =
                    JSONObject(body)

                val runs =
                    json.getJSONArray(
                        "workflow_runs"
                    )

                if (runs.length() == 0) {
                    return null
                }

                val item =
                    runs.getJSONObject(0)

                return RunInfo(
                    id = item.getLong("id"),
                    status =
                        item.getString("status"),
                    conclusion =
                        if (
                            item.isNull(
                                "conclusion"
                            )
                        ) {
                            null
                        } else {
                            item.getString(
                                "conclusion"
                            )
                        }
                )
            }
    }

    private fun getArtifactId(
        username: String,
        repository: String,
        token: String,
        runId: Long
    ): Long? {

        val request =
            Request.Builder()
                .url(
                    "https://api.github.com/repos/$username/$repository/actions/runs/$runId/artifacts"
                )
                .header(
                    "Authorization",
                    "Bearer $token"
                )
                .header(
                    "Accept",
                    "application/vnd.github+json"
                )
                .header(
                    "X-GitHub-Api-Version",
                    "2022-11-28"
                )
                .build()

        client.newCall(request)
            .execute()
            .use { response ->

                if (!response.isSuccessful) {
                    return null
                }

                val body =
                    response.body?.string()
                        ?: return null

                val json =
                    JSONObject(body)

                val artifacts =
                    json.getJSONArray(
                        "artifacts"
                    )

                for (
                    i in 0 until artifacts.length()
                ) {

                    val item =
                        artifacts.getJSONObject(i)

                    if (
                        item.getString("name") ==
                        "generated-apk"
                    ) {
                        return item.getLong("id")
                    }
                }

                return null
            }
    }
}
