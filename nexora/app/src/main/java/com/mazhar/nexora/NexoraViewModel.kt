package com.mazhar.nexora

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.UUID

class NexoraViewModel(application: Application) : AndroidViewModel(application) {
    private val database = NexoraDatabase.get(application)
    private val conversationDao = database.conversations()
    private val messageDao = database.messages()
    private val projectDao = database.projects()
    private val projectFileDao = database.projectFiles()
    private val vault = SecureVault(application)
    private val providerEngine = ProviderEngine()
    private val settingsPrefs = application.getSharedPreferences("nexora_settings", 0)

    val conversations: StateFlow<List<ConversationEntity>> = conversationDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val projects: StateFlow<List<ProjectEntity>> = projectDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId = _activeConversationId.asStateFlow()
    val messages: StateFlow<List<MessageEntity>> = _activeConversationId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else messageDao.observeForConversation(id)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _profiles = MutableStateFlow<List<ProviderProfile>>(emptyList())
    val profiles = _profiles.asStateFlow()
    private val _selectedProfileId = MutableStateFlow("")
    val selectedProfileId = _selectedProfileId.asStateFlow()
    val selectedProfile: StateFlow<ProviderProfile?> = kotlinx.coroutines.flow.combine(_profiles, _selectedProfileId) { items, id ->
        items.firstOrNull { it.id == id } ?: items.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _mode = MutableStateFlow(WorkspaceMode.GENERAL)
    val mode = _mode.asStateFlow()
    private val _streamingText = MutableStateFlow("")
    val streamingText = _streamingText.asStateFlow()
    private val _status = MutableStateFlow("Ready")
    val status = _status.asStateFlow()
    private val _voiceStatus = MutableStateFlow("Idle")
    val voiceStatus = _voiceStatus.asStateFlow()
    private val _voiceTranscript = MutableStateFlow("")
    val voiceTranscript = _voiceTranscript.asStateFlow()
    private val _voiceCallActive = MutableStateFlow(false)
    val voiceCallActive = _voiceCallActive.asStateFlow()
    private val _settings = MutableStateFlow(loadSettings())
    val settings = _settings.asStateFlow()
    private var generationJob: Job? = null

    init {
        _profiles.value = loadProfiles()
        _selectedProfileId.value = settingsPrefs.getString("selected_profile", "").orEmpty()
        viewModelScope.launch {
            val existing = conversationDao.getAll()
            _activeConversationId.value = existing.firstOrNull()?.id ?: createConversation()
        }
    }

    fun createConversation(): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            val profile = selectedProfile.value
            conversationDao.upsert(ConversationEntity(id, "New chat", now, now, providerId = profile?.providerId.orEmpty(), model = profile?.model.orEmpty()))
        }
        _activeConversationId.value = id
        _streamingText.value = ""
        _status.value = "Ready"
        return id
    }

    fun selectConversation(id: String) {
        generationJob?.cancel()
        _activeConversationId.value = id
        _streamingText.value = ""
        _status.value = "Ready"
    }

    fun renameConversation(id: String, title: String) {
        val clean = title.trim().ifBlank { "New chat" }.take(80)
        viewModelScope.launch { conversationDao.rename(id, clean, System.currentTimeMillis()) }
    }

    fun setPinned(id: String, pinned: Boolean) {
        viewModelScope.launch { conversationDao.setPinned(id, pinned, System.currentTimeMillis()) }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            messageDao.deleteForConversation(id)
            conversationDao.delete(id)
            if (_activeConversationId.value == id) {
                _activeConversationId.value = conversationDao.getAll().firstOrNull()?.id ?: createConversation()
            }
        }
    }

    fun duplicateConversation(id: String) {
        viewModelScope.launch {
            val original = conversationDao.getAll().firstOrNull { it.id == id } ?: return@launch
            val newId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            conversationDao.upsert(original.copy(id = newId, title = "Copy of ${original.title}".take(80), createdAt = now, updatedAt = now, pinned = false))
            messageDao.getForConversation(id).forEach { messageDao.insert(it.copy(id = UUID.randomUUID().toString(), conversationId = newId)) }
            _activeConversationId.value = newId
        }
    }

    fun setMode(newMode: WorkspaceMode) { _mode.value = newMode }

    fun sendMessage(text: String, activity: Activity? = null, onComplete: ((String) -> Unit)? = null) {
        val prompt = text.trim()
        if (prompt.isBlank() || generationJob?.isActive == true) return
        val profile = selectedProfile.value
        if (profile == null) {
            _status.value = "Add an AI provider in Settings first"
            return
        }
        val conversationId = _activeConversationId.value ?: createConversation()
        generationJob = viewModelScope.launch {
            val now = System.currentTimeMillis()
            messageDao.insert(MessageEntity(UUID.randomUUID().toString(), conversationId, "user", prompt, now))
            conversationDao.rename(conversationId, titleFromPrompt(prompt), now)
            _streamingText.value = ""
            _status.value = "Connecting · ${profile.name}"
            try {
                val history = messageDao.getForConversation(conversationId)
                    .takeLast(40)
                    .map { ChatLine(it.role, it.content) }
                val reply = providerEngine.generate(
                    profile = profile,
                    messages = history,
                    system = systemPrompt(_mode.value),
                    puterActivity = activity,
                    onDelta = { delta -> _streamingText.value += delta; _status.value = "Generating" }
                )
                messageDao.insert(MessageEntity(UUID.randomUUID().toString(), conversationId, "assistant", reply, System.currentTimeMillis()))
                conversationDao.upsert(
                    ConversationEntity(
                        id = conversationId,
                        title = conversationDao.getAll().firstOrNull { it.id == conversationId }?.title ?: titleFromPrompt(prompt),
                        createdAt = conversationDao.getAll().firstOrNull { it.id == conversationId }?.createdAt ?: now,
                        updatedAt = System.currentTimeMillis(),
                        providerId = profile.providerId,
                        model = profile.model
                    )
                )
                saveProjectIfPresent(reply, prompt, conversationId)
                _streamingText.value = ""
                _status.value = "Ready · ${profile.model}"
                onComplete?.invoke(reply)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                _streamingText.value = ""
                _status.value = "Stopped"
            } catch (error: Throwable) {
                _streamingText.value = ""
                _status.value = "Error · ${error.message ?: error.javaClass.simpleName}".take(180)
                messageDao.insert(MessageEntity(UUID.randomUUID().toString(), conversationId, "system", "NEXORA ERROR: ${error.message ?: "Provider request failed"}", System.currentTimeMillis()))
            }
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
        generationJob = null
        _streamingText.value = ""
        _status.value = "Stopped"
    }

    fun setVoiceStatus(status: String, transcript: String = _voiceTranscript.value) {
        _voiceStatus.value = status
        _voiceTranscript.value = transcript
    }

    fun setVoiceCallActive(active: Boolean) { _voiceCallActive.value = active }

    fun saveProvider(profile: ProviderProfile) {
        _profiles.value = (_profiles.value.filterNot { it.id == profile.id } + profile)
        persistProfiles()
        if (_selectedProfileId.value.isBlank()) selectProfile(profile.id)
    }

    fun selectProfile(id: String) {
        _selectedProfileId.value = id
        settingsPrefs.edit().putString("selected_profile", id).apply()
    }

    fun deleteProvider(id: String) {
        _profiles.value = _profiles.value.filterNot { it.id == id }
        persistProfiles()
        if (_selectedProfileId.value == id) {
            val fallback = _profiles.value.firstOrNull()?.id.orEmpty()
            selectProfile(fallback)
        }
    }

    fun testProvider(profile: ProviderProfile, onResult: (String) -> Unit) {
        viewModelScope.launch {
            onResult(runCatching {
                providerEngine.generate(profile, listOf(ChatLine("user", "Reply with exactly OK.")), "Connection test. Reply only OK.", null) { }
                "Connection works"
            }.getOrElse { "Connection failed: ${it.message ?: it.javaClass.simpleName}" })
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        _settings.value = newSettings
        settingsPrefs.edit()
            .putBoolean("auto_send_voice", newSettings.autoSendVoice)
            .putBoolean("reduced_motion", newSettings.reducedMotion)
            .putBoolean("speak_replies", newSettings.speakReplies)
            .apply()
    }

    suspend fun filesForProject(id: String): Pair<ProjectEntity?, List<ProjectFileEntity>> = withContext(Dispatchers.IO) {
        projectDao.get(id) to projectFileDao.getForProject(id)
    }

    fun deleteProject(id: String) {
        viewModelScope.launch { projectDao.deleteFiles(id); projectDao.delete(id) }
    }

    private suspend fun saveProjectIfPresent(reply: String, prompt: String, conversationId: String) {
        val currentMode = _mode.value
        if (currentMode == WorkspaceMode.GENERAL) return
        val parsed = ProjectParser.parse(reply, currentMode, projectNameFromPrompt(prompt)) ?: return
        val projectId = UUID.randomUUID().toString()
        projectDao.upsert(ProjectEntity(projectId, parsed.name, parsed.type, conversationId, System.currentTimeMillis(), parsed.previewHtml))
        projectFileDao.insertAll(parsed.files.map { (path, content) -> ProjectFileEntity(UUID.randomUUID().toString(), projectId, path, content) })
    }

    private fun loadProfiles(): List<ProviderProfile> {
        val source = vault.get("provider_profiles", "[]")
        val json = runCatching { JSONArray(source) }.getOrElse { JSONArray() }
        return buildList {
            for (i in 0 until json.length()) ProviderProfile.fromJson(json.optJSONObject(i))?.let(::add)
        }
    }

    private fun persistProfiles() {
        val json = JSONArray()
        _profiles.value.forEach { json.put(it.toJson()) }
        vault.put("provider_profiles", json.toString())
    }

    private fun loadSettings() = AppSettings(
        autoSendVoice = settingsPrefs.getBoolean("auto_send_voice", false),
        reducedMotion = settingsPrefs.getBoolean("reduced_motion", false),
        speakReplies = settingsPrefs.getBoolean("speak_replies", true)
    )

    private fun titleFromPrompt(prompt: String): String {
        val title = prompt.replace(Regex("\\s+"), " ").trim()
        return if (title.length <= 52) title else title.take(49) + "…"
    }

    private fun projectNameFromPrompt(prompt: String): String = prompt
        .replace(Regex("(?i)create|build|make|generate|website|android|app|project"), "")
        .trim().replace(Regex("\\s+"), " ").take(42).ifBlank { "Nexora ${_mode.value.label} project" }

    private fun systemPrompt(mode: WorkspaceMode): String = """
        You are NEXORA AI, a concise but capable personal AI workspace assistant.
        Respect the user's language for normal explanations; keep source code and filenames in English.
        Never claim an APK, website deployment, GitHub push, payment, or external action happened unless the app explicitly reports it.
        Current workspace mode: ${mode.label}.
        In Website, Code, or Android app mode, produce complete usable code, not a vague outline. For multi-file output, put a marker before each block such as <!-- FILE: index.html -->, // FILE: MainActivity.kt, or # FILE: README.md, followed by a fenced code block. Avoid secrets, hard-coded API keys, and unsafe permissions.
        For Android mode, explain that this app can generate source and export a ZIP; compiling an APK requires a build runner such as GitHub Actions.
    """.trimIndent()
}
