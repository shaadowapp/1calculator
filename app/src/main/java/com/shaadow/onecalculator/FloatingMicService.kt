package com.shaadow.onecalculator

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.shaadow.onecalculator.mathly.MathlyFragment

class FloatingMicService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isListening = true
    private lateinit var micIcon: View
    private lateinit var micPulse: View
    private lateinit var resetHandler: android.os.Handler
    private var resetRunnable: Runnable? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val inflater = LayoutInflater.from(this)
        floatingView = inflater.inflate(R.layout.floating_mic_dot, null)
        micIcon = floatingView.findViewById(R.id.floating_mic_icon)
        micPulse = floatingView.findViewById(R.id.mic_pulse)
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        )
        params.x = (screenWidth - floatingView.layoutParams.width) / 2
        params.y = 300
        windowManager.addView(floatingView, params)

        floatingView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    resetRunnable?.let { resetHandler.removeCallbacks(it) }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingView, params)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // After 2s, animate back to center
                    scheduleResetToCenter(params)
                    true
                }
                else -> false
            }
        }
        floatingView.setOnClickListener {
            toggleMic()
        }
        updateMicUI()
    }

    private fun toggleMic() {
        isListening = !isListening
        updateMicUI()
        // Send broadcast to toggle Mathly listening
        val intent = Intent("com.shaadow.onecalculator.ACTION_TOGGLE_MATHLY_LISTENING")
        intent.putExtra("is_listening", isListening)
        sendBroadcast(intent)
    }

    private fun updateMicUI() {
        micIcon.isSelected = isListening
        if (isListening) {
            micPulse.visibility = View.VISIBLE
            val anim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.mic_pulse_animation)
            micPulse.startAnimation(anim)
        } else {
            micPulse.clearAnimation()
            micPulse.visibility = View.GONE
        }
    }

    private fun scheduleResetToCenter(params: WindowManager.LayoutParams) {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        resetHandler = android.os.Handler(Looper.getMainLooper())
        val runnable = Runnable {
            val targetX = (screenWidth - floatingView.width) / 2
            val startX = params.x
            val deltaX = targetX - startX
            val duration = 400L
            val startTime = System.currentTimeMillis()
            val animator = object : Runnable {
                override fun run() {
                    val elapsed = System.currentTimeMillis() - startTime
                    val fraction = (elapsed / duration.toFloat()).coerceAtMost(1f)
                    params.x = startX + (deltaX * fraction).toInt()
                    windowManager.updateViewLayout(floatingView, params)
                    if (fraction < 1f) {
                        resetHandler.postDelayed(this, 16)
                    }
                }
            }
            animator.run()
        }
        resetRunnable = runnable
        resetHandler.postDelayed(runnable, 2000)
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(floatingView)
    }
} 