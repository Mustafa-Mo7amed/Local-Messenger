package com.example.localmessenger

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView

class MessageFragment : Fragment(R.layout.fragment_message) {
    private lateinit var lvMessages: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val messages = mutableListOf<String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) Find your ListView
        lvMessages = view.findViewById(R.id.lvChat)

        // 2) Create an ArrayAdapter backed by your `messages` list
        adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            messages
        )
        lvMessages.adapter = adapter

        // 3) Listen for the same key you used in ConnectFragment:
        parentFragmentManager.setFragmentResultListener(
            "chat_message",
            viewLifecycleOwner
        ) { _, bundle ->
            // 4) Extract data
            val text     = bundle.getString("text") ?: return@setFragmentResultListener
            val incoming = bundle.getBoolean("incoming")

            // 5) Format it however you like (here we prepend IN/OUT)
            val displayText = if (incoming) "⬅️ $text" else "➡️ $text"

            // 6) Add and refresh
            messages.add(displayText)
            adapter.notifyDataSetChanged()

            // 7) Scroll to bottom
            lvMessages.post {
                lvMessages.setSelection(messages.size - 1)
            }
        }

        val btnSend = view.findViewById<Button>(R.id.btnSend)
        val etMessage = view.findViewById<EditText>(R.id.etMessage)
        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            // 1) Show it immediately in this fragment:
            parentFragmentManager.setFragmentResult("chat_message", Bundle().apply {
                putString("text", text)
                putBoolean("incoming", false)   // mark as outgoing
            })

            // 2) Clear the input field:
            etMessage.text.clear()

            // 3) Also hand it off to the ConnectFragment to actually send over the socket:
            parentFragmentManager.setFragmentResult("send_over_socket", Bundle().apply {
                putString("text", text)
            })
        }
    }
}
