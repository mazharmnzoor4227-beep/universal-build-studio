package com.aistudio.universalbuilder

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ProjectImport {
    fun normalize(context: Context, uri: Uri, name: String): Uri {
        val dir = File(context.cacheDir, "normalized").apply { mkdirs() }
        val destination = File(dir, "project.zip")
        val html = name.endsWith(".html", true) || name.endsWith(".htm", true)
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Cannot read selected project" }
            var total = 0L
            fun copy(output: java.io.OutputStream) {
                val buffer = ByteArray(65536)
                while (true) {
                    val n = input.read(buffer); if (n < 0) break
                    total += n; require(total <= 300L * 1024 * 1024) { "Project exceeds 300 MB" }
                    output.write(buffer, 0, n)
                }
                require(total > 0) { "Selected file is empty" }
            }
            if (html) ZipOutputStream(destination.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("index.html")); copy(zip); zip.closeEntry()
            } else destination.outputStream().use { copy(it) }
        }
        java.util.zip.ZipFile(destination).use { require(it.size() > 0) { "ZIP is empty" } }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destination)
    }
}
