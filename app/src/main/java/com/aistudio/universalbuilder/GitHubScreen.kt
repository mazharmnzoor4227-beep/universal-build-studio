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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GitHubScreen(
    onBack: () -> Unit
) {

    var username by remember { mutableStateOf("") }
    var repository by remember {
        mutableStateOf("universal-app-builds")
    }
    var token by remember { mutableStateOf("") }
    var status by remember {
        mutableStateOf("Not configured")
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
            "GITHUB BUILDER",
            color = Color.White,
            fontSize = 25.sp,
            fontWeight = FontWeight.Black
        )

        Text(
            "Connect the build engine",
            color = Color(0xFF8D97A7),
            fontSize = 13.sp
        )

        Spacer(Modifier.height(22.dp))

        GitHubCard {

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
                visualTransformation =
                    PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = {
                    status =
                        if (
                            username.isNotBlank() &&
                            repository.isNotBlank() &&
                            token.isNotBlank()
                        ) {
                            "Configuration ready"
                        } else {
                            "Fill all fields"
                        }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("TEST CONFIGURATION")
            }
        }

        Spacer(Modifier.height(14.dp))

        GitHubCard {

            Text(
                "STATUS",
                color = Color(0xFFA58BFF),
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                status,
                color = Color.White
            )
        }

        Spacer(Modifier.height(14.dp))

        GitHubCard {

            Text(
                "IMPORTANT",
                color = Color(0xFFA58BFF),
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Never place your GitHub token inside your public repository. The token will later be stored locally on this phone.",
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
private fun GitHubCard(
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
