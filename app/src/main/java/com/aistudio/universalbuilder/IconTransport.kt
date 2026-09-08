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
        OkHttpClient.Builder()
            .build()

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

                val pngBytes =
                    readIconAsPng(
                        context,
                        iconUri
                    )
                        ?: return@withContext IconUploadResult(
                            false,
                            "Could not read selected icon"
                        )

                val releaseId =
                    getReleaseId(
                        username,
                        repository,
                        token
                    )
                        ?: return@withContext IconUploadResult(
                            false,
                            "Builder input release not found"
                        )

                deleteOldIcon(
                    username,
                    repository,
                    token,
                    releaseId
                )

                val body =
                    pngBytes.toRequestBody(
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
                    e.message
                        ?: "Icon upload failed"
                )
            }
        }

    private fun readIconAsPng(
        context: Context,
        uri: Uri
    ): ByteArray? {

        val bitmap =
            context.contentResolver
                .openInputStream(uri)
                ?.use { input ->
                    BitmapFactory.decodeStream(input)
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

    private fun deleteOldIcon(
        username: String,
        repository: String,
        token: String,
        releaseId: Long
    ) {

        val listRequest =
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

        client.newCall(listRequest)
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
                        ICON_ASSET_NAME
                    ) {

                        val id =
                            asset.getLong("id")

                        deleteAsset(
                            username,
                            repository,
                            token,
                            id
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
