package com.mazhar.mediapocket

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object MediaEngine {
    private val imageClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.MINUTES)
        .callTimeout(3, TimeUnit.MINUTES)
        .retryOnConnectionFailure(true)
        .build()

    @Synchronized
    fun initialize(context: Context) {
        YoutubeDL.init(context)
        FFmpeg.init(context)
    }

    fun request(url: String): YoutubeDLRequest = YoutubeDLRequest(LinkRules.parse(url)).apply {
        addOption("--no-playlist")
        addOption("--age-limit", "17")
        addOption("--socket-timeout", "30")
        addOption("--retries", "5")
        addOption("--fragment-retries", "5")
        addOption("--extractor-retries", "3")
        addOption("--concurrent-fragments", "3")
        addOption("--continue")
        addOption("--no-overwrites")
    }

    fun inspect(url: String, id: String): JSONObject {
        val req = request(url)
        req.addOption("--dump-single-json")
        req.addOption("--skip-download")
        val obj = JSONObject(YoutubeDL.execute(req, id, null).out)
        require(obj.optInt("age_limit", 0) < 18 && !obj.optBoolean("is_live")) { "This content is not supported" }
        require(obj.optString("_type") != "playlist") { "Paste one post or video, not a profile or playlist" }
        return obj
    }

    fun highestHeight(info: JSONObject): Int? {
        val formats = info.optJSONArray("formats") ?: return info.optInt("height").takeIf { it > 0 }
        var max = 0
        for (i in 0 until formats.length()) {
            val format = formats.optJSONObject(i) ?: continue
            if (format.optString("vcodec") != "none") max = maxOf(max, format.optInt("height"))
        }
        return max.takeIf { it > 0 }
    }

    fun download(url: String, height: Int, dir: File, id: String, progress: (Float, Long, String) -> Unit) {
        val req = request(url)
        val format = if (height <= 0) "bv*+ba/b" else "bv*[height<=${height}]+ba/b[height<=${height}]/b"
        req.addOption("-f", format)
        req.addOption("-o", File(dir, "%(id)s.%(ext)s").absolutePath)
        YoutubeDL.execute(req, id, progress)
    }

    fun fetchImage(url: String, file: File): String {
        require(url.startsWith("https://"))
        return imageClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
            require(response.isSuccessful) { "Photo download failed (${response.code})" }
            val type = response.header("Content-Type", "")!!.substringBefore(';')
            require(type in listOf("image/jpeg", "image/png", "image/webp", "image/gif")) { "Unsupported photo format" }
            var total = 0L
            response.body!!.byteStream().use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        total += n
                        require(total <= 100L * 1024 * 1024) { "Photo exceeds 100 MB" }
                        output.write(buffer, 0, n)
                    }
                }
            }
            type
        }
    }

    fun publish(context: Context, file: File, mime: String): Uri {
        val resolver = context.contentResolver
        val extension = when (mime) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> file.extension.ifBlank { "mp4" }
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "MediaPocket-${System.currentTimeMillis()}.$extension")
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, if (mime.startsWith("image/")) "Pictures/MediaPocket" else "Movies/MediaPocket")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection = if (mime.startsWith("image/")) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val uri = resolver.insert(collection, values) ?: error("Cannot save to Gallery")
        try {
            resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out, 1024 * 1024) } } ?: error("Cannot open Gallery destination")
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            return uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }
}
