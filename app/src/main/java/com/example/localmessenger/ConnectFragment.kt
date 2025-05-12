package com.example.localmessenger

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import org.w3c.dom.Text

class ConnectFragment : Fragment(R.layout.fragment_connect) {
    lateinit var connectionStatus: TextView
    lateinit var btnOnOff: Button
    lateinit var btnDiscover: Button
    lateinit var lvPeers: ListView
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etTypeMessage = view.findViewById<EditText>(R.id.etTypeMessage)

        // Example: bundle up the text when a button is clicked
        view.findViewById<Button>(R.id.btnSend).setOnClickListener {
            val text = etTypeMessage.text.toString()
            val bundle = Bundle().apply {
                putString("text", text)
            }
            // send that bundle on…
            parentFragmentManager.setFragmentResult("chat_message", bundle)
        }

        val tvMessage = view.findViewById<TextView>(R.id.tvMessage)
        parentFragmentManager.setFragmentResultListener("chat_message2", viewLifecycleOwner) { requestKey, bundle ->
            val text = bundle.getString("text")
            tvMessage.text = text
        }
    }
}