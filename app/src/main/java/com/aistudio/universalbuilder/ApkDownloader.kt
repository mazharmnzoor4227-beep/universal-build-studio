package com.aistudio.universalbuilder

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

data class ApkDownloadResult(
    val success: Boolean,
    val apkFile: File? = null,
    val message: String
)

object ApkDownloader {

    private val client = OkHttpClient()

    suspend fun downloadArtifact(
        context: Context,
        username: String,
        repository: String,
        token: String,
        artifactId: Long
    ): ApkDownloadResult = withContext(Dispatchers.IO) {

        try {

            val workDir =
                File(
                    context.cacheDir,
                    "generated_apk_pending"
                )

            if (workDir.exists()) {
                workDir.deleteRecursively()
            }

            workDir.mkdirs()

            val zipFile =
                File(
                    workDir,
                    "artifact.zip"
                )

            val request =
                Request.Builder()
                    .url(
                        "https://api.github.com/repos/$username/$repository/actions/artifacts/$artifactId/zip"
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

                        return@withContext ApkDownloadResult(
                            success = false,
                            message =
                                "APK download failed: ${response.code}"
                        )
                    }

                    val body =
                        response.body
                            ?: return@withContext ApkDownloadResult(
                                success = false,
                                message =
                                    "Empty APK download"
                            )

                    FileOutputStream(zipFile)
                        .use { output ->

                            body.byteStream()
                                .use { input ->

                                    input.copyTo(output)
                                }
                        }
                }

            val apkFile =
                extractApk(
                    zipFile,
                    workDir
                )

            if (apkFile == null) {

                return@withContext ApkDownloadResult(
                    success = false,
                    message =
                        "APK file was not found inside artifact"
                )
            }

            require(context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0) != null) { "Downloaded file is not a valid APK" }
            val saved = File(context.filesDir, "latest-generated.apk")
            val pending = File(context.filesDir, "latest-generated.apk.new")
            apkFile.copyTo(pending, overwrite = true)
            require(pending.renameTo(saved)) { "Could not replace saved APK" }
            workDir.deleteRecursively()
            ApkDownloadResult(
                success = true,
                apkFile = saved,
                message =
                    "APK downloaded successfully"
            )

        } catch (e: Exception) {

            ApkDownloadResult(
                success = false,
                message =
                    e.message
                        ?: "APK download failed"
            )
        }
    }

    private fun extractApk(
        zipFile: File,
        outputDir: File
    ): File? {

        ZipInputStream(
            zipFile.inputStream()
        ).use { zip ->

            var entry =
                zip.nextEntry

            while (entry != null) {

                if (
                    !entry.isDirectory &&
                    entry.name
                        .lowercase()
                        .endsWith(".apk")
                ) {

                    val apkFile =
                        File(
                            outputDir,
                            "generated-app.apk"
                        )

                    FileOutputStream(apkFile)
                        .use { output ->

                            zip.copyTo(output)
                        }

                    zip.closeEntry()

                    return apkFile
                }

                zip.closeEntry()

                entry =
                    zip.nextEntry
            }
        }

        return null
    }
}
