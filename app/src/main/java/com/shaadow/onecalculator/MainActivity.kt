package com.shaadow.onecalculator

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.shaadow.onecalculator.parser.Expression

class MainActivity : AppCompatActivity() {

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
            R.id.button_sqrt, R.id.button_power, R.id.button_factorial, R.id.button_pi
        )

        for (id in buttons) {
            val button = findViewById<MaterialButton>(id)

            button.setOnClickListener {
                val currentText = solutionTv.text.toString()
                val newText = currentText + button.text.toString()
                solutionTv.text = newText

                // Only try to evaluate if expression is valid (does not end in an operator)
                val expressionToEvaluate = convertSymbolsToOperators(newText)
                if (isExpressionComplete(expressionToEvaluate)) {
                    try {
                        val result = Expression.calculate(expressionToEvaluate)
                        resultTv.text = result.toString()
                    } catch (e: Exception) {
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
        }

        findViewById<MaterialButton>(R.id.button_backspace).setOnClickListener {
            val text = solutionTv.text.toString()
            if (text.isNotEmpty()) {
                val updatedText = text.substring(0, text.length - 1)
                solutionTv.text = updatedText

                val expressionToEvaluate = convertSymbolsToOperators(updatedText)
                if (isExpressionComplete(expressionToEvaluate)) {
                    try {
                        val result = Expression.calculate(expressionToEvaluate)
                        resultTv.text = result.toString()
                    } catch (e: Exception) {
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
                resultTv.text = result.toString()
            } catch (e: Exception) {
                resultTv.text = "Error"
            }
        }
    }

    private fun convertSymbolsToOperators(expression: String): String {
        return expression
            .replace("×", "*")
            .replace("÷", "/")
            .replace("π", "pi")
            .replace("√", "√") // assuming your parser handles √ directly
            .replace("^", "^")
            .replace("%", "%")
    }

    // Check if expression ends with a valid number, pi, or closing bracket
    private fun isExpressionComplete(expression: String): Boolean {
        return expression.isNotEmpty() &&
                (expression.last().isDigit() || expression.last() == ')' || expression.endsWith("pi"))
    }
}
