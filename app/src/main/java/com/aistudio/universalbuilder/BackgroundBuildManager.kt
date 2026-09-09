package com.aistudio.universalbuilder

import android.content.Context
import androidx.work.*
import java.util.UUID

object BackgroundBuildManager {

    const val WORK_NAME =
        "universal_apk_build"

    private const val PREFS =
        "background_build"

    private const val KEY_WORK_ID =
        "work_id"

    @Synchronized fun startBuild(
        context: Context,
        appName: String,
        packageName: String,
        projectName: String,
        projectUri: String,
        iconUri: String?
    ): UUID {

        val existing = WorkManager.getInstance(context).getWorkInfosForUniqueWork(WORK_NAME).get().firstOrNull { !it.state.isFinished }
        if (existing != null) { saveWorkId(context, existing.id); return existing.id }
        val input =
            workDataOf(
                BuildWorker.KEY_APP_NAME to appName,
                BuildWorker.KEY_PACKAGE_NAME to packageName,
                BuildWorker.KEY_PROJECT_NAME to projectName,
                BuildWorker.KEY_PROJECT_URI to projectUri,
                BuildWorker.KEY_ICON_URI to (iconUri ?: "")
            )

        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED
                )
                .build()

        val request =
            OneTimeWorkRequestBuilder<BuildWorker>()
                .setInputData(input)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 15, java.util.concurrent.TimeUnit.SECONDS)
                .build()

        WorkManager
            .getInstance(context)
            .enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )

        saveWorkId(
            context,
            request.id
        )

        return request.id
    }

    fun resume(context: Context, requestId: String) {
        val records = BuildRecords(context)
        val record = records.get(requestId) ?: return
        val manager = WorkManager.getInstance(context)
        if (manager.getWorkInfosForUniqueWork(WORK_NAME).get().any { !it.state.isFinished }) return
        record.put("monitoringStarted", System.currentTimeMillis())
        records.save(requestId, record)
        val input = workDataOf("request_id" to requestId,
            BuildWorker.KEY_APP_NAME to record.optString("name"),
            BuildWorker.KEY_PACKAGE_NAME to record.optString("package"),
            BuildWorker.KEY_PROJECT_NAME to record.optString("projectName"),
            BuildWorker.KEY_PROJECT_URI to record.optString("uri"),
            BuildWorker.KEY_ICON_URI to record.optString("icon"))
        val request = OneTimeWorkRequestBuilder<BuildWorker>().setInputData(input)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.LINEAR, 15, java.util.concurrent.TimeUnit.SECONDS).build()
        manager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
        saveWorkId(context, request.id)
    }

    fun getWorkManager(
        context: Context
    ): WorkManager {

        return WorkManager
            .getInstance(context)
    }

    fun savedWorkId(
        context: Context
    ): UUID? {

        val value =
            context
                .getSharedPreferences(
                    PREFS,
                    Context.MODE_PRIVATE
                )
                .getString(
                    KEY_WORK_ID,
                    null
                )
                ?: return null

        return try {
            UUID.fromString(value)
        } catch (_: Exception) {
            null
        }
    }

    private fun saveWorkId(
        context: Context,
        id: UUID
    ) {

        context
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                KEY_WORK_ID,
                id.toString()
            )
            .apply()
    }
}
