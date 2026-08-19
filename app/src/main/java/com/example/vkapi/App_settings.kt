package com.example.vkapi

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText

class AppSettings : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_app_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val db = DBHelper(this, null)

        val token: String = db.get_token()

        if (token == "") {
            Toast.makeText(this@AppSettings, "необходим токен", Toast.LENGTH_SHORT).show()
        }

        val token_input: TextInputEditText = findViewById(R.id.token_input_area)
        val save_button: Button = findViewById(R.id.save_settings_btn)
        val back_button: Button = findViewById(R.id.setting_back_button)

        back_button.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        save_button.setOnClickListener {
            val token = token_input.text.toString().trim()

            if (!token.isEmpty()) {
                val setting = App_setting(skey = "token", svalue = token)
                val db = DBHelper(this@AppSettings, null)

                db.add_setting(setting)
                Toast.makeText(this@AppSettings, "настройки сохранены", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this@AppSettings, "поля пусты", Toast.LENGTH_SHORT).show()
            }
        }
    }
}