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
import android.view.View
import android.widget.ImageView

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
                
                // Hide notification dot when Voice tab is selected
                if (position == 1) {
                    hideNotificationDot()
                }
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
        
        // Show notification dot for Voice tab (you can control this based on your logic)
        showNotificationDot()
    }
    
    private fun showNotificationDot() {
        // This method can be called when there are new voice features or notifications
        // For now, we'll show it by default
        val voiceTab = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val voiceItem = voiceTab.menu.findItem(R.id.nav_voice)
        // The notification dot will be handled by the custom layout
    }
    
    private fun hideNotificationDot() {
        // Hide the notification dot when Voice tab is selected
        // This can be implemented if you want to hide it on selection
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
