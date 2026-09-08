package com.example.vkapi.utils

import com.example.vkapi.models.MediaItem
import com.example.vkapi.models.MediaType
import org.json.JSONArray
import org.json.JSONObject

suspend fun parseAttachment(att: JSONObject): MediaItem? {
    return when (att.optString("type")) {
        "photo" -> {
            val photo = att.optJSONObject("photo") ?: return null
            val uri = bestPhotoUrl(photo.optJSONArray("sizes")) ?: return null
            MediaItem(uri, MediaType.PHOTO)
        }
        "video" -> {
            val video = att.optJSONObject("video") ?: return null
            val ownerId = video.optLong("owner_id")
            val videoId = video.optLong("id")
            val vurl = "https://vk.ru/clip$ownerId" + "_$videoId"

            MediaItem(vurl, MediaType.VIDEO)
        }
        else -> null
    }
}

fun bestPhotoUrl(sizes: JSONArray?): String? {
    if (sizes == null) return null
    var bestUrl: String? = null
    var bestArea = -1
    for (k in 0 until sizes.length()) {
        val size = sizes.getJSONObject(k)
        val w = size.optInt("width", 0)
        val h = size.optInt("height", 0)
        val url = size.optString("url", null) ?: continue
        val area = w * h
        if (area > bestArea) {
            bestArea = area
            bestUrl = url
        }
    }
    return bestUrl
}