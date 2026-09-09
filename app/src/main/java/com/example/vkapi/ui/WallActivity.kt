package com.example.vkapi.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.vkapi.App
import com.example.vkapi.utils.DBHelper
import com.example.vkapi.R
import com.example.vkapi.models.MediaItem
import com.example.vkapi.models.MediaType
import com.example.vkapi.models.Post
import com.example.vkapi.rvAdapters.PostAdapter
import com.example.vkapi.utils.fileDownload
import com.example.vkapi.utils.parseAttachment
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

suspend fun getWall(domain: String, offset: Int, token: String): String = withContext(Dispatchers.IO) {
    val url = "https://api.vk.com/method/wall.get" +
            "?domain=$domain" +
            "&count=10" +
            "&offset=$offset" +
            "&access_token=$token" +
            "&v=5.199"

    val request = Request.Builder()
        .url(url)
        .build()

    client.newCall(request).execute().use { response ->
        response.body?.string() ?: ""
    }
}

fun getLPD(json: String): Long {
    val root = JSONObject(json)
    var date: Long = 0
    if (root.optString("error").isEmpty()) {
        val item = root.getJSONObject("response").getJSONArray("items").getJSONObject(0)
        if (item.optInt("is_pinned") == 0) {
            date = item.getLong("date")
        }
        else {
            val secitem = root.getJSONObject("response").getJSONArray("items").getJSONObject(1)
            date = secitem.getLong("date")
        }
    }
    return date
}

fun getLPDFromList(json: JSONArray): Long {
    var date: Long = 0
    val item = json.getJSONObject(0)
    if (item.optInt("is_pinned") == 0) {
        date = item.getLong("date")
    }
    else {
        val secitem = json.getJSONObject(1)
        date = secitem.getLong("date")
    }
    return date
}

suspend fun parseWall(json: String): List<Post> {
    var posts = listOf(Post(1, emptyList(), "error", "error", "", 0, false, "", "", ""))
    val root = JSONObject(json)
    if (root.optString("error").isEmpty()) {
        val items = root.getJSONObject("response").getJSONArray("items")
        posts = parseArray(items)
    }
    else {
        val errormsg: String = root.getJSONObject("error").getString("error_msg")
        Log.e("Parse_wall", errormsg)
    }

    return posts
}

suspend fun parseArray(items: JSONArray): List<Post> {
    val posts = mutableListOf<Post>()

    for (i in 0 until items.length()) {
        val item = items.getJSONObject(i)
        val id = item.getLong("id")
        val text = item.optString("text", "")
        val ownerId = item.optInt("owner_id", -1).toString()
        val isPinned = item.optInt("is_pinned") == 1

        val media = mutableListOf<MediaItem>()
        item.optJSONArray("attachments")?.let { attachments ->
            for (j in 0 until attachments.length()) {
                parseAttachment(attachments.getJSONObject(j))?.let { media.add(it) }
            }
        }

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
        val comment = item.getJSONObject("comments").getLong("count")

        var repostText = ""
        var repOwnerId = ""
        if (item.optJSONArray("copy_history") != null) {
            repostText = item.getJSONArray("copy_history").getJSONObject(0).optString("text", "")
            repOwnerId = item.getJSONArray("copy_history").getJSONObject(0).optString("owner_id", "")
            item.getJSONArray("copy_history").getJSONObject(0).optJSONArray("attachments")?.let { attachments ->
                for (j in 0 until attachments.length()) {
                    parseAttachment(attachments.getJSONObject(j))?.let { media.add(it) }
                }
            }
        }

        posts.add(
            Post(
                id,
                media,
                text,
                date,
                dateCmp,
                comment,
                isPinned,
                ownerId,
                repostText,
                repOwnerId
            )
        )
    }
    return posts
}

