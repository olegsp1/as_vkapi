package com.example.vkapi

data class Comment(val avatar: String, val name: String, val text: String, val date: String, val vkId: Int, val answerTo: String, val mediaList: List<MediaItem>)
