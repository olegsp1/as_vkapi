package com.example.vkapi.ui

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vkapi.App
import com.example.vkapi.rvAdapters.CommentsAdapter
import com.example.vkapi.R
import com.example.vkapi.models.Comment
import com.example.vkapi.models.MediaItem
import com.example.vkapi.models.MediaType
import com.example.vkapi.models.Post
import com.example.vkapi.utils.findDownloadedFile
import com.example.vkapi.utils.parseAttachment
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

suspend fun getComments(ownerId: String, postId: String, token: String): String = withContext(Dispatchers.IO) {
    val url = "https://api.vk.com/method/wall.getComments" +
            "?owner_id=$ownerId" +
            "&post_id=$postId" +
            "&thread_items_count=10" +
            "&access_token=$token" +
            "&v=5.199"

    val request = Request.Builder()
        .url(url)
        .build()

    client.newCall(request).execute().use { response ->
        response.body?.string() ?: ""
    }
}

suspend fun parseComments(json: String): List<Comment> {
    val comments = mutableListOf<Comment>()
    val root = JSONObject(json)
    if (root.optString("error").isEmpty()) {
        val items = root.getJSONObject("response").getJSONArray("items")
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            val text = item.optString("text", "")
            val userId = item.optString("from_id", "error")
            val id = item.optInt("id", 0)
            val sdf = SimpleDateFormat("HH:mm:ss  d MMMM yyyy", Locale("ru"))
            val rawDate = item.getLong("date")
            val date = sdf.format(Date(rawDate * 1000))
            val secUtc: Long = System.currentTimeMillis() / 1000
            val cmp = secUtc - rawDate
            var dateCmp = ""
            when (cmp) {
                in 1..3600 -> dateCmp = "${cmp / 60} минут назад"
                in 3600..7200 -> dateCmp = "1 час назад"
                in 7200..86400 -> dateCmp = "${cmp / 3600} часа назад"
                in 86400..172800 -> dateCmp = "1 день назад"
                in 172800..2592000 -> dateCmp = "${cmp / 3600 / 24} дня назад"
                else -> dateCmp = "давно"
            }

            val media = mutableListOf<MediaItem>()
            item.optJSONArray("attachments")?.let { attachments ->
                for (j in 0 until attachments.length()) {
                    parseAttachment(attachments.getJSONObject(j))?.let { media.add(it) }
                }
            }

            comments.add(Comment("", userId, text, "$date   $dateCmp", id, "", media))

            val thread = item.getJSONObject("thread")

            if (thread.optInt("count") > 0) {
                val items = thread.getJSONArray("items")
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val text = item.optString("text", "")
                    val userId = item.optString("from_id", "error")
                    val id = item.optInt("id", 0)
                    val sdf = SimpleDateFormat("HH:mm:ss  d MMMM yyyy", Locale("ru"))
                    val rawDate = item.getLong("date")
                    val date = sdf.format(Date(rawDate * 1000))
                    val secUtc: Long = System.currentTimeMillis() / 1000
                    val cmp = secUtc - rawDate
                    var dateCmp = ""
                    when (cmp) {
                        in 1..3600 -> dateCmp = "${cmp / 60} минут назад"
                        in 3600..7200 -> dateCmp = "1 час назад"
                        in 7200..86400 -> dateCmp = "${cmp / 3600} часа назад"
                        in 86400..172800 -> dateCmp = "1 день назад"
                        in 172800..2592000 -> dateCmp = "${cmp / 3600 / 24} дня назад"
                        else -> dateCmp = "давно"
                    }

                    val media = mutableListOf<MediaItem>()
                    item.optJSONArray("attachments")?.let { attachments ->
                        for (j in 0 until attachments.length()) {
                            parseAttachment(attachments.getJSONObject(j))?.let { media.add(it) }
                        }
                    }

                    val replyTo = item.optInt("reply_to_comment", -1)
                    val answerTo = comments.find {it.vkId == replyTo} ?: Comment(
                        "",
                        "",
                        "error",
                        "",
                        0,
                        "",
                        emptyList()
                    )

                    comments.add(
                        Comment(
                            "",
                            userId,
                            text,
                            "$date   $dateCmp",
                            id,
                            answerTo.text,
                            media
                        )
                    )
                }
            }
        }
    }
    return comments
}

