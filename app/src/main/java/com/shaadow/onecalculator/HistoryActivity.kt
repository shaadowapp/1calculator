package com.shaadow.onecalculator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
// Import the shared adapter
import com.shaadow.onecalculator.HistoryAdapter

class HistoryActivity : AppCompatActivity() {
    private val historyList = listOf(
        "2 + 2 = 4",
        "5 * 6 = 30",
        "10 / 2 = 5",
        "sqrt(16) = 4",
        "100 - 45 = 55"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_history)

        val recyclerView = findViewById<RecyclerView>(R.id.history_recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = HistoryAdapter(historyList)

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