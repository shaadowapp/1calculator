package com.shaadow.onecalculator

import android.os.Bundle
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.widget.Toast
import android.widget.TextView
import android.widget.GridLayout
import android.content.res.Configuration
import androidx.window.layout.WindowMetricsCalculator
import org.json.JSONObject
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import android.text.Editable
import android.text.TextWatcher
import androidx.recyclerview.widget.LinearLayoutManager
import android.content.Intent
import com.google.android.material.transition.MaterialFadeThrough
import androidx.transition.TransitionManager
import com.shaadow.onecalculator.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var searchAdapter: SearchResultsAdapter
    private val allHistory = mutableListOf<HistoryEntity>()
    private val searchResults = mutableListOf<SearchResultSection>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupAdapters()
        setupRecyclerViews()
        setupSearchBar()
        setupCategoryButtons()
        setupFAB()
    }
    
    private fun setupAdapters() {
        // Setup search adapter
        searchAdapter = SearchResultsAdapter(searchResults) { result ->
            when (result) {
                is SearchResult.HistoryItem -> {
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    intent.putExtra("expression", result.entity.expression)
                    intent.putExtra("result", result.entity.result)
                    startActivity(intent)
                }
                is SearchResult.CalculatorItem -> {
                    val dialog = CalculatorHostDialog.newInstance(result.label)
                    dialog.show(parentFragmentManager, "calculator_dialog")
                }
            }
        }
    }
    
    private fun setupRecyclerViews() {
        // Setup search results recycler
        binding.searchResultsRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchAdapter
        }
    }
    
    private fun setupSearchBar() {
        // Get search EditText from the TextInputLayout
        val textInputLayout = binding.root.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.textInputLayout)
        val searchInput = textInputLayout.editText!!
        
        fun updateSearchResults(query: String) {
            searchResults.clear()
            if (query.isBlank()) {
                // Animate to home scroll view
                animateViewTransition(binding.searchResultsRecycler, binding.homeScrollview, false)
                return
            }
            
            val categories = loadCategoriesFromJson()
            
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
            // Animate to search results view
            animateViewTransition(binding.homeScrollview, binding.searchResultsRecycler, true)
        }
        
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                updateSearchResults(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Make search input focusable only after user taps it
        searchInput.setOnClickListener {
            if (!searchInput.isFocusable()) {
                searchInput.setFocusable(true)
                searchInput.setFocusableInTouchMode(true)
                searchInput.requestFocus()
                val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(searchInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }
    

    
    private fun setupCategoryButtons() {
        val categories = loadCategoriesFromJson()
        
        // After loading categories, display all categories on home screen
        for (cat in categories) {
            val resId = getCategoryGridLayoutId(cat.name)
            if (resId != null) {
                addGridButtons(resId, cat.buttons)
            }
        }
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Update grid layouts when configuration changes (e.g., orientation)
        updateGridLayouts()
    }
    
    private fun updateGridLayouts() {
        val categories = loadCategoriesFromJson()
        for (cat in categories) {
            val resId = getCategoryGridLayoutId(cat.name)
            if (resId != null) {
                val gridLayout = binding.root.findViewById<GridLayout>(resId)
                val newColumnCount = getResponsiveColumnCount()
                if (gridLayout.columnCount != newColumnCount) {
                    gridLayout.columnCount = newColumnCount
                    // Re-add buttons with new layout
                    addGridButtons(resId, cat.buttons)
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class HomeCategory(val name: String, val buttons: List<String>)
    
    private fun getResponsiveColumnCount(): Int {
        // Get window metrics to calculate the window width class
        val windowMetrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(requireActivity())
        val widthDp = windowMetrics.bounds.width() / resources.displayMetrics.density
        
        return when {
            widthDp < 600 -> resources.getInteger(R.integer.grid_columns_compact) // Compact
            widthDp < 840 -> resources.getInteger(R.integer.grid_columns_medium) // Medium
            else -> resources.getInteger(R.integer.grid_columns_expanded) // Expanded
        }
    }

    private fun animateViewTransition(fromView: View, toView: View, isShowingSearch: Boolean) {
        // Create MaterialFadeThrough transition
        val fadeThrough = MaterialFadeThrough().apply {
            duration = 300
        }
        
        // Begin delayed transition
        TransitionManager.beginDelayedTransition(
            binding.root,
            fadeThrough
        )
        
        // Update visibility
        fromView.visibility = View.GONE
        toView.visibility = View.VISIBLE
    }

    private fun loadCategoriesFromJson(): List<HomeCategory> {
        val categories = mutableListOf<HomeCategory>()
        try {
            val jsonStr = requireContext().assets.open("home_categories.json").bufferedReader().use { it.readText() }
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
            Toast.makeText(requireContext(), "Error loading categories", Toast.LENGTH_SHORT).show()
        }
        return categories
    }

    private fun getCategoryGridLayoutId(name: String): Int? {
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

    private fun addGridButtons(gridLayoutId: Int, buttons: List<String>) {
        val gridLayout = binding.root.findViewById<GridLayout>(gridLayoutId)
        gridLayout.removeAllViews()
        
        // Get responsive column count
        val columnCount = getResponsiveColumnCount()
        gridLayout.columnCount = columnCount
        
        for (buttonText in buttons) {
            val button = TextView(requireContext())
            button.text = buttonText
            button.setTextColor(resources.getColor(R.color.subtle_text, null))
            button.textSize = 16f  // Increased text size
            button.setPadding(12, 16, 12, 16)  // Increased padding
            button.background = resources.getDrawable(R.drawable.bg_button_rounded, null)
            button.isClickable = true
            button.isFocusable = true
            
            // Apply GridLayout specific attributes for 3-column layout with square buttons
            val params = GridLayout.LayoutParams()
            params.width = 0
            // Convert 120dp to pixels for proper square buttons
            val heightInDp = 120
            val heightInPx = (heightInDp * resources.displayMetrics.density).toInt()
            params.height = heightInPx
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            // Convert 8dp to pixels for proper margins
            val marginInDp = 8
            val marginInPx = (marginInDp * resources.displayMetrics.density).toInt()
            params.setMargins(marginInPx, marginInPx, marginInPx, marginInPx)
            button.layoutParams = params
            
            // Apply the required button attributes
            button.maxLines = 2
            button.ellipsize = android.text.TextUtils.TruncateAt.END
            button.gravity = android.view.Gravity.CENTER
            button.textAlignment = View.TEXT_ALIGNMENT_CENTER
            
            button.setOnClickListener {
                val dialog = CalculatorHostDialog.newInstance(buttonText)
                dialog.show(parentFragmentManager, "calculator_dialog")
            }
            
            gridLayout.addView(button)
        }
    }
    
    private fun setupFAB() {
        binding.fabCalculator.setOnClickListener {
            val intent = Intent(requireContext(), MainActivity::class.java)
            startActivity(intent)
        }
    }
}
