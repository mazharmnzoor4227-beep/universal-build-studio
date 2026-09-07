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

@Composable
fun GitHubScreen(
    onBack: () -> Unit
) {

    val context = LocalContext.current
    val store = remember { GitHubConfigStore(context) }

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
                savedConfig.token.isNotBlank()
            ) {
                "Saved on this phone"
            } else {
                "Not configured"
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07090E))
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {

        Spacer(Modifier.height(24.dp))

        Text(
            text = "GITHUB BUILDER",
            color = Color.White,
            fontSize = 25.sp,
            fontWeight = FontWeight.Black
        )

        Text(
            text = "Connect the real APK build engine",
            color = Color(0xFF8D97A7),
            fontSize = 13.sp
        )

        Spacer(Modifier.height(22.dp))

        SettingsCard {

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("GitHub Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = repository,
                onValueChange = { repository = it },
                label = { Text("Builder Repository") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Personal Access Token") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {

                    if (
                        username.isBlank() ||
                        repository.isBlank() ||
                        token.isBlank()
                    ) {

                        status = "Fill all fields"

                    } else {

                        store.save(
                            username = username.trim(),
                            repository = repository.trim(),
                            token = token.trim()
                        )

                        status = "Saved securely on this phone"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "SAVE CONFIGURATION",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = {

                    store.clear()

                    username = ""
                    repository = "universal-app-builds"
                    token = ""

                    status = "Configuration cleared"
                },
                modifier = Modifier.fillMaxWidth()
            ) {

                Text("CLEAR SAVED CONFIG")
            }
        }

        Spacer(Modifier.height(14.dp))

        SettingsCard {

            Text(
                text = "STATUS",
                color = Color(0xFFA58BFF),
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = status,
                color = Color.White
            )
        }

        Spacer(Modifier.height(14.dp))

        SettingsCard {

            Text(
                text = "SECURITY",
                color = Color(0xFFA58BFF),
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text =
                    "Your token is stored locally on this phone. Never paste the token into your public GitHub repository.",
                color = Color(0xFF9AA4B5),
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(20.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {

            Text("BACK TO BUILDER")
        }

        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun SettingsCard(
    content: @Composable ColumnScope.() -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFF11151D),
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp),
        content = content
    )
}
