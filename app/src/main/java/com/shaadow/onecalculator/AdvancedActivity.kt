package com.shaadow.onecalculator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Button
import android.view.View
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import android.graphics.drawable.GradientDrawable
import android.widget.LinearLayout
import androidx.core.view.marginTop
import org.json.JSONObject
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup

class AdvancedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.layout_advanced)
            supportActionBar?.hide()

            val searchInput = findViewById<EditText>(R.id.search_input)
            val searchResultsRecycler = findViewById<RecyclerView>(R.id.search_results_recycler)
            val homeScrollView = findViewById<View>(R.id.home_scrollview)
            val fab = findViewById<View>(R.id.fab_calculator)

            val categories = loadCategoriesFromJson()
            val recentHistory = mutableListOf<HistoryEntity>()
            val allHistory = mutableListOf<HistoryEntity>()
            val searchResults = mutableListOf<SearchResultSection>()
            val searchAdapter = SearchResultsAdapter(searchResults) { result ->
                when (result) {
                    is SearchResult.HistoryItem -> {
                        val intent = Intent(this, MainActivity::class.java)
                        intent.putExtra("expression", result.entity.expression)
                        intent.putExtra("result", result.entity.result)
                        startActivity(intent)
                    }
                    is SearchResult.CalculatorItem -> {
                        // TODO: Open respective calculator screen
                        Toast.makeText(this, "Open calculator: ${result.category} - ${result.label}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            searchResultsRecycler.layoutManager = LinearLayoutManager(this)
            searchResultsRecycler.adapter = searchAdapter

            fun updateSearchResults(query: String) {
                searchResults.clear()
                if (query.isBlank()) {
                    searchResultsRecycler.visibility = View.GONE
                    homeScrollView.visibility = View.VISIBLE
                    fab.visibility = View.VISIBLE
                    return
                }
                // Group: Recent Calculations
                val recent = recentHistory.filter {
                    it.expression.contains(query, true) || it.result.contains(query, true)
                }
                if (recent.isNotEmpty()) {
                    searchResults.add(SearchResultSection("From Recent Calculations", recent.map { SearchResult.HistoryItem(it) }))
                }
                // Group: All History
                val history = allHistory.filter {
                    it.expression.contains(query, true) || it.result.contains(query, true)
                }
                if (history.isNotEmpty()) {
                    searchResults.add(SearchResultSection("From All History", history.map { SearchResult.HistoryItem(it) }))
                }
                // Group: Categories
                for (cat in categories) {
                    val matches = cat.buttons.filter { it.contains(query, true) }
                    if (matches.isNotEmpty()) {
                        searchResults.add(SearchResultSection("From ${cat.name}", matches.map { SearchResult.CalculatorItem(cat.name, it) }))
                    }
                }
                searchAdapter.updateSections(searchResults)
                searchResultsRecycler.visibility = View.VISIBLE
                homeScrollView.visibility = View.GONE
                fab.visibility = View.GONE
            }

            // Load history from DB
            lifecycleScope.launch {
                try {
                    val db = HistoryDatabase.getInstance(this@AdvancedActivity)
                    db.historyDao().getAllHistory().collect { all ->
                        allHistory.clear()
                        allHistory.addAll(all)
                    }
                    val recent = db.historyDao().getRecentHistory()
                    recentHistory.clear()
                    recentHistory.addAll(recent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            searchInput.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val query = s?.toString() ?: ""
                    updateSearchResults(query)
                }
                override fun afterTextChanged(s: Editable?) {}
            })
            setupRecentHistory()
            findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }
            // findViewById<ImageButton>(R.id.btn_search).setOnClickListener {
            //     Toast.makeText(this, "Search clicked (stub)", Toast.LENGTH_SHORT).show()
            // }
            findViewById<ExtendedFloatingActionButton>(R.id.fab_calculator).setOnClickListener { finish() }
            findViewById<Button>(R.id.btn_view_all_history).setOnClickListener {
                startActivity(Intent(this, HistoryActivity::class.java))
            }
            
            // Hot Apps button
            findViewById<ImageButton>(R.id.btn_hot_apps).setOnClickListener {
                Toast.makeText(this, "Hot Apps", Toast.LENGTH_SHORT).show()
            }
            
            // Settings button
            findViewById<ImageButton>(R.id.btn_settings).setOnClickListener {
                Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()
            }

            // After loading categories, display all categories on home screen
            for (cat in categories) {
                val resId = getCategoryFlexboxId(cat.name)
                if (resId != null) {
                    addFlexButtons(resId, cat.buttons)
                }
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
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error initializing advanced screen", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    data class HomeCategory(val name: String, val buttons: List<String>)

    private fun loadCategoriesFromJson(): List<HomeCategory> {
        val categories = mutableListOf<HomeCategory>()
        try {
            val jsonStr = assets.open("home_categories.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonStr)
            val arr = root.getJSONArray("categories")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val name = obj.getString("name")
                val btnArr = obj.getJSONArray("buttons")
                val btns = mutableListOf<String>()
                for (j in 0 until btnArr.length()) {
                    btns.add(btnArr.getString(j))
                }
                categories.add(HomeCategory(name, btns))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Return default categories if JSON file is missing or corrupted
            categories.add(HomeCategory("Basic", listOf("Calculator", "History", "Settings")))
            Toast.makeText(this, "Error loading categories", Toast.LENGTH_SHORT).show()
        }
        return categories
    }

    private fun getCategoryFlexboxId(name: String): Int? {
        return when (name.lowercase()) {
            "algebra" -> R.id.algebra_buttons
            "geometry" -> R.id.geometry_buttons
            "finance" -> R.id.finance_buttons
            "insurance" -> R.id.insurance_buttons
            "health" -> R.id.health_buttons
            "date & time" -> R.id.date_time_buttons
            "unit converters" -> R.id.unit_converters_buttons
            "others" -> R.id.others_buttons
            else -> null
        }
    }

    private fun setupRecentHistory() {
        val container = findViewById<LinearLayout>(R.id.recent_history_container)
        container.removeAllViews()

        val density = resources.displayMetrics.density
        val boxWidth = (density * 180).toInt()
        val boxHeight = (density * 90).toInt()
        val margin = (density * 5).toInt()

        // Use Room DB for last 5
        lifecycleScope.launch {
            val db = HistoryDatabase.getInstance(this@AdvancedActivity)
            val allHistory = db.historyDao().getRecentHistory()
            val historyToShow = if (allHistory.isNotEmpty()) allHistory else listOf(HistoryEntity(expression = "No history yet", result = ""))
            runOnUiThread {
                for (item in historyToShow) {
                    val box = LinearLayout(this@AdvancedActivity)
                    box.orientation = LinearLayout.VERTICAL
                    val params = LinearLayout.LayoutParams(boxWidth, boxHeight)
                    params.setMargins(margin, 0, margin, 0)
                    box.layoutParams = params

                    val bg = GradientDrawable()
                    bg.setColor(0xFF181C20.toInt())
                    bg.setStroke(4, resources.getColor(R.color.muted_border, null))
                    bg.cornerRadius = 35f
                    box.background = bg
                    box.setPadding(35, 20, 35, 20)

                    val exprView = TextView(this@AdvancedActivity)
                    exprView.text = item.expression.replaceFirstChar { it.uppercase() }
                    exprView.setTextColor(android.graphics.Color.GRAY)
                    exprView.textSize = 20f
                    exprView.setSingleLine(true)

                    val solView = TextView(this@AdvancedActivity)
                    solView.text = item.result
                    solView.setTextColor(android.graphics.Color.WHITE)
                    solView.textSize = 28f
                    solView.setPadding(0,15,0,0)
                    solView.setSingleLine(true)

                    box.addView(exprView)
                    box.addView(solView)

                    box.setOnClickListener {
                        Toast.makeText(this@AdvancedActivity, "Clicked: ${item.expression} = ${item.result}", Toast.LENGTH_SHORT).show()
                    }

                    container.addView(box)
                }
            }
        }
    }

    private fun addFlexButtons(containerId: Int, calculators: List<String>) {
        val container = findViewById<FlexboxLayout>(containerId)
        container.removeAllViews()

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val density = displayMetrics.density

        val minBoxesPerRow = 3
        val parentPaddingPx = (16 * density).toInt() * 2 // 16dp left + 16dp right from parent LinearLayout
        val boxMarginPx = (8 * density).toInt()
        val totalSpacing = (minBoxesPerRow - 1) * boxMarginPx
        val availableWidth = screenWidth - parentPaddingPx - totalSpacing
        val boxWidth = availableWidth / minBoxesPerRow
        val boxHeight = (boxWidth * 0.9).toInt()

        for ((i, label) in calculators.withIndex()) {
            // If label has multiple words, insert a line break after the first word
            val words = label.trim().split(" ")
            val displayText = if (words.size > 1) words[0] + "\n" + words.subList(1, words.size).joinToString(" ") else label
            val button = Button(this).apply {
                text = displayText
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundResource(android.R.color.transparent)
                textSize = 18f
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                gravity = android.view.Gravity.CENTER
                isAllCaps = false
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                setLineSpacing(0f, 1.18f)
            }

            // Only add left margin to boxes after the first in a row
            val leftMargin = if (i % minBoxesPerRow == 0) 0 else boxMarginPx
            val params = FlexboxLayout.LayoutParams(boxWidth, boxHeight).apply {
                setMargins(leftMargin, boxMarginPx, 0, boxMarginPx)
            }

            val bg = GradientDrawable().apply {
                setColor(0xFF181C20.toInt())
                setStroke(4, resources.getColor(R.color.muted_border, null))
                cornerRadius = 35f
            }

            button.background = bg
            button.layoutParams = params
            button.setOnClickListener {
                Toast.makeText(this@AdvancedActivity, "${button.text} clicked (stub)", Toast.LENGTH_SHORT).show()
            }

            container.addView(button)
        }
    }

}

sealed class SearchResult {
    data class HistoryItem(val entity: HistoryEntity) : SearchResult()
    data class CalculatorItem(val category: String, val label: String) : SearchResult()
}
data class SearchResultSection(val title: String, val items: List<SearchResult>)

class SearchResultsAdapter(
    private var sections: List<SearchResultSection>,
    private val onClick: (SearchResult) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }
    private val flatList = mutableListOf<Pair<Int, Any>>() // 0=header, 1=item
    init { rebuildFlatList() }
    override fun getItemViewType(position: Int): Int = flatList[position].first
    override fun getItemCount(): Int = flatList.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val tv = TextView(parent.context)
            tv.setPadding(24, 32, 0, 12)
            tv.setTextColor(parent.context.getColor(R.color.category_grey))
            tv.textSize = 16f
            object : RecyclerView.ViewHolder(tv) {}
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.history_item, parent, false)
            object : RecyclerView.ViewHolder(view) {}
        }
    }
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val (type, data) = flatList[position]
        if (type == TYPE_HEADER) {
            (holder.itemView as TextView).text = data as String
        } else {
            val item = data as SearchResult
            val exprView = holder.itemView.findViewById<TextView>(R.id.tv_expression)
            val resView = holder.itemView.findViewById<TextView>(R.id.tv_result)
            val deleteBtn = holder.itemView.findViewById<ImageButton>(R.id.btn_delete)
            
            // Hide delete button for search results
            deleteBtn.visibility = View.GONE
            
            when (item) {
                is SearchResult.HistoryItem -> {
                    exprView.text = item.entity.expression
                    resView.text = item.entity.result
                    holder.itemView.setOnClickListener { onClick(item) }
                }
                is SearchResult.CalculatorItem -> {
                    exprView.text = item.label
                    resView.text = ""
                    holder.itemView.setOnClickListener { onClick(item) }
                }
            }
        }
    }
    private fun rebuildFlatList() {
        flatList.clear()
        for (section in sections) {
            flatList.add(TYPE_HEADER to section.title)
            for (item in section.items) {
                flatList.add(TYPE_ITEM to item)
            }
        }
    }
    fun updateSections(newSections: List<SearchResultSection>) {
        this.sections = newSections
        rebuildFlatList()
        super.notifyDataSetChanged()
    }
}
