package com.aistudio.universalbuilder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            UniversalBuildStudioApp()
        }
    }
}

@Composable
fun UniversalBuildStudioApp() {

    var selectedTab by remember {
        mutableStateOf("Builder")
    }

    var appName by remember {
        mutableStateOf("My App")
    }

    var packageName by remember {
        mutableStateOf("com.myapp.generated")
    }

    var projectName by remember {
        mutableStateOf("No project selected")
    }

    var iconSelected by remember {
        mutableStateOf(false)
    }

    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {

        when (selectedTab) {

            "Builder" -> BuilderScreen(
                appName = appName,
                packageName = packageName,
                projectName = projectName,
                iconSelected = iconSelected,

                onAppNameChange = {
                    appName = it
                },

                onPackageChange = {
                    packageName = it
                },

                onProjectSelected = {
                    projectName = it
                },

                onIconSelected = {
                    iconSelected = true
                },

                onOpenGitHub = {
                    selectedTab = "GitHub"
                },

                onOpenHistory = {
                    selectedTab = "History"
                }
            )

            "GitHub" -> GitHubScreen(
                onBack = {
                    selectedTab = "Builder"
                }
            )

            "History" -> HistoryScreen(
                onBack = {
                    selectedTab = "Builder"
                }
            )
        }
    }
}
