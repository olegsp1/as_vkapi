package com.example.vkapi

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.collections.chunked
import kotlin.collections.forEach

val client = OkHttpClient()

suspend fun getGroupInfo(domain: String, token: String): String = withContext(Dispatchers.IO) {
    val url = "https://api.vk.com/method/groups.getById" +
            "?group_ids=$domain" +
            "&fields=photo_200" +
            "&access_token=$token" +
            "&v=5.199"

    val request = Request.Builder()
        .url(url)
        .build()

    client.newCall(request).execute().use { response ->
        response.body?.string() ?: ""
    }
}

suspend fun getManyPosts(domain: String, token: String): String = withContext(Dispatchers.IO) {
    val url = "https://api.vk.com/method/wall.get" +
            "?domain=$domain" +
            "&count=100" +
            "&offset=0" +
            "&access_token=$token" +
            "&v=5.199"

    val request = Request.Builder()
        .url(url)
        .build()

    client.newCall(request).execute().use { response ->
        response.body?.string() ?: ""
    }
}

suspend fun countUnreadPosts(pub: Pub, token: String): List<String> {
    var count = 0
    val json = getManyPosts(pub.ref, token)
    val root = JSONObject(json)
    if (root.optString("error").isEmpty()) {
        val items = root.getJSONObject("response").getJSONArray("items")

        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            if (item.optInt("is_pinned") == 0) {
                val date = item.getLong("date")
                if (date > pub.last_post_date) {
                    count += 1
                }
                else {
                    break
                }
            }
        }
    }
    else {
        Log.d("getmanyposts", root.optString("error"))
    }
    return listOf(count.toString(), json)
}

suspend fun parseGroupInfo(json: String, pub: Pub, token: String): PubView {
    val jsonObject = JSONObject(json)
    val groups = jsonObject.getJSONObject("response").getJSONArray("groups")
    val group = groups.getJSONObject(0)
    val countAndJson = countUnreadPosts(pub, token)

    return PubView(
        ref = pub.ref,
        unreadPosts = countAndJson[0],
        name = group.getString("name"),
        pic = group.getString("photo_200"),
        posts = countAndJson[1]
    )
}

object JsonHolder {
    var jsonItem: String? = null
}

class MainActivity : AppCompatActivity() {
    private lateinit var adapter: GroupsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.Groupe)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val db = DBHelper(this, null)

        if ((application as App).token == "") {
            val token: String = db.get_token()

            if (token == "") {
                val intent = Intent(this, AppSettings::class.java)
                startActivity(intent)
            }
            else {
                (application as App).token = token
            }
        }
        val token: String = (application as App).token

        val new_pub_btn: Button = findViewById(R.id.add_pub_button)
        val settings: Button = findViewById(R.id.to_setting)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        new_pub_btn.setOnClickListener {
            val intent = Intent(this, add_new_pub::class.java)
            startActivity(intent)
        }

        settings.setOnClickListener {
            val intent = Intent(this, AppSettings::class.java)
            startActivity(intent)
        }

        val all_pub_inf = db.get_all_pub()
        val all_pub = mutableListOf<PubView>()

        lifecycleScope.launch {
            all_pub_inf.chunked(2).forEach { batch ->
                coroutineScope {
                    batch.forEach { i ->
                        launch {
                            try {
                                val json = getGroupInfo(i.ref, token)
                                val fullpub = parseGroupInfo(json, i, token)

                                all_pub.add(fullpub)
                                adapter.updateData(all_pub.toList())
                            } catch (e: Exception) {
                                Log.e("PUB_INF", "Ошибка для ${i.ref}", e)
                            }
                        }
                    }
                }
                delay(1000)
            }
        }

        adapter = GroupsAdapter(all_pub, this)
        recyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        val db = DBHelper(this, null)
        val token: String = (application as App).token

        val all_pub_inf = db.get_all_pub()
        val all_pub = mutableListOf<PubView>()

        lifecycleScope.launch {
            all_pub_inf.chunked(2).forEach { batch ->
                coroutineScope {
                    batch.forEach { i ->
                        launch {
                            try {
                                val json = getGroupInfo(i.ref, token)
                                val fullpub = parseGroupInfo(json, i, token)

                                all_pub.add(fullpub)
                                adapter.updateData(all_pub.toList())
                            } catch (e: Exception) {
                                Log.e("PUB_INF", "Ошибка для ${i.ref}", e)
                            }
                        }
                    }
                }
                delay(1000)
            }
        }
    }
}