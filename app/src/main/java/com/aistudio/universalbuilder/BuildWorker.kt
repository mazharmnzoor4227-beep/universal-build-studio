package com.aistudio.universalbuilder

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters

class BuildWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParams
) {

    override suspend fun doWork(): Result {

        return try {

            val appName =
                inputData.getString(KEY_APP_NAME)
                    ?: return Result.failure(
                        errorData("App name missing")
                    )

            val packageName =
                inputData.getString(KEY_PACKAGE_NAME)
                    ?: return Result.failure(
                        errorData("Package ID missing")
                    )

            val projectName =
                inputData.getString(KEY_PROJECT_NAME)
                    ?: "Imported Project"

            val uriText =
                inputData.getString(KEY_PROJECT_URI)
                    ?: return Result.failure(
                        errorData("Project file missing")
                    )

            val projectUri =
                Uri.parse(uriText)

            setProgress(
                Data.Builder()
                    .putString(
                        KEY_STATUS,
                        "Starting build..."
                    )
                    .build()
            )

            val controller =
                BuildController(
                    applicationContext
                )

            setProgress(
                Data.Builder()
                    .putString(
                        KEY_STATUS,
                        "Uploading project and building APK..."
                    )
                    .build()
            )

            val buildResult =
                controller.startBuild(
                    BuildRequest(
                        appName = appName,
                        packageName = packageName,
                        projectUri = projectUri,
                        projectName = projectName
                    )
                )

            if (
                buildResult.success &&
                buildResult.apkFile != null
            ) {

                Result.success(
                    Data.Builder()
                        .putString(
                            KEY_STATUS,
                            "APK READY"
                        )
                        .putString(
                            KEY_APK_PATH,
                            buildResult.apkFile.absolutePath
                        )
                        .build()
                )

            } else {

                Result.failure(
                    Data.Builder()
                        .putString(
                            KEY_STATUS,
                            buildResult.message
                        )
                        .build()
                )
            }

        } catch (e: Exception) {

            Result.failure(
                errorData(
                    e.message
                        ?: "Background build failed"
                )
            )
        }
    }

    private fun errorData(
        message: String
    ): Data {

        return Data.Builder()
            .putString(
                KEY_STATUS,
                message
            )
            .build()
    }

    companion object {

        const val KEY_APP_NAME =
            "app_name"

        const val KEY_PACKAGE_NAME =
            "package_name"

        const val KEY_PROJECT_NAME =
            "project_name"

        const val KEY_PROJECT_URI =
            "project_uri"

        const val KEY_STATUS =
            "build_status"

        const val KEY_APK_PATH =
            "apk_path"
    }
}
