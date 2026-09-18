package com.mazhar.mediapocket

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): ListenableWorker.Result = withContext(Dispatchers.IO) {
        val dir = File(applicationContext.cacheDir, id.toString()).apply { mkdirs() }
        val monitor = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                if (isStopped) {
                    YoutubeDL.destroyProcessById(id.toString())
                    PhotoEngine.cancel(id.toString())
                    break
                }
                delay(350)
            }
        }
        try {
            setForeground(notification("Preparing download…"))
            MediaEngine.initialize(applicationContext)
            if (inputData.getBoolean("update", false)) {
                setProgress(workDataOf("message" to "Updating download engine…"))
                YoutubeDL.updateYoutubeDL(applicationContext, YoutubeDL.UpdateChannel.STABLE)
                return@withContext ListenableWorker.Result.success(workDataOf("message" to "Download engine updated"))
            }
            val url = LinkRules.parse(inputData.getString("url") ?: "")
            if (inputData.getBoolean("photos", false)) {
                val images = PhotoEngine.images(applicationContext, url, id.toString())
                images.forEachIndexed { index, image ->
                    currentCoroutineContext().ensureActive()
                    val file = File(dir, "photo-$index")
                    val mime = MediaEngine.fetchImage(image, file)
                    val saved = MediaEngine.publish(applicationContext, file, mime)
                    record("Photo ${index + 1}", saved.toString(), mime, "Photo")
                    setProgress(workDataOf("message" to "Saved photo ${index + 1} / ${images.size}"))
                }
                ListenableWorker.Result.success(workDataOf("message" to "Saved ${images.size} photos to Gallery"))
            } else {
                setProgress(workDataOf("message" to "Checking source quality…"))
                val info = MediaEngine.inspect(url, id.toString())
                val title = info.optString("title", "Video").ifBlank { "Video" }
                val requested = inputData.getInt("height", -1)
                val maxHeight = MediaEngine.highestHeight(info)
                val qualityText = when {
                    requested <= 0 && maxHeight != null -> "Best / source up to ${maxHeight}p"
                    requested <= 0 -> "Best / Original"
                    maxHeight != null && maxHeight < requested -> "Source max ${maxHeight}p · no upscaling"
                    else -> "Up to ${requested}p"
                }
                setProgress(workDataOf("message" to "$qualityText · ${title.take(120)}"))
                MediaEngine.download(url, requested, dir, id.toString()) { progress, _, _ ->
                    setProgressAsync(workDataOf("message" to "Downloading ${progress.toInt().coerceIn(0, 100)}% · ${title.take(120)}", "progress" to progress.coerceIn(0f, 100f)))
                }
                currentCoroutineContext().ensureActive()
                val files = dir.listFiles().orEmpty().filter { it.extension.lowercase() in listOf("mp4", "webm", "mkv", "mov", "m4v") && it.length() > 0 }
                require(files.isNotEmpty()) { "No playable video was produced" }
                files.forEach { file ->
                    val mime = when (file.extension.lowercase()) {
                        "webm" -> "video/webm"
                        "mkv" -> "video/x-matroska"
                        "mov" -> "video/quicktime"
                        else -> "video/mp4"
                    }
                    val saved = MediaEngine.publish(applicationContext, file, mime)
                    record(title, saved.toString(), mime, qualityText)
                }
                ListenableWorker.Result.success(workDataOf("message" to "Saved to Gallery · ${title.take(400)}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ListenableWorker.Result.failure(workDataOf("message" to friendlyError(e).takeLast(1800)))
        } finally {
            monitor.cancel()
            PhotoEngine.cancel(id.toString())
            YoutubeDL.destroyProcessById(id.toString())
            dir.deleteRecursively()
        }
    }

    private fun friendlyError(e: Exception): String {
        val raw = e.message?.trim().orEmpty()
        return when {
            raw.contains("timed out", true) || raw.contains("timeout", true) -> "Connection timed out. Check Wi-Fi/mobile data and try again."
            raw.contains("HTTP Error 403", true) -> "The platform refused this link (403). Update the download engine and try again."
            raw.contains("HTTP Error 429", true) -> "The platform is rate-limiting downloads. Wait a little and try again."
            raw.contains("Sign in", true) || raw.contains("login", true) -> "This post currently requires login and cannot be downloaded anonymously."
            raw.isNotBlank() -> raw
            else -> "Download failed. Check the link and connection, then try again."
        }
    }

    private fun record(title: String, uri: String, mime: String, quality: String) {
        val prefs = applicationContext.getSharedPreferences("downloads", Context.MODE_PRIVATE)
        val list = JSONArray(prefs.getString("items", "[]"))
        val next = JSONArray().put(JSONObject().put("title", title).put("uri", uri).put("mime", mime).put("quality", quality).put("savedAt", System.currentTimeMillis()))
        for (i in 0 until minOf(list.length(), 99)) next.put(list.get(i))
        prefs.edit().putString("items", next.toString()).apply()
    }

    private fun notification(text: String): ForegroundInfo {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel("downloads", "Downloads", NotificationManager.IMPORTANCE_LOW))
        val notification = NotificationCompat.Builder(applicationContext, "downloads")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Media Pocket")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        return ForegroundInfo(42, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }
}
