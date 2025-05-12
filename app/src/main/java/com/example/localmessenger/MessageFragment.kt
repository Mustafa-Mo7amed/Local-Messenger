package com.example.localmessenger

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.EditText

class MessageFragment : Fragment(R.layout.fragment_message) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etMessage = view.findViewById<EditText>(R.id.etMessage)
    }
}