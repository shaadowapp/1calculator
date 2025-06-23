package com.shaadow.onecalculator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Toast
import android.widget.ImageButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.shaadow.onecalculator.UnitAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import android.graphics.drawable.GradientDrawable
import android.view.View

class AdvancedActivity : AppCompatActivity() {
    // Updated calculators for each category (only those specified)
    private val algebraCalculators = listOf("Percentage", "Average", "Ratio", "Equations", "Fractions")
    private val geometryCalculators = listOf("Shapes", "Bodies")
    private val financeCalculators = listOf("Currency Converter", "Unit Price", "Sales Tax", "Loan & Emi", "Interest", "Gst", "Fd", "Rd", "Sip")
    private val insuranceCalculators = listOf("Epf", "Mortage", "Apy", "Brokerage", "Retirement", "Loan", "Income Tax")
    private val healthCalculators = listOf("Bmi", "Caloric Burn", "Body Fat")
    private val dateTimeCalculators = listOf("Age Calculator", "Time Interval")
    private val unitConvertersCalculators = listOf(
        "Acceleration", "Angle", "Area", "Cooking", "Data Storage", "Data Transfer", "Discount", "Energy", "Force", "Fuel", "Length", "Numeric Base", "Power", "Pressure", "Roman Numerals", "Shoe Size", "Speed", "Tempreture", "Time", "Torque", "Volume", "Weight"
    )
    private val otherCalculators = listOf("Mileage", "Ohms Law")

    private val historyList = listOf(
        "2 + 2 = 4",
        "5 * 6 = 30",
        "10 / 2 = 5",
        "sqrt(16) = 4",
        "100 - 45 = 55",
        "3^2 = 9"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_advanced)
        supportActionBar?.hide()

        // Populate recent history as clickable boxes (horizontal)
        val recentHistoryContainer = findViewById<LinearLayout>(R.id.recent_history_container)
        recentHistoryContainer.removeAllViews()
        val boxMargin = resources.displayMetrics.density.times(5).toInt()
        val boxWidth = resources.displayMetrics.density.times(180).toInt()
        val boxHeight = resources.displayMetrics.density.times(90).toInt()
        for (item in historyList.take(5)) {
            val box = LinearLayout(this)
            box.orientation = LinearLayout.VERTICAL
            val params = LinearLayout.LayoutParams(boxWidth, boxHeight)
            params.setMargins(boxMargin, 0, boxMargin, 0)
            box.layoutParams = params
            val bg = GradientDrawable()
            bg.setColor(android.graphics.Color.parseColor("#181C20"))
            bg.setStroke(4, resources.getColor(R.color.muted_border, null))
            bg.cornerRadius = 35f
            box.background = bg
            box.setPadding(35, 20, 35, 20)

            // Split expression and solution
            val parts = item.split("=")
            val expr = parts.getOrNull(0)?.trim()?.replaceFirstChar { it.uppercase() } ?: ""
            val sol = parts.getOrNull(1)?.trim() ?: ""

            val exprView = TextView(this)
            exprView.text = expr
            exprView.setTextColor(android.graphics.Color.GRAY)
            exprView.textSize = 20f
            exprView.setSingleLine(true)
            exprView.ellipsize = android.text.TextUtils.TruncateAt.END
            exprView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)

            val solView = TextView(this)
            solView.text = sol
            solView.setTextColor(android.graphics.Color.WHITE)
            solView.textSize = 28f
            solView.setTypeface(null, android.graphics.Typeface.BOLD)
            solView.setSingleLine(true)
            solView.ellipsize = android.text.TextUtils.TruncateAt.END
            solView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

