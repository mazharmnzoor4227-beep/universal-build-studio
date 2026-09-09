package com.aistudio.universalbuilder

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry

class ProjectLibrary(private val context: Context) {
    private val root = File(context.filesDir, "projects").apply { mkdirs() }
    private fun folder(id: String): File { require(id.matches(Regex("[a-f0-9-]{36}"))); return File(root, id) }
    fun all(): List<JSONObject> = root.listFiles().orEmpty().filter { it.isDirectory }.mapNotNull { runCatching { JSONObject(File(it, "project.json").readText()) }.getOrNull() }.sortedByDescending { it.optLong("modified") }
    private fun uri(file: File): String = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file).toString()
    private fun copy(source: Uri, dest: File) {
        context.contentResolver.openInputStream(source)?.use { input -> dest.outputStream().use { output ->
            val buffer = ByteArray(8192); var total = 0L
            while (true) { val n = input.read(buffer); if (n < 0) break; total += n; require(total <= 300L * 1024 * 1024) { "File exceeds 300 MB" }; output.write(buffer, 0, n) }
        } } ?: error("Cannot read source; select it again")
    }
    fun saveDraft(): JSONObject {
        val draft = BuilderSessionStore(context).load()
        require(draft.projectUri.isNotBlank()) { "Choose a project or use your HTML code first" }
        val id = UUID.randomUUID().toString(); val dir = folder(id).apply { mkdirs() }
        try {
            copy(Uri.parse(draft.projectUri), File(dir, "source"))
            if (draft.iconUri.isNotBlank()) copy(Uri.parse(draft.iconUri), File(dir, "icon"))
            val data = JSONObject().put("id", id).put("name", draft.appName).put("package", draft.packageName).put("sourceName", draft.projectName).put("options", WebOptions(context).json()).put("modified", System.currentTimeMillis())
            File(dir, "project.json").writeText(data.toString()); return data
        } catch (e: Exception) { dir.deleteRecursively(); throw e }
    }
    fun open(data: JSONObject) {
        val dir = folder(data.getString("id")); require(File(dir, "source").isFile) { "Source missing" }
        BuilderSessionStore(context).save(data.getString("name"), data.getString("package"), data.getString("sourceName"), uri(File(dir,"source")), File(dir,"icon").takeIf { it.exists() }?.let { uri(it) })
        WebOptions(context).restore(data.optJSONObject("options") ?: JSONObject())
    }
    fun duplicate(data: JSONObject): JSONObject {
        val id = UUID.randomUUID().toString(); val dir = folder(id); folder(data.getString("id")).copyRecursively(dir)
        val copy = JSONObject(data.toString()).put("id",id).put("name",data.getString("name") + " copy").put("package", "com.studio.app" + id.replace("-", "")).put("modified", System.currentTimeMillis())
        File(dir,"project.json").writeText(copy.toString()); return copy
    }
    fun export(data: JSONObject, target: Uri) {
        context.contentResolver.openOutputStream(target)?.use { stream -> ZipOutputStream(stream).use { zip ->
            val dir = folder(data.getString("id"))
            listOf("project.json","source","icon").forEach { name -> val file = File(dir,name); if(file.exists()) { zip.putNextEntry(ZipEntry(name)); file.inputStream().use { it.copyTo(zip) }; zip.closeEntry() } }
        } } ?: error("Cannot write backup")
    }
    fun restore(source: Uri) {
        val dir = folder(UUID.randomUUID().toString()).apply { mkdirs() }
        try {
            context.contentResolver.openInputStream(source)?.use { stream -> ZipInputStream(stream).use { zip ->
                var total = 0L; val seen = mutableSetOf<String>()
                while (true) { val entry = zip.nextEntry ?: break; require(entry.name in listOf("project.json","source","icon") && seen.add(entry.name) && !entry.isDirectory) { "Invalid project backup" }
                    File(dir,entry.name).outputStream().use { out -> val buf=ByteArray(8192); while(true) { val n=zip.read(buf); if(n<0)break; total+=n; require(total<=320L*1024*1024) { "Backup too large" }; out.write(buf,0,n) } }
                }
            } } ?: error("Cannot read backup")
            require(File(dir,"project.json").length() < 128*1024 && File(dir,"source").isFile) { "Invalid project backup" }
            val data=JSONObject(File(dir,"project.json").readText()); require(data.getString("package").matches(Regex("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+")))
            data.getString("name"); data.getString("sourceName"); data.put("id",dir.name).put("modified",System.currentTimeMillis()); File(dir,"project.json").writeText(data.toString())
        } catch(e:Exception) { dir.deleteRecursively(); throw e }
    }
    fun bytes(): Long = root.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}
