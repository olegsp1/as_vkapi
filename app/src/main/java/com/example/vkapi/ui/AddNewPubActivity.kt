package com.example.vkapi.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.vkapi.App
import com.example.vkapi.utils.DBHelper
import com.example.vkapi.R
import com.example.vkapi.models.Pub
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

suspend fun getWallPosts(
    domain: String,
    count: Int,
    offset: Int,
    accessToken: String
): String = withContext(Dispatchers.IO) {
    val url = "https://api.vk.com/method/wall.get" +
            "?domain=$domain" +
            "&count=$count" +
            "&offset=$offset" +
            "&access_token=$accessToken" +
            "&v=5.199"

    val request = Request.Builder()
        .url(url)
        .build()

    client.newCall(request).execute().use { response ->
        response.body?.string() ?: ""
    }
}

class add_new_pub : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_new_pub)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.Groupe)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val token: String = (application as App).token

        val ref_inp: EditText = findViewById(R.id.pub_ref)
        val button: Button = findViewById(R.id.add_new_pub_btn)
        val back_btn: Button = findViewById(R.id.back_button)

        back_btn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        button.setOnClickListener {
            var ref = ref_inp.text.toString().trim()

            if (ref == "") {
                Toast.makeText(this, "введите адрес", Toast.LENGTH_LONG).show()
            }
            else {
                val prefix = "https://vk.ru/"
                if (ref.startsWith(prefix)) {
                    ref = ref.substring(prefix.length)
                }

                lifecycleScope.launch {
                    val json = getWallPosts(
                        domain = ref,
                        count = 1,
                        offset = 0,
                        accessToken = token
                    )

                    if (JSONObject(json).optString("error").isEmpty()) {
                        val pub = Pub(ref, 0)

                        val db = DBHelper(this@add_new_pub, null)
                        db.add_new_pub(pub)
                        Toast.makeText(this@add_new_pub, "группа добавлена", Toast.LENGTH_LONG).show()
                    }
                    else {
                        Toast.makeText(this@add_new_pub, "группа не найдена", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}