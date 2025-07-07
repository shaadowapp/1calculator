package com.shaadow.onecalculator.mathly.model

object RuleBasedMathlyModel {
    fun normalizeSpokenMath(input: String): String {
        var cleaned = input.lowercase().trim()
        // Command detection first (before further normalization)
        if (cleaned.contains("open history") || cleaned.contains("go to history") || cleaned.contains("show history")) return "__OPEN_HISTORY__"
        if (cleaned.contains("open calculator") || cleaned.contains("show calculator")) return "__OPEN_CALCULATOR__"
        if (cleaned.contains("open settings") || cleaned.contains("show settings")) return "__OPEN_SETTINGS__"
        if (cleaned.contains("clear all") || cleaned.contains("reset everything") || cleaned.contains("clear") || cleaned.contains("reset")) return "__CLEAR__"

        // Multi-word numbers (hundreds)
        val hundreds = listOf("one", "two", "three", "four", "five", "six", "seven", "eight", "nine")
        for ((i, word) in hundreds.withIndex()) {
            cleaned = cleaned.replace("$word hundred", "${(i+1)*100}")
        }
        // Multi-word numbers (thousands)
        for ((i, word) in hundreds.withIndex()) {
            cleaned = cleaned.replace("$word thousand", "${(i+1)*1000}")
        }

        // Number words (up to 100)
        val numberWords = mapOf(
            "zero" to "0", "one" to "1", "two" to "2", "three" to "3", "four" to "4", "five" to "5", "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9", "ten" to "10",
            "eleven" to "11", "twelve" to "12", "thirteen" to "13", "fourteen" to "14", "fifteen" to "15", "sixteen" to "16", "seventeen" to "17", "eighteen" to "18", "nineteen" to "19",
            "twenty" to "20", "thirty" to "30", "forty" to "40", "fifty" to "50", "sixty" to "60", "seventy" to "70", "eighty" to "80", "ninety" to "90", "hundred" to "100", "thousand" to "1000"
        )
        // Replace compound numbers (e.g., twenty one)
        for (tens in listOf("twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")) {
            for (ones in listOf("one", "two", "three", "four", "five", "six", "seven", "eight", "nine")) {
                cleaned = cleaned.replace("$tens $ones", (numberWords[tens]!!.toInt() + numberWords[ones]!!.toInt()).toString())
            }
        }
        // Replace single number words
        for ((word, digit) in numberWords) {
            cleaned = cleaned.replace(word, digit)
        }

        // Special pattern for 'half of X' (must come before any other replacements)
        cleaned = cleaned.replace(Regex("half of ([a-z0-9 ]+)", RegexOption.IGNORE_CASE) ) { m ->
            val numwords = m.groupValues[1].trim()
            var numstr = numwords
            val numberWords = mapOf(
                "zero" to "0", "one" to "1", "two" to "2", "three" to "3", "four" to "4", "five" to "5", "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9", "ten" to "10",
                "eleven" to "11", "twelve" to "12", "thirteen" to "13", "fourteen" to "14", "fifteen" to "15", "sixteen" to "16", "seventeen" to "17", "eighteen" to "18", "nineteen" to "19",
                "twenty" to "20", "thirty" to "30", "forty" to "40", "fifty" to "50", "sixty" to "60", "seventy" to "70", "eighty" to "80", "ninety" to "90", "hundred" to "100", "thousand" to "1000"
            )
            for (tens in listOf("twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")) {
                for (ones in listOf("one", "two", "three", "four", "five", "six", "seven", "eight", "nine")) {
                    numstr = numstr.replace("$tens $ones", (numberWords[tens]!!.toInt() + numberWords[ones]!!.toInt()).toString())
                }
            }
            val hundreds = listOf("one", "two", "three", "four", "five", "six", "seven", "eight", "nine")
            for ((i, word) in hundreds.withIndex()) {
                numstr = numstr.replace("$word hundred", "${(i+1)*100}")
            }
            for ((word, digit) in numberWords) {
                numstr = numstr.replace(word, digit)
            }
            "${numstr}*0.5"
        }

        // General math phrases
        cleaned = cleaned.replace("what is the result of", "")
            .replace("how much is", "")
            .replace("calculate the value of", "")
            .replace("give me the answer to", "")
            .replace("what is", "")
            .replace("calculate", "")
            .replace("equals", "")
            .replace("?", "")
        // Special patterns for double/triple/half of (multi-word numbers)
        cleaned = cleaned.replace(Regex("double ([0-9]+)") ) { m ->
            val num = m.groupValues[1].trim()
            "$num*2"
        }
        cleaned = cleaned.replace(Regex("triple ([0-9]+)") ) { m ->
            val num = m.groupValues[1].trim()
            "$num*3"
        }
        cleaned = cleaned.replace(Regex("cube of ([0-9]+)") ) { m ->
            val num = m.groupValues[1].trim()
            "$num^3"
        }
        cleaned = cleaned.replace(Regex("square of ([0-9]+)") ) { m ->
            val num = m.groupValues[1].trim()
            "$num^2"
        }
        // Math operations
        cleaned = cleaned.replace("plus", "+")
            .replace("add", "+")
            .replace("sum", "+")
            .replace("minus", "-")
            .replace("subtract", "-")
            .replace("difference", "-")
            .replace("less", "-")
            .replace("times", "*")
            .replace("multiplied by", "*")
            .replace("multiply by", "*")
            .replace("product", "*")
            .replace("x", "*")
            .replace("divided by", "/")
            .replace("over", "/")
            .replace("by", "/")
            .replace("quotient", "/")
            .replace("modulo", "%")
            .replace("mod", "%")
            .replace("remainder", "%")
            .replace("to the power of", "^")
            .replace("raised to", "^")
            .replace("power", "^")
            .replace("squared", "^2")
            .replace("cubed", "^3")
            // Use √ for square root
            .replace(Regex("square root of ([0-9]+)")) { m ->
                val num = m.groupValues[1].trim()
                "√$num"
            }
            .replace("root of", "√")
            .replace("sqrt", "√")
            .replace("factorial", "!")
            .replace("fact", "!")
            .replace("percent of", "*0.01*")
            .replace("percentage of", "*0.01*")
        // Parentheses
        cleaned = cleaned.replace("open bracket", "(")
            .replace("close bracket", ")")
            .replace("open parenthesis", "(")
            .replace("close parenthesis", ")")
        // Constants
        cleaned = cleaned.replace("pi", "π")
            .replace("tau", "2π")
            .replace("phi", "1.618")
            .replace("euler", "e")
            .replace("e", "e")
            .replace("dozen", "12")
        // Remove all spaces for parser compatibility
        cleaned = cleaned.replace(" ", "")
        return cleaned.trim()
    }

    fun detectCommand(cleaned: String): String? {
        // Not used anymore, handled in normalization
        return null
    }

    fun solveMath(input: String): String {
        val cleaned = normalizeSpokenMath(input)
        if (cleaned.startsWith("__") && cleaned.endsWith("__")) return cleaned
        return try {
            val result = com.shaadow.onecalculator.parser.Expression.calculate(cleaned)
            "$cleaned = $result"
        } catch (e: Exception) {
            "Sorry, I couldn't solve that."
        }
    }
} 