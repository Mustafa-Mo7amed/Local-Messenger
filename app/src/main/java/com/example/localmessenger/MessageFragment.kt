package com.example.localmessenger

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import android.widget.ListView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.util.*

class MessageFragment : Fragment(R.layout.fragment_message) {
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var lvMessages: ListView
    private lateinit var btnRefresh: Button
    private lateinit var adapter: ArrayAdapter<String>
    private val messagesList = mutableListOf<String>()

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = DatabaseHelper(requireContext())
        
        lvMessages = view.findViewById(R.id.lvMessages)
        btnRefresh = view.findViewById(R.id.btnRefresh)
        
        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, messagesList)
        lvMessages.adapter = adapter

        btnRefresh.setOnClickListener {
            loadMessages()
            Toast.makeText(context, "Messages refreshed", Toast.LENGTH_SHORT).show()
        }

        loadMessages()
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun loadMessages() {
        messagesList.clear()
        val sessions = dbHelper.getChatSessions()
        
        for (session in sessions) {
            messagesList.add("Chat between ${session.device1Address} and ${session.device2Address}")

            val messages = dbHelper.getMessagesForSession(session.device1Address, session.device2Address)
            for (message in messages) {
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
                val direction = if (message.isOutgoing) "➡️" else "⬅️"
                messagesList.add("$direction $time: ${message.content}")
            }
            messagesList.add("")
        }
        
        adapter.notifyDataSetChanged()

        if (messagesList.isNotEmpty())
            lvMessages.setSelection(messagesList.size - 1)
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
}
