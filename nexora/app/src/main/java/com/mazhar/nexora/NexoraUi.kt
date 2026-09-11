package com.mazhar.nexora

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Architecture
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Preview
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

private enum class AppTab { CHATS, PROJECTS, VOICE, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexoraApp(
    viewModel: NexoraViewModel,
    onRequestVoiceTyping: () -> Unit,
    onToggleVoiceCall: () -> Unit,
    onReadAloud: (String) -> Unit,
    onPreviewProject: (ProjectEntity) -> Unit,
    onVoiceTextDraft: (String) -> Unit
) {
    var tab by rememberSaveable { mutableStateOf(AppTab.CHATS) }
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf("") }
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val activeId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val streaming by viewModel.streamingText.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val selectedProfile by viewModel.selectedProfile.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val voiceActive by viewModel.voiceCallActive.collectAsStateWithLifecycle()
    val voiceStatus by viewModel.voiceStatus.collectAsStateWithLifecycle()
    val voiceTranscript by viewModel.voiceTranscript.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    val attachmentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "attachment"
            val text = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8).take(20_000) }.orEmpty() }.getOrDefault("")
            draft = if (text.isBlank()) "$draft\n[Attachment selected: $name — this file type is not converted automatically]" else "$draft\n\n[Attachment: $name]\n$text"
        }
    }
    val drawerState = androidx.compose.material3.rememberDrawerState(androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbars = remember { SnackbarHostState() }

    DisposableEffect(Unit) {
        NexoraUiEvents.registerVoiceTextConsumer { text ->
            draft = if (draft.isBlank()) text else "$draft $text"
            onVoiceTextDraft(text)
        }
        onDispose { NexoraUiEvents.registerVoiceTextConsumer { } }
    }
    LaunchedEffect(drawerOpen) {
        if (drawerOpen) drawerState.open() else drawerState.close()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = tab == AppTab.CHATS,
        drawerContent = {
            ChatDrawer(
                conversations = conversations,
                activeId = activeId,
                onClose = { drawerOpen = false },
                onNew = { viewModel.createConversation(); drawerOpen = false; tab = AppTab.CHATS },
                onSelect = { viewModel.selectConversation(it); drawerOpen = false; tab = AppTab.CHATS },
                onRename = { id, title -> viewModel.renameConversation(id, title) },
                onDelete = { viewModel.deleteConversation(it) },
                onPin = { id, pinned -> viewModel.setPinned(id, pinned) },
                onDuplicate = { viewModel.duplicateConversation(it) }
            )
        }
    ) {
        Scaffold(
            containerColor = NexoraColors.background,
            topBar = {
                SmallTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("NEXORA", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                when (tab) { AppTab.CHATS -> "WORKSPACE"; AppTab.PROJECTS -> "PROJECTS"; AppTab.VOICE -> "VOICE"; AppTab.SETTINGS -> "SETTINGS" },
                                color = NexoraColors.muted, fontSize = 11.sp, letterSpacing = 1.3.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { drawerOpen = true }) { Icon(Icons.Rounded.Menu, "Open chat history") }
                    },
                    actions = {
                        if (tab == AppTab.CHATS) {
                            ProfileChip(selectedProfile) { tab = AppTab.SETTINGS }
                            IconButton(onClick = { viewModel.createConversation() }) { Icon(Icons.Rounded.Add, "New chat") }
                        }
                    },
                    colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = NexoraColors.background)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = NexoraColors.surface) {
                    NavigationBarItem(tab == AppTab.CHATS, { tab = AppTab.CHATS }, icon = { Icon(Icons.Rounded.History, "Chats") }, label = { Text("Chats") })
                    NavigationBarItem(tab == AppTab.PROJECTS, { tab = AppTab.PROJECTS }, icon = { Icon(Icons.Rounded.Widgets, "Projects") }, label = { Text("Projects") })
                    NavigationBarItem(tab == AppTab.VOICE, { tab = AppTab.VOICE }, icon = { Icon(Icons.Rounded.GraphicEq, "Voice") }, label = { Text("Voice") })
                    NavigationBarItem(tab == AppTab.SETTINGS, { tab = AppTab.SETTINGS }, icon = { Icon(Icons.Rounded.Settings, "Settings") }, label = { Text("Settings") })
                }
            },
            snackbarHost = { SnackbarHost(snackbars) }
        ) { padding ->
            when (tab) {
                AppTab.CHATS -> ChatPage(
                    padding = padding,
                    messages = messages,
                    streaming = streaming,
                    status = status,
                    mode = mode,
                    draft = draft,
                    onDraft = { draft = it },
                    onMode = viewModel::setMode,
                    onSend = {
                        val prompt = draft.trim()
                        if (prompt.isNotBlank()) { draft = ""; viewModel.sendMessage(prompt, activity) }
                    },
                    onStop = viewModel::stopGeneration,
                    onVoiceTyping = onRequestVoiceTyping,
                    onAttachment = { attachmentPicker.launch(arrayOf("text/*", "application/json", "text/html", "text/markdown", "application/zip")) },
                    onReadAloud = onReadAloud
                )
                AppTab.PROJECTS -> ProjectsPage(padding, projects, viewModel, onPreviewProject)
                AppTab.VOICE -> VoicePage(padding, voiceActive, voiceStatus, voiceTranscript, settings.reducedMotion, onToggleVoiceCall)
                AppTab.SETTINGS -> SettingsPage(padding, viewModel, profiles, selectedProfile, settings, context)
            }
        }
    }
}

