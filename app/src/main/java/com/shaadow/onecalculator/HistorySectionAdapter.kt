package com.shaadow.onecalculator

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

sealed class HistoryItem {
    data class SectionHeader(val date: String) : HistoryItem()
    data class HistoryEntry(val entity: HistoryEntity, val position: Int) : HistoryItem()
}

class HistorySectionAdapter(
    private val onDelete: (HistoryEntity, Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items = mutableListOf<HistoryItem>()
    private var highlighter: ((HistoryEntity, String, HistoryViewHolder) -> Unit)? = null
    private var currentQuery: String = ""
    private var onItemClickListener: ((HistoryEntity) -> Unit)? = null

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    fun setHighlighter(highlighter: (HistoryEntity, String, HistoryViewHolder) -> Unit) {
        this.highlighter = highlighter
    }

    fun setOnItemClickListener(listener: (HistoryEntity) -> Unit) {
        this.onItemClickListener = listener
    }

    fun setQuery(query: String) {
        currentQuery = query
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is HistoryItem.SectionHeader -> TYPE_HEADER
            is HistoryItem.HistoryEntry -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.history_section_header, parent, false)
                SectionHeaderViewHolder(view)
            }
            TYPE_ITEM -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.history_item, parent, false)
                HistoryViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SectionHeaderViewHolder -> {
                val header = items[position] as HistoryItem.SectionHeader
                holder.bind(header)
            }
            is HistoryViewHolder -> {
                val entry = items[position] as HistoryItem.HistoryEntry
                holder.bind(entry.entity, entry.position)
                holder.btnDelete.setOnClickListener {
                    onDelete(entry.entity, entry.position)
                }
                
                // Add click listener to the entire item view
                holder.itemView.setOnClickListener {
                    onItemClickListener?.invoke(entry.entity)
                }
            }
        }
    }

    override fun getItemCount() = items.size

    fun updateHistory(historyList: List<HistoryEntity>) {
        items.clear()
        
        if (historyList.isEmpty()) {
            notifyDataSetChanged()
            return
        }

        // Group by date
        val groupedByDate = historyList.groupBy { entity ->
            val date = Date(entity.timestamp)
            val today = Calendar.getInstance()
            val itemCal = Calendar.getInstance().apply { time = date }
            
            when {
                today.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == itemCal.get(Calendar.DAY_OF_YEAR) -> "Today"
                today.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) - itemCal.get(Calendar.DAY_OF_YEAR) == 1 -> "Yesterday"
                else -> SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()).format(date)
            }
        }

        // Add items with headers
        groupedByDate.forEach { (date, entities) ->
            items.add(HistoryItem.SectionHeader(date))
            entities.forEachIndexed { index, entity ->
                items.add(HistoryItem.HistoryEntry(entity, index))
            }
        }

        notifyDataSetChanged()
    }

    fun removeAt(position: Int) {
        if (position >= 0 && position < items.size) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun getItemAt(index: Int): HistoryItem {
        return items[index]
    }

    inner class SectionHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headerText: TextView = itemView.findViewById(R.id.section_header_text)
        
        fun bind(header: HistoryItem.SectionHeader) {
            headerText.text = header.date
        }
    }

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvExpression: TextView = itemView.findViewById(R.id.tv_expression)
        val tvResult: TextView = itemView.findViewById(R.id.tv_result)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)
        val tvSource: TextView = itemView.findViewById(R.id.tv_source)
        
        fun bind(item: HistoryEntity, position: Int) {
            if (highlighter != null) {
                highlighter?.invoke(item, currentQuery, this)
            } else {
                tvExpression.text = item.expression
                tvResult.text = item.result
            }
            if (item.source == "Widget") {
                tvSource.visibility = View.VISIBLE
                tvSource.text = "Widget"
            } else {
                tvSource.visibility = View.GONE
            }
        }
    }
}