class WallActivity : AppCompatActivity() {
    private lateinit var adapter: PostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_wall)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.Groupe)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val token: String = (application as App).token

        val db = DBHelper(this, null)

        val name: TextView = findViewById(R.id.name)
        val pic: ImageView = findViewById(R.id.picture)
        val wall: RecyclerView = findViewById(R.id.wall)
        val next: TextView = findViewById(R.id.next)
        val back: TextView = findViewById(R.id.back)
        val pgnum: TextView = findViewById(R.id.pagenumber)

        name.text = intent.getStringExtra("name")
        pic.load(intent.getStringExtra("pic"))
        val ref = intent.getStringExtra("ref").toString()
        val unrp = intent.getStringExtra("unrp")?.toInt()

        val json = JsonHolder.jsonItem ?: ""
        JsonHolder.jsonItem = null
        var offset = unrp ?: 0
        offset = offset / 10 * 10

        wall.layoutManager = LinearLayoutManager(this)


        val loading = listOf(
            Post(
                1,
                emptyList(),
                "loading...",
                "loading...",
                "",
                0,
                false,
                "",
                "",
                ""
            )
        )
        adapter = PostAdapter(
            initialData = loading,
            onDownloadClick = { mediaList, clickedIndex -> startDownload(mediaList[clickedIndex], this) },
            onMediaClick = { mediaList, clickedIndex ->
                val intent = Intent(this, MediaViewerActivity::class.java).apply {
                    putParcelableArrayListExtra("mediaList", ArrayList(mediaList))
                    putExtra("startPosition", clickedIndex)
                }
                startActivity(intent)
            },
            context = this
        )

        fun update(offset: Int){
            if (offset >= 100 || json == "") {
                lifecycleScope.launch {
                    coroutineScope {
                        launch {
                            pgnum.text = offset.toString()
                            val json = getWall(ref, offset, token)
                            var postList = listOf(
                                Post(
                                    1,
                                    emptyList(),
                                    "error",
                                    "error",
                                    "",
                                    0,
                                    false,
                                    "",
                                    "",
                                    ""
                                )
                            )
                            try {
                                postList = parseWall(json).reversed()
                            }
                            catch (e: Exception) {
                                Log.e("parseError", e.toString())
                            }

                            adapter.updateData(postList)
                            wall.scrollToPosition(0)
                            fetchThumbnails(postList)
                        }
                    }
                }
            }
            else {
                lifecycleScope.launch {
                    coroutineScope {
                        launch {
                            pgnum.text = offset.toString()
                            val alllist = JSONObject(json).getJSONObject("response").getJSONArray("items")
                            val jsonList = JSONArray()
                            for (i in offset until (offset + 10)) {
                                if (i < alllist.length()) { // Защита от IndexOutOfBoundsException
                                    jsonList.put(alllist.get(i))
                                }
                            }

                            val postList = parseArray(jsonList).reversed()

                            adapter.updateData(postList)
                            wall.scrollToPosition(0)
                            fetchThumbnails(postList)
                        }
                    }
                }
            }
        }

        fun updateLPD(json: String, offset: Int, ref: String, token: String) {
            (application as App).MANU = true
            if (offset >= 100) {
                lifecycleScope.launch {
                    coroutineScope {
                        launch {
                            val list = getWall(ref, offset + 10, token)
                            db.updateLastPostTime(ref, getLPD(list))
                        }
                    }
                }
            }
            else {
                lifecycleScope.launch {
                    coroutineScope {
                        launch {
                            val alllist = JSONObject(json).getJSONObject("response").getJSONArray("items")
                            val jsonList = JSONArray()
                            for (i in offset until (offset + 10)) {
                                if (i < alllist.length()) { // Защита от IndexOutOfBoundsException
                                    jsonList.put(alllist.get(i))
                                }
                            }
                            db.updateLastPostTime(ref, getLPDFromList(jsonList))
                        }
                    }
                }
            }
        }

        update(offset)
        wall.adapter = adapter

        next.setOnClickListener {
            if (offset > 9) {
                offset -= 10
                update(offset)
                updateLPD(json, offset, ref, token)
            }
            else {
                updateLPD(json, offset, ref, token)
                Toast.makeText(this@WallActivity, "Посты закончились", Toast.LENGTH_LONG).show()
            }
        }

        back.setOnClickListener {
            offset += 10
            update(offset)
        }
    }

    private suspend fun fetchThumbnails(postList: List<Post>) = coroutineScope {
        postList.forEachIndexed { postIndex, post ->
            post.mediaList.forEach { media ->
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

    private fun startDownload(item: MediaItem, context: Context) {
        val downloadDir = File(
            getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "youtubedl-android"
        ).apply { if (!exists()) mkdirs() }

        lifecycleScope.launch {
            fileDownload(context, item, downloadDir)
        }
    }
}