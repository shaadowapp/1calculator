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


class MainActivity : AppCompatActivity() {
    private var isResultShown = false

    private lateinit var expressionTv: EditText
    private lateinit var solutionTv: TextView
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
                    val db = HistoryDatabase.getInstance(this@MainActivity)
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
            startActivity(Intent(this, AdvancedActivity::class.java))
        }
        findViewById<Button>(R.id.btn_history).setOnClickListener {
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
                    startActivity(Intent(this@MainActivity, HistoryActivity::class.java))
                    return true
                }
                if (deltaX > 120 && Math.abs(deltaX) > Math.abs(deltaY)) {
                    startActivity(Intent(this@MainActivity, AdvancedActivity::class.java))
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
                    startActivity(Intent(this@MainActivity, HistoryActivity::class.java))
                    return true
                }
                if (deltaX > 200 && Math.abs(velocityX) > 800 && Math.abs(deltaX) > Math.abs(deltaY)) {
                    startActivity(Intent(this@MainActivity, AdvancedActivity::class.java))
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

    private fun appendToExpression(value: String) {
        val current = expressionTv.text.toString()
        val selectionStart = expressionTv.selectionStart
        val selectionEnd = expressionTv.selectionEnd
        
        // Check if adding this value would create invalid consecutive operators
        if (!isValidInput(current, value, selectionStart, selectionEnd)) {
            return // Don't add the input if it would create invalid expression
        }
        
        if (selectionStart == selectionEnd) {
            // No text selected, insert at cursor position
            val beforeCursor = current.substring(0, selectionStart)
            val afterCursor = current.substring(selectionStart)
            val newText = beforeCursor + value + afterCursor
            val formattedText = formatExpressionWithCommas(newText)
            expressionTv.setText(formattedText)
            
            // Calculate correct cursor position after formatting
            val newCursorPosition = calculateCursorPositionAfterFormatting(
                beforeCursor, value, "", ""
            )
            expressionTv.setSelection(newCursorPosition)
        } else {
            // Text is selected, replace selection
            val beforeSelection = current.substring(0, selectionStart)
            val afterSelection = current.substring(selectionEnd)
            val newText = beforeSelection + value + afterSelection
            val formattedText = formatExpressionWithCommas(newText)
            expressionTv.setText(formattedText)
            
            // Calculate correct cursor position after formatting
            val newCursorPosition = calculateCursorPositionAfterFormatting(
                beforeSelection, value, "", ""
            )
            expressionTv.setSelection(newCursorPosition)
        }
    }

    private fun calculateCursorPositionAfterFormatting(
        beforeText: String, 
        insertedValue: String, 
        afterText: String, 
        formattedText: String
    ): Int {
        // The cursor should be at the end of the formatted inserted value
        val beforePlusInsertedFormatted = formatExpressionWithCommas(beforeText + insertedValue)
        return beforePlusInsertedFormatted.length
    }

    private fun isValidInput(currentText: String, newValue: String, selectionStart: Int, selectionEnd: Int): Boolean {
        // Define operators and special symbols
        val operators = setOf("+", "-", "×", "÷", "%", "^", "!")
        val specialSymbols = setOf("√", "π", "e", "(", ")")
        val allowedSymbols = operators + specialSymbols + setOf(".", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
        
        // If it's not an allowed symbol, reject it (prevents alphabets)
        if (newValue !in allowedSymbols) {
            return false
        }
        
        // Get the text that would result after insertion
        val beforeCursor = currentText.substring(0, selectionStart)
        val afterCursor = currentText.substring(selectionEnd)
        val resultingText = beforeCursor + newValue + afterCursor
        
        // Check for maximum number length (15 digits for any single number)
        val numberPattern = Regex("\\d+")
        val numbers = numberPattern.findAll(resultingText)
        for (match in numbers) {
            if (match.value.length > 15) {
                return false // Number too large
            }
        }
        
        // Check for consecutive operators
        val consecutiveOperators = listOf("++", "--", "××", "÷÷", "%%", "^^", "!!")
        
        for (consecutive in consecutiveOperators) {
            if (resultingText.contains(consecutive)) {
                return false
            }
        }
        
        // Check for operator at the beginning (except minus for negative numbers)
        if (resultingText.isNotEmpty() && newValue in operators && newValue != "-") {
            val firstChar = resultingText.first()
            if (firstChar.toString() in operators) {
                return false
            }
        }
        
        // Check for operator after another operator (except minus after other operators for negative numbers)
        if (beforeCursor.isNotEmpty()) {
            val lastChar = beforeCursor.last()
            if (lastChar.toString() in operators && newValue in operators) {
                // Allow minus after other operators (for negative numbers)
                if (newValue != "-") {
                    return false
                }
            }
        }
        
        // Check for consecutive commas (which would create confusing expressions)
        if (resultingText.contains(",,")) {
            return false
        }
        
        return true
    }

    private fun convertSymbolsToOperators(expression: String): String {
        return insertImplicitMultiplication(
            expression
                .replace("×", "*")
                .replace("÷", "/")
                .replace("π", "pi")
                .replace("√", "sqrt")
                .replace("^", "^")
                .replace("%", "%")
                .replace("!", "!")
                .replace("e", "e")
        )
    }

    private fun removeCommasFromExpression(expression: String): String {
        // Remove all commas from the expression for calculation
        val result = expression.replace(",", "")
        
        // Validate that all commas were removed
        if (result.contains(",")) {
            throw IllegalArgumentException("Failed to remove all commas from expression")
        }
        
        return result
    }

    private fun getExpressionForCalculation(): String {
        // Get the expression text and remove commas for calculation
        val expressionWithCommas = expressionTv.text.toString()
        
        try {
            val expressionWithoutCommas = removeCommasFromExpression(expressionWithCommas)
            return convertSymbolsToOperators(expressionWithoutCommas)
        } catch (e: Exception) {
            // Log the error and return a safe fallback
            println("Error processing expression: ${e.message}")
            return "0"
        }
    }

    private fun isExpressionComplete(expression: String): Boolean {
        return expression.isNotEmpty() &&
                (expression.last().isDigit() || expression.last() == ')' || expression.endsWith("pi") || expression.endsWith("e"))
    }

    private fun getNextBracket(expression: String): String {
        val open = expression.count { it == '(' }
        val close = expression.count { it == ')' }
        return if (open > close) ")" else "("
    }

    private fun showCustomPopup(anchor: View, isExpression: Boolean) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        val popupView = if (isExpression) {
            layoutInflater.inflate(R.layout.popup_menu_expression, null)
        } else {
            layoutInflater.inflate(R.layout.popup_menu_result, null)
        }

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val popupWidth = popupView.measuredWidth

        val popup = PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true)
        popup.elevation = 10f
        popup.setBackgroundDrawable(getDrawable(android.R.color.transparent))

        // Position to the right side of the TextView and slightly below
        val offsetX = anchor.width - popupWidth  // aligns to the right side of the anchor
        val offsetY = anchor.height / 3          // slight vertical spacing below the anchor

        popup.showAsDropDown(anchor, offsetX, offsetY)

        popupView.findViewById<TextView>(R.id.btn_copy).setOnClickListener {
            val textToCopy = if (isExpression) expressionTv.text.toString() else solutionTv.text.toString()
            if (textToCopy.isNotEmpty()) {
                val clip = ClipData.newPlainText("text", textToCopy)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
            }
            popup.dismiss()
        }

        if (isExpression) {
            popupView.findViewById<TextView>(R.id.btn_paste).setOnClickListener {
                if (clipboard.hasPrimaryClip()) {
                    val pasted = clipboard.primaryClip?.getItemAt(0)?.text.toString()
                    if (pasted.isNotEmpty() && isValidPastedExpression(pasted)) {
                        expressionTv.setText(pasted)
                        try {
                            val expressionWithoutCommas = removeCommasFromExpression(pasted)
                            val result = safeCalculate(convertSymbolsToOperators(expressionWithoutCommas))
                            val formattedResult = formatNumberWithCommas(doubleToStringWithoutScientificNotation(result))
                            solutionTv.text = formattedResult
                            adjustSolutionTextSize()
                        } catch (_: Exception) {
                            solutionTv.text = getString(R.string.error_text)
                            adjustSolutionTextSize()
                        }
                    } else {
                        Toast.makeText(this, "Invalid expression", Toast.LENGTH_SHORT).show()
                        adjustSolutionTextSize()
                    }
                }
                popup.dismiss()
            }
        }
    }

    private fun handleIncomingIntent() {
        val expression = intent.getStringExtra("expression")
        val result = intent.getStringExtra("result")
        
        if (expression != null && result != null) {
            expressionTv.setText(expression)
            solutionTv.text = result
            expressionTv.visibility = View.VISIBLE
            updateSolutionVisibility()
            adjustSolutionTextSize()
            isResultShown = false
        }
    }

    private fun hideSoftKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(expressionTv.windowToken, 0)
    }

    private fun adjustExpressionTextSize() {
        val text = expressionTv.text.toString()
        if (text.isEmpty()) {
            expressionTv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 58f)
            return
        }

        val deviceWidth = resources.displayMetrics.widthPixels
        val maxSize = 58f // Maximum text size in SP
        val minSize = 25f // Minimum text size in SP
        
        // Calculate available width (considering padding)
        val availableWidth = deviceWidth - (expressionTv.paddingLeft + expressionTv.paddingRight) * 2
        
        // Progressive reduction logic
        val paint = expressionTv.paint
        var currentSize = maxSize
        var reductionCount = 0
        
        // Keep reducing until text fits or we reach minimum size
        while (currentSize > minSize) {
            paint.textSize = currentSize * resources.displayMetrics.density
            val textWidth = paint.measureText(text)
            
            // Calculate the threshold for this reduction level
            val threshold = availableWidth + (availableWidth * reductionCount * 0.25f)
            
            if (textWidth <= threshold) {
                break // Text fits within the current threshold
            }
            
            // Reduce text size by 3sp for each level
            currentSize -= 3f
            reductionCount++
        }
        
        // If we've reached minimum size and text still doesn't fit, make it scrollable
        if (currentSize <= minSize) {
            paint.textSize = minSize * resources.displayMetrics.density
            val textWidth = paint.measureText(text)
            val maxThreshold = availableWidth + (availableWidth * 0.25f) // Final threshold
            
            if (textWidth > maxThreshold) {
                // Make it horizontally scrollable
                expressionTv.isHorizontalScrollBarEnabled = true
                expressionTv.scrollTo(textWidth.toInt(), 0)
            } else {
                expressionTv.isHorizontalScrollBarEnabled = false
            }
            currentSize = minSize
        } else {
            expressionTv.isHorizontalScrollBarEnabled = false
        }
        
        expressionTv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, currentSize)
        
        // Debug logging
        android.util.Log.d("TextSize", "Expression: '$text', Size: ${currentSize}sp, Width: ${paint.measureText(text)}px, Available: ${availableWidth}px")
    }

    private fun adjustSolutionTextSize() {
        val text = solutionTv.text.toString()
        if (text.isEmpty()) {
            solutionTv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 50f)
            return
        }

        val deviceWidth = resources.displayMetrics.widthPixels
        val maxSize = 50f // Maximum text size in SP
        val minSize = 25f // Minimum text size in SP
        
        // Calculate available width (considering padding)
        val availableWidth = deviceWidth - (solutionTv.paddingLeft + solutionTv.paddingRight) * 2
        
        // Progressive reduction logic
        val paint = solutionTv.paint
        var currentSize = maxSize
        var reductionCount = 0
        
        while (currentSize > minSize) {
            paint.textSize = currentSize * resources.displayMetrics.density
            val textWidth = paint.measureText(text)
            
            // Calculate the threshold for this reduction level
            val threshold = availableWidth + (availableWidth * reductionCount * 0.25f)
            
            if (textWidth <= threshold) {
                break // Text fits within the current threshold
            }
            
            // Reduce text size by 2sp for each level
            currentSize -= 2f
            reductionCount++
        }
        
        // If we've reached minimum size and text still doesn't fit, make it scrollable
        if (currentSize <= minSize) {
            paint.textSize = minSize * resources.displayMetrics.density
            val textWidth = paint.measureText(text)
            val maxThreshold = availableWidth + (availableWidth * 0.25f) // Final threshold
            
            if (textWidth > maxThreshold) {
                // Make it horizontally scrollable
                solutionTv.isHorizontalScrollBarEnabled = true
                solutionTv.scrollTo(textWidth.toInt(), 0)
            } else {
                solutionTv.isHorizontalScrollBarEnabled = false
            }
            currentSize = minSize
        } else {
            solutionTv.isHorizontalScrollBarEnabled = false
        }
        
        solutionTv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, currentSize)
        
        // Debug logging
        android.util.Log.d("TextSize", "Solution: '$text', Size: ${currentSize}sp, Width: ${paint.measureText(text)}px, Available: ${availableWidth}px")
    }

    private fun isValidPastedExpression(expression: String): Boolean {
        // Define operators
        val operators = setOf("+", "-", "×", "÷", "%", "^", "!")
        
        // Check for consecutive operators
        val consecutiveOperators = listOf("++", "--", "××", "÷÷", "%%", "^^", "!!")
        
        for (consecutive in consecutiveOperators) {
            if (expression.contains(consecutive)) {
                return false
            }
        }
        
        // Check for operator at the beginning (except minus for negative numbers)
        if (expression.isNotEmpty()) {
            val firstChar = expression.first().toString()
            if (firstChar in operators && firstChar != "-") {
                return false
            }
        }
        
        // Check for operator at the end
        if (expression.isNotEmpty()) {
            val lastChar = expression.last().toString()
            if (lastChar in operators) {
                return false
            }
        }
        
        // Check for multiple consecutive operators (except minus after other operators for negative numbers)
        for (i in 0 until expression.length - 1) {
            val current = expression[i].toString()
            val next = expression[i + 1].toString()
            
            if (current in operators && next in operators) {
                // Allow minus after other operators (for negative numbers)
                if (next != "-") {
                    return false
                }
            }
        }
        
        return true
    }

    private fun doubleToStringWithoutScientificNotation(number: Double): String {
        return if (number >= 1e6 || number <= -1e6) {
            String.format("%.0f", number)
        } else {
            number.toString().removeSuffix(".0")
        }
    }

    private fun formatNumberWithCommas(number: String): String {
        // Handle negative numbers
        val isNegative = number.startsWith("-")
        val absNumber = if (isNegative) number.substring(1) else number
        
        // Split into integer and decimal parts
        val parts = absNumber.split(".")
        val integerPart = parts[0]
        val decimalPart = if (parts.size > 1) "." + parts[1] else ""
        
        // Don't format 4-digit numbers (1000-9999)
        if (integerPart.length == 4) {
            val result = integerPart + decimalPart
            return if (isNegative) "-$result" else result
        }
        
        // Apply Indian comma system: last 3 digits, then every 2 digits
        val formattedInteger = StringBuilder()
        val length = integerPart.length
        
        // For Indian system: group from right to left
        // Last 3 digits, then every 2 digits
        // Calculate comma positions from right to left
        val commaPositions = mutableListOf<Int>()
        var pos = length - 3 // Start after last 3 digits
        
        while (pos > 0) {
            commaPositions.add(pos)
            pos -= 2 // Every 2 digits
        }
        
        // Build the formatted string
        for (i in 0 until length) {
            if (i in commaPositions) {
                formattedInteger.append(",")
            }
            formattedInteger.append(integerPart[i])
        }
        
        val result = formattedInteger.toString() + decimalPart
        return if (isNegative) "-$result" else result
    }

    private fun formatExpressionWithCommas(expression: String): String {
        // Format only the numbers in the expression, not operators
        val numberPattern = Regex("\\d+(\\.\\d+)?")
        return numberPattern.replace(expression) { matchResult ->
            formatNumberWithCommas(matchResult.value)
        }
    }

    private fun hasFunctionalButton(expression: String): Boolean {
        val functionalButtons = setOf("+", "-", "×", "÷", "%", "^", "!", "√", "π", "e", "(", ")")
        return functionalButtons.any { expression.contains(it) }
    }

    private fun updateSolutionVisibility() {
        val expression = expressionTv.text.toString()
        if (hasFunctionalButton(expression)) {
            solutionTv.visibility = View.VISIBLE
        } else {
            solutionTv.visibility = View.GONE
        }
    }

    private fun safeCalculate(expression: String): Double {
        return try {
            // Double-check that no commas remain
            if (expression.contains(",")) {
                throw IllegalArgumentException("Expression contains commas: $expression")
            }
            
            Expression.calculate(expression)
        } catch (e: Exception) {
            println("Calculation error: ${e.message}")
            throw e
        }
    }
}
