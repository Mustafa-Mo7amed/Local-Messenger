package com.example.localmessenger

import android.content.Context
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import androidx.annotation.RequiresApi

data class ChatSession(
    val device1Address: String,
    val device2Address: String
)

data class Message(
    val id: Long,
    val senderAddress: String,
    val receiverAddress: String,
    val isOutgoing: Boolean,
    val content: String,
    val timestamp: Long
)

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

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    fun getChatSessions(): List<ChatSession> {
        val db = this.readableDatabase
        val sessions = mutableSetOf<ChatSession>()
        
        val cursor = db.query(
            true, // distinct
            MESSAGE_TABLE,
            arrayOf(MESSAGE_SENDER_ADDRESS, MESSAGE_RECEIVER_ADDRESS),
            null,
            null,
            null,
            null,
            "$MESSAGE_TIMESTAMP DESC",
            null
        )

        cursor.use {
            while (it.moveToNext()) {
                val sender = it.getString(it.getColumnIndexOrThrow(MESSAGE_SENDER_ADDRESS))
                val receiver = it.getString(it.getColumnIndexOrThrow(MESSAGE_RECEIVER_ADDRESS))
                
                // Create a chat session with addresses in sorted order to ensure uniqueness
                val addresses = listOf(sender, receiver).sorted()
                sessions.add(ChatSession(addresses[0], addresses[1]))
            }
        }

        return sessions.toList()
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    fun getMessagesForSession(device1Address: String, device2Address: String): List<Message> {
        val db = this.readableDatabase
        val messages = mutableListOf<Message>()
        
        val selection = "($MESSAGE_SENDER_ADDRESS = ? AND $MESSAGE_RECEIVER_ADDRESS = ?) OR " +
                       "($MESSAGE_SENDER_ADDRESS = ? AND $MESSAGE_RECEIVER_ADDRESS = ?)"
        val selectionArgs = arrayOf(device1Address, device2Address, device2Address, device1Address)
        
        val cursor = db.query(
            MESSAGE_TABLE,
            null,
            selection,
            selectionArgs,
            null,
            null,
            "$MESSAGE_TIMESTAMP ASC"
        )

        cursor.use {
            while (it.moveToNext()) {
                messages.add(Message(
                    id = it.getLong(it.getColumnIndexOrThrow(MESSAGE_ID)),
                    senderAddress = it.getString(it.getColumnIndexOrThrow(MESSAGE_SENDER_ADDRESS)),
                    receiverAddress = it.getString(it.getColumnIndexOrThrow(MESSAGE_RECEIVER_ADDRESS)),
                    isOutgoing = it.getInt(it.getColumnIndexOrThrow(MESSAGE_OUTGOING)) == 1,
                    content = it.getString(it.getColumnIndexOrThrow(MESSAGE_CONTENT)),
                    timestamp = it.getLong(it.getColumnIndexOrThrow(MESSAGE_TIMESTAMP))
                ))
            }
        }

        return messages
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $MESSAGE_TABLE")
        onCreate(db)
    }
}