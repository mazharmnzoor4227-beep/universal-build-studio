package com.aistudio.universalbuilder

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable
fun WorkspaceScreen(onOpen: () -> Unit, onNew: () -> Unit) {
    val context=LocalContext.current; val scope=rememberCoroutineScope(); val library=remember { ProjectLibrary(context) }
    var projects by remember { mutableStateOf(emptyList<JSONObject>()) }; var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }; var backup by remember { mutableStateOf<JSONObject?>(null) }
    suspend fun refresh() { projects=withContext(Dispatchers.IO) { library.all() } }
    LaunchedEffect(Unit) { refresh() }
    fun perform(action: () -> String) { busy=true; scope.launch { message=withContext(Dispatchers.IO) { runCatching(action).getOrElse { it.message ?: "Unable to complete action" } }; refresh(); busy=false } }
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri -> if(uri!=null) backup?.let { item -> perform { library.export(item,uri); "Backup saved" } } }
    val restore=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if(uri!=null) perform { library.restore(uri); "Project restored" } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement=Arrangement.spacedBy(16.dp)) {
        StudioHeader("Your app workspace", "Create. Refine. Build again.")
        Surface(color=MaterialTheme.colorScheme.primary, shape=MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().padding(24.dp)) {
                Text("Make your next idea real.",style=MaterialTheme.typography.headlineSmall)
                Text("Start a fresh project or continue a saved app.",modifier=Modifier.padding(vertical=12.dp))
                FilledTonalButton(onClick={busy=true;scope.launch {
                    val active=withContext(Dispatchers.IO) { BuildStatusHelper.getCurrentState(context).isBuilding }
                    if(active) message="Wait for the current build" else { BuilderSessionStore(context).clear();WebOptions(context).clear();onOpen() };busy=false
                }},enabled=!busy) { Text("＋ New project") }
            }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick={ perform { library.saveDraft(); "Current draft saved as a snapshot" } },enabled=!busy,modifier=Modifier.weight(1f)) { Text("Save draft") }
            OutlinedButton(onClick={ restore.launch(arrayOf("application/zip","application/octet-stream")) },enabled=!busy,modifier=Modifier.weight(1f)) { Text("Restore ZIP") }
        }
        if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if(message.isNotBlank()) Text(message,style=MaterialTheme.typography.bodyMedium)
        Text("MY PROJECTS  ·  ${projects.size}",style=MaterialTheme.typography.labelLarge)
        if(projects.isEmpty()) BuilderCard("Room for your first app") { Text("Choose a template below, or open New project to import your source. Save a draft to keep a reusable copy.") }
        projects.forEach { item ->
            BuilderCard(item.optString("name")) {
                Text(item.optString("package"),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Text(item.optString("sourceName"),modifier=Modifier.padding(vertical=8.dp))
                Button(onClick={busy=true;scope.launch {
                    val active=withContext(Dispatchers.IO) { BuildStatusHelper.getCurrentState(context).isBuilding }
                    if(active) message="Wait for the current build" else runCatching { library.open(item);onOpen() }.onFailure { message=it.message ?: "Could not open project" };busy=false
                }},enabled=!busy) { Text("Open / update") }
                Row {
                    TextButton(onClick={perform { library.duplicate(item); "Copy created. For native projects also change the package inside the source." }},enabled=!busy) { Text("Duplicate") }
                    TextButton(onClick={ backup=item; export.launch("studio-project.zip") },enabled=!busy) { Text("Backup ZIP") }
                }
            }
        }
        Text("STARTER TEMPLATES",style=MaterialTheme.typography.labelLarge)
        listOf("Notes","Photo gallery","Audio player","Product catalogue").forEach { name ->
            OutlinedButton(onClick={
                busy=true
                scope.launch {
                    val active=withContext(Dispatchers.IO) { BuildStatusHelper.getCurrentState(context).isBuilding }
                    if(active) { message="Wait for your build to finish"; busy=false; return@launch }
                    val result=withContext(Dispatchers.IO) { CodeProjectCreator.create(context,StarterTemplates.html(name)) }
                    if(result.success && result.uri!=null) {
                        WebOptions(context).clear()
                        BuilderSessionStore(context).save(name,"com.studio.app"+System.currentTimeMillis(),"template.zip",result.uri.toString(),null)
                        onOpen()
                    } else message=result.message
                    busy=false
                }
            },enabled=!busy,modifier=Modifier.fillMaxWidth()) { Text(name) }
        }
    }
}
