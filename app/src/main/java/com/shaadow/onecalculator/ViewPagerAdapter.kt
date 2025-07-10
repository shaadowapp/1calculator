package com.shaadow.onecalculator

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.shaadow.onecalculator.mathly.MathlyFragment
import com.shaadow.onecalculator.ChatFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1 -> MathlyFragment()
            2 -> ChatFragment() // Changed from CategoriesFragment to ChatFragment
            else -> HomeFragment()
        }
    }
} 