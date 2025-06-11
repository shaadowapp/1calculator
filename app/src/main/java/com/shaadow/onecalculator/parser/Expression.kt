/**
 * Copyright © 2025 Shaadow Platforms
 * All rights reserved.
 *
 * This file is part of the proprietary app "My Calculator" developed by Shaadow Platforms.
 * Unauthorized use, reproduction, or distribution is strictly prohibited.
 * For learning purposes, permission must be requested at: suryasubhrajit@gmail.com
 * Website: https://shaadowplatforms.com
 */

package com.shaadow.onecalculator.parser

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Stack
import kotlin.math.sqrt
import kotlin.math.pow

object Expression {

    // Public method accessible from MainActivity
    fun calculate(expression: String): String {
        return try {
            val result = evaluateExpression(expression)
            formatResult(result)
        } catch (e: Exception) {
            "Error"
        }
    }

    // Internal evaluator returns BigDecimal
    private fun evaluateExpression(expr: String): BigDecimal {
        val output = Stack<BigDecimal>()
        val operators = Stack<Char>()

        var i = 0
        while (i < expr.length) {
            when (val c = expr[i]) {
                in '0'..'9', '.' -> {
                    val number = StringBuilder()
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                        number.append(expr[i])
                        i++
                    }
                    output.push(BigDecimal(number.toString()))
                    continue
                }
                '+', '-', '*', '/', '^' -> {
                    while (operators.isNotEmpty() && precedence(operators.peek()) >= precedence(c)) {
                        output.push(applyOperator(operators.pop(), output.pop(), output.pop()))
                    }
                    operators.push(c)
                }
                '(' -> operators.push(c)
                ')' -> {
                    while (operators.peek() != '(') {
                        output.push(applyOperator(operators.pop(), output.pop(), output.pop()))
                    }
                    operators.pop() // pop '('
                }
                '√' -> {
                    i++
                    val number = StringBuilder()
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                        number.append(expr[i])
                        i++
                    }
                    val value = BigDecimal(number.toString())
                    output.push(BigDecimal(sqrt(value.toDouble()), MathContext.DECIMAL64))
                    continue
                }
                '%' -> {
                    val value = output.pop()
                    output.push(value.divide(BigDecimal(100), MathContext.DECIMAL64))
                }
                '!' -> {
                    val value = output.pop().toInt()
                    output.push(BigDecimal(factorial(value)))
                }
                'π' -> {
                    if (output.isNotEmpty()) {
                        val prev = output.pop()
                        output.push(prev.multiply(BigDecimal(Math.PI, MathContext.DECIMAL64)))
                    } else {
                        output.push(BigDecimal(Math.PI, MathContext.DECIMAL64))
                    }
                }

            }
            i++
        }

        while (operators.isNotEmpty()) {
            output.push(applyOperator(operators.pop(), output.pop(), output.pop()))
        }

        return output.pop()
    }

    private fun precedence(op: Char): Int = when (op) {
        '+', '-' -> 1
        '*', '/' -> 2
        '^' -> 3
        else -> -1
    }

    private fun applyOperator(op: Char, b: BigDecimal, a: BigDecimal): BigDecimal {
        return when (op) {
            '+' -> a.add(b)
            '-' -> a.subtract(b)
            '*' -> a.multiply(b)
            '/' -> a.divide(b, 10, RoundingMode.HALF_UP)
            '^' -> BigDecimal(a.toDouble().pow(b.toDouble()), MathContext.DECIMAL64)
            else -> BigDecimal.ZERO
        }
    }

    private fun factorial(n: Int): Long {
        require(n >= 0) { "Factorial is undefined for negative numbers." }
        return if (n == 0 || n == 1) 1 else n * factorial(n - 1)
    }

    private fun formatResult(result: BigDecimal): String {
        return result.stripTrailingZeros().toPlainString()
    }
}
