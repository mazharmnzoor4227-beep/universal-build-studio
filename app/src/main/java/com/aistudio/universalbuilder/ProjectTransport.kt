package com.aistudio.universalbuilder

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import org.json.JSONArray
import org.json.JSONObject

class ProjectTransport(private val releaseTag: String = "universal-builder-input") {

    private val API = "https://api.github.com"
    private val UPLOADS = "https://uploads.github.com"

    private val RELEASE_TAG = "universal-builder-input"
    private val ASSET_NAME = "project-input.zip"

    private val client = OkHttpClient.Builder()
        .build()

    data class UploadResult(
        val success: Boolean,
        val message: String,
        val assetName: String = ASSET_NAME
    )

    suspend fun uploadProject(
        context: Context,
        projectUri: Uri,
        username: String,
        repository: String,
        token: String
    ): UploadResult = withContext(Dispatchers.IO) {

        try {

            val size = getFileSize(
                context,
                projectUri
            )

            if (size <= 0L) {
                return@withContext UploadResult(
                    false,
                    "Could not read project file"
                )
            }

            // 250 MB target safety check
            val maxSize =
                300L * 1024L * 1024L

            if (size > maxSize) {
                return@withContext UploadResult(
                    false,
                    "Project is larger than 300 MB"
                )
            }

            val releaseId =
                getOrCreateRelease(
                    username,
                    repository,
                    token
                )

            if (releaseId == null) {
                return@withContext UploadResult(
                    false,
                    "Could not prepare GitHub upload storage"
                )
            }

            deleteOldInputAsset(
                username,
                repository,
                token,
                releaseId
            )

            val uploadSuccess =
                uploadReleaseAsset(
                    context = context,
                    uri = projectUri,
                    username = username,
                    repository = repository,
                    token = token,
                    releaseId = releaseId,
                    size = size
                )

            if (!uploadSuccess) {

                UploadResult(
                    false,
                    "Project upload failed"
                )

            } else {

                UploadResult(
                    true,
                    "Project uploaded successfully",
                    ASSET_NAME
                )
            }

        } catch (e: Exception) {

            UploadResult(
                false,
                e.message ?: "Project upload failed"
            )
        }
    }

    private fun getOrCreateRelease(
        username: String,
        repository: String,
        token: String
    ): Long? {

        val existingRequest =
            Request.Builder()
                .url(
                    "$API/repos/$username/$repository/releases/tags/$releaseTag"
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

        client.newCall(existingRequest)
            .execute()
            .use { response ->

                if (response.isSuccessful) {

                    val body =
                        response.body
                            ?.string()
                            ?: return null

                    return JSONObject(body)
                        .getLong("id")
                }
            }

        val json =
            JSONObject().apply {
                put(
                    "tag_name",
                    releaseTag
                )

                put(
                    "name",
                    "Universal Builder Input"
                )

                put(
                    "body",
                    "Temporary build input storage."
                )

                put(
                    "draft",
                    false
                )

                put(
                    "prerelease",
                    true
                )
            }

        val request =
            Request.Builder()
                .url(
                    "$API/repos/$username/$repository/releases"
                )
                .post(
                    json.toString()
                        .toRequestBody(
                            "application/json"
                                .toMediaType()
                        )
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
                    response.body
                        ?.string()
                        ?: return null

                return JSONObject(body)
                    .getLong("id")
            }
    }

    private fun deleteOldInputAsset(
        username: String,
        repository: String,
        token: String,
        releaseId: Long
    ) {

        val request =
            Request.Builder()
                .url(
                    "$API/repos/$username/$repository/releases/$releaseId/assets"
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

                val body =
                    response.body
                        ?.string()
                        ?: return

                val assets =
                    JSONArray(body)

                for (i in 0 until assets.length()) {

                    val asset =
                        assets.getJSONObject(i)

                    if (
                        asset.getString("name") ==
                        ASSET_NAME
                    ) {

                        val assetId =
                            asset.getLong("id")

                        deleteAsset(
                            username,
                            repository,
                            token,
                            assetId
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
                    "$API/repos/$username/$repository/releases/assets/$assetId"
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

    private fun uploadReleaseAsset(
        context: Context,
        uri: Uri,
        username: String,
        repository: String,
        token: String,
        releaseId: Long,
        size: Long
    ): Boolean {

        val body =
            object : RequestBody() {

                override fun contentType() =
                    "application/zip"
                        .toMediaType()

                override fun contentLength() =
                    size

                override fun writeTo(
                    sink: BufferedSink
                ) {

                    context.contentResolver
                        .openInputStream(uri)
                        ?.use { input ->

                            val buffer =
                                ByteArray(
                                    1024 * 1024
                                )

                            while (true) {

                                val read =
                                    input.read(buffer)

                                if (read <= 0) {
                                    break
                                }

                                sink.write(
                                    buffer,
                                    0,
                                    read
                                )
                            }
                        }
                        ?: throw Exception(
                            "Cannot open selected project"
                        )
                }
            }

        val request =
            Request.Builder()
                .url(
                    "$UPLOADS/repos/$username/$repository/releases/$releaseId/assets?name=$ASSET_NAME"
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

                return response.isSuccessful
            }
    }

    private fun getFileSize(
        context: Context,
        uri: Uri
    ): Long {

        context.contentResolver
            .query(
                uri,
                arrayOf(
                    OpenableColumns.SIZE
                ),
                null,
                null,
                null
            )
            ?.use { cursor ->

                if (cursor.moveToFirst()) {

                    val index =
                        cursor.getColumnIndex(
                            OpenableColumns.SIZE
                        )

                    if (index >= 0) {
                        return cursor.getLong(
                            index
                        )
                    }
                }
            }

        return 0L
    }
}
