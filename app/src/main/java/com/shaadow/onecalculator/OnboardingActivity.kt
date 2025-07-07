package com.shaadow.onecalculator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.shaadow.onecalculator.onboarding.OnboardingPagerAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OnboardingActivity : AppCompatActivity() {
    private var chosenPreference: String? = null
    private lateinit var cardCalculator: MaterialCardView
    private lateinit var cardMathly: MaterialCardView
    private lateinit var btnGrantPermissions: MaterialButton
    private lateinit var viewPager: ViewPager2
    private lateinit var tabIndicator: TabLayout

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // After permissions, route to chosen screen
            routeToChosenScreen()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.onboarding_viewpager)
        tabIndicator = findViewById(R.id.onboarding_tab_indicator)
        cardCalculator = findViewById(R.id.card_calculator)
        cardMathly = findViewById(R.id.card_mathly)
        btnGrantPermissions = findViewById(R.id.btn_grant_permissions)

        // Set up carousel
        viewPager.adapter = OnboardingPagerAdapter(this)
        TabLayoutMediator(tabIndicator, viewPager) { _, _ -> }.attach()

        // Preference card click listeners
        cardCalculator.setOnClickListener {
            highlightPreference("calculator")
            chosenPreference = "calculator"
            btnGrantPermissions.visibility = View.VISIBLE
        }
        cardMathly.setOnClickListener {
            highlightPreference("mathly")
            chosenPreference = "mathly"
            btnGrantPermissions.visibility = View.VISIBLE
        }

        btnGrantPermissions.setOnClickListener {
            if (chosenPreference == null) {
                Toast.makeText(this, "Please choose a default screen first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            savePreferenceAndRequestPermissions(chosenPreference!!)
        }
    }

    private fun highlightPreference(choice: String) {
        // Visual feedback for selected card
        if (choice == "calculator") {
            cardCalculator.strokeWidth = 8
            cardCalculator.strokeColor = ContextCompat.getColor(this, R.color.brand_green)
            cardMathly.strokeWidth = 0
        } else {
            cardMathly.strokeWidth = 8
            cardMathly.strokeColor = ContextCompat.getColor(this, R.color.brand_green)
            cardCalculator.strokeWidth = 0
        }
    }

    private fun savePreferenceAndRequestPermissions(choice: String) {
        lifecycleScope.launch {
            val prefDao = HistoryDatabase.getInstance(this@OnboardingActivity).preferenceDao()
            withContext(Dispatchers.IO) {
                prefDao.setPreference(PreferenceEntity("default_screen", choice))
            }
            // Request mic & camera permissions
            permissionLauncher.launch(arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
            ))
        }
    }

    private fun routeToChosenScreen() {
        when (chosenPreference) {
            "calculator" -> startActivity(Intent(this, BasicActivity::class.java))
            "mathly" -> startActivity(Intent(this, MainTabActivity::class.java).apply {
                putExtra("tab_index", 1)
            })
            else -> startActivity(Intent(this, BasicActivity::class.java))
        }
        finish()
    }

    override fun onBackPressed() {
        // Prevent skipping onboarding
        Toast.makeText(this, "Please complete onboarding to continue.", Toast.LENGTH_SHORT).show()
    }
} 