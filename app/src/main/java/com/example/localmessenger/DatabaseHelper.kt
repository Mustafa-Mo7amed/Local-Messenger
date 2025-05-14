package com.example.localmessenger

import android.content.Context
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context): SQLiteOpenHelper(context, "messenger.db", null, 1) {
    private val MESSAGE_TABLE = "messages"
    private val MESSAGE_ID = "message_id"
    private val MESSAGE_SENDER_ADDRESS = "sender_address"
    private val MESSAGE_RECEIVER_ADDRESS = "receiver_address"
    private val MESSAGE_OUTGOING = "is_outgoing"
    private val MESSAGE_CONTENT = "content"
    private val MESSAGE_TIMESTAMP = "timestamp"

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("""
        CREATE TABLE $MESSAGE_TABLE (
            $MESSAGE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $MESSAGE_SENDER_ADDRESS TEXT NOT NULL,
            $MESSAGE_RECEIVER_ADDRESS TEXT NOT NULL,
            $MESSAGE_OUTGOING INTEGER NOT NULL,
            $MESSAGE_CONTENT TEXT NOT NULL,
            $MESSAGE_TIMESTAMP INTEGER NOT NULL
        )
    """.trimIndent())
    }

    fun insertMessage(senderAddress: String, receiverAddress: String, isOutgoing: Boolean, content: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(MESSAGE_SENDER_ADDRESS, senderAddress)
            put(MESSAGE_RECEIVER_ADDRESS, receiverAddress)
            put(MESSAGE_OUTGOING, if (isOutgoing) 1 else 0)
            put(MESSAGE_CONTENT, content)
            put(MESSAGE_TIMESTAMP, System.currentTimeMillis())
        }
        db.insert(MESSAGE_TABLE, null, values)
        db.close()
    }

    fun getAllMessages(): String {
        val db = this.readableDatabase
        val messages = StringBuilder()
        
        val cursor = db.query(
            MESSAGE_TABLE,
            null,
            null,
            null,
            null,
            null,
            "$MESSAGE_TIMESTAMP ASC"
        )

        cursor.use {
            while (it.moveToNext()) {
                val sender = it.getString(it.getColumnIndexOrThrow(MESSAGE_SENDER_ADDRESS))
                val content = it.getString(it.getColumnIndexOrThrow(MESSAGE_CONTENT))
                val isOutgoing = it.getInt(it.getColumnIndexOrThrow(MESSAGE_OUTGOING)) == 1
                
                messages.append(if (isOutgoing) "➡️ " else "⬅️ ")
                messages.append("$sender: $content\n")
            }
        }

        return messages.toString()
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Not implemented
    }
}