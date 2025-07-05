package com.shaadow.onecalculator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import android.widget.ImageButton
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.view.MenuItem
import android.widget.PopupMenu
import android.content.Intent

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

        // Check if we should navigate to history tab
        if (intent.getBooleanExtra("navigate_to_history", false)) {
            viewPager.currentItem = 2 // History tab (categories tab)
            bottomNav.menu.getItem(2).isChecked = true
        }
            
        // Hot Apps button
        findViewById<ImageButton>(R.id.btn_hot_apps).setOnClickListener {
            Toast.makeText(this, "Hot Apps", Toast.LENGTH_SHORT).show()
        }
            
        // Settings button with popup menu
        findViewById<ImageButton>(R.id.btn_settings).setOnClickListener { view ->
            showSettingsPopupMenu(view)
        }
    }
    
    private fun showSettingsPopupMenu(view: android.view.View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.settings_popup_menu, popup.menu)
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_history -> {
                    val intent = Intent(this, HistoryActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.menu_settings -> {
                    Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_about -> {
                    Toast.makeText(this, "About", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }
}
