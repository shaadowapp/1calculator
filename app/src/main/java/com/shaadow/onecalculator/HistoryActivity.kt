package com.shaadow.onecalculator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shaadow.onecalculator.HistorySectionAdapter
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
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
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest

class HistoryActivity : AppCompatActivity() {
    private lateinit var adapter: HistorySectionAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var db: HistoryDatabase
    private var allItems = mutableListOf<HistoryEntity>() // for search
    private var historyJob: Job? = null
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.layout_history)

            initializeViews()
            setupGestureDetector()
            setupRecyclerView()
            setupSearch()
            loadHistory()
            setupClickListeners()
            
            // Add a test entry to verify functionality
            addTestEntry()
        } catch (e: Exception) {
            // Log the error and show a user-friendly message
            e.printStackTrace()
            Toast.makeText(this, "Error initializing history screen", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initializeViews() {
        try {
            recyclerView = findViewById(R.id.history_recycler)
            recyclerView.layoutManager = LinearLayoutManager(this)
            db = HistoryDatabase.getInstance(this)
            
            // Test database connectivity
            testDatabaseConnection()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error initializing views", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun testDatabaseConnection() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Try to access the database to ensure it's working
                db.historyDao().getRecentHistory()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    e.printStackTrace()
                    Toast.makeText(this@HistoryActivity, "Database connection error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupGestureDetector() {
        val rootView = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.history_root)
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                if (e1 == null || e2 == null) return false
                val deltaY = e2.y - e1.y
                val deltaX = e2.x - e1.x
                if (deltaY < -120 && Math.abs(deltaY) > Math.abs(deltaX)) {
                    finish() // Swipe up to return to calculator
                    return true
                }
                if (deltaX > 120 && Math.abs(deltaX) > Math.abs(deltaY)) {
                    startActivity(Intent(this@HistoryActivity, AdvancedActivity::class.java)) // Swipe right for home
                    return true
                }
                return false
            }
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null || e2 == null) return false
                val deltaY = e2.y - e1.y
                val deltaX = e2.x - e1.x
                if (deltaY < -200 && Math.abs(velocityY) > 800 && Math.abs(deltaY) > Math.abs(deltaX)) {
                    finish()
                    return true
                }
                if (deltaX > 200 && Math.abs(velocityX) > 800 && Math.abs(deltaX) > Math.abs(deltaY)) {
                    startActivity(Intent(this@HistoryActivity, AdvancedActivity::class.java))
                    return true
                }
                return false
            }
        })
        rootView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    private fun setupRecyclerView() {
        adapter = HistorySectionAdapter { item, position ->
            deleteHistoryItem(item, position)
        }
        recyclerView.adapter = adapter

        // Swipe to delete (LEFT)
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {
                val pos = vh.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    // Find the actual history item from the adapter
                    val item = adapter.getItemAt(pos)
                    if (item is HistoryItem.HistoryEntry) {
                        deleteHistoryItem(item.entity, item.position)
                    }
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun deleteHistoryItem(item: HistoryEntity, position: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                db.historyDao().deleteById(item.id)
                withContext(Dispatchers.Main) {
                    allItems.remove(item)
                    adapter.updateHistory(allItems)
                    if (allItems.isEmpty()) showNoHistory()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@HistoryActivity, "Error deleting item", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupSearch() {
        val searchInput = findViewById<EditText>(R.id.search_input) ?: return

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
            adapter.updateHistory(filtered)
            val notFoundText = findViewById<TextView>(R.id.no_history_found)
            notFoundText?.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
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

        // Make search input focusable only after user taps it
        searchInput.setOnClickListener {
            if (!searchInput.isFocusable) {
                searchInput.isFocusable = true
                searchInput.isFocusableInTouchMode = true
                searchInput.requestFocus()
                val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(searchInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    private fun loadHistory() {
        historyJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                db.historyDao().getAllHistory().collectLatest { allHistory ->
                    withContext(Dispatchers.Main) {
                        try {
                            allItems.clear()
                            allItems.addAll(allHistory)
                            adapter.updateHistory(allItems)
                            if (allItems.isEmpty()) showNoHistory()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(this@HistoryActivity, "Error updating UI", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    e.printStackTrace()
                    Toast.makeText(this@HistoryActivity, "Error loading history", Toast.LENGTH_SHORT).show()
                    showNoHistory()
                }
            }
        }
    }

    private fun setupClickListeners() {
        findViewById<Button>(R.id.btn_clear_all)?.setOnClickListener {
            clearAllHistory()
        }

        findViewById<android.widget.ImageButton>(R.id.btn_back)?.setOnClickListener {
            finish()
        }
        
        findViewById<com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton>(R.id.fab_calculator)?.setOnClickListener {
            finish()
        }

        setupNoHistoryText()
    }

    private fun clearAllHistory() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                db.historyDao().clearAll()
                withContext(Dispatchers.Main) {
                    allItems.clear()
                    adapter.updateHistory(allItems)
                    showNoHistory()
                    Toast.makeText(this@HistoryActivity, R.string.clear_all, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@HistoryActivity, "Error clearing history", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupNoHistoryText() {
        val notFoundText = TextView(this).apply {
            id = View.generateViewId()
            text = "No history found"
            setTextColor(Color.LTGRAY)
            textSize = 18f
            visibility = View.GONE
        }
        val layout = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.history_root)
        layout?.addView(notFoundText)
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
        try {
            allItems.clear()
            allItems.add(HistoryEntity(expression = getString(R.string.no_history), result = ""))
            adapter.updateHistory(allItems)
        } catch (e: Exception) {
            // Fallback if string resource is not available
            allItems.clear()
            allItems.add(HistoryEntity(expression = "No history yet", result = ""))
            adapter.updateHistory(allItems)
        }
    }

    private fun addTestEntry() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Add a test entry if no history exists
                val recentHistory = db.historyDao().getRecentHistory()
                if (recentHistory.isEmpty()) {
                    db.historyDao().insert(HistoryEntity(
                        expression = "2 + 2",
                        result = "4"
                    ))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        historyJob?.cancel()
        searchJob?.cancel()
    }

    override fun onPause() {
        super.onPause()
        // Cancel any ongoing operations when activity is paused
        historyJob?.cancel()
        searchJob?.cancel()
    }

    override fun onResume() {
        super.onResume()
        // Reload history when activity resumes
        if (allItems.isEmpty()) {
            loadHistory()
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // Handle configuration changes gracefully
        // The activity will not be recreated due to configChanges in manifest
    }

    override fun onLowMemory() {
        super.onLowMemory()
        // Cancel jobs and clear unnecessary data when memory is low
        historyJob?.cancel()
        searchJob?.cancel()
    }
} 