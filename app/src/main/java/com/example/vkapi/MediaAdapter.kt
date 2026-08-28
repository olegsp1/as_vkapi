package com.example.vkapi

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.vkapi.databinding.ItemMediaBinding

class MediaAdapter(
    private val items: List<MediaItem>,
    private val onDownloadClick: (List<MediaItem>, Int) -> Unit,
    private val onMediaClick: (List<MediaItem>, Int) -> Unit
) : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

    inner class MediaViewHolder(val binding: ItemMediaBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val item = items[position]
        holder.binding.ivPlayIcon.visibility =
            if (item.type == MediaType.VIDEO) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            if (items[position].path != "" || item.type == MediaType.PHOTO) {
                onMediaClick(items, position) // передаём весь список медиа этого поста + позицию
            }
            else {
                onDownloadClick(items, position)
            }
        }

        // Загрузка изображения/превью видео через Glide
        Log.d("glide", item.toString())
        Glide.with(holder.itemView)
            .load(if (item.type == MediaType.VIDEO) item.thumbnail else item.uri) // для видео — thumbnail
            .fitCenter()
            .into(holder.binding.ivMedia)

    }

    override fun getItemCount() = items.size
}