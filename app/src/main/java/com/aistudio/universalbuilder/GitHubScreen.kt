package com.aistudio.universalbuilder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun GitHubScreen(
    onBack: () -> Unit
) {

    val context = LocalContext.current
    val store = remember {
        GitHubConfigStore(context)
    }

    val scope = rememberCoroutineScope()

    val savedConfig = remember {
        store.load()
    }

    var username by remember {
        mutableStateOf(savedConfig.username)
    }

    var repository by remember {
        mutableStateOf(savedConfig.repository)
    }

    var token by remember {
        mutableStateOf(savedConfig.token)
    }

    var status by remember {
        mutableStateOf(
            if (
                savedConfig.username.isNotBlank() &&
                savedConfig.repository.isNotBlank() &&
                savedConfig.token.isNotBlank()
            ) {
                "Configuration saved"
            } else {
                "Not configured"
            }
        )
    }

    var isTesting by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(18.dp)
    ) {

        StudioHeader("Workspace settings", "Connect once. Build whenever you’re ready.")
        SettingsCard {

            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                },
                label = {
                    Text("GitHub Username")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(
                Modifier.height(12.dp)
            )

            OutlinedTextField(
                value = repository,
                onValueChange = {
                    repository = it
                },
                label = {
                    Text("Builder Repository")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(
                Modifier.height(12.dp)
            )

            OutlinedTextField(
                value = token,
                onValueChange = {
                    token = it
                },
                label = {
                    Text("Personal Access Token")
                },
                visualTransformation =
                    PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(
                Modifier.height(16.dp)
            )

            Button(
                onClick = {

                    if (
                        username.isBlank() ||
                        repository.isBlank() ||
                        token.isBlank()
                    ) {

                        status =
                            "Fill all fields first"

                    } else {

                        store.save(
                            username.trim(),
                            repository.trim(),
                            token.trim()
                        )

                        status =
                            "Configuration saved"
                    }
                },
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(
                    "Save settings",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                Modifier.height(10.dp)
            )

            Button(
                onClick = {

                    if (
                        username.isBlank() ||
                        repository.isBlank() ||
                        token.isBlank()
                    ) {

                        status =
                            "Fill all fields first"

                        return@Button
                    }

                    isTesting = true
                    status = "Connecting to GitHub..."

                    scope.launch {

                        val result =
                            GitHubApiClient.testConnection(
                                username =
                                    username.trim(),
                                repository =
                                    repository.trim(),
                                token =
                                    token.trim()
                            )

                        isTesting = false

                        status =
                            if (result.isSuccess) {

                                store.save(
                                    username.trim(),
                                    repository.trim(),
                                    token.trim()
                                )

                                "✓ GitHub connected"

                            } else {

                                "Connection failed: ${
                                    result.exceptionOrNull()
                                        ?.message
                                        ?: "Unknown error"
                                }"
                            }
                    }
                },
                enabled = !isTesting,
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                if (isTesting) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )

                    Spacer(
                        Modifier.width(10.dp)
                    )
                }

                Text(
                    if (isTesting)
                        "Checking connection…"
                    else
                        "Test connection"
                )
            }

            Spacer(
                Modifier.height(10.dp)
            )

            OutlinedButton(
                onClick = {

                    store.clear()

                    username = ""
                    repository =
                        "universal-app-builds"
                    token = ""

                    status =
                        "Configuration cleared"
                },
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(
                    "Disconnect account"
                )
            }
        }

        Spacer(
            Modifier.height(14.dp)
        )

        SettingsCard {

            Text(
                text = "Connection status",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                Modifier.height(8.dp)
            )

            Text(
                text = status,
                color =
                    if (
                        status.contains(
                            "connected",
                            ignoreCase = true
                        )
                    ) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                fontSize = 14.sp
            )
        }

        Spacer(
            Modifier.height(14.dp)
        )

        SettingsCard {

            Text(
                text = "Stored on your phone",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                Modifier.height(8.dp)
            )

            Text(
                text =
                    "Your GitHub token is stored locally on this phone. Never paste it into your public repository or share it with anyone.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }

        Spacer(
            Modifier.height(20.dp)
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(20.dp), content = content)
    }
}
