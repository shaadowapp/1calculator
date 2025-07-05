package com.shaadow.onecalculator

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    
    private val messages = mutableListOf<ChatMessage>()
    
    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AI = 2
    }
    
    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }
    
    fun addUserMessage(text: String) {
        addMessage(ChatMessage(text, true))
    }
    
    fun addAIMessage(text: String) {
        addMessage(ChatMessage(text, false))
    }
    
    fun addMessages(newMessages: List<ChatMessage>) {
        val startPosition = messages.size
        messages.addAll(newMessages)
        notifyItemRangeInserted(startPosition, newMessages.size)
    }
    
    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) VIEW_TYPE_USER else VIEW_TYPE_AI
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_message_user, parent, false)
                UserMessageViewHolder(view)
            }
            VIEW_TYPE_AI -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_message_ai, parent, false)
                AIMessageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is UserMessageViewHolder -> holder.bind(message)
            is AIMessageViewHolder -> holder.bind(message)
        }
    }
    
    override fun getItemCount(): Int = messages.size
    
    inner class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.message_text)
        
        fun bind(message: ChatMessage) {
            messageText.text = message.text
        }
    }
    
    inner class AIMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.message_text)
        
        fun bind(message: ChatMessage) {
            messageText.text = message.text
        }
    }
} 