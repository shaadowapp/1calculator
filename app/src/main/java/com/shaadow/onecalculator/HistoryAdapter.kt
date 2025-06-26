package com.shaadow.onecalculator

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(
    private val items: MutableList<HistoryEntity>,
    private val onDelete: (HistoryEntity, Int) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private var highlighter: ((HistoryEntity, String, HistoryViewHolder) -> Unit)? = null
    private var currentQuery: String = ""

    fun setHighlighter(highlighter: (HistoryEntity, String, HistoryViewHolder) -> Unit) {
        this.highlighter = highlighter
    }

    fun setQuery(query: String) {
        currentQuery = query
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.history_item, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = items[position]
        if (highlighter != null) {
            highlighter?.invoke(item, currentQuery, holder)
        } else {
            holder.bind(item)
        }
        holder.btnDelete.setOnClickListener {
            onDelete(item, position)
        }
    }

    override fun getItemCount() = items.size

    fun removeAt(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvExpression: TextView = itemView.findViewById(R.id.tv_expression)
        val tvResult: TextView = itemView.findViewById(R.id.tv_result)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)
        fun bind(item: HistoryEntity) {
            tvExpression.text = item.expression
            tvResult.text = item.result
        }
    }
} 