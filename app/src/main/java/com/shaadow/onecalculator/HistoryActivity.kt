package com.shaadow.onecalculator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
// Import the shared adapter
import com.shaadow.onecalculator.HistoryAdapter
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class HistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_history)

        val recyclerView = findViewById<RecyclerView>(R.id.history_recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        lifecycleScope.launch {
            val db = HistoryDatabase.getInstance(this@HistoryActivity)
            val allHistory = db.historyDao().getAllHistory()
            runOnUiThread {
                val adapter = if (allHistory.isEmpty()) HistoryAdapter(listOf(HistoryEntity(expression = "No history yet", result = ""))) else HistoryAdapter(allHistory)
                recyclerView.adapter = adapter
            }
        }

        findViewById<android.widget.ImageButton>(R.id.btn_back).setOnClickListener {
            finish()
        }
        findViewById<android.widget.ImageButton>(R.id.btn_search).setOnClickListener {
            android.widget.Toast.makeText(this, "Search clicked (stub)", android.widget.Toast.LENGTH_SHORT).show()
        }
        findViewById<com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton>(R.id.fab_calculator).setOnClickListener {
            finish()
        }
    }
} 