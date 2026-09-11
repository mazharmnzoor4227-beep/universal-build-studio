package com.mazhar.nexora

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ParsedProject(val name: String, val type: String, val files: Map<String, String>, val previewHtml: String)

object ProjectParser {
    private val blockPattern = Regex("```(?:[a-zA-Z0-9_+#.-]+)?(?:\\s+([^\\n]+))?\\n([\\s\\S]*?)```")
    private val fileMarker = Regex("(?:<!--|//|#)\\s*FILE\\s*:\\s*([^\\n*]+)(?:-->|)?", RegexOption.IGNORE_CASE)

    fun parse(response: String, mode: WorkspaceMode, fallbackName: String): ParsedProject? {
        val blocks = blockPattern.findAll(response).toList()
        val files = linkedMapOf<String, String>()
        blocks.forEachIndexed { index, match ->
            val hint = match.groupValues.getOrNull(1).orEmpty().trim()
            val body = match.groupValues.getOrNull(2).orEmpty().trim()
            if (body.isBlank()) return@forEachIndexed
            val marker = fileMarker.find(body)?.groupValues?.getOrNull(1)?.trim()
            val path = sanitizePath(marker ?: hint.substringAfter("filename=", "").trim().ifBlank { guessPath(body, index, mode) })
            files[path] = body.replace(fileMarker, "").trim()
        }
        if (files.isEmpty()) {
            val html = extractHtml(response)
            if (mode == WorkspaceMode.WEBSITE && html.isNotBlank()) files["index.html"] = html
        }
        if (files.isEmpty()) return null
        val html = files["index.html"] ?: files.entries.firstOrNull { it.key.endsWith(".html") }?.value.orEmpty()
        return ParsedProject(
            name = fallbackName.ifBlank { "Nexora ${mode.label} project" },
            type = mode.name,
            files = files,
            previewHtml = if (html.contains("<html", true) || html.contains("<body", true)) html else ""
        )
    }

    private fun extractHtml(text: String): String {
        val start = text.indexOf("<html", ignoreCase = true)
        val end = text.lastIndexOf("</html>", ignoreCase = true)
        return if (start >= 0 && end > start) text.substring(start, end + 7).trim() else ""
    }

    private fun guessPath(body: String, index: Int, mode: WorkspaceMode): String {
        return when {
            body.contains("<html", true) || body.contains("<body", true) -> if (index == 0) "index.html" else "pages/page-$index.html"
            body.contains("package ") || body.contains("fun main") -> "src/main/java/Generated$index.kt"
            body.contains("android {") || body.contains("plugins {") -> "build.gradle.kts"
            body.contains("<manifest") -> "src/main/AndroidManifest.xml"
            body.contains("function ") || body.contains("const ") || body.contains("=>") -> "js/app-$index.js"
            body.contains("{") && (body.contains("color:") || body.contains("display:")) -> "css/styles-$index.css"
            mode == WorkspaceMode.DOCUMENT -> "README.md"
            else -> "generated-$index.txt"
        }
    }

    private fun sanitizePath(path: String): String {
        val clean = path.replace('\\', '/').replace("..", "").trim().trim('/').ifBlank { "generated.txt" }
        return clean.take(160)
    }
}

object ProjectExporter {
    fun share(context: Context, project: ProjectEntity, files: List<ProjectFileEntity>) {
        val root = File(context.cacheDir, "shared").apply { mkdirs() }
        val zip = File(root, "${project.name.replace(Regex("[^A-Za-z0-9._-]"), "_")}-${UUID.randomUUID().toString().take(8)}.zip")
        ZipOutputStream(FileOutputStream(zip)).use { output ->
            files.forEach { file ->
                output.putNextEntry(ZipEntry(file.path))
                output.write(file.content.toByteArray(StandardCharsets.UTF_8))
                output.closeEntry()
            }
        }
        val uri = FileProvider.getUriForFile(context, context.getString(com.mazhar.nexora.R.string.file_provider_authority), zip)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export ${project.name}"))
    }
}
