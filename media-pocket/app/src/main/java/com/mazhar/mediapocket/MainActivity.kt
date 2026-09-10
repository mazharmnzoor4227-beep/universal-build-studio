package com.mazhar.mediapocket
import android.os.Bundle
import android.content.Intent
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.work.*
import org.json.JSONArray

class MainActivity:ComponentActivity() {
 private var incoming by mutableStateOf("")
 override fun onCreate(state:Bundle?) {super.onCreate(state);readShared(intent);setContent { Screen() }}
 override fun onNewIntent(intent:Intent) {super.onNewIntent(intent);readShared(intent)}
 private fun readShared(intent:Intent?) { if(intent?.action==Intent.ACTION_SEND)incoming=intent.getStringExtra(Intent.EXTRA_TEXT) ?: "" }
 @Composable fun Screen() {
  var link by rememberSaveable { mutableStateOf(incoming) };LaunchedEffect(incoming){if(incoming.isNotBlank())link=incoming}
  var photo by rememberSaveable {mutableStateOf(false)};var quality by rememberSaveable {mutableStateOf(1080)}
  var error by remember {mutableStateOf("")};var tab by rememberSaveable {mutableStateOf(0)}
  val manager=remember {WorkManager.getInstance(this)}
  val works by manager.getWorkInfosForUniqueWorkLiveData("download").observeAsState(emptyList())
  val work=works.maxByOrNull {it.generation};val active=works.any {!it.state.isFinished}
  var history by remember {mutableStateOf(JSONArray())}
  LaunchedEffect(works,tab) {history=JSONArray(getSharedPreferences("downloads",MODE_PRIVATE).getString("items","[]"))}
  val notifications=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
  fun start(update:Boolean=false) {
   try {
    val url=if(update)"" else LinkRules.parse(link)
    if(android.os.Build.VERSION.SDK_INT>=33)notifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    val request=OneTimeWorkRequestBuilder<DownloadWorker>().setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).setInputData(workDataOf("url" to url,"photos" to photo,"height" to quality,"update" to update)).build()
    manager.enqueueUniqueWork("download",ExistingWorkPolicy.KEEP,request);error=""
   } catch(e:Exception) {error=e.message ?: "Invalid link"}
  }
  MaterialTheme(colorScheme=darkColorScheme(primary=Color.White,onPrimary=Color.Black,background=Color(0xFF101014),surface=Color(0xFF1C1C22))) {
   Scaffold(bottomBar={NavigationBar { NavigationBarItem(selected=tab==0,onClick={tab=0},icon={Text("↓")},label={Text("Download")});NavigationBarItem(selected=tab==1,onClick={tab=1},icon={Text("▤")},label={Text("Saved")}) }}) { padding ->
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(22.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
     Text("Media Pocket",style=MaterialTheme.typography.headlineLarge)
     Text("Your links. Your media.",color=MaterialTheme.colorScheme.onSurfaceVariant)
     if(tab==0) {
      Card {Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
       Text("Paste a post link",style=MaterialTheme.typography.titleLarge)
       OutlinedTextField(value=link,onValueChange={link=it},label={Text("https://…")},modifier=Modifier.fillMaxWidth(),enabled=!active)
       TextButton(onClick={val clipboard=getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager;link=clipboard.primaryClip?.getItemAt(0)?.coerceToText(this@MainActivity)?.toString() ?: ""},enabled=!active){Text("Paste from clipboard")}
       Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(selected=!photo,onClick={photo=false},label={Text("Video")},enabled=!active);FilterChip(selected=photo,onClick={photo=true},label={Text("Photos")},enabled=!active)}
       if(!photo)Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf(480,720,1080).forEach {height->FilterChip(selected=quality==height,onClick={quality=height},label={Text("${height}p")},enabled=!active)}}
       Text(if(photo)"Finds original images exposed by public pages. Some albums and photo posts are not supported." else "Quality is a preference; availability depends on the source. Original streams are used when available. Existing creator marks are kept.",style=MaterialTheme.typography.bodySmall)
       Button(onClick={start()},enabled=!active && link.isNotBlank(),modifier=Modifier.fillMaxWidth().height(54.dp)){Text(if(active)"Working…" else "Download to Gallery")}
      }}
      if(active){LinearProgressIndicator(Modifier.fillMaxWidth());TextButton(onClick={manager.cancelUniqueWork("download")}){Text("Cancel")}}
      val status=work?.let {if(it.state.isFinished)it.outputData.getString("message") else it.progress.getString("message") ?: "Queued · waiting for connection"}
      if(status!=null)Text(status,style=MaterialTheme.typography.bodyMedium)
      if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
      Text("Instagram · Facebook · TikTok · Pinterest · YouTube",style=MaterialTheme.typography.labelMedium)
      Text("Public downloadable posts only. No login, private-post access or age-restriction bypass. Platform changes can break links. This app adds no watermark; it cannot guarantee a watermark-free source.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
      OutlinedButton(onClick={start(true)},enabled=!active){Text("Update download engine")}
      TextButton(onClick={startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/mazharmnzoor4227-beep/universal-build-studio/tree/main/media-pocket")))}){Text("About · source & licenses")}
     } else {
      Text("Saved to your Gallery",style=MaterialTheme.typography.titleLarge)
      if(history.length()==0)Text("Your completed downloads will appear here.")
      for(i in 0 until history.length()) {val item=history.getJSONObject(i)
       Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)) {Text(item.optString("title"));Row {
        TextButton(onClick={runCatching {startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(item.getString("uri")),item.getString("mime")).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))}.onFailure {error="File missing or no viewer installed";tab=0}}){Text("Open")}
        TextButton(onClick={startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType(item.getString("mime")).putExtra(Intent.EXTRA_STREAM,Uri.parse(item.getString("uri"))).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),"Share media"))}){Text("Share")}
       }}}
      }
     }
    }
   }
  }
 }
}
