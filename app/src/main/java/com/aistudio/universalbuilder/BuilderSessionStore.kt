package com.aistudio.universalbuilder

import android.content.Context

data class BuilderSession(
    val appName: String,
    val packageName: String,
    val projectName: String,
    val projectUri: String,
    val iconUri: String
)

class BuilderSessionStore(
    context: Context
) {

    private val prefs =
        context.getSharedPreferences(
            "builder_session",
            Context.MODE_PRIVATE
        )

    fun save(
        appName: String,
        packageName: String,
        projectName: String,
        projectUri: String?,
        iconUri: String?
    ) {

        prefs.edit()
            .putString(
                KEY_APP_NAME,
                appName
            )
            .putString(
                KEY_PACKAGE_NAME,
                packageName
            )
            .putString(
                KEY_PROJECT_NAME,
                projectName
            )
            .putString(
                KEY_PROJECT_URI,
                projectUri ?: ""
            )
            .putString(
                KEY_ICON_URI,
                iconUri ?: ""
            )
            .apply()
    }

    fun load(): BuilderSession {

        return BuilderSession(
            appName =
                prefs.getString(
                    KEY_APP_NAME,
                    "My App"
                ) ?: "My App",

            packageName =
                prefs.getString(
                    KEY_PACKAGE_NAME,
                    "com.myapp.generated"
                ) ?: "com.myapp.generated",

            projectName =
                prefs.getString(
                    KEY_PROJECT_NAME,
                    "No project selected"
                ) ?: "No project selected",

            projectUri =
                prefs.getString(
                    KEY_PROJECT_URI,
                    ""
                ) ?: "",

            iconUri =
                prefs.getString(
                    KEY_ICON_URI,
                    ""
                ) ?: ""
        )
    }

    fun clear() {

        prefs.edit()
            .clear()
            .apply()
    }

    companion object {

        private const val KEY_APP_NAME =
            "app_name"

        private const val KEY_PACKAGE_NAME =
            "package_name"

        private const val KEY_PROJECT_NAME =
            "project_name"

        private const val KEY_PROJECT_URI =
            "project_uri"

        private const val KEY_ICON_URI =
            "icon_uri"
    }
}
