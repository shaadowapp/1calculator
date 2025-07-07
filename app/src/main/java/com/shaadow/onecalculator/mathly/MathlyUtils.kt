package com.shaadow.onecalculator.mathly

import com.shaadow.onecalculator.mathly.model.RuleBasedMathlyModel

object MathlyUtils {
    fun normalizeSpokenMath(input: String): String = RuleBasedMathlyModel.normalizeSpokenMath(input)
    fun solveMath(input: String): String = RuleBasedMathlyModel.solveMath(input)
} 