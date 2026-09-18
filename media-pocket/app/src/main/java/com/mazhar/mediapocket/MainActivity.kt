package com.mazhar.mediapocket

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.*
import org.json.JSONArray

private val Bg = Color(0xFF080B12)
private val Panel = Color(0xFF111722)
private val Soft = Color(0xFF151C29)
private val Line = Color(0xFF293448)
private val Muted = Color(0xFFAAB6CF)
private val Blue = Color(0xFF2E9CFF)
private val Purple = Color(0xFF754DFF)
private val Glow = Brush.horizontalGradient(listOf(Blue, Purple))
private enum class Page { DOWNLOAD, SAVED, SETTINGS }

class MainActivity : ComponentActivity() {
    private var incoming by mutableStateOf("")

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        enableEdgeToEdge()
        readShare(intent)
        setContent { App() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readShare(intent)
    }

    private fun readShare(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) incoming = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
    }

    @Composable private fun App() {
        val ctx = LocalContext.current
        val settings = remember { getSharedPreferences("settings", MODE_PRIVATE) }
        val downloads = remember { getSharedPreferences("downloads", MODE_PRIVATE) }
        val wm = remember { WorkManager.getInstance(ctx) }
        val works by wm.getWorkInfosForUniqueWorkLiveData("download").observeAsState(emptyList())
        var page by rememberSaveable { mutableStateOf(Page.DOWNLOAD) }
        var link by rememberSaveable { mutableStateOf(incoming) }
        var photos by rememberSaveable { mutableStateOf(false) }
        var quality by rememberSaveable { mutableIntStateOf(settings.getInt("defaultQuality", -1)) }
        var mobile by rememberSaveable { mutableStateOf(settings.getBoolean("allowMobileData", true)) }
        var autoPaste by rememberSaveable { mutableStateOf(settings.getBoolean("autoPaste", false)) }
        var notifications by rememberSaveable { mutableStateOf(settings.getBoolean("notifications", true)) }
        var error by rememberSaveable { mutableStateOf("") }
        var historyTick by remember { mutableIntStateOf(0) }

        val current = works.firstOrNull { !it.state.isFinished } ?: works.firstOrNull()
        val active = works.any { !it.state.isFinished }
        val progress = current?.progress?.getFloat("progress", -1f) ?: -1f
        val status = current?.let {
            if (it.state.isFinished) it.outputData.getString("message") else it.progress.getString("message") ?: "Queued · waiting for connection"
        }
        val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

        fun persist() = settings.edit().putInt("defaultQuality", quality)
            .putBoolean("allowMobileData", mobile).putBoolean("autoPaste", autoPaste)
            .putBoolean("notifications", notifications).apply()

        fun start(update: Boolean = false) {
            try {
                val url = if (update) "" else LinkRules.parse(link)
                if (notifications && Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                persist()
                val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(if (mobile) NetworkType.CONNECTED else NetworkType.UNMETERED).build())
                    .setInputData(workDataOf("url" to url, "photos" to photos, "height" to quality, "update" to update))
                    .build()
                downloads.edit().putString("currentWork", request.id.toString()).apply()
                wm.enqueueUniqueWork("download", ExistingWorkPolicy.KEEP, request)
                error = ""
            } catch (e: Exception) { error = e.message ?: "Invalid link" }
        }

        LaunchedEffect(incoming) { if (incoming.isNotBlank()) { link = incoming; page = Page.DOWNLOAD } }
        LaunchedEffect(works) { historyTick++ }
        LaunchedEffect(autoPaste, page) {
            if (autoPaste && page == Page.DOWNLOAD && link.isBlank()) {
                val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cb.primaryClip?.getItemAt(0)?.coerceToText(this@MainActivity)?.toString()?.takeIf { it.startsWith("https://") }?.let { link = it }
            }
        }

        MaterialTheme(colorScheme = darkColorScheme(primary = Color.White, background = Bg, surface = Panel, surfaceVariant = Soft, outline = Line, onSurfaceVariant = Muted)) {
            Scaffold(
                containerColor = Bg,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = { if (page != Page.SETTINGS) BottomBar(page) { page = it } }
            ) { pad ->
                when (page) {
                    Page.DOWNLOAD -> DownloadPage(Modifier.padding(pad), link, { link = it }, photos, { photos = it }, quality, { quality = it; persist() }, active, progress, status, error,
                        onPaste = { val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; link = cb.primaryClip?.getItemAt(0)?.coerceToText(this@MainActivity)?.toString().orEmpty() },
                        onDownload = { start() }, onCancel = { wm.cancelUniqueWork("download") }, onSettings = { page = Page.SETTINGS })
                    Page.SAVED -> SavedPage(Modifier.padding(pad), downloads, historyTick, { page = Page.SETTINGS }) { error = it; page = Page.DOWNLOAD }
                    Page.SETTINGS -> SettingsPage(Modifier.padding(pad), quality, { quality = it; persist() }, mobile, { mobile = it; persist() }, autoPaste, { autoPaste = it; persist() }, notifications, { notifications = it; persist() }, active, status, { start(true) }) { page = Page.DOWNLOAD }
                }
            }
        }
    }
}

