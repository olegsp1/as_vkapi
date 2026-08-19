package com.example.vkapi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vkapi.databinding.ItemPostBinding
import java.time.format.DateTimeFormatter
import java.util.Locale

class PostAdapter(
    initialData: List<Post>,
    private val onDownloadClick: (List<MediaItem>, Int) -> Unit,
    private val onMediaClick: (List<MediaItem>, Int) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {
    private val posts = initialData.toMutableList()

    inner class PostViewHolder(val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.binding.tvText.text = post.text
        holder.binding.date.text = post.date
        holder.binding.comment.text = "Комментарии: " + post.comment.toString()

        if (post.mediaList.isEmpty()) {
            holder.binding.rvMedia.visibility = View.GONE
        } else {
            holder.binding.rvMedia.visibility = View.VISIBLE
            holder.binding.rvMedia.apply {
                layoutManager = LinearLayoutManager(
                    context, LinearLayoutManager.HORIZONTAL, false
                )
                holder.binding.rvMedia.adapter = MediaAdapter(post.mediaList, onDownloadClick, onMediaClick)
                setHasFixedSize(true)
            }
        }
    }

    fun updateData(new: List<Post>) {
        posts.clear()
        posts.addAll(new)
        notifyDataSetChanged()
    }

    override fun getItemCount() = posts.size
}