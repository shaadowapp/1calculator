package com.shaadow.onecalculator

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Toast
import com.shaadow.onecalculator.UnitAdapter

class AdvancedFragment : Fragment(R.layout.fragment_advanced) {
    private val unitCalculators = listOf(
        "Length Converter",
        "Weight Converter",
        "Temperature Converter",
        "Area Converter",
        "Volume Converter"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Optionally show a message or leave empty
    }
} 