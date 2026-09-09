package com.example.vkapi.utils

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.vkapi.models.AppSetting
import com.example.vkapi.models.Pub

class DBHelper(val context: Context, val factory: SQLiteDatabase.CursorFactory?) : SQLiteOpenHelper(context, "appdb", factory, 2) {
    override fun onCreate(db: SQLiteDatabase?) {
        val pubs = "create table pubs (id INTEGER PRIMARY KEY AUTOINCREMENT,  ref TEXT, last_post INT)"
        val setting = "create table app_settings (skey TEXT PRIMARY KEY,  svalue TEXT)"
        db?.execSQL(pubs)
        db?.execSQL(setting)
    }

    override fun onUpgrade(
        db: SQLiteDatabase?,
        oldVersion: Int,
        newVersion: Int
    ) {
        db?.execSQL("drop table if exists pubs")
        db?.execSQL("drop table if exists app_settings")
        onCreate(db)
    }

    fun add_setting(setting: AppSetting) {
        val values = ContentValues()
        values.put("skey", setting.skey)
        values.put("svalue", setting.svalue)

        val db = this.writableDatabase
        db.insert("app_settings", null, values)
        db.close()
    }

    fun get_token(): String {
        val db = this.readableDatabase
        val selection = "skey = ?"
        val selectionArgs = arrayOf("token")

        val cursor = db.query(
            "app_settings",          // Имя таблицы
            null,             // Столбцы (null = все)
            selection,        // Условие WHERE
            selectionArgs,    // Значения для условия
            null, null, null  // GROUP BY, HAVING, ORDER BY
        )

        if (cursor.moveToFirst()) {
            val token_value = cursor.getString(cursor.getColumnIndexOrThrow("svalue"))
            cursor.close()
            db.close()
            return token_value
        }
        else {
            cursor.close()
            db.close()
            return ""
        }
    }

    fun add_new_pub(pub: Pub) {
        val values = ContentValues()
        values.put("ref", pub.ref)
        values.put("last_post", pub.last_post_date)

        val db = this.writableDatabase
        db.insert("pubs", null, values)
        db.close()
    }

    fun get_all_pub(): List<Pub> {
        val db = this.readableDatabase
        val pubs = db.rawQuery("select * from pubs", null)
        val res = mutableListOf<Pub>()
        while (pubs.moveToNext()) {
            val ref = pubs.getString(pubs.getColumnIndexOrThrow("ref"))
            val last_post = pubs.getInt(pubs.getColumnIndexOrThrow("last_post"))
            res.add(Pub(ref, last_post))
        }
        db.close()
        pubs.close()
        return res
    }
    fun delete_pub(ref: String) {
        val db = writableDatabase
        db.delete("pubs", "ref=?", arrayOf(ref))
        db.close()
    }

    fun updateLastPostTime(ref: String, lpd: Long) {
        val db = this.writableDatabase

        val values = ContentValues().apply {
            put("last_post", lpd)
        }

        val whereClause = "ref = ? AND last_post < ?"
        val whereArgs = arrayOf(ref, lpd.toString())

        db.update("pubs", values, whereClause, whereArgs)
    }
}