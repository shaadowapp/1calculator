package com.shaadow.onecalculator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shaadow.onecalculator.HistoryAdapter
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import android.view.GestureDetector
import android.view.MotionEvent
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageButton
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.graphics.Color
import android.view.View
import android.widget.TextView

class HistoryActivity : AppCompatActivity() {
    private lateinit var adapter: HistoryAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var db: HistoryDatabase
    private var items = mutableListOf<HistoryEntity>()
    private var allItems = mutableListOf<HistoryEntity>() // for search

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_history)

        val rootView = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.history_root)
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                val deltaY = e2.y - (e1?.y ?: 0f)
                if (deltaY < -200 && Math.abs(velocityY) > 800 && Math.abs(deltaY) > Math.abs(e2.x - (e1?.x ?: 0f))) {
                    finish()
                    return true
                }
                return false
            }
        })
        rootView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }

        recyclerView = findViewById(R.id.history_recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        db = HistoryDatabase.getInstance(this)

        adapter = HistoryAdapter(items) { item, position ->
            lifecycleScope.launch {
                db.historyDao().deleteById(item.id)
                runOnUiThread {
                    adapter.removeAt(position)
                    if (adapter.itemCount == 0) showNoHistory()
                }
            }
        }
        recyclerView.adapter = adapter

        // Swipe to delete (LEFT)
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                val pos = vh.bindingAdapterPosition
                val item = items[pos]
                lifecycleScope.launch {
                    db.historyDao().deleteById(item.id)
                    runOnUiThread {
                        adapter.removeAt(pos)
                        if (adapter.itemCount == 0) showNoHistory()
                    }
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)

        val searchInput = findViewById<EditText>(R.id.search_input)
        val searchBtn = findViewById<ImageButton>(R.id.btn_search)

        // Remove search button
        searchBtn.visibility = View.GONE

        // Search logic with highlight
        fun highlightMatch(text: String, query: String): SpannableString {
            if (query.isBlank()) return SpannableString(text)
            val lowerText = text.lowercase()
            val lowerQuery = query.lowercase()
            val start = lowerText.indexOf(lowerQuery)
            return if (start >= 0) {
                val end = start + query.length
                val spannable = SpannableString(text)
                spannable.setSpan(
                    BackgroundColorSpan(Color.YELLOW),
                    start, end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannable
            } else {
                SpannableString(text)
            }
        }
        fun filterHistory(query: String) {
            val filtered = if (query.isBlank()) allItems else allItems.filter {
                it.expression.contains(query, ignoreCase = true) || it.result.contains(query, ignoreCase = true)
            }
            items.clear()
            items.addAll(filtered)
            adapter.notifyDataSetChanged()
            val notFoundText = findViewById<TextView>(R.id.no_history_found)
            notFoundText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        }
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                adapter.setQuery(query)
                filterHistory(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Highlight in adapter
        adapter.setHighlighter { item, query, holder ->
            holder.tvExpression.text = highlightMatch(item.expression, query)
            holder.tvResult.text = highlightMatch(item.result, query)
        }

        // Load history
        lifecycleScope.launch {
            val allHistory = db.historyDao().getAllHistory().toMutableList()
            runOnUiThread {
                allItems.clear()
                allItems.addAll(allHistory)
                items.clear()
                items.addAll(allHistory)
                adapter.notifyDataSetChanged()
                if (items.isEmpty()) showNoHistory()
            }
        }

        findViewById<Button>(R.id.btn_clear_all).setOnClickListener {
            lifecycleScope.launch {
                db.historyDao().clearAll()
                runOnUiThread {
                    items.clear()
                    adapter.notifyDataSetChanged()
                    showNoHistory()
                    Toast.makeText(this@HistoryActivity, R.string.clear_all, Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<android.widget.ImageButton>(R.id.btn_back).setOnClickListener {
            finish()
        }
        findViewById<android.widget.ImageButton>(R.id.btn_search).setOnClickListener {
            Toast.makeText(this, "Search clicked (stub)", Toast.LENGTH_SHORT).show()
        }
        findViewById<com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton>(R.id.fab_calculator).setOnClickListener {
            finish()
        }

        val notFoundText = TextView(this).apply {
            id = View.generateViewId()
            text = "No history found"
            setTextColor(Color.LTGRAY)
            textSize = 18f
            visibility = View.GONE
        }
        val layout = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.history_root)
        layout.addView(notFoundText)
        val params = notFoundText.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        params.width = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
        params.height = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
        params.topToBottom = R.id.search_bar_container
        params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
        params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
        params.topMargin = 24
        notFoundText.layoutParams = params
    }

    private fun showNoHistory() {
        items.clear()
        allItems.clear()
        allItems.add(HistoryEntity(expression = getString(R.string.no_history), result = ""))
        adapter.notifyDataSetChanged()
    }
} 