@Composable
private fun ProfileChip(profile: ProviderProfile?, onClick: () -> Unit) {
    Surface(onClick = onClick, color = NexoraColors.surfaceHigh, shape = RoundedCornerShape(50), modifier = Modifier.padding(end = 4.dp)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(if (profile == null) NexoraColors.warning else NexoraColors.accentDeep))
            Spacer(Modifier.width(6.dp))
            Text(profile?.model ?: "Connect", fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = NexoraColors.muted)
        }
    }
}

@Composable
private fun ChatPage(
    padding: PaddingValues,
    messages: List<MessageEntity>,
    streaming: String,
    status: String,
    mode: WorkspaceMode,
    draft: String,
    onDraft: (String) -> Unit,
    onMode: (WorkspaceMode) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onVoiceTyping: () -> Unit,
    onAttachment: () -> Unit,
    onReadAloud: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current
    LaunchedEffect(messages.size, streaming) {
        if (messages.isNotEmpty() || streaming.isNotBlank()) listState.animateScrollToItem((messages.size + if (streaming.isNotBlank()) 1 else 0).coerceAtLeast(1) - 1)
    }
    Column(Modifier.fillMaxSize().padding(padding)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            ModePicker(mode, onMode)
            Spacer(Modifier.weight(1f))
            Text(status, color = if (status.startsWith("Error")) NexoraColors.warning else NexoraColors.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (streaming.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onStop, modifier = Modifier.size(30.dp)) { Icon(Icons.Rounded.StopCircle, "Stop generating", tint = NexoraColors.warning) }
            }
        }
        if (messages.isEmpty() && streaming.isBlank()) EmptyChat(mode, onMode)
        else LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(message, onCopy = { clipboard.setText(AnnotatedString(message.content)) }, onReadAloud = onReadAloud)
            }
            if (streaming.isNotBlank()) item { StreamingBubble(streaming) }
        }
        Composer(draft, onDraft, onSend, onVoiceTyping, onAttachment)
    }
}

@Composable
private fun EmptyChat(mode: WorkspaceMode, onMode: (WorkspaceMode) -> Unit) {
    Column(Modifier.fillMaxWidth().weight(1f).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        NexoraCore(Modifier.size(142.dp), "IDLE", false)
        Spacer(Modifier.height(22.dp))
        Text("What can I build with you?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("A private workspace for conversations, code and projects.", color = NexoraColors.muted, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(WorkspaceMode.GENERAL, WorkspaceMode.CODE, WorkspaceMode.WEBSITE, WorkspaceMode.ANDROID).forEach {
                FilterChip(selected = mode == it, onClick = { onMode(it) }, label = { Text(it.label) }, leadingIcon = { Text(it.icon) })
            }
        }
    }
}

