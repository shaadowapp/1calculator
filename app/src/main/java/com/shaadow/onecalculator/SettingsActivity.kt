package com.shaadow.onecalculator

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val spinner = findViewById<Spinner>(R.id.spinner_default_screen)
        val btnSave = findViewById<Button>(R.id.btn_save_default)
        val btnClear = findViewById<Button>(R.id.btn_clear_default)

        val options = listOf("Calculator", "Mathly")
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)

        btnSave.setOnClickListener {
            val choice = if (spinner.selectedItemPosition == 0) "calculator" else "mathly"
            lifecycleScope.launch {
                val prefDao = HistoryDatabase.getInstance(this@SettingsActivity).preferenceDao()
                withContext(Dispatchers.IO) {
                    prefDao.setPreference(PreferenceEntity("default_screen", choice))
                }
                Toast.makeText(this@SettingsActivity, "Default screen set to $choice", Toast.LENGTH_SHORT).show()
            }
        }

        btnClear.setOnClickListener {
            lifecycleScope.launch {
                val prefDao = HistoryDatabase.getInstance(this@SettingsActivity).preferenceDao()
                withContext(Dispatchers.IO) {
                    prefDao.deletePreference("default_screen")
                }
                Toast.makeText(this@SettingsActivity, "Default screen preference cleared", Toast.LENGTH_SHORT).show()
            }
        }
    }
} 