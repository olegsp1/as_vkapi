package com.example.vkapi

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.vkapi.databinding.ActivityMediaViewerBinding

class MediaViewerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMediaViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Полноэкранный режим (скрыть статус-бар)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val mediaList = intent.getParcelableArrayListExtra<MediaItem>("mediaList") ?: arrayListOf()
        val startPosition = intent.getIntExtra("startPosition", 0)

        binding.viewPager.adapter = MediaViewerAdapter(mediaList, this)
        binding.viewPager.setCurrentItem(startPosition, false)
    }
}