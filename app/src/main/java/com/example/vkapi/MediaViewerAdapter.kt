package com.example.vkapi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.vkapi.databinding.ItemMediaFullscreenBinding

class MediaViewerAdapter(
    private val items: List<MediaItem>,
    private val lifecycleOwner: LifecycleOwner
) : RecyclerView.Adapter<MediaViewerAdapter.ViewHolder>() {

    // Храним ссылки на все активные ViewHolder'ы с плеерами
    private val activeHolders = mutableSetOf<ViewHolder>()

    init {
        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                releaseAllPlayers()
                lifecycleOwner.lifecycle.removeObserver(this)
            }
        })
    }

    inner class ViewHolder(val binding: ItemMediaFullscreenBinding) :
        RecyclerView.ViewHolder(binding.root) {
        var player: ExoPlayer? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMediaFullscreenBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        if (item.type == MediaType.PHOTO) {
            holder.binding.photoView.visibility = View.VISIBLE
            holder.binding.playerView.visibility = View.GONE

            Glide.with(holder.itemView)
                .load(item.uri)
                .into(holder.binding.photoView)

        } else {
            holder.binding.photoView.visibility = View.GONE
            holder.binding.playerView.visibility = View.VISIBLE

            val player = ExoPlayer.Builder(holder.itemView.context).build()
            holder.binding.playerView.player = player
            player.setMediaItem(androidx.media3.common.MediaItem.fromUri(item.path))
            player.playWhenReady = true
            player.prepare()
            holder.player = player

            activeHolders.add(holder) // регистрируем как активный
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.player?.release()
        holder.player = null
        activeHolders.remove(holder)
        super.onViewRecycled(holder)
    }

    private fun releaseAllPlayers() {
        activeHolders.forEach { holder ->
            holder.player?.release()
            holder.player = null
        }
        activeHolders.clear()
    }

    override fun getItemCount() = items.size
}