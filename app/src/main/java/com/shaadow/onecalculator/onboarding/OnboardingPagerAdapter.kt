package com.shaadow.onecalculator.onboarding

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class OnboardingPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
    override fun getItemCount(): Int = 3
    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> OnboardingWelcomeFragment()
        1 -> OnboardingFeaturesFragment()
        2 -> OnboardingPermissionsFragment()
        else -> OnboardingWelcomeFragment()
    }
} 