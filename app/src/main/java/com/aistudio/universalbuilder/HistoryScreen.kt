package com.aistudio.universalbuilder

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var entries by remember { mutableStateOf(BuildRecords(context).all()) }
    Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(18.dp)) {
        Text("BUILD HISTORY", style = MaterialTheme.typography.headlineSmall)
        OutlinedButton(onClick = { entries = BuildRecords(context).all() }) { Text("REFRESH") }
        if (entries.isEmpty()) Text("No builds recorded yet. Builds made before this update are not imported.")
        entries.forEach { item ->
            Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(item.optString("name"), style = MaterialTheme.typography.titleMedium)
                    Text(item.optString("package"))
                    Text(DateFormat.getDateTimeInstance().format(Date(item.optLong("created"))))
                    Text(item.optString("status"))
                    TextButton(onClick = { scope.launch {
                        withContext(Dispatchers.IO) { BackgroundBuildManager.resume(context, item.getString("id")) }
                        onBack()
                    } }) { Text("CHECK STATUS / DOWNLOAD") }
                    val url = item.optString("url")
                    if (url.startsWith("https://github.com/")) TextButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }) { Text("OPEN BUILD / DOWNLOAD ARTIFACT") }
                }
            }
        }
        Text("Generated artifacts expire after one day. Export APKs you want to keep. Only the latest APK is cached.")
        OutlinedButton(onClick = onBack) { Text("BACK TO BUILDER") }
    }
}
