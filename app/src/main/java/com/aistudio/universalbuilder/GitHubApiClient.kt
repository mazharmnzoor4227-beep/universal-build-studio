package com.aistudio.universalbuilder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object GitHubApiClient {

    private val client = OkHttpClient()

    private const val API =
        "https://api.github.com"

    suspend fun testConnection(
        username: String,
        repository: String,
        token: String
    ): Result<String> = withContext(Dispatchers.IO) {

        try {

            val request = Request.Builder()
                .url(
                    "$API/repos/$username/$repository"
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

                    if (response.isSuccessful) {

                        Result.success(
                            "GitHub connected"
                        )

                    } else {

                        Result.failure(
                            Exception(
                                "GitHub error ${response.code}"
                            )
                        )
                    }
                }

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    suspend fun triggerWorkflow(
        username: String,
        repository: String,
        token: String,
        workflowFile: String =
            "build-generated-app.yml"
    ): Result<String> = withContext(Dispatchers.IO) {

        try {

            val json =
                """
                {
                  "ref": "main",
                    "inputs": {"request_id": "$requestId"}
                }
                """.trimIndent()

            val body = json.toRequestBody(
                "application/json".toMediaType()
            )

            val request = Request.Builder()
                .url(
                    "$API/repos/$username/$repository/actions/workflows/$workflowFile/dispatches"
                )
                .post(body)
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

                    if (
                        response.code == 204 ||
                        response.isSuccessful
                    ) {

                        Result.success(
                            "Build started"
                        )

                    } else {

                        Result.failure(
                            Exception(
                                "Workflow error ${response.code}"
                            )
                        )
                    }
                }

        } catch (e: Exception) {

            Result.failure(e)
        }
    }
}
