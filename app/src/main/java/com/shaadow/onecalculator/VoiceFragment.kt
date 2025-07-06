package com.shaadow.onecalculator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*

class VoiceFragment : Fragment() {
    
    private lateinit var micButton: FloatingActionButton
    private lateinit var statusText: TextView
    private lateinit var waveContainer: View
    private lateinit var waveCircle1: View
    private lateinit var waveCircle2: View
    private lateinit var waveCircle3: View
    private lateinit var resultContainer: View
    private lateinit var expressionText: TextView
    private lateinit var resultText: TextView
    private lateinit var permissionContainer: View
    private lateinit var grantPermissionButton: com.google.android.material.button.MaterialButton
    private lateinit var skipPermissionButton: com.google.android.material.button.MaterialButton
    private lateinit var askMathlyContainer: View
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var hasPermission = false
    private var permissionDeniedCount = 0
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 123
        private const val MAX_PERMISSION_DENIALS = 2
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_voice, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupClickListeners()
        checkPermission()
    }
    
    private fun initializeViews(view: View) {
        micButton = view.findViewById(R.id.mic_button)
        statusText = view.findViewById(R.id.status_text)
        waveContainer = view.findViewById(R.id.wave_container)
        waveCircle1 = view.findViewById(R.id.wave_circle_1)
        waveCircle2 = view.findViewById(R.id.wave_circle_2)
        waveCircle3 = view.findViewById(R.id.wave_circle_3)
        resultContainer = view.findViewById(R.id.result_container)
        expressionText = view.findViewById(R.id.expression_text)
        resultText = view.findViewById(R.id.result_text)
        permissionContainer = view.findViewById(R.id.permission_container)
        grantPermissionButton = view.findViewById(R.id.grant_permission_button)
        skipPermissionButton = view.findViewById(R.id.skip_permission_button)
        askMathlyContainer = view.findViewById(R.id.ask_mathly_container)
    }
    
    private fun setupClickListeners() {
        micButton.setOnClickListener {
            if (hasPermission) {
                if (isListening) {
                    stopListening()
                } else {
                    startListening()
                }
            } else {
                requestPermission()
            }
        }
        
        grantPermissionButton.setOnClickListener {
            requestPermission()
        }
        
        skipPermissionButton.setOnClickListener {
            redirectToHome()
        }
    }
    
    private fun checkPermission() {
        hasPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        
        if (hasPermission) {
            showAskMathlyScreen()
        } else {
            showPermissionDialog()
        }
    }
    
    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.RECORD_AUDIO),
            PERMISSION_REQUEST_CODE
        )
    }
    
    private fun showPermissionDialog() {
        permissionContainer.visibility = View.VISIBLE
        askMathlyContainer.visibility = View.GONE
    }
    
    private fun hidePermissionDialog() {
        permissionContainer.visibility = View.GONE
    }
    
    private fun showAskMathlyScreen() {
        permissionContainer.visibility = View.GONE
        askMathlyContainer.visibility = View.VISIBLE
        setupSpeechRecognizer()
    }
    
    private fun redirectToHome() {
        // Navigate back to home tab
        requireActivity().finish()
    }
    
    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                statusText.text = "Listening... (tap to stop)"
                startWaveAnimation()
            }
            
            override fun onBeginningOfSpeech() {
                statusText.text = "I hear you..."
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Audio level changed - could add visual feedback here
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {
                // Audio buffer received
            }
            
            override fun onEndOfSpeech() {
                statusText.text = "Processing..."
                stopWaveAnimation()
            }
            
            override fun onError(error: Int) {
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> {
                        // No speech detected, restart listening if still in listening mode
                        if (isListening) {
                            statusText.text = "Listening... (tap to stop)"
                            restartListening()
                        }
                    }
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        // Speech timeout, restart listening if still in listening mode
                        if (isListening) {
                            statusText.text = "Listening... (tap to stop)"
                            restartListening()
                        }
                    }
                    SpeechRecognizer.ERROR_AUDIO -> {
                        statusText.text = "Audio error, retrying..."
                        if (isListening) {
                            restartListening()
                        }
                    }
                    SpeechRecognizer.ERROR_CLIENT -> {
                        statusText.text = "Client error, retrying..."
                        if (isListening) {
                            restartListening()
                        }
                    }
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        statusText.text = getString(R.string.voice_no_permission)
                        stopListening()
                    }
                    SpeechRecognizer.ERROR_NETWORK -> {
                        statusText.text = "Network error, retrying..."
                        if (isListening) {
                            restartListening()
                        }
                    }
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                        statusText.text = "Network timeout, retrying..."
                        if (isListening) {
                            restartListening()
                        }
                    }
                    SpeechRecognizer.ERROR_SERVER -> {
                        statusText.text = "Server error, retrying..."
                        if (isListening) {
                            restartListening()
                        }
                    }
                    else -> {
                        statusText.text = "Error occurred, retrying..."
                        if (isListening) {
                            restartListening()
                        }
                    }
                }
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0]
                    displaySpokenText(spokenText)
                }
                
                // If still listening, restart for continuous listening
                if (isListening) {
                    restartListening()
                }
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!partialMatches.isNullOrEmpty()) {
                    val partialText = partialMatches[0]
                    if (partialText.isNotEmpty()) {
                        statusText.text = "I heard: $partialText"
                    }
                }
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {
                // Event occurred
            }
        })
    }
    
    private fun startListening() {
        if (speechRecognizer != null) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, requireContext().packageName)
                // Enhanced speech optimization
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 4000)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 300)
                // Additional accuracy settings
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            }
            
            speechRecognizer?.startListening(intent)
            isListening = true
            micButton.setImageResource(R.drawable.ic_stop)
            statusText.text = "Listening... (tap to stop)"
            resultContainer.visibility = View.GONE
            startWaveAnimation()
            startMicPulseAnimation()
        }
    }
    
    private fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
        micButton.setImageResource(R.drawable.ic_microphone)
        statusText.text = "Tap mic to start listening"
        stopWaveAnimation()
        stopMicPulseAnimation()
        resultContainer.visibility = View.GONE
    }
    
    private fun restartListening() {
        // Smart restart with exponential backoff
        val delay = if (isListening) 800 else 500
        micButton.postDelayed({
            if (isListening) {
                startListening()
            }
        }, delay.toLong())
    }
    
    private fun startWaveAnimation() {
        waveContainer.visibility = View.VISIBLE
        waveContainer.alpha = 1.0f
        
        // Animate the circles with custom scale animation
        val waveAnimation1 = AnimationUtils.loadAnimation(requireContext(), R.anim.wave_scale_animation)
        waveAnimation1.repeatMode = Animation.RESTART
        waveAnimation1.repeatCount = Animation.INFINITE
        waveCircle1.startAnimation(waveAnimation1)
        
        val waveAnimation2 = AnimationUtils.loadAnimation(requireContext(), R.anim.wave_scale_animation)
        waveAnimation2.startOffset = 800
        waveAnimation2.repeatMode = Animation.RESTART
        waveAnimation2.repeatCount = Animation.INFINITE
        waveCircle2.startAnimation(waveAnimation2)
        
        val waveAnimation3 = AnimationUtils.loadAnimation(requireContext(), R.anim.wave_scale_animation)
        waveAnimation3.startOffset = 1600
        waveAnimation3.repeatMode = Animation.RESTART
        waveAnimation3.repeatCount = Animation.INFINITE
        waveCircle3.startAnimation(waveAnimation3)
    }
    
    private fun stopWaveAnimation() {
        waveCircle1.clearAnimation()
        waveCircle2.clearAnimation()
        waveCircle3.clearAnimation()
        
        // Fade out animation
        waveContainer.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                waveContainer.visibility = View.GONE
                waveContainer.alpha = 1.0f
            }
            .start()
    }
    
    private fun startMicPulseAnimation() {
        val pulseAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.mic_pulse_animation)
        pulseAnimation.repeatMode = Animation.REVERSE
        pulseAnimation.repeatCount = Animation.INFINITE
        micButton.startAnimation(pulseAnimation)
    }
    
    private fun stopMicPulseAnimation() {
        micButton.clearAnimation()
    }
    
    private fun displaySpokenText(spokenText: String) {
        // Display the spoken text with better formatting
        expressionText.text = "You said:"
        resultText.text = "\"$spokenText\""
        resultContainer.visibility = View.VISIBLE
        
        // Show success feedback briefly
        statusText.text = "✓ Captured! Listening for more..."
        
        // Reset status text after a delay
        micButton.postDelayed({
            if (isListening) {
                statusText.text = "Listening... (tap mic to stop)"
            }
        }, 2000)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    hasPermission = true
                    permissionDeniedCount = 0
                    showAskMathlyScreen()
                } else {
                    permissionDeniedCount++
                    if (permissionDeniedCount >= MAX_PERMISSION_DENIALS) {
                        // Permission denied twice, redirect to home
                        redirectToHome()
                    } else {
                        // Ask again
                        hasPermission = false
                        statusText.text = getString(R.string.voice_no_permission)
                    }
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }
} 