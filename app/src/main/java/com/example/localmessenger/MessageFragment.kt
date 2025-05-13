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

        lvMessages = view.findViewById(R.id.lvHistoryChat)

        adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            messages
        )
        lvMessages.adapter = adapter

        parentFragmentManager.setFragmentResultListener(
            "chat_message",
            viewLifecycleOwner
        ) { _, bundle ->
            val text     = bundle.getString("text") ?: return@setFragmentResultListener
            val incoming = bundle.getBoolean("incoming")

            val displayText = if (incoming) "⬅️ $text" else "➡️ $text"

            messages.add(displayText)
            adapter.notifyDataSetChanged()

            lvMessages.post {
                lvMessages.setSelection(messages.size - 1)
            }
        }

        val btnSend = view.findViewById<Button>(R.id.btnSend)
        val etMessage = view.findViewById<EditText>(R.id.etMessage)
        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            parentFragmentManager.setFragmentResult("chat_message", Bundle().apply {
                putString("text", text)
                putBoolean("incoming", false)   // mark as outgoing
            })

            etMessage.text.clear()

            parentFragmentManager.setFragmentResult("send_over_socket", Bundle().apply {
                putString("text", text)
            })
        }
    }
}
