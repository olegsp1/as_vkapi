package com.example.vkapi.ui

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.vkapi.utils.DBHelper
import com.example.vkapi.R
import com.example.vkapi.models.AppSetting
import com.example.vkapi.utils.clearAppDownloadedVideos
import com.example.vkapi.utils.getAppMediaTotalSize
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.io.File

class AppSettings : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_app_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val db = DBHelper(this, null)

        val token: String = db.get_token()

        if (token == "") {
            Toast.makeText(this@AppSettings, "необходим токен", Toast.LENGTH_SHORT).show()
        }

        val token_input: TextInputEditText = findViewById(R.id.token_input_area)
        val save_button: Button = findViewById(R.id.save_settings_btn)
        val back_button: Button = findViewById(R.id.setting_back_button)
        val clear_btn: Button = findViewById(R.id.clear_media_btn)
        val stor_view: TextView = findViewById(R.id.storage_view)

        val downloadDir = File(
            getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "youtubedl-android"
        )

        lifecycleScope.launch {
            val size = downloadDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum() + getAppMediaTotalSize(this@AppSettings) / 1024 / 1024
            stor_view.text = "$size MB"
        }

        back_button.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        clear_btn.setOnClickListener {
            lifecycleScope.launch {
                if (downloadDir.exists() && downloadDir.isDirectory) {
                    downloadDir.listFiles()?.forEach { file ->
                        file.deleteRecursively() // удаляет каждый файл или подпапку внутри
                    }
                }
                clearAppDownloadedVideos(this@AppSettings)
                stor_view.text = "0 MB"
                Toast.makeText(this@AppSettings, "файлы удаленны", Toast.LENGTH_SHORT).show()
            }
        }

        save_button.setOnClickListener {
            val token = token_input.text.toString().trim()

            if (!token.isEmpty()) {
                val setting = AppSetting(skey = "token", svalue = token)
                val db = DBHelper(this@AppSettings, null)

                db.add_setting(setting)
                Toast.makeText(this@AppSettings, "настройки сохранены", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this@AppSettings, "поля пусты", Toast.LENGTH_SHORT).show()
            }
        }

        if (downloadDir.exists() && downloadDir.isDirectory) {
            val size = downloadDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum() / 1024 / 1024
            stor_view.text = "$size MB"
        }
    }
}