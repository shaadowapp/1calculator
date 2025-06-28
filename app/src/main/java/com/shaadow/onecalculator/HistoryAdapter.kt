package com.shaadow.onecalculator

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

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
        // Set date label
        val date = Date(item.timestamp)
        val today = Calendar.getInstance()
        val itemCal = Calendar.getInstance().apply { time = date }
        val isToday = today.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == itemCal.get(Calendar.DAY_OF_YEAR)
        val dateStr = if (isToday) {
            "Today"
        } else {
            SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()).format(date)
        }
        holder.tvDate.text = dateStr
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

    fun updateHistory(newItems: MutableList<HistoryEntity>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        val tvExpression: TextView = itemView.findViewById(R.id.tv_expression)
        val tvResult: TextView = itemView.findViewById(R.id.tv_result)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)
        fun bind(item: HistoryEntity) {
            tvDate.text = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()).format(Date(item.timestamp))
            tvExpression.text = item.expression
            tvResult.text = item.result
        }
    }
} 