class CommentsActivity : AppCompatActivity() {
    private lateinit var adapter: CommentsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_comments)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val token: String = (application as App).token

        val recyclerView = findViewById<RecyclerView>(R.id.commentsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val ownerId = intent.getStringExtra("ownerId") ?: "error"
        val postId = intent.getStringExtra("postId") ?: "error"
        Log.d("comments_info", "$ownerId   $postId")

        val loading = listOf(
            Comment(
                "",
                "loading...",
                "loading...",
                "loading...",
                0,
                "",
                emptyList()
            )
        )
        adapter = CommentsAdapter(
            initialData = loading,
            onDownloadClick = { mediaList, clickedIndex -> startDownload(mediaList[clickedIndex]) },
            onMediaClick = { mediaList, clickedIndex ->
                val intent = Intent(this, MediaViewerActivity::class.java).apply {
                    putParcelableArrayListExtra("mediaList", ArrayList(mediaList))
                    putExtra("startPosition", clickedIndex)
                }
                startActivity(intent)
            },
            context = this
        )
        recyclerView.adapter = adapter

        lifecycleScope.launch {
            coroutineScope {
                launch {
                    val json = getComments(ownerId, postId, token)
                    val comments = parseComments(json)
                    adapter.updateData(comments)
                    fetchThumbnails(comments)
                }
            }
        }
    }

    private suspend fun fetchThumbnails(postList: List<Comment>) = coroutineScope {
        postList.forEachIndexed { postIndex, comment ->
            comment.mediaList.forEach { media ->
                if (media.type == MediaType.VIDEO) {
                    launch {
                        val info = withContext(Dispatchers.IO) {
                            try {
                                YoutubeDL.getInstance().getInfo(media.uri)
                            } catch (e: YoutubeDLException) {
                                Log.e("ytdlp", "Не удалось получить инфо для ${media.uri}", e)
                                null
                            }
                        }
                        if (info != null) {
                            media.thumbnail = info.thumbnail
                            // сообщаем адаптеру, что именно этот элемент изменился
                            withContext(Dispatchers.Main) {
                                adapter.notifyItemChanged(postIndex)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startDownload(item: MediaItem) {
        val downloadDir = File(
            getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "youtubedl-android"
        ).apply { if (!exists()) mkdirs() }

        lifecycleScope.launch {
            val resultPath = withContext(Dispatchers.IO) {
                try {
                    val dateStamp = SimpleDateFormat("yyyy-MM-dd-hh-mm-ss", Locale.getDefault())
                        .format(Date())

                    val request = YoutubeDLRequest(item.uri).apply {
                        addOption("-o", downloadDir.absolutePath + "/%(title)s_${dateStamp}.%(ext)s")
                        // ограничиваем формат, чтобы результат был одним готовым mp4 файлом
                        addOption("-f", "best[ext=mp4]/best")
                    }

                    YoutubeDL.getInstance().execute(request)

                    findDownloadedFile(downloadDir, item)
                } catch (e: YoutubeDLException) {
                    Log.e("dlp", "Ошибка скачивания ${item.uri}", e)
                    null
                }
            }

            if (resultPath != null) {
                item.path = resultPath
                Toast.makeText(this@CommentsActivity, "Видео скачанно ${item.uri}", Toast.LENGTH_SHORT).show()
                Log.d("dlp", "download ${item}")
            } else {
                Toast.makeText(this@CommentsActivity, "Ошибка загрузки видео", Toast.LENGTH_SHORT).show()
            }
        }
    }
}