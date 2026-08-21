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
        if (post.text != "") {
            holder.binding.tvText.visibility = View.VISIBLE
            holder.binding.tvText.text = post.text
        }
        else {
            holder.binding.tvText.visibility = View.GONE
        }
        holder.binding.date.text = "${post.date }   ${post.dateCmp}"
        holder.binding.comment.text = "Комментарии: ${post.comment}"

        if (post.mediaList.size < 2) {
            holder.binding.mediaCount.visibility = View.GONE
        }
        else {
            holder.binding.mediaCount.visibility = View.VISIBLE
            holder.binding.mediaCount.text = "${post.mediaList.size} файлов"
        }

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