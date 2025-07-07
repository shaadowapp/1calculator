package com.shaadow.onecalculator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class MainTabActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNav: ModernBottomNavigationView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_tab) // You may need to create this layout or reuse the old one
        viewPager = findViewById(R.id.view_pager)
        bottomNav = findViewById(R.id.bottom_navigation)
        viewPager.adapter = ViewPagerAdapter(this)

        // Get tab index from intent (default to 0)
        val tabIndex = intent.getIntExtra("tab_index", 0)
        viewPager.setCurrentItem(tabIndex, false)
        bottomNav.setSelectedItem(tabIndex)

        // Sync tab selection between ViewPager and BottomNav
        bottomNav.setOnTabSelectedListener { position ->
            viewPager.currentItem = position
        }
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                bottomNav.setSelectedItem(position)
            }
        })
    }
} 