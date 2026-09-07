package com.aistudio.universalbuilder

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class GitHubConfig(
    val username: String,
    val repository: String,
    val token: String
)

class GitHubConfigStore(
    context: Context
) {

    private val masterKey =
        MasterKey.Builder(context)
            .setKeyScheme(
                MasterKey.KeyScheme.AES256_GCM
            )
            .build()

    private val prefs =
        EncryptedSharedPreferences.create(
            context,
            "github_secure_config",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    fun save(
        username: String,
        repository: String,
        token: String
    ) {

        prefs.edit()
            .putString("username", username)
            .putString("repository", repository)
            .putString("token", token)
            .apply()
    }

    fun load(): GitHubConfig {

        return GitHubConfig(
            username =
                prefs.getString(
                    "username",
                    ""
                ) ?: "",

            repository =
                prefs.getString(
                    "repository",
                    "universal-app-builds"
                ) ?: "universal-app-builds",

            token =
                prefs.getString(
                    "token",
                    ""
                ) ?: ""
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
