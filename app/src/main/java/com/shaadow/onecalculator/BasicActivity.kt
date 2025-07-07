package com.shaadow.onecalculator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.shaadow.onecalculator.parser.Expression
import com.shaadow.onecalculator.parser.Expression.insertImplicitMultiplication
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.EditText
import android.widget.Toast
import android.widget.LinearLayout
import android.content.Intent
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.GestureDetector.SimpleOnGestureListener

class BasicActivity : AppCompatActivity() {
    private var isResultShown = false

    private lateinit var expressionTv: EditText
    private lateinit var solutionTv: TextView
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basic)

        // Show popup if in landscape (should not happen, but as a fallback)
        if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            android.widget.Toast.makeText(this, "Landscape mode is not supported.", android.widget.Toast.LENGTH_LONG).show()
        }

        expressionTv = findViewById(R.id.expression_tv)
        solutionTv = findViewById(R.id.solution_tv)

        // Prevent soft keyboard from showing up
        expressionTv.showSoftInputOnFocus = false
        
        // Hide keyboard when EditText gets focus
        expressionTv.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                hideSoftKeyboard()
            }
        }

        // Dynamically adjust expression text size
        expressionTv.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                adjustExpressionTextSize()
                updateSolutionVisibility()
                adjustSolutionTextSize()
            }
        })

        // Handle incoming intent with expression and result
        handleIncomingIntent()

        val buttons = listOf(
            R.id.button_0, R.id.button_1, R.id.button_2, R.id.button_3, R.id.button_4,
            R.id.button_5, R.id.button_6, R.id.button_7, R.id.button_8, R.id.button_9,
            R.id.button_plus, R.id.button_minus, R.id.button_multiply, R.id.button_divide,
            R.id.button_dot, R.id.button_percent, R.id.button_brackets,
            R.id.button_sqrt, R.id.button_power, R.id.button_factorial,
            R.id.button_pi, R.id.button_e
        )

        for (id in buttons) {
            val button = findViewById<MaterialButton>(id)
            button.setOnClickListener {
                val input = when (id) {
                    R.id.button_multiply -> "×"
                    R.id.button_divide -> "÷"
                    R.id.button_plus -> "+"
                    R.id.button_minus -> "-"
                    R.id.button_percent -> "%"
                    R.id.button_sqrt -> "√"
                    R.id.button_power -> "^"
                    R.id.button_factorial -> "!"
                    R.id.button_pi -> "π"
                    R.id.button_e -> "e"
                    R.id.button_dot -> "."
                    R.id.button_brackets -> getNextBracket(expressionTv.text.toString())
                    else -> button.text.toString()
                }

                if (isResultShown) {
                    expressionTv.setText(solutionTv.text.toString().removeSuffix(".0"))
                    isResultShown = false
                }

                expressionTv.visibility = View.VISIBLE
                adjustSolutionTextSize()
                appendToExpression(input)

                val expressionToEvaluate = getExpressionForCalculation()
                if (isExpressionComplete(expressionToEvaluate)) {
                    try {
                        val result = safeCalculate(expressionToEvaluate)
                        val formattedResult = formatNumberWithCommas(doubleToStringWithoutScientificNotation(result))
                        solutionTv.text = formattedResult
                        adjustSolutionTextSize()
                    } catch (_: Exception) {
                        solutionTv.text = ""
                        adjustSolutionTextSize()
                    }
                } else {
                    solutionTv.text = ""
                    adjustSolutionTextSize()
                }
            }
        }

        findViewById<MaterialButton>(R.id.button_ac).setOnClickListener {
            expressionTv.setText("")
            solutionTv.text = "0"
            expressionTv.visibility = View.VISIBLE
            solutionTv.textSize = 50f
            updateSolutionVisibility()
            adjustSolutionTextSize()
            isResultShown = false
        }

        findViewById<MaterialButton>(R.id.button_backspace).setOnClickListener {
            val text = expressionTv.text.toString()
            val selectionStart = expressionTv.selectionStart
            val selectionEnd = expressionTv.selectionEnd
            
            if (text.isNotEmpty()) {
                if (selectionStart == selectionEnd) {
                    // No text selected, delete character before cursor
                    if (selectionStart > 0) {
                        val beforeCursor = text.substring(0, selectionStart - 1)
                        val afterCursor = text.substring(selectionStart)
                        expressionTv.setText(beforeCursor + afterCursor)
                        expressionTv.setSelection(selectionStart - 1)
                    }
                } else {
                    // Text is selected, delete selection
                    val beforeSelection = text.substring(0, selectionStart)
                    val afterSelection = text.substring(selectionEnd)
                    expressionTv.setText(beforeSelection + afterSelection)
                    expressionTv.setSelection(selectionStart)
                }

                val expressionToEvaluate = getExpressionForCalculation()
                if (isExpressionComplete(expressionToEvaluate)) {
                    try {
                        val result = safeCalculate(expressionToEvaluate)
                        val formattedResult = formatNumberWithCommas(doubleToStringWithoutScientificNotation(result))
                        solutionTv.text = formattedResult
                        adjustSolutionTextSize()
                    } catch (_: Exception) {
                        solutionTv.text = ""
                        adjustSolutionTextSize()
                    }
                } else {
                    solutionTv.text = ""
                    adjustSolutionTextSize()
                }
            }
        }

        findViewById<MaterialButton>(R.id.button_equals).setOnClickListener {
            val expression = expressionTv.text.toString()
            val formattedExpression = getExpressionForCalculation()
            try {
                val result = safeCalculate(formattedExpression)
                val formattedResult = formatNumberWithCommas(doubleToStringWithoutScientificNotation(result))
                solutionTv.text = formattedResult
                expressionTv.visibility = View.GONE
                solutionTv.textSize = 58f
                isResultShown = true
                // Save to Room DB
                val expr = expression
                val res = doubleToStringWithoutScientificNotation(result)
                lifecycleScope.launch {
                    val db = HistoryDatabase.getInstance(this@BasicActivity)
                    db.historyDao().insert(HistoryEntity(expression = expr, result = res))
                }
                adjustSolutionTextSize()
            } catch (_: Exception) {
                solutionTv.text = getString(R.string.error_text)
                adjustSolutionTextSize()
            }
        }

        expressionTv.setOnLongClickListener {
            showCustomPopup(it, true)
            true
        }

        solutionTv.setOnLongClickListener {
            showCustomPopup(it, false)
            true
        }

        findViewById<android.widget.ImageButton>(R.id.btn_menu).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }
        findViewById<TextView>(R.id.btn_history).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        
        val outputArea = findViewById<LinearLayout>(R.id.output_display_area)
        gestureDetector = GestureDetector(this, object : GestureDetector.OnGestureListener {
            override fun onDown(e: MotionEvent): Boolean = true
            override fun onShowPress(e: MotionEvent) {}
            override fun onSingleTapUp(e: MotionEvent): Boolean = false
            override fun onScroll(
                e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float
            ): Boolean {
                if (e1 == null) return false
                val deltaY = e2.y - e1.y
                val deltaX = e2.x - e1.x
                if (deltaY > 120 && Math.abs(deltaY) > Math.abs(deltaX)) {
                    startActivity(Intent(this@BasicActivity, HistoryActivity::class.java))
                    return true
                }
                if (deltaX > 120 && Math.abs(deltaX) > Math.abs(deltaY)) {
                    startActivity(Intent(this@BasicActivity, HomeActivity::class.java))
                    return true
                }
                return false
            }
            override fun onLongPress(e: MotionEvent) {}
            override fun onFling(
                e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val deltaY = e2.y - e1.y
                val deltaX = e2.x - e1.x
                if (deltaY > 200 && Math.abs(velocityY) > 800 && Math.abs(deltaY) > Math.abs(deltaX)) {
                    startActivity(Intent(this@BasicActivity, HistoryActivity::class.java))
                    return true
                }
                if (deltaX > 200 && Math.abs(velocityX) > 800 && Math.abs(deltaX) > Math.abs(deltaY)) {
                    startActivity(Intent(this@BasicActivity, HomeActivity::class.java))
                    return true
                }
                return false
            }
        })
        outputArea.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    // --- Helper methods migrated from old MainActivity ---

    private fun hideSoftKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(expressionTv.windowToken, 0)
    }

    private fun adjustExpressionTextSize() {
        val length = expressionTv.text.length
        expressionTv.textSize = when {
            length > 20 -> 22f
            length > 12 -> 28f
            else -> 36f
        }
    }

    private fun updateSolutionVisibility() {
        solutionTv.visibility = if (expressionTv.text.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
    }

    private fun getNextBracket(expr: String): String {
        val open = expr.count { it == '(' }
        val close = expr.count { it == ')' }
        return if (open > close) ")" else "("
    }

    private fun safeCalculate(expr: String): Double {
        return Expression.calculate(expr)
    }

    private fun formatNumberWithCommas(number: String): String {
        return try {
            if (number.contains(".")) {
                val parts = number.split(".")
                val intPart = parts[0].toLongOrNull()?.let { java.text.NumberFormat.getInstance().format(it) } ?: parts[0]
                "$intPart.${parts[1]}"
            } else {
                number.toLongOrNull()?.let { java.text.NumberFormat.getInstance().format(it) } ?: number
            }
        } catch (e: Exception) {
            number
        }
    }

    private fun doubleToStringWithoutScientificNotation(d: Double): String {
        return if (d % 1.0 == 0.0) {
            d.toLong().toString()
        } else {
            java.math.BigDecimal(d).stripTrailingZeros().toPlainString()
        }
    }

    private fun showCustomPopup(view: View, isExpression: Boolean) {
        val popup = PopupWindow(this)
        val textView = TextView(this)
        textView.text = if (isExpression) expressionTv.text else solutionTv.text
        textView.setPadding(32, 24, 32, 24)
        textView.textSize = 20f
        textView.setTextColor(resources.getColor(android.R.color.white, null))
        textView.setBackgroundColor(resources.getColor(android.R.color.black, null))
        textView.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied Text", textView.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied!", Toast.LENGTH_SHORT).show()
            popup.dismiss()
        }
        popup.contentView = textView
        popup.isFocusable = true
        popup.width = LinearLayout.LayoutParams.WRAP_CONTENT
        popup.height = LinearLayout.LayoutParams.WRAP_CONTENT
        popup.showAsDropDown(view, 0, -view.height * 2)
    }

    private fun handleIncomingIntent() {
        val expr = intent.getStringExtra("expression")
        val res = intent.getStringExtra("result")
        if (!expr.isNullOrEmpty()) {
            expressionTv.setText(expr)
        }
        if (!res.isNullOrEmpty()) {
            solutionTv.text = res
        }
    }

    private fun appendToExpression(input: String) {
        val start = expressionTv.selectionStart
        val end = expressionTv.selectionEnd
        val text = expressionTv.text.toString()
        val newText = text.substring(0, start) + input + text.substring(end)
        expressionTv.setText(newText)
        expressionTv.setSelection(start + input.length)
    }

    private fun getExpressionForCalculation(): String {
        return expressionTv.text.toString().replace(",", "")
    }

    private fun isExpressionComplete(expr: String): Boolean {
        if (expr.isEmpty()) return false
        val open = expr.count { it == '(' }
        val close = expr.count { it == ')' }
        if (open != close) return false
        val last = expr.lastOrNull() ?: return false
        return last.isDigit() || last == ')' || last == '!' || last == 'π' || last == 'e'
    }

    private fun adjustSolutionTextSize() {
        val length = solutionTv.text.length
        solutionTv.textSize = when {
            length > 18 -> 28f
            length > 10 -> 38f
            else -> 50f
        }
    }
} 