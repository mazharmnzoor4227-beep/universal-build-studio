package com.aistudio.universalbuilder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

object BuildMonitor {
    private val client = OkHttpClient()
    private fun get(config: GitHubConfig, path: String): JSONObject {
        val request = Request.Builder().url("https://api.github.com/repos/${config.username}/${config.repository}/$path")
            .header("Authorization", "Bearer ${config.token}").header("Accept", "application/vnd.github+json").build()
        return client.newCall(request).execute().use {
            if (!it.isSuccessful) throw IOException("GitHub status ${it.code}; check connection and repository permissions")
            JSONObject(it.body?.string() ?: throw IOException("Empty GitHub response"))
        }
    }
    suspend fun check(config: GitHubConfig, requestId: String, runId: Long): JSONObject = withContext(Dispatchers.IO) {
        val run = if (runId > 0) get(config, "actions/runs/$runId") else {
            var found: JSONObject? = null
            for (page in 1..5) {
                val runs = get(config, "actions/workflows/build-generated-app.yml/runs?event=workflow_dispatch&per_page=100&page=$page").getJSONArray("workflow_runs")
                for (i in 0 until runs.length()) {
                    val candidate = runs.getJSONObject(i)
                    if (candidate.optString("display_title") == "Build $requestId") { found = candidate; break }
                }
                if (found != null || runs.length() < 100) break
            }
            found ?: return@withContext JSONObject().put("status", "waiting_for_run")
        }
        if (run.optString("conclusion") == "success") {
            val items = get(config, "actions/runs/${run.getLong("id")}/artifacts").getJSONArray("artifacts")
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                if (item.optString("name") == "generated-apk" && !item.optBoolean("expired")) {
                    run.put("artifact_id", item.getLong("id")); break
                }
            }
        }
        run
    }
}
