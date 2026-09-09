package com.aistudio.universalbuilder
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun StudioUtilityCard() {
    val context=LocalContext.current; val scope=rememberCoroutineScope()
    var message by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }
    var cache by remember { mutableStateOf(0L) }; var projects by remember { mutableStateOf(0L) }
    var updateUrl by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { cache=StudioTools.cacheBytes(context);projects=ProjectLibrary(context).bytes() } }
    BuilderCard("Storage & updates") {
        Text("Projects: ${projects / 1024 / 1024} MB  ·  Cache: ${cache / 1024 / 1024} MB")
        Text("Your projects and exported APKs are kept when cleaning temporary build files.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick={busy=true;scope.launch {
            message=withContext(Dispatchers.IO) { if(BuildStatusHelper.getCurrentState(context).isBuilding) "Wait for the current build" else "Cleared ${StudioTools.clean(context)/1024} KB of temporary files" };cache=withContext(Dispatchers.IO) { StudioTools.cacheBytes(context) };busy=false
        }},enabled=!busy) { Text("Clean temporary files") }
        HorizontalDivider(Modifier.padding(vertical=12.dp))
        Text("Universal Build Studio ${BuildConfig.VERSION_NAME}",style=MaterialTheme.typography.titleMedium)
        TextButton(onClick={busy=true;scope.launch {
            message=withContext(Dispatchers.IO) { runCatching {
                val release=StudioTools.update();val version=release.optString("tag_name")
                updateUrl=release.getString("html_url");"Published version: $version. Compare it with your installed version before downloading."
            }.getOrElse { it.message ?: "Update check failed" } };busy=false
        }},enabled=!busy) { Text("Check for updates") }
        if(updateUrl.isNotBlank()) OutlinedButton(onClick={context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(updateUrl)))}) { Text("Open APK download") }
        if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if(message.isNotBlank()) Text(message,style=MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        Text("App updates need the same signing key. Configure your private release key once in GitHub Actions secrets; keep a backup. Existing debug installs may need a one-time reinstall.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
