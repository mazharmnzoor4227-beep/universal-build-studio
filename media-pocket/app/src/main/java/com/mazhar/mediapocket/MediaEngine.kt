package com.mazhar.mediapocket
import android.content.Context
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.ffmpeg.FFmpeg
import org.json.JSONObject
import org.jsoup.Jsoup
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

object MediaEngine {
 @Synchronized fun initialize(context:Context) { YoutubeDL.init(context);FFmpeg.init(context) }
 fun request(url:String):YoutubeDLRequest=YoutubeDLRequest(LinkRules.parse(url)).apply {
  addOption("--no-playlist");addOption("--age-limit","17");addOption("--socket-timeout","25");addOption("--retries","2");addOption("--max-filesize","1G")
 }
 fun inspect(url:String,id:String):JSONObject {
  val req=request(url);req.addOption("--dump-single-json");req.addOption("--skip-download")
  val obj=JSONObject(YoutubeDL.execute(req,id,null).out)
  require(obj.optInt("age_limit",0)<18 && !obj.optBoolean("is_live")) { "This content is not supported" }
  require(obj.optString("_type")!="playlist") { "Paste one post or video, not a profile or playlist" }
  return obj
 }
 fun download(url:String,height:Int,dir:File,id:String,progress:(Float,Long,String)->Unit) {
  val req=request(url)
  req.addOption("-f","bv*[height<=${height}]+ba/b[height<=${height}]/b")
  req.addOption("--merge-output-format","mp4")
  req.addOption("-o",File(dir,"%(id)s.%(ext)s").absolutePath)
  // TikTok's extractor selects its available original streams; do not crop creator marks.
  YoutubeDL.execute(req,id,progress)
 }
 fun images(url:String):List<String> {
  val doc=Jsoup.connect(LinkRules.parse(url)).timeout(25000).maxBodySize(3*1024*1024).get()
  val results=linkedSetOf<String>()
  fun collect(value:Any?) {
   when(value) {
    is JSONObject -> {
     if(value.optString("@type")=="ImageObject") {
      val u=value.optString("contentUrl").ifBlank { value.optString("url") };if(u.startsWith("https://"))results.add(u)
     }
     value.keys().forEach { collect(value.opt(it)) }
    }
    is org.json.JSONArray -> for(i in 0 until value.length())collect(value.opt(i))
   }
  }
  doc.select("script[type=application/ld+json]").forEach { runCatching { collect(org.json.JSONTokener(it.data()).nextValue()) } }
  require(results.isNotEmpty()) { "No original photo found in this public page. Private posts, slideshows and some photo posts need platform-specific support. A video thumbnail is not an original photo." }
  return results.take(30).toList()
 }
 fun fetchImage(url:String,file:File):String {
  require(url.startsWith("https://"))
  val client=OkHttpClient.Builder().callTimeout(2,TimeUnit.MINUTES).build()
  return client.newCall(Request.Builder().url(url).build()).execute().use { response ->
   require(response.isSuccessful) { "Photo download failed (${response.code})" }
   val type=response.header("Content-Type","")!!.substringBefore(';')
   require(type in listOf("image/jpeg","image/png","image/webp","image/gif")) { "Unsupported photo format" }
   var total=0L; response.body!!.byteStream().use { input ->file.outputStream().use { output ->val b=ByteArray(8192);while(true){val n=input.read(b);if(n<0)break;total+=n;require(total<=50L*1024*1024){"Photo exceeds 50 MB"};output.write(b,0,n)} } };type
  }
 }
 fun publish(context:Context,file:File,mime:String):Uri {
  val resolver=context.contentResolver
  val values=ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME,"MediaPocket-${System.currentTimeMillis()}.${when(mime){"image/jpeg"->"jpg";"image/png"->"png";"image/webp"->"webp";"image/gif"->"gif";else->file.extension}}")
   put(MediaStore.MediaColumns.MIME_TYPE,mime);put(MediaStore.MediaColumns.RELATIVE_PATH,if(mime.startsWith("image/"))"Pictures/MediaPocket" else "Movies/MediaPocket");put(MediaStore.MediaColumns.IS_PENDING,1) }
  val collection=if(mime.startsWith("image/"))MediaStore.Images.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
  val uri=resolver.insert(collection,values) ?: error("Cannot save to Gallery")
  try { resolver.openOutputStream(uri)!!.use { out->file.inputStream().use { it.copyTo(out) } };resolver.update(uri,ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING,0) },null,null);return uri }
  catch(e:Exception) {resolver.delete(uri,null,null);throw e}
 }
}