@Composable
private fun ModePicker(mode: WorkspaceMode, onMode: (WorkspaceMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp), border = BorderStroke(1.dp, NexoraColors.line)) {
            Text("${mode.icon}  ${mode.label}", fontSize = 12.sp)
            Icon(Icons.Rounded.ArrowDropDown, null, Modifier.size(18.dp))
        }
        DropdownMenu(expanded, { expanded = false }) {
            WorkspaceMode.entries.forEach { choice ->
                DropdownMenuItem(text = { Text("${choice.icon}  ${choice.label}") }, onClick = { onMode(choice); expanded = false }, leadingIcon = { if (choice == mode) Icon(Icons.Rounded.Check, null) })
            }
        }
    }
}

@Composable
private fun Composer(draft: String, onDraft: (String) -> Unit, onSend: () -> Unit, onVoiceTyping: () -> Unit, onAttachment: () -> Unit) {
    Surface(color = NexoraColors.surface, tonalElevation = 4.dp) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraft,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Message NEXORA…") },
                minLines = 2,
                maxLines = 6,
                shape = RoundedCornerShape(18.dp),
                trailingIcon = { IconButton(onClick = onSend, enabled = draft.isNotBlank()) { Icon(Icons.AutoMirrored.Rounded.Send, "Send") } }
            )
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onVoiceTyping) { Icon(Icons.Rounded.Mic, "Voice typing", tint = NexoraColors.accentDeep) }
                IconButton(onClick = onAttachment) { Icon(Icons.Rounded.AttachFile, "Choose an attachment", tint = NexoraColors.muted) }
                Text("Voice typing keeps the transcript editable before Send", color = NexoraColors.muted, fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                Text("${draft.length}/8000", color = NexoraColors.muted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun MessageBubble(message: MessageEntity, onCopy: () -> Unit, onReadAloud: (String) -> Unit) {
    val user = message.role == "user"
    val system = message.role == "system"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) {
        Column(Modifier.fillMaxWidth(if (user) 0.88f else 0.96f)) {
            Text(if (user) "YOU" else if (system) "NEXORA · NOTICE" else "NEXORA", color = if (system) NexoraColors.warning else NexoraColors.muted, fontSize = 10.sp, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp))
            Surface(color = if (user) NexoraColors.accent else if (system) Color(0xFF2A2115) else NexoraColors.surfaceHigh, shape = RoundedCornerShape(18.dp), border = if (system) BorderStroke(1.dp, Color(0xFF69532C)) else null) {
                Column(Modifier.padding(14.dp)) {
                    Text(message.content, color = if (user) Color(0xFF07100D) else NexoraColors.text, fontSize = 15.sp, lineHeight = 22.sp, fontFamily = if (message.content.contains("```")) FontFamily.Monospace else FontFamily.Default)
                    if (!user && !system) {
                        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            TextButton(onClick = onCopy, contentPadding = PaddingValues(horizontal = 8.dp)) { Icon(Icons.Rounded.ContentCopy, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("Copy", fontSize = 11.sp) }
                            TextButton(onClick = { onReadAloud(message.content) }, contentPadding = PaddingValues(horizontal = 8.dp)) { Icon(Icons.Rounded.VolumeUp, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("Read", fontSize = 11.sp) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingBubble(text: String) {
    Row(Modifier.fillMaxWidth()) {
        Surface(color = NexoraColors.surfaceHigh, shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(14.dp)) {
                Text("NEXORA · GENERATING", color = NexoraColors.accentDeep, fontSize = 10.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                Text(text, color = NexoraColors.text, fontSize = 15.sp, lineHeight = 22.sp, fontFamily = if (text.contains("```")) FontFamily.Monospace else FontFamily.Default)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.55f), color = NexoraColors.accentDeep, trackColor = NexoraColors.line)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ChatDrawer(
    conversations: List<ConversationEntity>,
    activeId: String?,
    onClose: () -> Unit,
    onNew: () -> Unit,
    onSelect: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onPin: (String, Boolean) -> Unit,
    onDuplicate: (String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<ConversationEntity?>(null) }
    ModalDrawerSheet(drawerContainerColor = NexoraColors.surface, modifier = Modifier.fillMaxWidth(0.86f)) {
        Column(Modifier.fillMaxHeight().padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("NEXORA", fontWeight = FontWeight.Bold, letterSpacing = 2.sp); Text("Conversation history", color = NexoraColors.muted, fontSize = 12.sp) }
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Close history") }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onNew, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = NexoraColors.accent, contentColor = Color(0xFF07100D))) { Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("New chat") }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Search chats") }, leadingIcon = { Icon(Icons.Rounded.Search, null) })
            Spacer(Modifier.height(14.dp))
            val visible = conversations.filter { query.isBlank() || it.title.contains(query, true) }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(visible, key = { it.id }) { item ->
                    var menu by remember { mutableStateOf(false) }
                    Surface(onClick = { onSelect(item.id) }, color = if (item.id == activeId) NexoraColors.surfaceHigh else Color.Transparent, shape = RoundedCornerShape(14.dp)) {
                        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                                Text(if (item.pinned) "Pinned" else relativeDate(item.updatedAt), color = NexoraColors.muted, fontSize = 10.sp)
                            }
                            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Conversation actions", tint = NexoraColors.muted) }
                            DropdownMenu(menu, { menu = false }) {
                                DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; renameTarget = item })
                                DropdownMenuItem(text = { Text(if (item.pinned) "Unpin" else "Pin") }, onClick = { menu = false; onPin(item.id, !item.pinned) }, leadingIcon = { Icon(Icons.Rounded.PushPin, null) })
                                DropdownMenuItem(text = { Text("Duplicate") }, onClick = { menu = false; onDuplicate(item.id) })
                                DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete(item.id) }, leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null) })
                            }
                        }
                    }
                }
            }
            Text("Chats stay on this device unless you send them to your selected provider.", color = NexoraColors.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp))
        }
    }
    renameTarget?.let { target -> RenameDialog(target.title, { title -> onRename(target.id, title); renameTarget = null }, { renameTarget = null }) }
}

