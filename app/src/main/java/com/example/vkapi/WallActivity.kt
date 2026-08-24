package com.example.vkapi

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
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDL.getInstance
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.ZoneOffset
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
private suspend fun parseAttachment(att: JSONObject): MediaItem? {
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

private fun bestPhotoUrl(sizes: JSONArray?): String? {
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

fun getLPD(json: String): Long {
    val root = JSONObject(json)
    var date: Long = 0
    if (root.optString("error").isEmpty()) {
        val item = root.getJSONObject("response").getJSONArray("items").getJSONObject(0)
        Log.d("posts", item.toString())
        if (item.optInt("is_pinned") == 0) {
            date = item.getLong("date")
            Log.d("posts_0", date.toString())
        }
        else {
            val secitem = root.getJSONObject("response").getJSONArray("items").getJSONObject(1)
            Log.d("posts_1", secitem.toString())
            date = secitem.getLong("date")
            Log.d("posts_1", date.toString())
        }
    }
    return date
}

fun getLPDFromList(json: JSONArray): Long {
    var date: Long = 0
    val item = json.getJSONObject(0)
    Log.d("posts", item.toString())
    if (item.optInt("is_pinned") == 0) {
        date = item.getLong("date")
        Log.d("posts_0", date.toString())
    }
    else {
        val secitem = json.getJSONObject(1)
        Log.d("posts_1", secitem.toString())
        date = secitem.getLong("date")
        Log.d("posts_1", date.toString())
    }
    return date
}

suspend fun parseWall(json: String): List<Post> {
    val posts = mutableListOf<Post>()
    val root = JSONObject(json)
    if (root.optString("error").isEmpty()) {
        val items = root.getJSONObject("response").getJSONArray("items")

        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            val id = item.getLong("id")
            val text = item.optString("text", "")
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

            posts.add(Post(id, media, text, date, dateCmp, comment, isPinned))
        }
    }
    else {
        val errormsg: String = root.getJSONObject("error").getString("error_msg")
        posts.add(Post(1, emptyList(), errormsg, "loading...", "", 0, false))
    }

    return posts
}

suspend fun parseArray(items: JSONArray): List<Post> {
    val posts = mutableListOf<Post>()

    for (i in 0 until items.length()) {
        val item = items.getJSONObject(i)
        val id = item.getLong("id")
        val text = item.optString("text", "")
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

        posts.add(Post(id, media, text, date, dateCmp, comment, isPinned))
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


        val loading = listOf(Post(1, emptyList(), "loading...", "loading...", "", 0, false))
        adapter = PostAdapter(
            initialData = loading,
            onDownloadClick = { mediaList, clickedIndex -> startDownload(mediaList[clickedIndex]) },
            onMediaClick = { mediaList, clickedIndex ->
                val intent = Intent(this, MediaViewerActivity::class.java).apply {
                    putParcelableArrayListExtra("mediaList", ArrayList(mediaList))
                    putExtra("startPosition", clickedIndex)
                }
                startActivity(intent)
            }
        )

        fun update(offset: Int){
            if (offset >= 100) {
                lifecycleScope.launch {
                    delay(1000)
                    coroutineScope {
                        launch {
                            pgnum.text = offset.toString()
                            val json = getWall(ref, offset, token)
                            val postList = parseWall(json).reversed()

                            initYoutubeDL(postList)

                            adapter.updateData(postList)
                            wall.scrollToPosition(0)
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

                            initYoutubeDL(postList)

                            adapter.updateData(postList)
                            wall.scrollToPosition(0)
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

    /** yt-dlp инициализируется один раз, в фоне — операция может занять время при первом запуске */
    private fun initYoutubeDL(postList: List<Post>) {
        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    YoutubeDL.getInstance().init(this@WallActivity)
                    true
                } catch (e: YoutubeDLException) {
                    Log.e("dlp", "Ошибка инициализации youtubedl-android", e)
                    false
                }
            }
            if (!success) {
                Toast.makeText(this@WallActivity, "Не удалось инициализировать yt-dlp", Toast.LENGTH_LONG).show()
                return@launch
            }

            postList.forEach { it.mediaList.forEach { if (it.type == MediaType.VIDEO) fetchInfo(it) } }
        }
    }

    private fun fetchInfo(item: MediaItem) {
        lifecycleScope.launch {
            val info = withContext(Dispatchers.IO) {
                try {
                    YoutubeDL.getInstance().getInfo(item.uri)
                } catch (e: YoutubeDLException) {
                    Log.e("dlp", "Не удалось получить инфо для ${item.uri}", e)
                    null
                }
            }
            if (info != null) {
                item.thumbnail = info.thumbnail
            }
        }
    }

    private fun findDownloadedFile(dir: File, item: MediaItem): String? {
        // Простой поиск самого свежего файла в папке — для прод-кода лучше сверять по shortTitle/ID из VideoInfo
        return dir.listFiles()
            ?.filter { it.isFile }
            ?.maxByOrNull { it.lastModified() }
            ?.absolutePath
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

                    // Ищем скачанный файл в целевой директории по названию видео
                    findDownloadedFile(downloadDir, item)
                } catch (e: YoutubeDLException) {
                    Log.e("dlp", "Ошибка скачивания ${item.uri}", e)
                    null
                }
            }

            if (resultPath != null) {
                item.path = resultPath
                Toast.makeText(this@WallActivity, "Видео скачанно ${item.uri}", Toast.LENGTH_SHORT).show()
                Log.d("dlp", "download ${item}")
            } else {
                Toast.makeText(this@WallActivity, "Ошибка загрузки видео", Toast.LENGTH_SHORT).show()
            }
        }
    }
}