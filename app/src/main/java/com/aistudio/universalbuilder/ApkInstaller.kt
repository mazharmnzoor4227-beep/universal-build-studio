package com.aistudio.universalbuilder

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object ApkInstaller {

    fun install(
        context: Context,
        apkFile: File
    ) {
        try {

            val apkUri =
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )

            val intent =
                Intent(Intent.ACTION_VIEW).apply {

                    setDataAndType(
                        apkUri,
                        "application/vnd.android.package-archive"
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }

            context.startActivity(intent)

        } catch (e: Exception) {
            throw Exception(
                "Unable to open APK installer: ${e.message}"
            )
        }
    }
}
