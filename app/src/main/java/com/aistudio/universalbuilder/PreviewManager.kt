package com.aistudio.universalbuilder

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

data class PreviewResult(
    val success: Boolean,
    val previewUrl: String? = null,
    val message: String
)

object PreviewManager {

    suspend fun preparePreview(
        context: Context,
        projectUri: Uri,
        projectName: String
    ): PreviewResult = withContext(Dispatchers.IO) {

        try {

            val previewRoot =
                File(
                    context.cacheDir,
                    "universal_preview"
                )

            if (previewRoot.exists()) {
                previewRoot.deleteRecursively()
            }

            previewRoot.mkdirs()

            val lowerName =
                projectName.lowercase()

            when {

                lowerName.endsWith(".zip") -> {

                    val extractResult =
                        extractZipSafely(
                            context = context,
                            uri = projectUri,
                            outputDir = previewRoot
                        )

                    if (!extractResult) {

                        return@withContext PreviewResult(
                            success = false,
                            message = "ZIP extract failed"
                        )
                    }

                    val indexFile =
                        findIndexHtml(
                            previewRoot
                        )

                    if (indexFile == null) {

                        return@withContext PreviewResult(
                            success = false,
                            message =
                                "index.html not found inside project"
                        )
                    }

                    PreviewResult(
                        success = true,
                        previewUrl =
                            "file://${indexFile.absolutePath}",
                        message =
                            "Live web preview ready"
                    )
                }

                lowerName.endsWith(".html") ||
                lowerName.endsWith(".htm") -> {

                    val htmlFile =
                        File(
                            previewRoot,
                            "index.html"
                        )

                    context.contentResolver
                        .openInputStream(projectUri)
                        ?.use { input ->

                            FileOutputStream(
                                htmlFile
                            ).use { output ->

                                input.copyTo(output)
                            }
                        }
                        ?: return@withContext PreviewResult(
                            success = false,
                            message =
                                "Unable to open HTML file"
                        )

                    PreviewResult(
                        success = true,
                        previewUrl =
                            "file://${htmlFile.absolutePath}",
                        message =
                            "HTML preview ready"
                    )
                }

                else -> {

                    PreviewResult(
                        success = false,
                        message =
                            "Live preview currently supports ZIP/HTML web projects. Native Android projects need a compiled APK preview."
                    )
                }
            }

        } catch (e: Exception) {

            PreviewResult(
                success = false,
                message =
                    e.message
                        ?: "Preview preparation failed"
            )
        }
    }

    private fun extractZipSafely(
        context: Context,
        uri: Uri,
        outputDir: File
    ): Boolean {

        return try {

            val rootPath =
                outputDir.canonicalPath +
                    File.separator

            context.contentResolver
                .openInputStream(uri)
                ?.use { input ->

                    ZipInputStream(input)
                        .use { zip ->

                            var entries = 0
                            var expanded = 0L
                            var entry =
                                zip.nextEntry

                            val buffer =
                                ByteArray(
                                    64 * 1024
                                )

                            while (entry != null) {
                            require(++entries <= 20000) { "ZIP has too many entries" }

                                val target =
                                    File(
                                        outputDir,
                                        entry.name
                                    )

                                val canonical =
                                    target.canonicalPath

                                if (
                                    !canonical.startsWith(
                                        rootPath
                                    )
                                ) {

                                    throw SecurityException(
                                        "Unsafe ZIP path"
                                    )
                                }

                                if (entry.isDirectory) {

                                    target.mkdirs()

                                } else {

                                    target.parentFile
                                        ?.mkdirs()

                                    FileOutputStream(
                                        target
                                    ).use { output ->

                                        while (true) {

                                            val count =
                                                zip.read(
                                                    buffer
                                                )

                                            if (
                                                count <= 0
                                            ) {
                                                break
                                            }

                                            expanded += count
                                        require(expanded <= 1024L * 1024 * 1024) { "Expanded ZIP exceeds 1 GB" }
                                        output.write(
                                                buffer,
                                                0,
                                                count
                                            )
                                        }
                                    }
                                }

                                zip.closeEntry()

                                entry =
                                    zip.nextEntry
                            }
                        }
                }
                ?: return false

            true

        } catch (_: Exception) {

            false
        }
    }

    private fun findIndexHtml(
        root: File
    ): File? {

        if (!root.exists()) {
            return null
        }

        val direct =
            File(
                root,
                "index.html"
            )

        if (direct.exists()) {
            return direct
        }

        return root
            .walkTopDown()
            .firstOrNull { file ->

                file.isFile &&
                file.name.equals(
                    "index.html",
                    ignoreCase = true
                )
            }
    }
}
