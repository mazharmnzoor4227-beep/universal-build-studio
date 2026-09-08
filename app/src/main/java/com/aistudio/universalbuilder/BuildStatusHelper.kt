package com.aistudio.universalbuilder

import android.content.Context
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.io.File

data class BackgroundBuildState(
    val isBuilding: Boolean,
    val status: String,
    val apkFile: File? = null
)

object BuildStatusHelper {

    suspend fun getCurrentState(
        context: Context
    ): BackgroundBuildState {

        val workId =
            BackgroundBuildManager
                .savedWorkId(context)
                ?: return BackgroundBuildState(
                    isBuilding = false,
                    status = "Ready"
                )

        return try {

            val workInfo =
                WorkManager
                    .getInstance(context)
                    .getWorkInfoById(workId)
                    .get()

            if (workInfo == null) {

                return BackgroundBuildState(
                    isBuilding = false,
                    status = "Ready"
                )
            }

            when (workInfo.state) {

                WorkInfo.State.ENQUEUED,
                WorkInfo.State.BLOCKED -> {

                    BackgroundBuildState(
                        isBuilding = true,
                        status = BuildRecords(context).get(workId.toString())?.optString("status") ?: "Build queued..."
                    )
                }

                WorkInfo.State.RUNNING -> {

                    val message =
                        workInfo.progress
                            .getString(
                                BuildWorker.KEY_STATUS
                            )
                            ?: "BUILDING APK..."

                    BackgroundBuildState(
                        isBuilding = true,
                        status = message
                    )
                }

                WorkInfo.State.SUCCEEDED -> {

                    val path =
                        workInfo.outputData
                            .getString(
                                BuildWorker.KEY_APK_PATH
                            )

                    val apk =
                        path
                            ?.let { File(it) }
                            ?.takeIf {
                                it.exists()
                            }

                    BackgroundBuildState(
                        isBuilding = false,
                        status =
                            if (apk != null)
                                "APK READY ✓"
                            else
                                "Build completed",
                        apkFile = apk
                    )
                }

                WorkInfo.State.FAILED -> {

                    val message =
                        workInfo.outputData
                            .getString(
                                BuildWorker.KEY_STATUS
                            )
                            ?: "BUILD FAILED"

                    BackgroundBuildState(
                        isBuilding = false,
                        status = message
                    )
                }

                WorkInfo.State.CANCELLED -> {

                    BackgroundBuildState(
                        isBuilding = false,
                        status = "Build cancelled"
                    )
                }
            }

        } catch (e: Exception) {

            BackgroundBuildState(
                isBuilding = false,
                status =
                    e.message
                        ?: "Unable to read build status"
            )
        }
    }
}
