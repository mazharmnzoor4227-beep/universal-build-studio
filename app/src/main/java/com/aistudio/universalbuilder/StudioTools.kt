package com.aistudio.universalbuilder

import android.content.Context
import android.net.Uri
import java.util.zip.ZipFile
import org.json.JSONObject
import okhttp3.OkHttpClient
import okhttp3.Request

object StudioTools {
    fun validate(context: Context, s:BuilderSession=BuilderSessionStore(context).load()): String {
        require(s.appName.isNotBlank()) { "Enter an app name" }
        require(s.packageName.matches(Regex("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+"))) { "Package ID: use com.example.myapp" }
        require(s.projectUri.isNotBlank()) { "Choose a source project" }
        val normalized=ProjectImport.normalize(context,Uri.parse(s.projectUri),s.projectName)
        val file=java.io.File(context.cacheDir,"preflight.zip")
        try {
            context.contentResolver.openInputStream(normalized)!!.use { input -> file.outputStream().use { input.copyTo(it) } }
            return ZipFile(file).use { zip ->
                val entries=zip.entries().asSequence().toList(); require(entries.size<=20000) { "Too many files" }
                var expanded=0L
                entries.forEach { e ->
                    require(!e.name.startsWith("/") && !e.name.contains('\\') && e.name.split('/').none { it==".." } && e.name.none { it=='\n' || it=='\r' }) { "Unsafe ZIP path" }
                    require(e.size>=0) { "Unknown ZIP entry size" }; expanded+=e.size; require(expanded<=1024L*1024*1024) { "Expanded ZIP exceeds 1 GB" }
                }
                val names=entries.map { it.name }
                val route=when {
                    names.any { it.endsWith("pubspec.yaml") } -> "Flutter"
                    names.any { it.endsWith("build.gradle") || it.endsWith("build.gradle.kts") } -> "Android Gradle"
                    names.any { it.endsWith("package.json") } -> "Node / static web"
                    names.any { it=="index.html" || it.endsWith("/index.html") } -> "HTML"
                    else -> error("No index.html, package.json, Gradle build or pubspec.yaml found")
                }
                "$route project • ${entries.size} files. Basic checks passed; compilation checks dependencies and code."
            }
        } finally { file.delete() }
    }
    fun html(context:Context):String {
        val draft=BuilderSessionStore(context).load();require(draft.projectUri.isNotBlank()) { "Choose a project first" }
        val uri=Uri.parse(draft.projectUri)
        if(draft.projectName.endsWith(".html",true) || draft.projectName.endsWith(".htm",true)) return context.contentResolver.openInputStream(uri)!!.use { String(it.readBytesLimited(2*1024*1024),Charsets.UTF_8) }
        val file=java.io.File(context.cacheDir,"edit-source.zip")
        try {
            context.contentResolver.openInputStream(uri)!!.use { input -> file.outputStream().use { input.copyTo(it) } }
            return ZipFile(file).use { zip -> val entry=zip.entries().asSequence().firstOrNull { it.name=="index.html" || it.name.endsWith("/index.html") } ?: error("No HTML entry found")
                zip.getInputStream(entry).use { String(it.readBytesLimited(2*1024*1024),Charsets.UTF_8) }
            }
        } finally { file.delete() }
    }
    private fun java.io.InputStream.readBytesLimited(limit:Int):ByteArray {
        val out=java.io.ByteArrayOutputStream();val b=ByteArray(8192)
        while(true) { val n=read(b);if(n<0)break;require(out.size()+n<=limit) { "Editor limit: 2 MB" };out.write(b,0,n) };return out.toByteArray()
    }
    fun logs(context:Context,item:JSONObject):String {
        val config=GitHubConfigStore(context).load()
        require(config.username==item.optString("owner") && config.repository==item.optString("repo")) { "Use the original repository settings" }
        val id=item.optLong("runId");require(id>0) { "No workflow run recorded yet" }
        val request=Request.Builder().url("https://api.github.com/repos/${config.username}/${config.repository}/actions/runs/$id/logs").header("Authorization","Bearer ${config.token}").build()
        return OkHttpClient().newCall(request).execute().use { response ->
            require(response.isSuccessful) { "Logs not ready or expired (${response.code})" }
            java.util.zip.ZipInputStream(response.body!!.byteStream()).use { zip ->
                val result=StringBuilder();var count=0
                while(true) { val e=zip.nextEntry ?: break; if(++count>100) break
                    if(!e.isDirectory) { val text=String(zip.readBytesLimited(2*1024*1024),Charsets.UTF_8)
                        val lines=text.lineSequence().filter { it.contains("error",true) || it.contains("failed",true) || it.contains("What went wrong") || it.contains("e: ") }.take(80).joinToString("\n")
                        if(lines.isNotBlank()) result.append(e.name).append("\n").append(lines).append("\n")
                    }
                    if(result.length>24000)break
                }
                result.toString().take(24000).ifBlank { "No matching error lines. Open the full workflow log on GitHub." }.replace(config.token,"[redacted]")
            }
        }
    }
    fun update(): JSONObject {
        val request=Request.Builder().url("https://api.github.com/repos/mazharmnzoor4227-beep/universal-build-studio/releases/latest").header("Accept","application/vnd.github+json").build()
        return OkHttpClient().newCall(request).execute().use { response ->
            require(response.isSuccessful) { "No published update found yet" }; val json=JSONObject(response.body!!.string())
            require(json.getString("html_url").startsWith("https://github.com/mazharmnzoor4227-beep/universal-build-studio/releases/")); json
        }
    }
    fun cacheBytes(context:Context)=context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    fun clean(context:Context):Long {
        val before=cacheBytes(context)
        // Preserve pasted source, selected icons and saved projects.
        listOf("generated_apk_pending","normalized","preflight.zip").forEach { java.io.File(context.cacheDir,it).deleteRecursively() }
        return before-cacheBytes(context)
    }
}
