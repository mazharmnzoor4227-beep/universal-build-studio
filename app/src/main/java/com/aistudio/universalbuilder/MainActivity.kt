package com.aistudio.universalbuilder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContent {
            UniversalBuildStudioApp()
        }
    }
}

@Composable
fun UniversalBuildStudioApp() {

    val context =
        androidx.compose.ui.platform.LocalContext.current

    val sessionStore =
        remember {
            BuilderSessionStore(context)
        }

    val savedSession =
        remember {
            sessionStore.load()
        }

    var selectedTab by remember {
        mutableStateOf("Builder")
    }

    var appName by remember {
        mutableStateOf(
            savedSession.appName
        )
    }

    var packageName by remember {
        mutableStateOf(
            savedSession.packageName
        )
    }

    var projectName by remember {
        mutableStateOf(
            savedSession.projectName
        )
    }

    var iconSelected by remember {
        mutableStateOf(
            sessionStore.load().iconUri
                .isNotBlank()
        )
    }

    MaterialTheme(
        colorScheme =
            darkColorScheme()
    ) {

        when (selectedTab) {

            "Builder" -> {

                BuilderScreen(
                    appName = appName,
                    packageName =
                        packageName,
                    projectName =
                        projectName,
                    iconSelected =
                        iconSelected,

                    onAppNameChange = {

                        appName = it

                        sessionStore.save(
                            appName =
                                appName,
                            packageName =
                                packageName,
                            projectName =
                                projectName,
                            projectUri =
                                sessionStore.load().projectUri,
                            iconUri =
                                sessionStore.load().iconUri
                        )
                    },

                    onPackageChange = {

                        packageName = it

                        sessionStore.save(
                            appName =
                                appName,
                            packageName =
                                packageName,
                            projectName =
                                projectName,
                            projectUri =
                                sessionStore.load().projectUri,
                            iconUri =
                                sessionStore.load().iconUri
                        )
                    },

                    onProjectSelected = {

                        projectName = it

                        sessionStore.save(
                            appName =
                                appName,
                            packageName =
                                packageName,
                            projectName =
                                projectName,
                            projectUri =
                                sessionStore.load().projectUri,
                            iconUri =
                                sessionStore.load().iconUri
                        )
                    },

                    onIconSelected = {

                        iconSelected =
                            true
                    },

                    onOpenGitHub = {

                        selectedTab =
                            "GitHub"
                    },

                    onOpenHistory = {

                        selectedTab =
                            "History"
                    }
                )
            }

            "GitHub" -> {

                GitHubScreen(
                    onBack = {

                        selectedTab =
                            "Builder"
                    }
                )
            }

            "History" -> {

                HistoryScreen(
                    onBack = {

                        selectedTab =
                            "Builder"
                    }
                )
            }
        }
    }
}
