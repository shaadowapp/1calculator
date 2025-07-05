package com.shaadow.onecalculator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import android.widget.ImageButton
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
            setContentView(R.layout.layout_home)
            supportActionBar?.hide()

        val viewPager = findViewById<ViewPager2>(R.id.view_pager)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter
        viewPager.isUserInputEnabled = false // Disable swipe if you want only tab click

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> viewPager.currentItem = 0
                R.id.nav_voice -> viewPager.currentItem = 1
                R.id.nav_categories -> viewPager.currentItem = 2
            }
            true
        }
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                bottomNav.menu.getItem(position).isChecked = true
            }
        })
            
            // Hot Apps button
            findViewById<ImageButton>(R.id.btn_hot_apps).setOnClickListener {
                Toast.makeText(this, "Hot Apps", Toast.LENGTH_SHORT).show()
            }
            
            // Settings button
            findViewById<ImageButton>(R.id.btn_settings).setOnClickListener {
                Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()
            }
    }
}