@Composable private fun Header(onSettings: () -> Unit, back: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (back != null) IconButton(onClick = back) { Icon(Icons.Rounded.ArrowBack, "Back") }
        else Image(painterResource(R.drawable.app_icon), "Media Pocket", Modifier.size(54.dp).clip(RoundedCornerShape(15.dp)))
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text("Media Pocket", fontSize = 25.sp, fontWeight = FontWeight.Black)
            Text("Download videos in original quality", color = Muted, fontSize = 12.sp)
        }
        if (back == null) IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, "Settings", tint = Muted) }
    }
}

@Composable private fun BottomBar(page: Page, change: (Page) -> Unit) {
    NavigationBar(containerColor = Color(0xFF0C111A), tonalElevation = 0.dp) {
        NavigationBarItem(page == Page.DOWNLOAD, { change(Page.DOWNLOAD) }, { Icon(Icons.Rounded.Download, null) }, label = { Text("Download") })
        NavigationBarItem(page == Page.SAVED, { change(Page.SAVED) }, { Icon(Icons.Rounded.Folder, null) }, label = { Text("Saved") })
    }
}

@Composable private fun DownloadPage(
    modifier: Modifier, link: String, setLink: (String) -> Unit, photos: Boolean, setPhotos: (Boolean) -> Unit,
    quality: Int, setQuality: (Int) -> Unit, active: Boolean, progress: Float, status: String?, error: String,
    onPaste: () -> Unit, onDownload: () -> Unit, onCancel: () -> Unit, onSettings: () -> Unit
) {
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 18.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
        item { Header(onSettings) }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Save What", fontSize = 36.sp, lineHeight = 38.sp, fontWeight = FontWeight.Black)
                    Text("You Love", color = Color(0xFF7B73FF), fontSize = 36.sp, lineHeight = 38.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(7.dp)); Text("Videos. Photos. Always with you.", color = Muted)
                }
                Box(Modifier.size(104.dp).background(Brush.radialGradient(listOf(Purple.copy(.35f), Color.Transparent)), CircleShape), contentAlignment = Alignment.Center) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF17233D)), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF416DFF)), shape = RoundedCornerShape(20.dp)) {
                        Box(Modifier.size(70.dp), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Movie, null, tint = Color(0xFFA5B5FF), modifier = Modifier.size(32.dp)) }
                    }
                }
            }
        }
        item {
            PanelCard {
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Video URL", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f)); Text("Supported platforms", color = Muted, fontSize = 11.sp) }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(link, setLink, Modifier.fillMaxWidth(), enabled = !active, singleLine = true, placeholder = { Text("Paste video URL here") }, leadingIcon = { Icon(Icons.Rounded.Link, null) }, shape = RoundedCornerShape(17.dp))
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onPaste, Modifier.fillMaxWidth().height(50.dp), enabled = !active, shape = RoundedCornerShape(16.dp)) { Icon(Icons.Rounded.ContentPaste, null); Spacer(Modifier.width(8.dp)); Text("Paste from Clipboard", fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth().background(Color(0xFF0C111A), RoundedCornerShape(18.dp)).padding(4.dp)) {
                    Mode("Video", Icons.Rounded.Movie, !photos, !active, Modifier.weight(1f)) { setPhotos(false) }
                    Mode("Photos", Icons.Rounded.Image, photos, !active, Modifier.weight(1f)) { setPhotos(true) }
                }
                if (!photos) {
                    Spacer(Modifier.height(14.dp)); Text("Quality", fontWeight = FontWeight.Bold, fontSize = 18.sp); Spacer(Modifier.height(9.dp)); Qualities(quality, setQuality, !active)
                    Spacer(Modifier.height(9.dp)); Text("Best keeps the highest source quality. 4K is used only when the source provides it — no fake upscaling.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
                }
                Spacer(Modifier.height(15.dp)); GradientButton(if (active) "Downloading…" else "Download Now", active.not() && link.isNotBlank(), onDownload)
            }
        }
        if (active || !status.isNullOrBlank()) item { StatusCard(active, progress, status.orEmpty(), onCancel) }
        if (error.isNotBlank()) item { Surface(color = MaterialTheme.colorScheme.errorContainer.copy(.35f), shape = RoundedCornerShape(18.dp)) { Text(error, Modifier.padding(14.dp)) } }
        item { Platforms() }
        item { Text("Public downloadable posts only. Platform availability can change. No artificial upscaling or creator-watermark removal.", color = Muted.copy(.75f), fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(horizontal = 4.dp)) }
    }
}

