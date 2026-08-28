package com.example.vkapi

import android.app.Application
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException

class App : Application() {
    var token: String = ""
    var MANU: Boolean = false

    override fun onCreate() {
        super.onCreate()
        try {
            YoutubeDL.getInstance().init(this)
        } catch (e: YoutubeDLException) {
            Log.e("dlp", "Ошибка инициализации youtubedl-android", e)
        }
    }
}