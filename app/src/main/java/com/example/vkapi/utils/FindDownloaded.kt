package com.example.vkapi.utils

import com.example.vkapi.models.MediaItem
import java.io.File

fun findDownloadedFile(dir: File, item: MediaItem): String? {
    // Простой поиск самого свежего файла в папке — для прод-кода лучше сверять по shortTitle/ID из VideoInfo
    return dir.listFiles()
        ?.filter { it.isFile }
        ?.maxByOrNull { it.lastModified() }
        ?.absolutePath
}