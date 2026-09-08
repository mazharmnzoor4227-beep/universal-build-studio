package com.aistudio.universalbuilder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

object IconTransport {

    private const val API =
        "https://api.github.com"

    private const val UPLOADS =
        "https://uploads.github.com"

    private const val RELEASE_TAG =
        "universal-builder-input"

    const val ICON_ASSET_NAME =
        "app-icon.png"

    private val client =
        OkHttpClient()

    data class IconUploadResult(
        val success: Boolean,
        val message: String
    )

    suspend fun uploadIcon(
        context: Context,
        iconUri: Uri,
        username: String,
        repository: String,
        token: String
    ): IconUploadResult =
        withContext(Dispatchers.IO) {

            try {

                val bytes =
                    readIcon(
                        context,
                        iconUri
                    )
                        ?: return@withContext IconUploadResult(
                            false,
                            "Could not read icon"
                        )

                val releaseId =
                    getReleaseId(
                        username,
                        repository,
                        token
                    )
                        ?: return@withContext IconUploadResult(
                            false,
                            "Release not found"
                        )

                deleteOldIcon(
                    username,
                    repository,
                    token,
                    releaseId
                )

                val body =
                    bytes.toRequestBody(
                        "image/png".toMediaType()
                    )

                val request =
                    Request.Builder()
                        .url(
                            "$UPLOADS/repos/" +
                                "$username/$repository/" +
                                "releases/$releaseId/assets" +
                                "?name=$ICON_ASSET_NAME"
                        )
                        .post(body)
                        .headers(
                            headers(token)
                        )
                        .build()

                client.newCall(request)
                    .execute()
                    .use {

                        if (it.isSuccessful) {
                            IconUploadResult(
                                true,
                                "Icon uploaded"
                            )
                        } else {
                            IconUploadResult(
                                false,
                                "Icon upload failed"
                            )
                        }
                    }

            } catch (e: Exception) {

                IconUploadResult(
                    false,
                    e.message ?: "Icon upload failed"
                )
            }
        }

    suspend fun clearIcon(
        username: String,
        repository: String,
        token: String
    ): IconUploadResult =
        withContext(Dispatchers.IO) {

            try {

                val releaseId =
                    getReleaseId(
                        username,
                        repository,
                        token
                    )
                        ?: return@withContext IconUploadResult(
                            false,
                            "Release not found"
                        )

                deleteOldIcon(
                    username,
                    repository,
                    token,
                    releaseId
                )

                IconUploadResult(
                    true,
                    "Old icon cleared"
                )

            } catch (e: Exception) {

                IconUploadResult(
                    false,
                    e.message ?: "Icon clear failed"
                )
            }
        }

    private fun readIcon(
        context: Context,
        uri: Uri
    ): ByteArray? {

        val bitmap =
            context.contentResolver
                .openInputStream(uri)
                ?.use {
                    BitmapFactory.decodeStream(it)
                }
                ?: return null

        val output =
            ByteArrayOutputStream()

        bitmap.compress(
            Bitmap.CompressFormat.PNG,
            100,
            output
        )

        bitmap.recycle()

        return output.toByteArray()
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
                        "releases/tags/$RELEASE_TAG"
                )
                .headers(
                    headers(token)
                )
                .build()

        client.newCall(request)
            .execute()
            .use {

                if (!it.isSuccessful) {
                    return null
                }

                val text =
                    it.body?.string()
                        ?: return null

                return JSONObject(text)
                    .getLong("id")
            }
    }

    private fun deleteOldIcon(
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
                .headers(
                    headers(token)
                )
                .build()

        client.newCall(request)
            .execute()
            .use {

                if (!it.isSuccessful) {
                    return
                }

                val data =
                    JSONArray(
                        it.body?.string() ?: "[]"
                    )

                for (i in 0 until data.length()) {

                    val asset =
                        data.getJSONObject(i)

                    if (
                        asset.getString("name") ==
                        ICON_ASSET_NAME
                    ) {

                        val deleteRequest =
                            Request.Builder()
                                .url(
                                    "$API/repos/$username/$repository/" +
                                        "releases/assets/" +
                                        asset.getLong("id")
                                )
                                .delete()
                                .headers(
                                    headers(token)
                                )
                                .build()

                        client.newCall(deleteRequest)
                            .execute()
                            .close()
                    }
                }
            }
    }

    private fun headers(
        token: String
    ) =
        okhttp3.Headers.Builder()
            .add(
                "Authorization",
                "Bearer $token"
            )
            .add(
                "Accept",
                "application/vnd.github+json"
            )
            .add(
                "X-GitHub-Api-Version",
                "2022-11-28"
            )
            .build()
}
