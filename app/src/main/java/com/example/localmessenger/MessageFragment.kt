package com.example.localmessenger

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

class MessageFragment : Fragment(R.layout.fragment_message) {
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var rvChatSessions: RecyclerView
    private lateinit var chatSessionAdapter: ChatSessionAdapter
    private lateinit var btnRefresh: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = DatabaseHelper(requireContext())
        
        rvChatSessions = view.findViewById(R.id.rvChatSessions)
        btnRefresh = view.findViewById(R.id.btnRefresh)
        
        rvChatSessions.layoutManager = LinearLayoutManager(context)
        chatSessionAdapter = ChatSessionAdapter()
        rvChatSessions.adapter = chatSessionAdapter

        btnRefresh.setOnClickListener {
            loadChatSessions()
            Toast.makeText(context, "Messages refreshed", Toast.LENGTH_SHORT).show()
        }

        // Initial load of messages
        loadChatSessions()
    }

    private fun loadChatSessions() {
        val sessions = dbHelper.getChatSessions()
        chatSessionAdapter.updateSessions(sessions)
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }

    inner class ChatSessionAdapter : RecyclerView.Adapter<ChatSessionAdapter.ChatSessionViewHolder>() {
        private var sessions = listOf<ChatSession>()
        private val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        fun updateSessions(newSessions: List<ChatSession>) {
            sessions = newSessions
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatSessionViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_session, parent, false)
            return ChatSessionViewHolder(view)
        }

        override fun onBindViewHolder(holder: ChatSessionViewHolder, position: Int) {
            val session = sessions[position]
            holder.bind(session)
        }

        override fun getItemCount() = sessions.size

        inner class ChatSessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvDevice1: TextView = itemView.findViewById(R.id.tvDevice1)
            private val tvDevice2: TextView = itemView.findViewById(R.id.tvDevice2)
            private val rvMessages: RecyclerView = itemView.findViewById(R.id.rvMessages)
            private val messageAdapter = MessageAdapter()

            init {
                rvMessages.layoutManager = LinearLayoutManager(context)
                rvMessages.adapter = messageAdapter
            }

            fun bind(session: ChatSession) {
                tvDevice1.text = session.device1Address
                tvDevice2.text = session.device2Address
                
                val messages = dbHelper.getMessagesForSession(session.device1Address, session.device2Address)
                messageAdapter.updateMessages(messages)
            }
        }
    }

    inner class MessageAdapter : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {
        private var messages = listOf<Message>()
        private val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        fun updateMessages(newMessages: List<Message>) {
            messages = newMessages
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message, parent, false)
            return MessageViewHolder(view)
        }

        override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
            val message = messages[position]
            holder.bind(message)
        }

        override fun getItemCount() = messages.size

        inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
            private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)

            fun bind(message: Message) {
                tvMessage.text = message.content
                tvTimestamp.text = dateFormat.format(Date(message.timestamp))
                
                // Align messages based on whether they're outgoing or incoming
                val params = tvMessage.layoutParams as ViewGroup.MarginLayoutParams
                if (message.isOutgoing) {
                    params.marginStart = 48
                    params.marginEnd = 0
                } else {
                    params.marginStart = 0
                    params.marginEnd = 48
                }
                tvMessage.layoutParams = params
            }
        }
    }
}
