package com.example.vkapi

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MediaItem(
    val uri: String,
    val type: MediaType,
    var path: String = "",
    var thumbnail: String? = null
) : Parcelable
enum class MediaType { PHOTO, VIDEO }

data class Post(
    val id: Long,
    val mediaList: List<MediaItem>,
    val text: String,
    val date: String,
    val comment: Long
)