@Composable private fun PanelCard(content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), border = androidx.compose.foundation.BorderStroke(1.dp, Line), shape = RoundedCornerShape(25.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(17.dp), content = content) }
}

@Composable private fun Mode(text: String, icon: ImageVector, selected: Boolean, enabled: Boolean, modifier: Modifier, click: () -> Unit) {
    val brush = if (selected) Glow else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
    Row(modifier.clip(RoundedCornerShape(14.dp)).background(brush).clickable(enabled = enabled, onClick = click).padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (selected) Color.White else Muted, modifier = Modifier.size(19.dp)); Spacer(Modifier.width(7.dp)); Text(text, color = if (selected) Color.White else Muted, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun Qualities(selected: Int, choose: (Int) -> Unit, enabled: Boolean) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DownloadQuality.entries.forEach { q ->
            val on = q.height == selected
            Column(Modifier.width(if (q.height < 0) 92.dp else 80.dp).clip(RoundedCornerShape(14.dp)).background(if (on) Glow else Brush.horizontalGradient(listOf(Color(0xFF101620), Color(0xFF101620)))).border(1.dp, if (on) Color.Transparent else Line, RoundedCornerShape(14.dp)).clickable(enabled) { choose(q.height) }.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(q.label, fontWeight = FontWeight.Black); Text(q.detail, color = if (on) Color.White.copy(.8f) else Muted, fontSize = 10.sp)
            }
        }
    }
}

@Composable private fun GradientButton(text: String, enabled: Boolean, click: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(57.dp).clip(RoundedCornerShape(17.dp)).background(Brush.horizontalGradient(listOf(Blue.copy(if (enabled) 1f else .45f), Purple.copy(if (enabled) 1f else .45f)))).clickable(enabled, onClick = click), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Download, null); Spacer(Modifier.width(10.dp)); Text(text, fontSize = 19.sp, fontWeight = FontWeight.Black)
    }
}

@Composable private fun StatusCard(active: Boolean, progress: Float, status: String, cancel: () -> Unit) {
    PanelCard {
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(if (active) Icons.Rounded.Download else Icons.Rounded.CheckCircle, null, tint = if (active) Color(0xFF7EA2FF) else Color(0xFF7FE3A1)); Spacer(Modifier.width(9.dp)); Text(status, Modifier.weight(1f), maxLines = 3, overflow = TextOverflow.Ellipsis) }
        if (active) { Spacer(Modifier.height(10.dp)); if (progress >= 0) LinearProgressIndicator(progress = { (progress / 100f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth()) else LinearProgressIndicator(Modifier.fillMaxWidth()); TextButton(cancel, Modifier.align(Alignment.End)) { Text("Cancel") } }
    }
}

@Composable private fun Platforms() {
    PanelCard {
        Text("Supported Platforms", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally)); Spacer(Modifier.height(11.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) { listOf("Instagram", "TikTok", "Facebook", "Pinterest", "YouTube").forEach { name -> Surface(color = Color(0xFF171E2B), shape = RoundedCornerShape(18.dp)) { Text(name, Modifier.padding(horizontal = 11.dp, vertical = 8.dp), fontSize = 11.sp) } } }
    }
}

private data class Saved(val title: String, val uri: String, val mime: String, val quality: String)

@Composable private fun SavedPage(modifier: Modifier, prefs: android.content.SharedPreferences, tick: Int, settings: () -> Unit, fail: (String) -> Unit) {
    val ctx = LocalContext.current
    val saved = remember(tick) {
        val a = JSONArray(prefs.getString("items", "[]")); buildList { for (i in 0 until a.length()) a.optJSONObject(i)?.let { add(Saved(it.optString("title", "Media"), it.optString("uri"), it.optString("mime", "video/mp4"), it.optString("quality", "Saved"))) } }
    }
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 18.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Header(settings) }
        item { Column(Modifier.padding(vertical = 10.dp)) { Text("Saved Downloads", fontSize = 35.sp, fontWeight = FontWeight.Black); Text("All your downloaded content in one place.", color = Muted) } }
        if (saved.isEmpty()) item { PanelCard { Column(Modifier.fillMaxWidth().padding(vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Rounded.Folder, null, tint = Color(0xFF7F91FF), modifier = Modifier.size(46.dp)); Spacer(Modifier.height(10.dp)); Text("Nothing saved yet", fontWeight = FontWeight.Bold); Text("Completed downloads will appear here.", color = Muted, fontSize = 12.sp) } } }
        else items(saved) { item ->
            PanelCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(62.dp).background(Color(0xFF19223A), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) { Icon(if (item.mime.startsWith("image/")) Icons.Rounded.Image else Icons.Rounded.Movie, null, tint = Color(0xFF8FA3FF), modifier = Modifier.size(29.dp)) }
                    Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(item.title, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis); Text(item.quality, color = Muted, fontSize = 11.sp); Spacer(Modifier.height(8.dp)); Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        OutlinedButton({ try { ctx.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(item.uri), item.mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) } catch (_: Exception) { fail("File missing or no compatible viewer is installed.") } }) { Icon(Icons.Rounded.OpenInNew, null, Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("Open") }
                        OutlinedButton({ try { ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType(item.mime).putExtra(Intent.EXTRA_STREAM, Uri.parse(item.uri)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Share media")) } catch (_: Exception) { fail("This file could not be shared.") } }) { Icon(Icons.Rounded.Share, null, Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("Share") }
                    } }
                }
            }
        }
    }
}

