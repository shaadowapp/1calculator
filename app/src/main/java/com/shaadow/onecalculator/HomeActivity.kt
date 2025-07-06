package com.shaadow.onecalculator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import android.widget.ImageButton
import androidx.viewpager2.widget.ViewPager2
import android.widget.PopupMenu
import android.content.Intent
import android.view.View

class HomeActivity : AppCompatActivity() {

    private lateinit var topBar: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_home)
        supportActionBar?.hide()

        val viewPager = findViewById<ViewPager2>(R.id.view_pager)
        val bottomNav = findViewById<ModernBottomNavigationView>(R.id.bottom_navigation)
        topBar = findViewById(R.id.top_bar)
        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter
        viewPager.isUserInputEnabled = false // Disable swipe if you want only tab click

        // Set up bottom navigation listener
        bottomNav.setOnTabSelectedListener { position ->
            viewPager.currentItem = position
            updateTopBarVisibility(position)
        }

        // Set up ViewPager page change callback
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                bottomNav.setSelectedItem(position)
                updateTopBarVisibility(position)
            }
        })

        // Check if we should navigate to history tab
        if (intent.getBooleanExtra("navigate_to_history", false)) {
            viewPager.currentItem = 2 // History tab (categories tab)
            bottomNav.setSelectedItem(2)
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
    
    private fun updateTopBarVisibility(position: Int) {
        // Show top bar only on home tab (position 0)
        topBar.visibility = if (position == 0) View.VISIBLE else View.GONE
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
