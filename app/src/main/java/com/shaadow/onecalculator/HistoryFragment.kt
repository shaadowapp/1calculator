package com.shaadow.onecalculator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collect

class HistoryFragment : Fragment() {

    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var noHistoryFound: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var btnClearAll: Button
    private lateinit var db: HistoryDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Room database
        db = HistoryDatabase.getInstance(requireContext())

        // Initialize views
        historyRecyclerView = view.findViewById(R.id.history_recycler)
        noHistoryFound = view.findViewById(R.id.no_history_found)
        btnBack = view.findViewById(R.id.btn_back)
        btnClearAll = view.findViewById(R.id.btn_clear_all)

        // Setup RecyclerView
        setupRecyclerView()

        // Setup click listeners
        setupClickListeners()

        // Load history data
        loadHistoryData()

        // Setup swipe-to-delete
        setupSwipeToDelete()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(
            onItemClick = { _ ->
                // Handle item click - could copy to clipboard or navigate to calculator
                // For now, just show a toast or handle as needed
            },
            onDeleteClick = { historyItem ->
                deleteHistoryItem(historyItem)
            }
        )

        historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = historyAdapter
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            // Navigate back
            requireActivity().finish()
        }

        btnClearAll.setOnClickListener {
            clearAllHistory()
        }
    }

    private fun setupSwipeToDelete() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val historyItem = historyAdapter.currentList[position]
                deleteHistoryItem(historyItem)
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(historyRecyclerView)
    }

    private fun loadHistoryData() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db.historyDao().getAllHistory().collect { historyEntities ->
                    val items = historyEntities.map { historyEntity ->
                        HistoryItem(
                            id = historyEntity.id.toLong(),
                            expression = historyEntity.expression,
                            result = historyEntity.result,
                            timestamp = historyEntity.timestamp
                        )
                    }
                    
                    withContext(Dispatchers.Main) {
                        if (items.isEmpty()) {
                            noHistoryFound.visibility = View.VISIBLE
                            historyRecyclerView.visibility = View.GONE
                } else {
                            noHistoryFound.visibility = View.GONE
                            historyRecyclerView.visibility = View.VISIBLE
                            historyAdapter.submitList(items)
                        }
                    }
                }
            }
        }
    }

    private fun deleteHistoryItem(historyItem: HistoryItem) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db.historyDao().deleteById(historyItem.id.toInt())
            }
            // Reload the list after deletion
            loadHistoryData()
        }
    }

    private fun clearAllHistory() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db.historyDao().clearAll()
            }
            // Reload the list after clearing
            loadHistoryData()
        }
    }
} 