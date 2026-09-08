package com.example.vkapi.rvAdapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.vkapi.R
import com.example.vkapi.models.Comment
import com.example.vkapi.models.MediaItem

class CommentsAdapter(initialData: List<Comment>,
                      private val onDownloadClick: (List<MediaItem>, Int) -> Unit,
                      private val onMediaClick: (List<MediaItem>, Int) -> Unit,
                      var context: Context
) :  RecyclerView.Adapter<CommentsAdapter.CommentsViewHolder>() {
    private val comments = initialData.toMutableList()
    class CommentsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ans: TextView = view.findViewById(R.id.answerTo)
        val ansP: View = view.findViewById(R.id.view3)
        val name: TextView = view.findViewById(R.id.commentName)
        val text: TextView = view.findViewById(R.id.commentText)
        val date: TextView = view.findViewById(R.id.commentDate)
        val avatar: ImageView = view.findViewById(R.id.commentAvatar)
        val media: RecyclerView = view.findViewById(R.id.crvMedia)
        val mediaCount: TextView = view.findViewById(R.id.attachedFiles)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentsViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentsViewHolder, position: Int) {
        val comment = comments[position]
        holder.name.text = comment.name
        holder.date.text = comment.date

        if (comment.text != "") {
            holder.text.visibility = View.VISIBLE
            holder.text.text = comment.text
        }
        else {
            holder.text.visibility = View.GONE
        }

        if (comment.answerTo != "") {
            holder.ans.visibility = View.VISIBLE
            holder.ansP.visibility = View.VISIBLE
            holder.ans.text = comment.answerTo
        }
        else {
            holder.ans.visibility = View.GONE
            holder.ansP.visibility = View.GONE
        }

        if (comment.avatar != "") {
            holder.avatar.load(comment.avatar)
        }
        else {
            holder.avatar.setImageResource(R.drawable.user)
        }

        if (comment.mediaList.isEmpty()) {
            holder.media.visibility = View.GONE
            holder.mediaCount.visibility = View.GONE
        } else {
            holder.mediaCount.visibility = View.VISIBLE
            holder.mediaCount.text = "${comment.mediaList.size} файлов"
            holder.media.visibility = View.VISIBLE
            holder.media.apply {
                layoutManager = LinearLayoutManager(
                    context, LinearLayoutManager.HORIZONTAL, false
                )
                holder.media.adapter =
                    MediaAdapter(comment.mediaList, onDownloadClick, onMediaClick)
                setHasFixedSize(true)
            }
        }
    }

    override fun getItemCount(): Int = comments.size

    fun updateData(new: List<Comment>) {
        comments.clear()
        comments.addAll(new)
        notifyDataSetChanged()
    }
}