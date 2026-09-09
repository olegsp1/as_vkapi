package com.example.vkapi.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.vkapi.models.MediaItem
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

suspend fun fileDownload(context: Context, item: MediaItem, downloadDir: File) {
    val resultPath = withContext(Dispatchers.IO) {
        try {
            val dateStamp = SimpleDateFormat("yyyy-MM-dd-hh-mm-ss", Locale.getDefault())
                .format(Date())

            val request = YoutubeDLRequest(item.uri).apply {
                addOption("-o", downloadDir.absolutePath + "/%(title)s_${dateStamp}.%(ext)s")
                // ограничиваем формат, чтобы результат был одним готовым mp4 файлом
                addOption("-f", "best[ext=mp4]/best")
            }

            YoutubeDL.getInstance().execute(request)

            val downloadedFile = downloadDir.listFiles()
                ?.filter { it.name.contains(dateStamp) }
                ?.maxByOrNull { it.lastModified() }
                ?: return@withContext null

            val resultUri = copyFileToGallery(context, downloadedFile)
            downloadedFile.delete()
            resultUri.toString()
        } catch (e: YoutubeDLException) {
            Log.e("dlp", "Ошибка скачивания ${item.uri}", e)
            null
        }
    }

    if (resultPath != null) {
        item.path = resultPath
        Toast.makeText(context, "Видео скачанно ${item.uri}", Toast.LENGTH_SHORT).show()
        Log.d("yt-dlp", "download ${item}")
    } else {
        Toast.makeText(context, "Ошибка загрузки видео", Toast.LENGTH_SHORT).show()
    }
}