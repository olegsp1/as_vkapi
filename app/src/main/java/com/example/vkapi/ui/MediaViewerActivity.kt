package com.example.vkapi.ui

import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.vkapi.R
import com.example.vkapi.rvAdapters.MediaViewerAdapter
import com.example.vkapi.databinding.ActivityMediaViewerBinding
import com.example.vkapi.models.MediaItem
import com.example.vkapi.utils.downloadImageToGallery
import kotlinx.coroutines.launch

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

        binding.viewOpt.setOnClickListener { view ->
            showPopupMenu(view, mediaList)
        }
    }

    private fun showPopupMenu(view: View, items: List<MediaItem>) {
        val popup = PopupMenu(view.context, view)
        popup.menuInflater.inflate(R.menu.fullscreen_view_menu, popup.menu)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.optDownload -> {
                    items.forEach { item ->
                        lifecycleScope.launch {
                            downloadImageToGallery(view.context, item.uri, (10000..99999).random().toString())
                            Toast.makeText(this@MediaViewerActivity, "Картинка скачанна", Toast.LENGTH_SHORT).show()
                        }
                    }
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
}