@Composable
private fun RenameDialog(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember(initial) { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Rename chat") }, text = { OutlinedTextField(value, { value = it }, singleLine = true, label = { Text("Title") }) }, confirmButton = { TextButton(onClick = { onSave(value) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun VoicePage(padding: PaddingValues, active: Boolean, status: String, transcript: String, reducedMotion: Boolean, onToggle: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(28.dp))
        Text("NEXORA CORE", color = NexoraColors.muted, fontSize = 11.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(22.dp))
        NexoraCore(Modifier.size(260.dp), status.uppercase(), reducedMotion)
        Spacer(Modifier.height(18.dp))
        Text(status, color = NexoraColors.accent, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Text(if (active) "Speak naturally. NEXORA will listen after each turn." else "Turn-based voice conversation with local Android speech services.", color = NexoraColors.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        if (transcript.isNotBlank()) Surface(Modifier.fillMaxWidth().padding(top = 22.dp), color = NexoraColors.surfaceHigh, shape = RoundedCornerShape(18.dp)) { Text(transcript, Modifier.padding(16.dp), color = NexoraColors.text, fontSize = 14.sp) }
        Spacer(Modifier.weight(1f))
        Button(onClick = onToggle, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = if (active) Color(0xFF3B2522) else NexoraColors.accent, contentColor = if (active) Color(0xFFFFD8CE) else Color(0xFF07100D))) {
            Icon(if (active) Icons.Rounded.StopCircle else Icons.Rounded.Call, null); Spacer(Modifier.width(8.dp)); Text(if (active) "End voice call" else "Start voice call")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun NexoraCore(modifier: Modifier, state: String, reducedMotion: Boolean) {
    val transition = rememberInfiniteTransition(label = "core")
    val pulse by transition.animateFloat(0.92f, 1.08f, infiniteRepeatable(tween(if (reducedMotion) 2400 else 1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(if (reducedMotion) 16000 else 7000), RepeatMode.Restart), label = "rotation")
    Canvas(modifier.scale(if (reducedMotion) 1f else pulse).rotate(if (reducedMotion) 0f else rotation)) {
        val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension * .27f
        drawCircle(Brush.radialGradient(listOf(NexoraColors.accent.copy(alpha = .75f), NexoraColors.accentDeep.copy(alpha = .25f), Color.Transparent), center, radius * 2.7f), radius * 2.7f, center)
        drawCircle(Color(0xFF0D1718), radius * 1.55f, center)
        for (i in 0..3) {
            val inset = i * radius * .19f
            drawRoundRect(Brush.linearGradient(listOf(NexoraColors.accent.copy(alpha = .65f - i * .1f), NexoraColors.accentDeep.copy(alpha = .2f))), topLeft = androidx.compose.ui.geometry.Offset(center.x - radius + inset, center.y - radius + inset), size = androidx.compose.ui.geometry.Size((radius * 2 - inset * 2), radius * 2 - inset * 2), cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius * .22f), style = Stroke(width = if (i == 0) 5f else 2.5f, cap = StrokeCap.Round))
        }
        drawCircle(NexoraColors.accent, radius * .18f, center)
    }
}

@Composable
private fun ProjectsPage(padding: PaddingValues, projects: List<ProjectEntity>, viewModel: NexoraViewModel, onPreview: (ProjectEntity) -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Project library", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold); Text("Saved code and generated workspaces", color = NexoraColors.muted, fontSize = 13.sp) }
            Icon(Icons.Rounded.Build, null, tint = NexoraColors.accent)
        }
        if (projects.isEmpty()) EmptyProjects()
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            items(projects, key = { it.id }) { project ->
                Card(colors = CardDefaults.cardColors(containerColor = NexoraColors.surface), border = BorderStroke(1.dp, NexoraColors.line)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(NexoraColors.surfaceHigh), contentAlignment = Alignment.Center) { Icon(if (project.type == WorkspaceMode.WEBSITE.name) Icons.Rounded.Language else Icons.Rounded.Description, null, tint = NexoraColors.accent) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) { Text(project.name, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis); Text("${project.type.lowercase().replace('_', ' ')}  ·  ${relativeDate(project.updatedAt)}", color = NexoraColors.muted, fontSize = 11.sp) }
                            IconButton(onClick = { viewModel.deleteProject(project.id) }) { Icon(Icons.Rounded.DeleteOutline, "Delete project", tint = NexoraColors.muted) }
                        }
                        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onPreview(project) }, enabled = project.previewHtml.isNotBlank()) { Icon(Icons.Rounded.Preview, null, Modifier.size(17.dp)); Spacer(Modifier.width(4.dp)); Text("Preview") }
                            OutlinedButton(onClick = { kotlinx.coroutines.MainScope().launch { val (item, files) = viewModel.filesForProject(project.id); if (item != null) ProjectExporter.share(context, item, files) } }) { Icon(Icons.Rounded.ArrowBack, null, Modifier.size(17.dp)); Spacer(Modifier.width(4.dp)); Text("Export ZIP") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyProjects() {
    Column(Modifier.fillMaxSize().padding(bottom = 80.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Rounded.Widgets, null, Modifier.size(52.dp), tint = NexoraColors.accentDeep)
        Spacer(Modifier.height(14.dp)); Text("No saved projects yet", style = MaterialTheme.typography.titleLarge)
        Text("Switch Chat mode to Website, Code or Android app, then ask NEXORA to build something.", color = NexoraColors.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun SettingsPage(padding: PaddingValues, viewModel: NexoraViewModel, profiles: List<ProviderProfile>, selected: ProviderProfile?, settings: AppSettings, context: android.content.Context) {
    var editor by remember { mutableStateOf<ProviderProfile?>(null) }
    var addProfile by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp))
        Text("Control providers, voice and local data", color = NexoraColors.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp))
        SettingsSection("AI PROVIDERS", Icons.Rounded.AutoAwesome)
        profiles.forEach { profile ->
            ProviderRow(profile, profile.id == selected?.id, onSelect = { viewModel.selectProfile(profile.id) }, onEdit = { editor = profile }, onDelete = { viewModel.deleteProvider(profile.id) })
        }
        OutlinedButton(onClick = { addProfile = true }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, NexoraColors.line)) { Icon(Icons.Rounded.AddCircleOutline, null); Spacer(Modifier.width(8.dp)); Text("Add provider or free API") }
        Spacer(Modifier.height(22.dp))
        SettingsSection("FREE API HUB", Icons.Rounded.Language)
        FreeApiCard("OpenRouter · Free Models Router", "Use the openrouter/free model with a normal OpenAI-compatible key.", "https://openrouter.ai/openrouter/free", context)
        FreeApiCard("Google AI Studio", "Create a Gemini API key and use the Gemini adapter.", "https://aistudio.google.com/api-keys", context)
        FreeApiCard("Groq", "Fast OpenAI-compatible endpoint with a developer free tier.", "https://console.groq.com/", context)
        FreeApiCard("Puter.js", "Optional no-key web connector. It opens an isolated hosted SDK bridge and follows Puter terms/limits.", "https://developer.puter.com/tutorials/free-unlimited-openrouter-api/", context)
        Spacer(Modifier.height(22.dp))
        SettingsSection("VOICE", Icons.Rounded.GraphicEq)
        ToggleRow("Speak AI replies", "Use Android Text-to-Speech for voice-call replies.", settings.speakReplies) { viewModel.updateSettings(settings.copy(speakReplies = it)) }
        ToggleRow("Reduced motion", "Keep the Nexora Core still and use less animation.", settings.reducedMotion) { viewModel.updateSettings(settings.copy(reducedMotion = it)) }
        ToggleRow("Auto-send voice typing", "Reserved for future opt-in; voice typing currently stays editable.", settings.autoSendVoice) { viewModel.updateSettings(settings.copy(autoSendVoice = it)) }
        Spacer(Modifier.height(22.dp))
        SettingsSection("PRIVACY & DATA", Icons.Rounded.Security)
        OutlinedButton(onClick = { showPrivacy = true }, modifier = Modifier.fillMaxWidth()) { Text("How data and API keys are handled") }
        Text("NEXORA AI 1.0 · keys use Android Keystore-backed encryption · chat/project data stays local by default", color = NexoraColors.muted, fontSize = 11.sp, modifier = Modifier.padding(vertical = 22.dp))
    }
    if (addProfile) ProviderEditorDialog(null, { viewModel.saveProvider(it); addProfile = false }, { addProfile = false }, viewModel::testProvider)
    editor?.let { ProviderEditorDialog(it, { viewModel.saveProvider(it); editor = null }, { editor = null }, viewModel::testProvider) }
    if (showPrivacy) AlertDialog(onDismissRequest = { showPrivacy = false }, title = { Text("Privacy") }, text = { Text("Chat content is sent directly to the AI provider profile you select. API keys are encrypted with Android Keystore-backed AES-GCM storage and are never written into project exports. NEXORA does not include a hidden backend or automatic paid fallback. Voice recognition and text-to-speech use Android services on your device.") }, confirmButton = { TextButton(onClick = { showPrivacy = false }) { Text("Done") } })
}

@Composable
private fun SettingsSection(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) { Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = NexoraColors.accentDeep, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(title, color = NexoraColors.accentDeep, fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold) } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderRow(profile: ProviderProfile, selected: Boolean, onSelect: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(onClick = onSelect, colors = CardDefaults.cardColors(containerColor = if (selected) NexoraColors.surfaceHigh else NexoraColors.surface), border = BorderStroke(1.dp, if (selected) NexoraColors.accentDeep else NexoraColors.line), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(if (selected) NexoraColors.accent else NexoraColors.line))
            Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(profile.name, fontWeight = FontWeight.Medium); Text("${profile.model}  ·  ${profile.maskedKey()}", color = NexoraColors.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, "Edit provider", tint = NexoraColors.muted) }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.DeleteOutline, "Delete provider", tint = NexoraColors.muted) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FreeApiCard(title: String, description: String, url: String, context: android.content.Context) {
    Card(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }, colors = CardDefaults.cardColors(containerColor = NexoraColors.surface), border = BorderStroke(1.dp, NexoraColors.line), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Language, null, tint = NexoraColors.accent); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Medium); Text(description, color = NexoraColors.muted, fontSize = 11.sp, lineHeight = 16.sp) }; Text("OPEN", color = NexoraColors.accentDeep, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun ToggleRow(title: String, description: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, fontSize = 14.sp); Text(description, color = NexoraColors.muted, fontSize = 11.sp) }; Switch(checked, onCheckedChange = onChecked) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderEditorDialog(existing: ProviderProfile?, onSave: (ProviderProfile) -> Unit, onDismiss: () -> Unit, onTest: (ProviderProfile, (String) -> Unit) -> Unit) {
    var presetId by remember(existing) { mutableStateOf(existing?.providerId ?: "openrouter") }
    val preset = ProviderCatalog.preset(presetId)
    var name by remember(existing) { mutableStateOf(existing?.name ?: preset.label) }
    var model by remember(existing) { mutableStateOf(existing?.model ?: preset.defaultModel) }
    var baseUrl by remember(existing) { mutableStateOf(existing?.baseUrl ?: preset.baseUrl) }
    var key by remember(existing) { mutableStateOf(existing?.apiKey ?: "") }
    var headers by remember(existing) { mutableStateOf(existing?.customHeaders ?: "") }
    var expanded by remember { mutableStateOf(false) }
    var testMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    fun currentProfile() = ProviderProfile(existing?.id ?: java.util.UUID.randomUUID().toString(), name.trim().ifBlank { preset.label }, preset.id, model.trim(), baseUrl.trim(), key.trim(), preset.protocol, headers.trim())
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (existing == null) "Add AI provider" else "Edit AI provider") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Box {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(preset.label, Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Start); Icon(Icons.Rounded.ArrowDropDown, null) }
                DropdownMenu(expanded, { expanded = false }) { ProviderCatalog.presets.forEach { p -> DropdownMenuItem(text = { Text(p.label) }, onClick = { presetId = p.id; name = p.label; model = p.defaultModel; baseUrl = p.baseUrl; expanded = false }) } }
            }
            OutlinedTextField(name, { name = it }, label = { Text("Profile name") }, singleLine = true)
            OutlinedTextField(model, { model = it }, label = { Text("Model ID") }, singleLine = true)
            if (preset.protocol != ApiProtocol.PUTER_WEB) OutlinedTextField(baseUrl, { baseUrl = it }, label = { Text("HTTPS base URL") }, singleLine = true)
            OutlinedTextField(key, { key = it }, label = { Text(if (preset.protocol == ApiProtocol.PUTER_WEB) "API key not required" else "API key") }, singleLine = true)
            if (presetId == "custom") OutlinedTextField(headers, { headers = it }, label = { Text("Optional headers JSON") }, minLines = 2)
            Text(preset.description, color = NexoraColors.muted, fontSize = 11.sp)
            if (testMessage.isNotBlank()) Text(testMessage, color = if (testMessage.startsWith("Connection works")) NexoraColors.accent else NexoraColors.warning, fontSize = 12.sp)
        }
    }, confirmButton = {
        TextButton(onClick = {
            if (preset.protocol != ApiProtocol.PUTER_WEB && key.isBlank()) { testMessage = "API key required"; return@TextButton }
            onSave(currentProfile())
        }) { Text("Save") }
    }, dismissButton = {
        Row { TextButton(onClick = { testMessage = "Testing…"; onTest(currentProfile()) { result -> testMessage = result } }) { Text("Test connection") }; TextButton(onClick = onDismiss) { Text("Cancel") } }
    })
}

private fun relativeDate(time: Long): String {
    val delta = System.currentTimeMillis() - time
    return when {
        delta < 60_000 -> "just now"
        delta < 3_600_000 -> "${delta / 60_000}m ago"
        delta < 86_400_000 -> "${delta / 3_600_000}h ago"
        else -> "${delta / 86_400_000}d ago"
    }
}
