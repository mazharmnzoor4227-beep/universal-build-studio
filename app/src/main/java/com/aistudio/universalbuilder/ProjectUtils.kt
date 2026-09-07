package com.aistudio.universalbuilder

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

fun readProjectFileName(
    context: Context,
    uri: Uri
): String {

    context.contentResolver
        .query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )
        ?.use { cursor ->

            if (cursor.moveToFirst()) {

                val index =
                    cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                    )

                if (index >= 0) {
                    return cursor.getString(index)
                }
            }
        }

    return uri.lastPathSegment
        ?: "Imported Project"
}

fun detectProjectType(
    fileName: String
): String {

    val name = fileName.lowercase()

    return when {

        name.endsWith(".html") ||
        name.endsWith(".htm") ->
            "Web / HTML Project"

        name.contains("godot") ->
            "Godot Project"

        name.endsWith(".zip") &&
        (
            name.contains("android") ||
            name.contains("studio")
        ) ->
            "Android / Gradle Project"

        name.endsWith(".zip") ->
            "ZIP Project"

        else ->
            "Unknown Project"
    }
}
