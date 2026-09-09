package com.aistudio.universalbuilder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class BuildMetaTransport(private val releaseTag: String = "universal-builder-input") {

    private val API =
        "https://api.github.com"

    private val UPLOADS =
        "https://uploads.github.com"

    private val RELEASE_TAG =
        "universal-builder-input"

    val META_ASSET_NAME =
        "build-meta.json"

    private val client =
        OkHttpClient.Builder()
            .build()

    data class MetaUploadResult(
        val success: Boolean,
        val message: String
    )

    suspend fun uploadMeta(
        appName: String,
        packageName: String,
        username: String,
        repository: String,
        token: String,
        options: org.json.JSONObject = org.json.JSONObject()
    ): MetaUploadResult =
        withContext(Dispatchers.IO) {

            try {

                val releaseId =
                    getReleaseId(
                        username,
                        repository,
                        token
                    )
                        ?: return@withContext MetaUploadResult(
                            false,
                            "Builder input release not found"
                        )

                deleteOldMeta(
                    username,
                    repository,
                    token,
                    releaseId
                )

                val json =
                    JSONObject().apply {
                        put("options", options)

                        put(
                            "appName",
                            appName.trim()
                        )

                        put(
                            "packageName",
                            packageName.trim()
                        )
                    }

                val body =
                    json.toString()
                        .toRequestBody(
                            "application/json"
                                .toMediaType()
                        )

                val request =
                    Request.Builder()
                        .url(
                            "$UPLOADS/repos/" +
                                "$username/$repository/" +
                                "releases/$releaseId/assets" +
                                "?name=$META_ASSET_NAME"
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

                        if (response.isSuccessful) {

                            MetaUploadResult(
                                true,
                                "Build metadata uploaded"
                            )

                        } else {

                            MetaUploadResult(
                                false,
                                "Build metadata upload failed"
                            )
                        }
                    }

            } catch (e: Exception) {

                MetaUploadResult(
                    false,
                    e.message
                        ?: "Build metadata upload failed"
                )
            }
        }

    private fun getReleaseId(
        username: String,
        repository: String,
        token: String
    ): Long? {

        val request =
            Request.Builder()
                .url(
                    "$API/repos/$username/$repository/" +
                        "releases/tags/$releaseTag"
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

                val text =
                    response.body
                        ?.string()
                        ?: return null

                return JSONObject(text)
                    .getLong("id")
            }
    }

    private fun deleteOldMeta(
        username: String,
        repository: String,
        token: String,
        releaseId: Long
    ) {

        val request =
            Request.Builder()
                .url(
                    "$API/repos/$username/$repository/" +
                        "releases/$releaseId/assets"
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
                    return
                }

                val text =
                    response.body
                        ?.string()
                        ?: return

                val assets =
                    JSONArray(text)

                for (
                    i in 0 until assets.length()
                ) {

                    val asset =
                        assets.getJSONObject(i)

                    if (
                        asset.getString("name") ==
                        META_ASSET_NAME
                    ) {

                        deleteAsset(
                            username,
                            repository,
                            token,
                            asset.getLong("id")
                        )
                    }
                }
            }
    }

    private fun deleteAsset(
        username: String,
        repository: String,
        token: String,
        assetId: Long
    ) {

        val request =
            Request.Builder()
                .url(
                    "$API/repos/$username/$repository/" +
                        "releases/assets/$assetId"
                )
                .delete()
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
            .close()
    }
}
