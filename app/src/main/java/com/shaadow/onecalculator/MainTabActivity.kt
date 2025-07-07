package com.shaadow.onecalculator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class MainTabActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_tab) // You may need to create this layout or reuse the old one
        viewPager = findViewById(R.id.view_pager)
        viewPager.adapter = ViewPagerAdapter(this)
        // Add tab switching logic here
    }
} 