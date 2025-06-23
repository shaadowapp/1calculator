package com.shaadow.onecalculator

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
// Import the shared adapter
import com.shaadow.onecalculator.HistoryAdapter

class HistoryFragment : Fragment(R.layout.layout_history) {
    private val historyList = listOf(
        "2 + 2 = 4",
        "5 * 6 = 30",
        "10 / 2 = 5",
        "sqrt(16) = 4",
        "100 - 45 = 55"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.history_recycler)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = HistoryAdapter(historyList)
    }
} 