            box.addView(exprView)
            box.addView(solView)
            box.setOnClickListener {
                Toast.makeText(this, "Clicked: $item", Toast.LENGTH_SHORT).show()
            }
            recentHistoryContainer.addView(box)
        }
        findViewById<Button>(R.id.btn_view_all_history).setOnClickListener {
            Toast.makeText(this, "View All History clicked", Toast.LENGTH_SHORT).show()
        }

        // Helper to add grid boxes for a category (3 columns, fixed size, styled)
        addGridButtons(R.id.algebra_buttons, algebraCalculators)
        addGridButtons(R.id.geometry_buttons, geometryCalculators)
        addGridButtons(R.id.finance_buttons, financeCalculators)
        addGridButtons(R.id.health_buttons, healthCalculators)
        addGridButtons(R.id.date_time_buttons, dateTimeCalculators)
        addGridButtons(R.id.other_units_buttons, unitConvertersCalculators)
        addGridButtons(R.id.insurance_buttons, insuranceCalculators)
        // Add others section if you want to display separately
        // addGridButtons(R.id.others_buttons, otherCalculators)

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            finish()
        }
        findViewById<ImageButton>(R.id.btn_search).setOnClickListener {
            Toast.makeText(this, "Search clicked (stub)", Toast.LENGTH_SHORT).show()
        }
        findViewById<ExtendedFloatingActionButton>(R.id.fab_calculator).setOnClickListener {
            finish()
        }
    }

    // Helper to add grid boxes for a category (3 columns, fixed size, styled)
    private fun addGridButtons(containerId: Int, calculators: List<String>) {
        val container = findViewById<LinearLayout>(containerId)
        container.removeAllViews()
        val columns = 3
        val rowVerticalMargin = resources.displayMetrics.density.times(8).toInt()
        val boxHorizontalMargin = resources.displayMetrics.density.times(8).toInt()
        val boxWidth = resources.displayMetrics.density.times(110).toInt()
        val boxHeight = resources.displayMetrics.density.times(72).toInt()
        var row: LinearLayout? = null
        calculators.forEachIndexed { i, calc ->
            if (i % columns == 0) {
                row = LinearLayout(this)
                row!!.orientation = LinearLayout.HORIZONTAL
                val rowParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                rowParams.setMargins(0, rowVerticalMargin, 0, rowVerticalMargin)
                row!!.layoutParams = rowParams
            }
            val trimmed = calc.trim()
            val capText = if (trimmed.length > 1) trimmed.substring(0, 1).uppercase() + trimmed.substring(1).lowercase() else trimmed.uppercase()
            val btn = Button(this)
            btn.text = capText
            btn.setTextColor(android.graphics.Color.WHITE)
            btn.setBackgroundResource(android.R.color.transparent)
            btn.textSize = 18f
            btn.textAlignment = View.TEXT_ALIGNMENT_CENTER
            btn.gravity = android.view.Gravity.CENTER
            btn.isAllCaps = false
            btn.setPadding(16, 24, 16, 24)
            btn.maxLines = 2
            btn.ellipsize = android.text.TextUtils.TruncateAt.END
            val params = LinearLayout.LayoutParams(boxWidth, boxHeight)
            params.setMargins(boxHorizontalMargin, 0, boxHorizontalMargin, 0)
            btn.layoutParams = params
            // Style the box
            val bg = GradientDrawable()
            bg.setColor(android.graphics.Color.parseColor("#181C20"))
            bg.setStroke(4, resources.getColor(R.color.muted_border, null))
            bg.cornerRadius = 24f
            btn.background = bg
            btn.setOnClickListener {
                Toast.makeText(this, "$capText clicked (stub)", Toast.LENGTH_SHORT).show()
            }
            row?.addView(btn)
            // If last item in row or last item overall, fill row with invisible spacers if needed
            val isLastInRow = (i % columns == columns - 1)
            val isLastItem = (i == calculators.lastIndex)
            if (isLastInRow || isLastItem) {
                if (isLastItem && (i % columns != columns - 1)) {
                    val emptyCount = columns - 1 - (i % columns)
                    repeat(emptyCount) {
                        val spacer = View(this)
                        spacer.layoutParams = LinearLayout.LayoutParams(boxWidth, boxHeight)
                        row?.addView(spacer)
                    }
                }
                container.addView(row)
            }
        }
    }
} 