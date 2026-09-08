package com.aistudio.universalbuilder

import android.content.Context
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object CodeProjectCreator {

    data class Result(
        val success: Boolean,
        val uri: android.net.Uri? = null,
        val previewUrl: String? = null,
        val message: String
    )

    fun create(
        context: Context,
        html: String
    ): Result {

        return try {

            if (html.isBlank()) {
                return Result(
                    false,
                    message = "Paste HTML code first"
                )
            }

            val root =
                File(
                    context.cacheDir,
                    "pasted-code-project"
                )

            if (root.exists()) {
                root.deleteRecursively()
            }

            root.mkdirs()

            val index =
                File(
                    root,
                    "index.html"
                )

            index.writeText(
                html,
                Charsets.UTF_8
            )

            val zipFile =
                File(
                    context.cacheDir,
                    "pasted-code-project.zip"
                )

            if (zipFile.exists()) {
                zipFile.delete()
            }

            ZipOutputStream(
                FileOutputStream(zipFile)
            ).use { zip ->

                zip.putNextEntry(
                    ZipEntry("index.html")
                )

                index.inputStream().use {
                    it.copyTo(zip)
                }

                zip.closeEntry()
            }

            val uri =
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    zipFile
                )

            Result(
                success = true,
                uri = uri,
                previewUrl =
                    index.toURI().toString(),
                message =
                    "Code project ready"
            )

        } catch (e: Exception) {

            Result(
                false,
                message =
                    e.message
                        ?: "Could not create project"
            )
        }
    }
}
