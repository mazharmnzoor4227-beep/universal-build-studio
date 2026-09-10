package com.mazhar.mediapocket
import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.*
import kotlinx.coroutines.*
import com.yausername.youtubedl_android.YoutubeDL
import org.json.JSONObject
import org.json.JSONArray
import java.io.File

class DownloadWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
 override suspend fun doWork():Result=withContext(Dispatchers.IO) {
  val dir=File(applicationContext.cacheDir,id.toString()).apply { mkdirs() }
  val monitor=CoroutineScope(Dispatchers.IO).launch { while(isActive) { if(isStopped) {YoutubeDL.destroyProcessById(id.toString());PhotoEngine.cancel(id.toString());break};delay(400) } }
  try {
   setForeground(notification())
   MediaEngine.initialize(applicationContext)
   if(inputData.getBoolean("update",false)) {
    YoutubeDL.updateYoutubeDL(applicationContext,YoutubeDL.UpdateChannel.STABLE)
    return@withContext Result.success(workDataOf("message" to "Download engine updated"))
   }
   val url=LinkRules.parse(inputData.getString("url") ?: "")
   if(inputData.getBoolean("photos",false)) {
    val images=PhotoEngine.images(applicationContext,url,id.toString())
    images.forEachIndexed { index,image ->
     currentCoroutineContext().ensureActive();val file=File(dir,"photo-$index")
     val mime=MediaEngine.fetchImage(image,file);val saved=MediaEngine.publish(applicationContext,file,mime)
     record("Photo ${index+1}",saved.toString(),mime)
     setProgress(workDataOf("message" to "Saved photo ${index+1} / ${images.size}"))
    }
    Result.success(workDataOf("message" to "Saved ${images.size} photos to Gallery"))
   } else {
    setProgress(workDataOf("message" to "Finding available video…"))
    val info=MediaEngine.inspect(url,id.toString());val title=info.optString("title","Video")
    setProgress(workDataOf("message" to title.take(300)))
    MediaEngine.download(url,inputData.getInt("height",1080),dir,id.toString()) { progress,_,_ ->
     setProgressAsync(workDataOf("message" to "Downloading ${progress.toInt()}% · ${title.take(160)}","progress" to progress))
    }
    currentCoroutineContext().ensureActive()
    val files=dir.listFiles().orEmpty().filter { it.extension in listOf("mp4","webm","mkv","mov") && it.length()>0 }
    require(files.isNotEmpty()) { "No playable video was produced" }
    files.forEach { file -> val mime=when(file.extension){"webm"->"video/webm";"mkv"->"video/x-matroska";else->"video/mp4"};val saved=MediaEngine.publish(applicationContext,file,mime);record(title,saved.toString(),mime) }
    Result.success(workDataOf("message" to "Saved to Gallery · $title".take(500)))
   }
  } catch(e:CancellationException) { throw e }
  catch(e:Exception) { Result.failure(workDataOf("message" to (e.message ?: "Download failed").takeLast(1800))) }
  finally { monitor.cancel();PhotoEngine.cancel(id.toString());YoutubeDL.destroyProcessById(id.toString());dir.deleteRecursively() }
 }
 private fun record(title:String,uri:String,mime:String) {
  val prefs=applicationContext.getSharedPreferences("downloads",Context.MODE_PRIVATE)
  val list=JSONArray(prefs.getString("items","[]"));val next=JSONArray().put(JSONObject().put("title",title).put("uri",uri).put("mime",mime))
  for(i in 0 until minOf(list.length(),99))next.put(list.get(i));prefs.edit().putString("items",next.toString()).apply()
 }
 private fun notification():ForegroundInfo {
  val manager=applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  manager.createNotificationChannel(NotificationChannel("downloads","Downloads",NotificationManager.IMPORTANCE_LOW))
  val n=NotificationCompat.Builder(applicationContext,"downloads").setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle("Media Pocket").setContentText("Processing your download").setOngoing(true).build()
  return ForegroundInfo(42,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
 }
}
