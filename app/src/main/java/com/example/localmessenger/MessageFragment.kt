package com.example.localmessenger

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

class MessageFragment : Fragment(R.layout.fragment_message) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        parentFragmentManager.setFragmentResultListener(
            "chat_message",              // 1) requestKey you’re interested in
            viewLifecycleOwner        // 2) lifecycleOwner to auto-clear listener
        ) { requestKey, bundle ->
            // 3) callback invoked when a result is set
            val text = bundle.getString("text")
            val tvDisplay = view.findViewById<TextView>(R.id.tvDisplayMessage)
            tvDisplay.text = text
        }

        val etSendMessage = view.findViewById<EditText>(R.id.etSendMessage)
        view.findViewById<Button>(R.id.btnSend).setOnClickListener {
            val bundle = Bundle().apply {
                val text = etSendMessage.text.toString()
                putString("text", text)
            }
            parentFragmentManager.setFragmentResult("chat_message2", bundle)
        }
    }
}