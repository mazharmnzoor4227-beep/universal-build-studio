package com.aistudio.universalbuilder

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

data class BuildRequest(val appName: String, val packageName: String, val projectUri: Uri,
    val projectName: String, val iconUri: Uri? = null)
data class BuildResult(val success: Boolean, val message: String, val apkFile: File? = null, val pending: Boolean = false)

class BuildController(private val context: Context) {
    suspend fun startBuild(request: BuildRequest, requestId: String): BuildResult {
        val records = BuildRecords(context)
        val config = GitHubConfigStore(context).load()
        require(config.username.matches(Regex("[A-Za-z0-9-]+")) && config.repository.matches(Regex("[A-Za-z0-9_.-]+")) && config.token.isNotBlank()) { "Configure GitHub first" }
        require(request.packageName.matches(Regex("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+"))) { "Use a package ID such as com.example.myapp" }
        val record = records.get(requestId) ?: JSONObject().put("id", requestId).put("name", request.appName)
            .put("package", request.packageName).put("created", System.currentTimeMillis())
            .put("options", WebOptions(context).json()).put("owner", config.username).put("repo", config.repository).put("status", "Preparing")
        require(record.optString("owner") == config.username && record.optString("repo") == config.repository) { "Restore the original GitHub repository settings to resume this build" }
        fun save(status: String) { record.put("status", status); records.save(requestId, record) }
        try {
            if (!record.optBoolean("uploaded")) {
                save("Uploading project")
                val tag = "builder-$requestId"
                val normalized = ProjectImport.normalize(context, request.projectUri, request.projectName)
                val upload = ProjectTransport(tag).uploadProject(context, normalized, config.username, config.repository, config.token)
                require(upload.success) { upload.message }
                if (request.iconUri != null) {
                    val icon = IconTransport(tag).uploadIcon(context, request.iconUri, config.username, config.repository, config.token)
                    require(icon.success) { icon.message }
                }
                val meta = BuildMetaTransport(tag).uploadMeta(request.appName, request.packageName, config.username, config.repository, config.token, record.optJSONObject("options") ?: JSONObject())
                require(meta.success) { meta.message }
                record.put("uploaded", true); save("Upload complete")
            }
            // Save intent before dispatch. Ambiguous network failures are reconciled by request ID, never blindly dispatched twice.
            if (!record.optBoolean("dispatchAttempted")) {
                record.put("dispatchAttempted", true).put("dispatchedAt", System.currentTimeMillis()); save("Starting GitHub build")
                GitHubApiClient.triggerWorkflow(config.username, config.repository, config.token, requestId = requestId).getOrThrow()
            }
            val run = BuildMonitor.check(config, requestId, record.optLong("runId"))
            if (run.has("id")) record.put("runId", run.getLong("id")).put("url", run.optString("html_url"))
            if (run.optString("status") != "completed") {
                if (run.optString("status") == "waiting_for_run" && System.currentTimeMillis() - record.optLong("dispatchedAt") > 10 * 60 * 1000L) {
                    save("No matching run found; inspect Actions before starting another build")
                    return BuildResult(false, record.getString("status"))
                }
                save(run.optString("status", "Queued"))
                return BuildResult(false, record.getString("status"), pending = true)
            }
            if (run.optString("conclusion") != "success") {
                save("Build ${run.optString("conclusion")}; open build log in History")
                return BuildResult(false, record.getString("status"))
            }
            val artifact = run.optLong("artifact_id")
            if (artifact == 0L) { save("APK artifact missing or expired"); return BuildResult(false, record.getString("status")) }
            save("Downloading APK")
            val result = ApkDownloader.downloadArtifact(context, config.username, config.repository, config.token, artifact)
            if (!result.success) throw IOException(result.message)
            record.put("apk", result.apkFile?.absolutePath); save("APK READY")
            return BuildResult(true, "APK READY", result.apkFile)
        } catch (e: CancellationException) { throw e
        } catch (e: IOException) { save("Waiting for connection / retry"); return BuildResult(false, record.getString("status"), pending = true)
        } catch (e: Exception) { save(e.message ?: "Build failed"); return BuildResult(false, record.getString("status")) }
    }
}
