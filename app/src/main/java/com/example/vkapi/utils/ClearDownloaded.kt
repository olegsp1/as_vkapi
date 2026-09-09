package com.example.vkapi.utils

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun clearAppDownloadedVideos(context: Context) = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(MediaStore.Video.Media._ID)

    val selection: String
    val selectionArgs: Array<String>

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        selection = "${MediaStore.Video.Media.OWNER_PACKAGE_NAME} = ?"
        selectionArgs = arrayOf(context.packageName)
    } else {
        selection = "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?"
        selectionArgs = arrayOf("Movies/MyAppDownloads%")
    }

    resolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val itemUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI.buildUpon()
                .appendPath(id.toString())
                .build()
            resolver.delete(itemUri, null, null)
        }
    }
}