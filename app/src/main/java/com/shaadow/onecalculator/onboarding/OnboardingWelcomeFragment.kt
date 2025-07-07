package com.shaadow.onecalculator.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.shaadow.onecalculator.R
import com.google.android.material.textview.MaterialTextView
import android.widget.ImageView

class OnboardingWelcomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_onboarding_welcome, container, false)
        return view
    }
} 