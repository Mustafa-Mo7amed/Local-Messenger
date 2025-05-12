package com.example.localmessenger

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context): SQLiteOpenHelper(context, "messenger.db", null, 1) {
    private val USER_TABLE = "user"
    private val USER_DEVICE_ADDRESS = "device_address"

    private val MESSAGE_TABLE = "messages"
    private val MESSAGE_ID = "message_id"
    private val MESSAGE_SENDER_ADDRESS = "sender_address"
    private val MESSAGE_RECEIVER_ADDRESS = "receiver_address"
    private val MESSAGE_OUTGOING = "is_outgoing"
    private val MESSAGE_CONTENT = "content"
    private val MESSAGE_TIMESTAMP = "timestamp"

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("""
        CREATE TABLE $USER_TABLE (
            $USER_DEVICE_ADDRESS TEXT PRIMARY KEY
        )
    """.trimIndent())

        db?.execSQL("""
        CREATE TABLE $MESSAGE_TABLE (
            $MESSAGE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $MESSAGE_SENDER_ADDRESS TEXT NOT NULL,
            $MESSAGE_RECEIVER_ADDRESS TEXT NOT NULL,
            $MESSAGE_OUTGOING INTEGER NOT NULL,
            $MESSAGE_CONTENT TEXT NOT NULL,
            $MESSAGE_TIMESTAMP INTEGER NOT NULL,
        )
    """.trimIndent())
    }


    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        TODO("Not yet implemented")
    }

}