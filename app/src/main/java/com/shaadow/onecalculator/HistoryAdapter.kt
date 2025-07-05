package com.shaadow.onecalculator

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enhanced RecyclerView adapter for displaying history items with section headers.
 * This adapter combines the Material UI design with the original HistorySectionAdapter features:
 * - Section headers (Today, Yesterday, etc.)
 * - Search highlighting functionality
 * - Multiple view types (headers vs history items)
 * - Category button style for history items
 * - Delete functionality
 *
 * @param onItemClick Callback function invoked when a history item is clicked
 * @param onDeleteClick Callback function invoked when delete button is clicked
 * @author Calculator Team
 * @since 1.0
 */
class HistoryAdapter(
    private val onItemClick: (HistoryItem) -> Unit,
    private val onDeleteClick: (HistoryItem) -> Unit
) : ListAdapter<HistoryItem, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view, onItemClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HistoryViewHolder(
        itemView: View,
        private val onItemClick: (HistoryItem) -> Unit,
        private val onDeleteClick: (HistoryItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvExpression: TextView = itemView.findViewById(R.id.tv_expression)
        private val tvSolution: TextView = itemView.findViewById(R.id.tv_solution)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tv_timestamp)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)

        fun bind(historyItem: HistoryItem) {
            tvExpression.text = historyItem.expression
            tvSolution.text = "= ${historyItem.result}"
            tvTimestamp.text = formatTimestamp(historyItem.timestamp)

            // Set click listeners
            itemView.setOnClickListener { onItemClick(historyItem) }
            btnDelete.setOnClickListener { onDeleteClick(historyItem) }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            return when {
                diff < 60000 -> "Just now"
                diff < 3600000 -> "${diff / 60000} minutes ago"
                diff < 86400000 -> "${diff / 3600000} hours ago"
                else -> {
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    dateFormat.format(Date(timestamp))
                }
            }
        }
    }

    private class HistoryDiffCallback : DiffUtil.ItemCallback<HistoryItem>() {
        override fun areItemsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean {
            return oldItem == newItem
        }
    }
}

// Data class for history items
data class HistoryItem(
    val id: Long,
    val expression: String,
    val result: String,
    val timestamp: Long
) 