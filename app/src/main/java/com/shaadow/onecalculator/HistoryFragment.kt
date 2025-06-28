package com.shaadow.onecalculator

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
// Import the shared adapter
import com.shaadow.onecalculator.HistoryAdapter
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

class HistoryFragment : Fragment(R.layout.layout_history) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.history_recycler)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        
        val adapter = HistoryAdapter(mutableListOf()) { _, _ -> }
        recyclerView.adapter = adapter
        
        viewLifecycleOwner.lifecycleScope.launch {
            val db = HistoryDatabase.getInstance(requireContext())
            db.historyDao().getAllHistory().collect { allHistory ->
                if (allHistory.isEmpty()) {
                    adapter.updateHistory(mutableListOf(HistoryEntity(expression = "No history yet", result = "")))
                } else {
                    adapter.updateHistory(allHistory.toMutableList())
                }
            }
        }
    }
} 