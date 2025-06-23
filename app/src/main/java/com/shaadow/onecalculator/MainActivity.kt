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
import android.widget.Toast
import android.widget.LinearLayout
import android.widget.Button
import android.content.Intent


class MainActivity : AppCompatActivity() {
    private var isResultShown = false

    private lateinit var solutionTv: TextView
    private lateinit var resultTv: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        solutionTv = findViewById(R.id.solution_tv)
        resultTv = findViewById(R.id.result_tv)

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
                    R.id.button_brackets -> getNextBracket(solutionTv.text.toString())
                    else -> button.text.toString()
                }

                if (isResultShown) {
                    solutionTv.text = resultTv.text.toString().removeSuffix(".0")
                    isResultShown = false
                }

                solutionTv.visibility = View.VISIBLE
                resultTv.textSize = 36f
                appendToExpression(input)

                val expressionToEvaluate = convertSymbolsToOperators(solutionTv.text.toString())
                if (isExpressionComplete(expressionToEvaluate)) {
                    try {
                        val result = Expression.calculate(expressionToEvaluate)
                        resultTv.text = result.toString().removeSuffix(".0")
                    } catch (_: Exception) {
                        resultTv.text = ""
                    }
                } else {
                    resultTv.text = ""
                }
            }
        }

        findViewById<MaterialButton>(R.id.button_ac).setOnClickListener {
            solutionTv.text = ""
            resultTv.text = "0"
            solutionTv.visibility = View.VISIBLE
            resultTv.textSize = 36f
            isResultShown = false
        }

        findViewById<MaterialButton>(R.id.button_backspace).setOnClickListener {
            val text = solutionTv.text.toString()
            if (text.isNotEmpty()) {
                solutionTv.text = text.dropLast(1)

                val expressionToEvaluate = convertSymbolsToOperators(solutionTv.text.toString())
                if (isExpressionComplete(expressionToEvaluate)) {
                    try {
                        val result = Expression.calculate(expressionToEvaluate)
                        resultTv.text = result.toString().removeSuffix(".0")
                    } catch (_: Exception) {
                        resultTv.text = ""
                    }
                } else {
                    resultTv.text = ""
                }
            }
        }

        findViewById<MaterialButton>(R.id.button_equals).setOnClickListener {
            val expression = solutionTv.text.toString()
            val formattedExpression = convertSymbolsToOperators(expression)
            try {
                val result = Expression.calculate(formattedExpression)
                resultTv.text = result.toString().removeSuffix(".0")
                solutionTv.visibility = View.GONE
                resultTv.textSize = 50f
                isResultShown = true
            } catch (_: Exception) {
                resultTv.text = getString(R.string.error_text)
            }
        }

        solutionTv.setOnLongClickListener {
            showCustomPopup(it, true)
            true
        }

        resultTv.setOnLongClickListener {
            showCustomPopup(it, false)
            true
        }

        findViewById<Button>(R.id.nav_basic).setOnClickListener {
            // Do nothing, already on Basic
        }
        findViewById<Button>(R.id.nav_advanced).setOnClickListener {
            startActivity(Intent(this, AdvancedActivity::class.java))
        }
        findViewById<Button>(R.id.nav_history).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

    }

    private fun appendToExpression(value: String) {
        val current = solutionTv.text.toString()
        val updated = getString(R.string.expression_append, current, value)
        solutionTv.text = updated
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
            val textToCopy = if (isExpression) solutionTv.text.toString() else resultTv.text.toString()
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
                    if (pasted.isNotEmpty()) {
                        solutionTv.text = pasted
                        try {
                            val result = Expression.calculate(convertSymbolsToOperators(pasted))
                            resultTv.text = result.toString().removeSuffix(".0")
                        } catch (_: Exception) {
                            resultTv.text = getString(R.string.error_text)
                        }
                    }
                }
                popup.dismiss()
            }
        }
    }

}
