package com.shaadow.onecalculator

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

/**
 * RecyclerView adapter for displaying history items in a category button style format.
 * This adapter uses DiffUtil for efficient list updates and provides click handling
 * for individual history items and delete functionality.
 *
 * @param onItemClick Callback function invoked when a history item is clicked
 * @param onDeleteClick Callback function invoked when delete button is clicked
 * @author Calculator Team
 * @since 1.0
 */
class HistoryAdapter(
    private val onItemClick: (HistoryEntity) -> Unit,
    private val onDeleteClick: (HistoryEntity) -> Unit
) : ListAdapter<HistoryEntity, HistoryAdapter.ViewHolder>(HistoryDiffCallback()) {

    /**
     * DiffUtil callback for efficiently comparing HistoryEntity items.
     * Enables smooth animations and performance optimization when the list updates.
     */
    class HistoryDiffCallback : DiffUtil.ItemCallback<HistoryEntity>() {
        override fun areItemsTheSame(oldItem: HistoryEntity, newItem: HistoryEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HistoryEntity, newItem: HistoryEntity): Boolean {
            return oldItem == newItem
        }
    }

    /**
     * Creates a new ViewHolder for the history item.
     *
     * @param parent The parent ViewGroup
     * @param viewType The view type (unused in this adapter)
     * @return A new ViewHolder instance
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.history_item, parent, false)
        return ViewHolder(view)
    }

    /**
     * Binds the HistoryEntity data to the ViewHolder.
     *
     * @param holder The ViewHolder to bind data to
     * @param position The position of the item in the list
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder class for history items.
     * Manages the view binding and click handling for individual history entries.
     *
     * @param itemView The root view of the history item
     */
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val historyItemText: TextView = itemView.findViewById(R.id.history_item_text)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.btn_delete)

        /**
         * Binds the history entity data to the view elements and sets up click handling.
         *
         * @param item The HistoryEntity to display
         */
        fun bind(item: HistoryEntity) {
            // Display expression and result in a single TextView
            historyItemText.text = "${item.expression}\n= ${item.result}"

            // Set up click listeners
            historyItemText.setOnClickListener {
                onItemClick(item)
            }

            deleteButton.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }
} 