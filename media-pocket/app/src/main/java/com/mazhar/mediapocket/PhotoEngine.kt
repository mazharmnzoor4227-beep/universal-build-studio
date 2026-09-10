package com.mazhar.mediapocket
import android.content.Context
import java.io.File
import java.util.zip.ZipInputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap
object PhotoEngine {
 private val running=ConcurrentHashMap<String,Process>()
 fun cancel(id:String){running.remove(id)?.destroy()}
 @Synchronized private fun unpack(context:Context):File {
  val root=File(context.noBackupFilesDir,"gallery-1.32.11")
  if(File(root,"ready").exists())return root
  root.deleteRecursively();root.mkdirs()
  context.assets.open("gallery.zip").use { stream->ZipInputStream(stream).use {zip->while(true){val entry=zip.nextEntry ?: break;val dest=File(root,entry.name);require(dest.canonicalPath.startsWith(root.canonicalPath+File.separator));if(entry.isDirectory)dest.mkdirs() else {dest.parentFile!!.mkdirs();dest.outputStream().use {zip.copyTo(it)}}}}}
  File(root,"ready").writeText("ok");return root
 }
 fun images(context:Context,url:String,id:String):List<String> {
  val root=unpack(context);val base=File(context.noBackupFilesDir,"youtubedl-android/packages");val python=File(base,"python/usr")
  val log=File(context.cacheDir,"photos-$id.log")
  val pb=ProcessBuilder(File(context.applicationInfo.nativeLibraryDir,"libpython.so").absolutePath,"-m","gallery_dl","--ignore-config","--get-urls","--range","1-30","--retries","1","--http-timeout","20",LinkRules.parse(url))
  pb.environment().apply {put("PYTHONHOME",python.absolutePath);put("PYTHONPATH",root.absolutePath);put("LD_LIBRARY_PATH",File(python,"lib").absolutePath);put("SSL_CERT_FILE",File(python,"etc/tls/cert.pem").absolutePath);put("TMPDIR",context.cacheDir.absolutePath)}
  pb.redirectErrorStream(true);pb.redirectOutput(log)
  val process=pb.start();running[id]=process
  try {
   require(process.waitFor(120,TimeUnit.SECONDS)) {"Photo lookup timed out"}
   require(log.length()<1024*1024) {"Photo response too large"}
   val lines=log.readLines();val urls=lines.filter {it.startsWith("https://")}.take(30)
   require(process.exitValue()==0 && urls.isNotEmpty()) {"No downloadable public photos found. The post may require login or may not be supported."};return urls
  } finally {process.destroy();running.remove(id);log.delete()}
 }
}
