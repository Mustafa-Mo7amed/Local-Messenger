package com.example.localmessenger

import android.os.Bundle
import android.view.View
import android.text.method.ScrollingMovementMethod
import android.widget.TextView
import androidx.fragment.app.Fragment

class MessageFragment : Fragment(R.layout.fragment_message) {
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var tvMessages: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = DatabaseHelper(requireContext())
        
        tvMessages = view.findViewById(R.id.tvMessages)
        tvMessages.movementMethod = ScrollingMovementMethod()
        
        loadMessages()
    }

    private fun loadMessages() {
        val messages = dbHelper.getAllMessages()
        tvMessages.text = messages
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
}
