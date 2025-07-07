package com.shaadow.onecalculator.mathly

import com.shaadow.onecalculator.mathly.model.RuleBasedMathlyModel
import org.junit.Assert.assertEquals
import org.junit.Test

class MathlyModelTest {
    @Test
    fun testSimpleAddition() {
        assertEquals("21+35 = 56.0", RuleBasedMathlyModel.solveMath("twenty one plus thirty five"))
    }

    @Test
    fun testSubtraction() {
        assertEquals("99-42 = 57.0", RuleBasedMathlyModel.solveMath("ninety nine minus forty two"))
    }

    @Test
    fun testSquareRoot() {
        assertEquals("√81 = 9.0", RuleBasedMathlyModel.solveMath("square root of eighty one"))
    }

    @Test
    fun testDouble() {
        assertEquals("7*2 = 14.0", RuleBasedMathlyModel.solveMath("double seven"))
    }

    @Test
    fun testCube() {
        assertEquals("3^3 = 27.0", RuleBasedMathlyModel.solveMath("cube of three"))
    }

    @Test
    fun testPiTimesTwo() {
        val result = RuleBasedMathlyModel.solveMath("pi times two")
        assert(result.contains("π*2"))
    }

    @Test
    fun testHalfOfHundred() {
        val result = RuleBasedMathlyModel.solveMath("half of one hundred")
        println("Result for 'half of one hundred': $result")
        assertEquals("100*0.5 = 50.0", result)
    }

    @Test
    fun testGoToHistoryCommand() {
        assertEquals("__OPEN_HISTORY__", RuleBasedMathlyModel.solveMath("go to history"))
    }

    @Test
    fun testClearAllCommand() {
        assertEquals("__CLEAR__", RuleBasedMathlyModel.solveMath("clear all"))
    }

    @Test
    fun testNaturalLanguage() {
        assertEquals("12+8 = 20.0", RuleBasedMathlyModel.solveMath("What is the result of twelve plus eight?"))
    }
} 