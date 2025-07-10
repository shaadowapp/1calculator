package com.shaadow.onecalculator

import android.content.Context
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import android.graphics.Typeface

class ModernBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var currentSelectedPosition = 0
    private var onTabSelectedListener: ((Int) -> Unit)? = null

    // Tab views
    private lateinit var homeTab: View
    private lateinit var voiceTab: View
    private lateinit var chatTab: View // Renamed from categoryTab

    // Tab icons
    private lateinit var homeIcon: ImageView
    private lateinit var voiceIcon: ImageView
    private lateinit var chatIcon: ImageView // Renamed from categoryIcon

    // Tab labels
    private lateinit var homeLabel: TextView
    private lateinit var voiceLabel: TextView
    private lateinit var chatLabel: TextView // Renamed from categoryLabel

    // Tab containers
    private lateinit var homeContainer: FrameLayout
    private lateinit var voiceContainer: FrameLayout
    private lateinit var chatContainer: FrameLayout // Renamed from categoryContainer

    init {
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        // Inflate the bottom navigation layout
        LayoutInflater.from(context).inflate(R.layout.bottom_navigation, this, true)

        // Initialize tab views
        homeTab = findViewById(R.id.home_tab)
        voiceTab = findViewById(R.id.voice_tab)
        chatTab = findViewById(R.id.category_tab) // Using same id for now

        // Initialize icon views
        homeIcon = homeTab.findViewById(R.id.tab_icon)
        voiceIcon = voiceTab.findViewById(R.id.tab_icon)
        chatIcon = chatTab.findViewById(R.id.tab_icon)

        // Initialize label views
        homeLabel = homeTab.findViewById(R.id.tab_label)
        voiceLabel = voiceTab.findViewById(R.id.tab_label)
        chatLabel = chatTab.findViewById(R.id.tab_label)

        // Initialize container views
        homeContainer = homeTab.findViewById(R.id.icon_container)
        voiceContainer = voiceTab.findViewById(R.id.icon_container)
        chatContainer = chatTab.findViewById(R.id.icon_container)

        // Set up tab data
        setupTabData()

        // Set up click listeners
        setupClickListeners()

        // Set initial selection
        setSelectedTab(0)
    }

    private fun setupTabData() {
        // Home tab
        homeIcon.setImageResource(R.drawable.ic_home)
        homeLabel.text = "Home"

        // Voice tab
        voiceIcon.setImageResource(R.drawable.ic_microphone)
        val askMathlyText = SpannableString("Ask Mathly")
        askMathlyText.setSpan(StyleSpan(Typeface.BOLD), 4, 10, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        voiceLabel.text = askMathlyText

        // Chat tab
        chatIcon.setImageResource(R.drawable.ic_ai) // Fallback to AI icon if ic_chat does not exist
        chatLabel.text = "Chat"
    }

    private fun setupClickListeners() {
        homeTab.setOnClickListener {
            setSelectedTab(0)
            onTabSelectedListener?.invoke(0)
        }

        voiceTab.setOnClickListener {
            setSelectedTab(1)
            onTabSelectedListener?.invoke(1)
        }

        chatTab.setOnClickListener {
            setSelectedTab(2)
            onTabSelectedListener?.invoke(2)
        }
    }

    private fun setSelectedTab(position: Int) {
        // Update previous selection
        updateTabAppearance(currentSelectedPosition, false)

        // Update new selection
        currentSelectedPosition = position
        updateTabAppearance(position, true)
    }

    private fun updateTabAppearance(position: Int, isSelected: Boolean) {
        val (container, icon, label) = when (position) {
            0 -> Triple(homeContainer, homeIcon, homeLabel)
            1 -> Triple(voiceContainer, voiceIcon, voiceLabel)
            2 -> Triple(chatContainer, chatIcon, chatLabel)
            else -> return
        }

        // Update container selection state
        container.isSelected = isSelected

        // Update icon and label colors
        val iconColor = if (isSelected) {
            ContextCompat.getColor(context, R.color.bottom_nav_active_icon)
        } else {
            ContextCompat.getColor(context, R.color.bottom_nav_inactive_icon)
        }

        val textColor = if (isSelected) {
            ContextCompat.getColor(context, R.color.bottom_nav_active_text)
        } else {
            ContextCompat.getColor(context, R.color.bottom_nav_inactive_text)
        }

        icon.setColorFilter(iconColor)
        label.setTextColor(textColor)
    }

    fun setOnTabSelectedListener(listener: (Int) -> Unit) {
        onTabSelectedListener = listener
    }

    fun setSelectedItem(position: Int) {
        setSelectedTab(position)
    }

    fun getCurrentSelectedPosition(): Int = currentSelectedPosition
} 