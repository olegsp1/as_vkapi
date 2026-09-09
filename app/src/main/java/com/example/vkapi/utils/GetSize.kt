package com.example.vkapi.utils

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun getAppMediaTotalSize(context: Context): Long = withContext(Dispatchers.IO) {
    var totalSize = 0L
    val resolver = context.contentResolver

    val collections = listOf(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI to MediaStore.Video.Media.SIZE,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI to MediaStore.Images.Media.SIZE,
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI to MediaStore.Audio.Media.SIZE
    )

    for ((collectionUri, sizeColumn) in collections) {
        val projection = arrayOf(sizeColumn)

        val selection: String
        val selectionArgs: Array<String>

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selection = "${MediaStore.MediaColumns.OWNER_PACKAGE_NAME} = ?"
            selectionArgs = arrayOf(context.packageName)
        } else {
            selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
            selectionArgs = arrayOf("%vk-api-download%")
        }

        resolver.query(collectionUri, projection, selection, selectionArgs, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndexOrThrow(sizeColumn)
            while (cursor.moveToNext()) {
                totalSize += cursor.getLong(sizeIndex)
            }
        }
    }

    totalSize
}