@Composable private fun SettingsPage(modifier: Modifier, quality: Int, setQuality: (Int) -> Unit, mobile: Boolean, setMobile: (Boolean) -> Unit, auto: Boolean, setAuto: (Boolean) -> Unit, notifications: Boolean, setNotifications: (Boolean) -> Unit, active: Boolean, status: String?, update: () -> Unit, back: () -> Unit) {
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 18.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 26.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Header({}, back) }
        item { Column(Modifier.padding(vertical = 8.dp)) { Text("Settings", fontSize = 37.sp, fontWeight = FontWeight.Black); Text("Customize your download experience", color = Muted) } }
        item { PanelCard { SettingTitle(Icons.Rounded.HighQuality, "Default Quality", "Choose preferred video quality"); Spacer(Modifier.height(14.dp)); Qualities(quality, setQuality, !active); Spacer(Modifier.height(9.dp)); Text("Higher source quality can create much larger files.", color = Muted, fontSize = 11.sp) } }
        item { PanelCard { Toggle(Icons.Rounded.Wifi, "Allow Mobile Data", "Use Wi-Fi or mobile data", mobile, setMobile); HorizontalDivider(Modifier.padding(vertical = 7.dp), color = Line); Toggle(Icons.Rounded.ContentPaste, "Auto-paste Clipboard", "Detect copied HTTPS links", auto, setAuto); HorizontalDivider(Modifier.padding(vertical = 7.dp), color = Line); Toggle(Icons.Rounded.Notifications, "Download Notifications", "Allow Android download status", notifications, setNotifications) } }
        item { PanelCard { Row(Modifier.fillMaxWidth().clickable(enabled = !active, onClick = update), verticalAlignment = Alignment.CenterVertically) { SettingIcon(Icons.Rounded.Refresh); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("Update Download Engine", fontWeight = FontWeight.Bold); Text("Refresh extractor rules", color = Muted, fontSize = 11.sp) }; Icon(Icons.Rounded.Refresh, null, tint = Muted) }; if (!status.isNullOrBlank()) { Spacer(Modifier.height(9.dp)); Text(status, color = Muted, fontSize = 11.sp) } } }
        item { PanelCard { SettingTitle(Icons.Rounded.Info, "About", "Media Pocket 2.0 · Android 10+"); Spacer(Modifier.height(8.dp)); Text("Best/Original plus real 4K, 2K, 1080p, 720p and 480p source-quality choices. No fake upscaling.", color = Muted, fontSize = 12.sp, lineHeight = 17.sp) } }
    }
}

@Composable private fun SettingIcon(icon: ImageVector) { Box(Modifier.size(44.dp).background(Color(0xFF1A2540), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color(0xFF9BAAFF), modifier = Modifier.size(21.dp)) } }
@Composable private fun SettingTitle(icon: ImageVector, title: String, sub: String) { Row(verticalAlignment = Alignment.CenterVertically) { SettingIcon(icon); Spacer(Modifier.width(12.dp)); Column { Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp); Text(sub, color = Muted, fontSize = 11.sp) } } }
@Composable private fun Toggle(icon: ImageVector, title: String, sub: String, checked: Boolean, set: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { SettingIcon(icon); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(sub, color = Muted, fontSize = 11.sp) }; Switch(checked, set) } }
