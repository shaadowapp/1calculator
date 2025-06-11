package com.shaadow.onecalculator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var resultTv: TextView
    private lateinit var solutionTv: TextView

    private var bracketToggleState = true // true = "(", false = ")"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        resultTv = findViewById(R.id.result_tv)
        solutionTv = findViewById(R.id.solution_tv)

        assignId(R.id.button_backspace)
        assignId(R.id.button_bracket)
        assignId(R.id.button_percent)
        assignId(R.id.button_divide)
        assignId(R.id.button_multiply)
        assignId(R.id.button_plus)
        assignId(R.id.button_minus)
        assignId(R.id.button_equals)
        assignId(R.id.button_0)
        assignId(R.id.button_1)
        assignId(R.id.button_2)
        assignId(R.id.button_3)
        assignId(R.id.button_4)
        assignId(R.id.button_5)
        assignId(R.id.button_6)
        assignId(R.id.button_7)
        assignId(R.id.button_8)
        assignId(R.id.button_9)
        assignId(R.id.button_ac)
        assignId(R.id.button_dot)
    }

    private fun assignId(id: Int) {
        findViewById<MaterialButton>(id).setOnClickListener(this)
    }

    override fun onClick(view: View) {
        val button = view as MaterialButton
        val buttonText = button.text.toString()
        var dataToCalculate = solutionTv.text.toString()

        when (button.id) {

            R.id.button_ac -> {
                solutionTv.text = ""
                resultTv.text = "0"
                return
            }

            R.id.button_equals -> {
                solutionTv.text = resultTv.text
                return
            }

            R.id.button_backspace -> {
                if (dataToCalculate.isNotEmpty()) {
                    dataToCalculate = dataToCalculate.dropLast(1)
                }
            }

            R.id.button_bracket -> {
                val openCount = dataToCalculate.count { it == '(' }
                val closeCount = dataToCalculate.count { it == ')' }

                dataToCalculate += if (openCount > closeCount) ")" else "("
            }

            R.id.button_percent -> {
                // Convert percent to division by 100 for JS engine compatibility
                dataToCalculate += "/100"
            }

            else -> {
                dataToCalculate += buttonText
            }
        }

        solutionTv.text = dataToCalculate

        val finalResult = getResult(dataToCalculate)
        if (finalResult !== "Error") {
            resultTv.text = finalResult
        } else {
            resultTv.text = "..."
        }

    }

    private fun getResult(expression: String): String {
        if (expression.isBlank()) return ""

        val rhino = Context.enter()
        rhino.optimizationLevel = -1
        return try {
            val scope: Scriptable = rhino.initStandardObjects()
            val result = rhino.evaluateString(scope, expression, "JavaScript", 1, null)

            if (result == Context.getUndefinedValue()) {
                ""
            } else {
                val resultStr = result.toString()
                if (resultStr.endsWith(".0")) {
                    resultStr.dropLast(2) // remove ".0"
                } else {
                    resultStr
                }
            }
        }
        catch (e: Exception) {
            "Error"
        } finally {
            Context.exit()
        }
    }
}
