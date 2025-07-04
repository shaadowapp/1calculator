package com.shaadow.onecalculator

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.shaadow.onecalculator.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

open class CalculatorWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action?.startsWith(ACTION_BUTTON_PREFIX) == true) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, this::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (appWidgetId in appWidgetIds) {
                handleButtonPress(context, appWidgetManager, appWidgetId, intent.action!!)
            }
        }
    }

    open fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val expression = prefs.getString(KEY_EXPRESSION, "") ?: ""
        val resultShown = prefs.getBoolean(KEY_RESULT_SHOWN, false)
        val views = RemoteViews(context.packageName, R.layout.widget_calculator_black)
        views.setTextViewText(R.id.widget_expression, if (expression.isEmpty()) "0" else expression)

        // Set up button click handlers
        val buttons = listOf(
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "+", "-", "×", "÷", "=", "C"
        )
        val buttonIds = listOf(
            R.id.widget_btn_0, R.id.widget_btn_1, R.id.widget_btn_2, R.id.widget_btn_3, R.id.widget_btn_4,
            R.id.widget_btn_5, R.id.widget_btn_6, R.id.widget_btn_7, R.id.widget_btn_8, R.id.widget_btn_9,
            R.id.widget_btn_dot, R.id.widget_btn_plus, R.id.widget_btn_minus, R.id.widget_btn_multiply, R.id.widget_btn_divide, R.id.widget_btn_equals
        )
        for ((i, btn) in buttons.withIndex()) {
            if (i < buttonIds.size) {
                views.setOnClickPendingIntent(buttonIds[i], getButtonIntent(context, btn))
            }
        }
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    open fun getButtonIntent(context: Context, button: String): PendingIntent {
        val intent = Intent(context, this::class.java).apply {
            action = ACTION_BUTTON_PREFIX + button
        }
        return PendingIntent.getBroadcast(context, button.hashCode() + this::class.java.name.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
    }

    fun handleButtonPress(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, action: String) {
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        var expression = prefs.getString(KEY_EXPRESSION, "") ?: ""
        var resultShown = prefs.getBoolean(KEY_RESULT_SHOWN, false)
        val button = action.removePrefix(ACTION_BUTTON_PREFIX)
        if (resultShown && button != "=" && button != "C") {
            expression = ""
            resultShown = false
        }
        when (button) {
            in "0".."9", ".", "+", "-", "×", "÷" -> {
                expression += button
            }
            "C" -> {
                expression = ""
                resultShown = false
            }
            "=" -> {
                val result = safeCalculate(expression)
                saveToHistory(context, expression, result)
                expression = result
                resultShown = true
            }
        }
        prefs.edit().putString(KEY_EXPRESSION, expression).putBoolean(KEY_RESULT_SHOWN, resultShown).apply()
        // Use the correct widget class to update
        // Only the black widget is supported now
        val widget = CalculatorWidgetBlack()
        widget.updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    private fun safeCalculate(expr: String): String {
        return try {
            val cleanExpr = expr.replace("×", "*").replace("÷", "/")
            val result = com.shaadow.onecalculator.parser.Expression.calculate(cleanExpr)
            if (result % 1.0 == 0.0) result.toLong().toString() else result.toString()
        } catch (e: Exception) {
            "Error"
        }
    }

    private fun saveToHistory(context: Context, expression: String, result: String) {
        if (expression.isBlank() || result == "Error") return
        runBlocking {
            val db = HistoryDatabase.getInstance(context)
            val recent = db.historyDao().getRecentHistory()
            if (recent.isNotEmpty() && recent[0].expression == expression && recent[0].result == result && recent[0].source == "Widget") {
                return@runBlocking
            }
            db.historyDao().insert(HistoryEntity(expression = expression, result = result, source = "Widget"))
        }
    }

    companion object {
        private const val ACTION_BUTTON_PREFIX = "com.shaadow.onecalculator.widget.BUTTON_"
        private const val KEY_EXPRESSION = "widget_expression"
        private const val KEY_RESULT_SHOWN = "widget_result_shown"
    }
} 