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

class AdvancedActivity : AppCompatActivity() {

    private val historyList = listOf(
        "2 + 2 = 4", "5 * 6 = 30", "10 / 2 = 5", "sqrt(16) = 4", "100 - 45 = 55", "3^2 = 9"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_advanced)
        supportActionBar?.hide()

        val categories = loadCategoriesFromJson()
        for (cat in categories) {
            val resId = getCategoryFlexboxId(cat.name)
            if (resId != null) {
                addFlexButtons(resId, cat.buttons)
            }
        }
        setupRecentHistory()
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btn_search).setOnClickListener {
            Toast.makeText(this, "Search clicked (stub)", Toast.LENGTH_SHORT).show()
        }
        findViewById<ExtendedFloatingActionButton>(R.id.fab_calculator).setOnClickListener { finish() }
        findViewById<Button>(R.id.btn_view_all_history).setOnClickListener {
            Toast.makeText(this, "View All History clicked", Toast.LENGTH_SHORT).show()
        }
    }

    data class HomeCategory(val name: String, val buttons: List<String>)

    private fun loadCategoriesFromJson(): List<HomeCategory> {
        val categories = mutableListOf<HomeCategory>()
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
            "unit converters" -> R.id.others_buttons
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
        val margin = (density * 5).toInt() // more spacing between boxes

        for (item in historyList.take(5)) {
            val box = LinearLayout(this)
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

            val parts = item.split("=")
            val expr = parts.getOrNull(0)?.trim()?.replaceFirstChar { it.uppercase() } ?: ""
            val sol = parts.getOrNull(1)?.trim() ?: ""

            val exprView = TextView(this)
            exprView.text = expr
            exprView.setTextColor(android.graphics.Color.GRAY)
            exprView.textSize = 20f
            exprView.setSingleLine(true)

            val solView = TextView(this)
            solView.text = sol
            solView.setTextColor(android.graphics.Color.WHITE)
            solView.textSize = 28f
            solView.setPadding(0,15,0,0)
            solView.setTypeface(null, android.graphics.Typeface.BOLD)
            solView.setSingleLine(true)

            box.addView(exprView)
            box.addView(solView)

            box.setOnClickListener {
                Toast.makeText(this, "Clicked: $item", Toast.LENGTH_SHORT).show()
            }

            container.addView(box)
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
            val button = Button(this).apply {
                text = label.replaceFirstChar { it.uppercase() }
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundResource(android.R.color.transparent)
                textSize = 17f
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                gravity = android.view.Gravity.CENTER
                isAllCaps = false
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
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
