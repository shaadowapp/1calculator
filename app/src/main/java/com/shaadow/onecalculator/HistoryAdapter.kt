package com.shaadow.onecalculator

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(private val items: List<HistoryEntity>) : RecyclerView.Adapter<HistoryViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return HistoryViewHolder(view)
    }
    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = items[position]
        holder.bind("${item.expression}${if (item.result.isNotEmpty()) " = ${item.result}" else ""}")
    }
    override fun getItemCount() = items.size
}

class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(text: String) {
        (itemView as TextView).apply {
            this.text = text
            this.setTextColor(android.graphics.Color.WHITE)
        }
    }
} 