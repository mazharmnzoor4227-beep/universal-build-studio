package com.aistudio.universalbuilder

import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
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

object BuildMonitor {

    private val client = OkHttpClient()

    suspend fun waitForBuild(
        username: String,
        repository: String,
        token: String,
        maxChecks: Int = 60
    ): BuildMonitorResult = withContext(Dispatchers.IO) {

        repeat(maxChecks) {

            val run = getLatestGeneratedRun(
                username,
                repository,
                token
            )

            if (run != null) {

                val runId = run.first
                val status = run.second
                val conclusion = run.third

                if (status == "completed") {

                    if (conclusion == "success") {

                        val artifactId =
                            getArtifactId(
                                username,
                                repository,
                                token,
                                runId
                            )

                        if (artifactId != null) {

                            return@withContext BuildMonitorResult(
                                success = true,
                                status = "completed",
                                runId = runId,
                                artifactId = artifactId,
                                message = "APK ready"
                            )
                        }

                        return@withContext BuildMonitorResult(
                            success = false,
                            status = "completed",
                            runId = runId,
                            message = "Build succeeded but APK artifact was not found"
                        )
                    }

                    return@withContext BuildMonitorResult(
                        success = false,
                        status = "failed",
                        runId = runId,
                        message = "GitHub build failed"
                    )
                }
            }

            delay(5000)
        }

        BuildMonitorResult(
            success = false,
            status = "timeout",
            message = "Build is taking too long"
        )
    }

    private fun getLatestGeneratedRun(
        username: String,
        repository: String,
        token: String
    ): Triple<Long, String, String?>? {

        val request = Request.Builder()
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

                val id =
                    item.getLong("id")

                val status =
                    item.getString("status")

                val conclusion =
                    if (item.isNull("conclusion")) {
                        null
                    } else {
                        item.getString("conclusion")
                    }

                return Triple(
                    id,
                    status,
                    conclusion
                )
            }
    }

    private fun getArtifactId(
        username: String,
        repository: String,
        token: String,
        runId: Long
    ): Long? {

        val request = Request.Builder()
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

                for (i in 0 until artifacts.length()) {

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
