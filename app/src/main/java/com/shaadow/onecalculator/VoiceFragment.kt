package com.shaadow.onecalculator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
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
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import org.json.JSONObject
import org.json.JSONArray

class VoiceFragment : Fragment() {
    private lateinit var micButton: FloatingActionButton
    private lateinit var stopButton: MaterialButton
    private lateinit var statusText: TextView
    private lateinit var liveTranscriptionText: TextView
    private lateinit var mathlyStatus: TextView
    private lateinit var speakingIndicator: TextView
    private lateinit var waveContainer: View
    private lateinit var waveCircle1: View
    private lateinit var waveCircle2: View
    private lateinit var waveCircle3: View
    private lateinit var resultContainer: View
    private lateinit var expressionText: TextView
    private lateinit var resultText: TextView
    private lateinit var permissionContainer: View
    private lateinit var grantPermissionButton: MaterialButton
    private lateinit var skipPermissionButton: MaterialButton
    private lateinit var askMathlyContainer: View
    private lateinit var versionText: TextView
    private lateinit var disclaimerText: TextView
    private lateinit var errorCodeText: TextView
    private lateinit var debugInfoText: TextView

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isListening = false
    private var isMathlyActive = false
    private var hasPermission = false
    private var permissionDeniedCount = 0
    private var currentTranscription = ""
    private var isManualStop = false
    private var isSpeaking = false
    private var isAppActive = true
    private var conversationMode = false
    private var lastUserInput = ""
    private var listeningSessionCount = 0
    private var errorRetryCount = 0
    private var maxRetryAttempts = 3
    private var currentErrorCode = 0
    private var speechEngineIndex = 0
    private var consecutiveErrors = 0
    private var lastSuccessfulRecognition = 0L
    private var debugMode = true
    private var consecutiveNoMatchCount = 0
    private var lastRestartTime = 0L
    private var coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val micDotReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (intent?.action == "com.shaadow.onecalculator.ACTION_TOGGLE_MATHLY_LISTENING") {
                val shouldListen = intent.getBooleanExtra("is_listening", false)
                if (shouldListen) startListening() else stopListening()
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 123
        private const val MAX_PERMISSION_DENIALS = 2
        private const val LISTENING_SESSION_DURATION = 30000L // 30 seconds
        private const val RESTART_DELAY = 5000L // 5 seconds - less aggressive
        private const val ERROR_RETRY_DELAY = 8000L // 8 seconds for errors - more patient
        private const val CONVERSATION_TIMEOUT = 10000L // 10 seconds
        private const val MATHLY_VERSION = "1.0.0"
        private const val MAX_CONSECUTIVE_ERRORS = 10
        private const val ERROR_RESET_THRESHOLD = 60000L // 60 seconds
        
        private val MATHLY_WAKE_PHRASES = listOf("hi mathly", "hello mathly", "hey mathly", "mathly", "activate mathly")
        private val MATHLY_SOLVE_PHRASES = listOf("solve", "calculate", "what is", "compute", "find", "evaluate")
        private val MATHLY_NAVIGATION_PHRASES = mapOf(
            "open calculator" to "calculator",
            "go to calculator" to "calculator", 
            "show calculator" to "calculator",
            "open history" to "history",
            "go to history" to "history",
            "show history" to "history",
            "open home" to "home",
            "go to home" to "home",
            "show home" to "home",
            "go back" to "back",
            "back" to "back"
        )
        private val MATHLY_GREETING_PHRASES = listOf("how are you", "what can you do", "help", "hello", "hi", "capabilities")
        
        // Error codes for better debugging
        private val ERROR_CODES = mapOf(
            0 to "No Error",
            1 to "Network Error",
            2 to "Audio Error", 
            3 to "Client Error",
            4 to "Server Error",
            5 to "Network Timeout",
            6 to "No Match",
            7 to "Speech Timeout",
            8 to "Permission Error",
            9 to "Recognition Service Error",
            10 to "Multiple Recognition Failures",
            11 to "Engine Switch Required",
            12 to "AI Processing Error"
        )
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
        startDebugInfoUpdates()
    }

    override fun onResume() {
        super.onResume()
        isAppActive = true
        if (hasPermission && !isListening && !isManualStop) {
            startBackgroundListening()
        }
    }

    override fun onPause() {
        super.onPause()
        isAppActive = false
        stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        isAppActive = false
        isManualStop = true
        isListening = false
        
        coroutineScope.cancel()
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
        
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = android.content.IntentFilter("com.shaadow.onecalculator.ACTION_TOGGLE_MATHLY_LISTENING")
        requireContext().registerReceiver(micDotReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(micDotReceiver)
    }

    private fun initializeViews(view: View) {
        micButton = view.findViewById(R.id.mic_button)
        stopButton = view.findViewById(R.id.stop_button)
        statusText = view.findViewById(R.id.status_text)
        liveTranscriptionText = view.findViewById(R.id.live_transcription_text)
        mathlyStatus = view.findViewById(R.id.mathly_status)
        speakingIndicator = view.findViewById(R.id.speaking_indicator)
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
        versionText = view.findViewById(R.id.version_text)
        disclaimerText = view.findViewById(R.id.disclaimer_text)
        errorCodeText = view.findViewById(R.id.error_code_text)
        debugInfoText = view.findViewById(R.id.debug_info_text)
        
        // Set version and disclaimer
        versionText.text = "Mathly v$MATHLY_VERSION"
        disclaimerText.text = "Remember: Mathly can make mistakes, so double check it"
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
        stopButton.setOnClickListener { 
            manualStop()
            updateAllUI("Stopped", "", isMathlyActive)
        }
        grantPermissionButton.setOnClickListener { requestPermission() }
        skipPermissionButton.setOnClickListener { redirectToHome() }
        liveTranscriptionText.setOnClickListener {
            if (isMathlyActive) {
                speakResponse("Transcription test: I can hear you clearly!")
            } else {
                updateAllUI("Test: Tap mic to start listening", "Test: Tap mic to start listening", isMathlyActive)
            }
        }
        
        // Add test button for speech recognition
        debugInfoText.setOnClickListener {
            testSpeechRecognition()
        }
    }

    private fun startDebugInfoUpdates() {
        coroutineScope.launch {
            while (isActive) {
                updateDebugInfo()
                delay(2000) // Update every 2 seconds
            }
        }
        
        // Reset error counts periodically to prevent false engine switching
        coroutineScope.launch {
            while (isActive) {
                delay(ERROR_RESET_THRESHOLD)
                if (consecutiveErrors > 0 && System.currentTimeMillis() - lastSuccessfulRecognition > ERROR_RESET_THRESHOLD) {
                    consecutiveErrors = 0
                    // Log.d("Mathly", "Reset consecutive error count")
                }
            }
        }
    }

    private fun updateDebugInfo() {
        try {
            val debugInfo = buildString {
                append("Status: ${if (isMathlyActive) "Active" else "Inactive"}\n")
                append("Listening: ${if (isListening) "Yes" else "No"}\n")
                append("Session: $listeningSessionCount")
            }
            debugInfoText.text = debugInfo
        } catch (e: Exception) {
            // Ignore debug update errors
        }
    }
    
    private fun testSpeechRecognition() {
        // Log.d("Mathly", "Testing speech recognition...")
        updateAllUI("Testing speech recognition...", "", isMathlyActive)
        
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            updateAllUI("Speech recognition not available on this device", "", isMathlyActive)
            speakResponse("Speech recognition is not available on this device.")
            return
        }
        
        if (speechRecognizer == null) {
            updateAllUI("Speech recognizer not initialized", "", isMathlyActive)
            speakResponse("Speech recognizer is not initialized. Let me fix that.")
            setupSpeechRecognizer()
            return
        }
        
        speakResponse("Speech recognition test initiated. Please say something.")
        startBackgroundListening()
    }

    private fun getCurrentEngineName(): String {
        return when (speechEngineIndex) {
            0 -> "Google STT"
            1 -> "System STT"
            2 -> "Enhanced STT"
            else -> "Unknown"
        }
    }

    private fun logError(message: String, errorCode: Int) {
        currentErrorCode = errorCode
        // Log.e("Mathly", "Error $errorCode: $message")
        updateErrorDisplay()
    }

    private fun updateErrorDisplay() {
        try {
            val errorMessage = ERROR_CODES[currentErrorCode] ?: "Unknown Error"
            errorCodeText.text = "Error $currentErrorCode: $errorMessage"
        } catch (e: Exception) {
            // Ignore error display updates
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
        setupTextToSpeech()
        updateAllUI("Mathly v$MATHLY_VERSION is ready! Say 'Hi Mathly' to start", "", false)
        startBackgroundListening()
    }

    private fun redirectToHome() { 
        try {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        } catch (e: Exception) {
            logError("Navigation error: ${e.message}", 14)
        }
    }

    private fun setupTextToSpeech() {
        textToSpeech = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
                textToSpeech?.setSpeechRate(0.9f)
                textToSpeech?.setPitch(1.0f)
                speakResponse("Hello! I'm Mathly, your AI assistant. I'm ready to help with math and navigation!")
            } else {
                updateAllUI("Text-to-speech not available", currentTranscription, isMathlyActive)
                logError("TTS initialization failed", 15)
            }
        }
    }

    private fun speakResponse(response: String) {
        if (textToSpeech != null && !isSpeaking) {
            isSpeaking = true
            speakingIndicator.visibility = View.VISIBLE
            // Don't update transcription with Mathly's speech - keep user's voice only
            updateAllUI("Mathly: $response", currentTranscription, isMathlyActive)
            textToSpeech?.speak(response, TextToSpeech.QUEUE_FLUSH, null, "MATHLY_RESPONSE")
            val estimatedDuration = (response.length * 80).toLong()
            micButton.postDelayed({
                speakingIndicator.visibility = View.GONE
                isSpeaking = false
                // Continue listening after speaking
                if (isMathlyActive && isAppActive) {
                    startBackgroundListening()
                }
            }, estimatedDuration + 1000)
        }
    }

    private fun setupSpeechRecognizer() {
        try {
            // Check if speech recognition is available
            if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
                logError("Speech recognition not available on this device", 9)
                updateAllUI("Speech recognition not available", currentTranscription, isMathlyActive)
                return
            }
            
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
            
            if (speechRecognizer == null) {
                logError("Failed to create speech recognizer", 9)
                updateAllUI("Failed to initialize speech recognition", currentTranscription, isMathlyActive)
                return
            }
            
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    // Log.d("Mathly", "Ready for speech")
                    updateAllUI("Listening...", currentTranscription, isMathlyActive)
                    startWaveAnimation()
                }
                override fun onBeginningOfSpeech() {
                    // Log.d("Mathly", "Beginning of speech detected")
                    updateAllUI("I hear you...", currentTranscription, isMathlyActive)
                }
                override fun onRmsChanged(rmsdB: Float) {
                    // Update wave animation based on volume
                    updateWaveIntensity(rmsdB)
                }
                override fun onBufferReceived(buffer: ByteArray?) {
                    // Log.d("Mathly", "Buffer received")
                }
                override fun onEndOfSpeech() {
                    // Log.d("Mathly", "End of speech")
                    updateAllUI("Processing...", currentTranscription, isMathlyActive)
                    stopWaveAnimation()
                }
                override fun onError(error: Int) {
                    // Log.d("Mathly", "Speech recognition error: $error")
                    handleSpeechError(error)
                }
                override fun onResults(results: Bundle?) {
                    // Log.d("Mathly", "Speech recognition results received")
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spokenText = matches?.firstOrNull()?.lowercase(Locale.getDefault()) ?: ""
                    // Log.d("Mathly", "Recognized text: '$spokenText'")
                    // Log.d("Mathly", "All matches: ${matches?.joinToString(", ")}")
                    
                    // If final result is empty but we have partial results, use the last partial result
                    val finalText = if (spokenText.isEmpty() && currentTranscription.isNotEmpty()) {
                        // Log.d("Mathly", "Using partial result as final: '$currentTranscription'")
                        currentTranscription
                    } else {
                        spokenText
                    }
                    
                    processRecognitionEvent(finalText, isFinal = true)
                    if (isListening && !isManualStop && isAppActive) {
                        restartListeningWithDelay()
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partialText = partialMatches?.firstOrNull() ?: ""
                    if (partialText.isNotEmpty()) {
                        // Log.d("Mathly", "Partial result: '$partialText'")
                        processRecognitionEvent(partialText, isFinal = false)
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {
                    // Log.d("Mathly", "Speech recognition event: $eventType")
                }
            })
            
            // Log.d("Mathly", "Speech recognizer setup completed successfully")
        } catch (e: Exception) {
            logError("Speech recognizer setup failed: ${e.message}", 16)
            // Log.e("Mathly", "Speech recognizer setup exception", e)
        }
    }

    private fun updateWaveIntensity(rmsdB: Float) {
        try {
            val intensity = (rmsdB / 10f).coerceIn(0f, 1f)
            waveCircle1.alpha = 0.8f * intensity
            waveCircle2.alpha = 0.6f * intensity
            waveCircle3.alpha = 0.4f * intensity
        } catch (e: Exception) {
            // Ignore wave intensity errors
        }
    }

    private fun handleSpeechError(error: Int) {
        // Only increment consecutive errors for serious errors, not for normal "no match" scenarios
        when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> {
                consecutiveNoMatchCount++
                // Don't log this as an error in debug mode, it's normal during continuous listening
                if (debugMode) {
                    // Log.d("Mathly", "No speech match detected (normal during listening) - Count: $consecutiveNoMatchCount")
                } else {
                    logError("No speech match detected", 6)
                }
                handleNoMatchError()
            }
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                // Don't count timeouts as consecutive errors during continuous listening
                if (debugMode) {
                    // Log.d("Mathly", "Speech timeout (normal during listening)")
                } else {
                    logError("Speech timeout", 7)
                }
                handleSpeechTimeoutError()
            }
            SpeechRecognizer.ERROR_AUDIO -> {
                consecutiveErrors++
                logError("Audio error", 2)
                handleAudioError()
            }
            SpeechRecognizer.ERROR_CLIENT -> {
                consecutiveErrors++
                logError("Client error", 3)
                handleClientError()
            }
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                logError("Permission error", 8)
                handlePermissionError()
            }
            SpeechRecognizer.ERROR_NETWORK -> {
                consecutiveErrors++
                logError("Network error", 1)
                handleNetworkError()
            }
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                consecutiveErrors++
                logError("Network timeout", 5)
                handleNetworkTimeoutError()
            }
            SpeechRecognizer.ERROR_SERVER -> {
                consecutiveErrors++
                logError("Server error", 4)
                handleServerError()
            }
            else -> {
                consecutiveErrors++
                logError("Unknown error: $error", 17)
                handleGenericError(error)
            }
        }
        
        // Check if we need to switch engines only for serious errors
        if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
            switchSpeechEngine()
        }
    }

    private fun switchSpeechEngine() {
        speechEngineIndex = (speechEngineIndex + 1) % 3
        consecutiveErrors = 0
        logError("Switching to ${getCurrentEngineName()}", 11)
        updateAllUI("Switching to ${getCurrentEngineName()}...", currentTranscription, isMathlyActive)
        
        // Add a delay before switching to avoid rapid engine switching
        coroutineScope.launch {
            delay(2000) // 2 second delay
            setupSpeechRecognizer()
            if (isAppActive && !isManualStop) {
                startBackgroundListening()
            }
        }
    }

    private fun handleNoMatchError() {
        if (isListening && !isManualStop && isAppActive) {
            updateAllUI("Listening for commands...", currentTranscription, isMathlyActive)
            
            // Smart restart logic - reduce frequency during quiet periods
            val currentTime = System.currentTimeMillis()
            val timeSinceLastRestart = currentTime - lastRestartTime
            val baseDelay = RESTART_DELAY
            
            // Increase delay if we've had many consecutive no-matches
            val adjustedDelay = when {
                consecutiveNoMatchCount > 10 -> baseDelay * 3 // 15 seconds
                consecutiveNoMatchCount > 5 -> baseDelay * 2  // 10 seconds
                else -> baseDelay // 5 seconds
            }
            
            // Ensure minimum time between restarts
            val finalDelay = maxOf(adjustedDelay, 3000L - timeSinceLastRestart)
            
            // Log.d("Mathly", "Smart restart: consecutiveNoMatch=$consecutiveNoMatchCount, delay=${finalDelay}ms")
            restartListeningWithDelay(finalDelay)
            lastRestartTime = currentTime
        } else {
            updateAllUI("No speech detected", currentTranscription, isMathlyActive)
        }
    }

    private fun handleSpeechTimeoutError() {
        if (isListening && !isManualStop && isAppActive) {
            updateAllUI("Listening for commands...", currentTranscription, isMathlyActive)
            restartListeningWithDelay()
        } else {
            updateAllUI("Speech timeout", currentTranscription, isMathlyActive)
        }
    }

    private fun handleAudioError() {
        errorRetryCount++
        if (errorRetryCount <= maxRetryAttempts && isAppActive && !isManualStop) {
            updateAllUI("Audio error, retrying... (${errorRetryCount}/$maxRetryAttempts)", currentTranscription, isMathlyActive)
            restartListeningWithDelay(ERROR_RETRY_DELAY)
        } else {
            updateAllUI("Audio error - please check your microphone", currentTranscription, isMathlyActive)
            speakResponse("There's an audio issue. Please check your microphone and try again.")
            errorRetryCount = 0
        }
    }

    private fun handleClientError() {
        errorRetryCount++
        if (errorRetryCount <= maxRetryAttempts && isAppActive && !isManualStop) {
            updateAllUI("Retrying... (${errorRetryCount}/$maxRetryAttempts)", currentTranscription, isMathlyActive)
            restartListeningWithDelay(ERROR_RETRY_DELAY)
        } else {
            updateAllUI("Restarting speech recognition", currentTranscription, isMathlyActive)
            // Don't speak technical issues to user - just restart silently
            errorRetryCount = 0
            recreateSpeechRecognizer()
        }
    }

    private fun handlePermissionError() {
        updateAllUI("Microphone permission required", currentTranscription, isMathlyActive)
        speakResponse("I need microphone permission to hear you. Please grant it in settings.")
        stopListening()
    }

    private fun handleNetworkError() {
        errorRetryCount++
        if (errorRetryCount <= maxRetryAttempts && isAppActive && !isManualStop) {
            updateAllUI("Network error, retrying... (${errorRetryCount}/$maxRetryAttempts)", currentTranscription, isMathlyActive)
            restartListeningWithDelay(ERROR_RETRY_DELAY)
        } else {
            updateAllUI("Network error - check your internet connection", currentTranscription, isMathlyActive)
            speakResponse("Network issue detected. Please check your internet connection and try again.")
            errorRetryCount = 0
        }
    }

    private fun handleNetworkTimeoutError() {
        errorRetryCount++
        if (errorRetryCount <= maxRetryAttempts && isAppActive && !isManualStop) {
            updateAllUI("Network timeout, retrying... (${errorRetryCount}/$maxRetryAttempts)", currentTranscription, isMathlyActive)
            restartListeningWithDelay(ERROR_RETRY_DELAY)
        } else {
            updateAllUI("Network timeout - connection is slow", currentTranscription, isMathlyActive)
            speakResponse("Network is slow. Please try again when your connection is better.")
            errorRetryCount = 0
        }
    }

    private fun handleServerError() {
        errorRetryCount++
        if (errorRetryCount <= maxRetryAttempts && isAppActive && !isManualStop) {
            updateAllUI("Server error, retrying... (${errorRetryCount}/$maxRetryAttempts)", currentTranscription, isMathlyActive)
            restartListeningWithDelay(ERROR_RETRY_DELAY)
        } else {
            updateAllUI("Server error - try again later", currentTranscription, isMathlyActive)
            speakResponse("Server issue. Please try again in a few moments.")
            errorRetryCount = 0
        }
    }

    private fun handleGenericError(error: Int) {
        errorRetryCount++
        if (errorRetryCount <= maxRetryAttempts && isAppActive && !isManualStop) {
            updateAllUI("Error $error, retrying... (${errorRetryCount}/$maxRetryAttempts)", currentTranscription, isMathlyActive)
            restartListeningWithDelay(ERROR_RETRY_DELAY)
        } else {
            updateAllUI("Error $error - something went wrong", currentTranscription, isMathlyActive)
            speakResponse("Something went wrong. Please try again.")
            errorRetryCount = 0
        }
    }

    private fun recreateSpeechRecognizer() {
        try {
            // Smart approach: Add delay before recreating to prevent client errors
            coroutineScope.launch {
                delay(2000) // Wait 2 seconds before recreating
                
                try {
                    speechRecognizer?.destroy()
                    speechRecognizer = null
                    
                    delay(1000) // Additional delay for cleanup
                    
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
                    setupSpeechRecognizer()
                    
                    if (isAppActive && !isManualStop) {
                        startBackgroundListening()
                    }
                } catch (e: Exception) {
                    logError("Failed to recreate speech recognizer: ${e.message}", 18)
                }
            }
        } catch (e: Exception) {
            logError("Failed to recreate speech recognizer: ${e.message}", 18)
        }
    }

    private fun processRecognitionEvent(text: String, isFinal: Boolean) {
        if (text.isBlank()) return
        
        currentTranscription = text
        lastUserInput = text
        errorRetryCount = 0 // Reset error count on successful recognition
        
        if (isFinal) {
            lastSuccessfulRecognition = System.currentTimeMillis()
            consecutiveErrors = 0
            consecutiveNoMatchCount = 0 // Reset no-match count on successful recognition
            listeningSessionCount++
        }
        
        updateAllUI(if (isFinal) "Heard: $text" else "Listening...", text, isMathlyActive)
        
        if (isFinal) {
            // Check for wake phrase
            if (!isMathlyActive && containsAny(text, MATHLY_WAKE_PHRASES)) {
                activateMathly()
                return
            }
            
            // If Mathly is active, process commands
            if (isMathlyActive) {
                when {
                    containsAny(text, MATHLY_SOLVE_PHRASES) -> {
                        solveMathExpression(text)
                    }
                    containsAny(text, MATHLY_NAVIGATION_PHRASES.keys) -> {
                        handleNavigationCommand(text)
                    }
                    containsAny(text, MATHLY_GREETING_PHRASES) -> {
                        handleGreeting(_text = text)
                    }
                    containsAny(text, listOf("goodbye", "bye", "stop", "exit", "deactivate")) -> {
                        deactivateMathly()
                    }
                    else -> {
                        handleUnknownCommand(_text = text)
                    }
                }
            }
        }
    }

    private fun containsAny(text: String, phrases: Collection<String>): Boolean {
        val lower = text.lowercase(Locale.getDefault())
        return phrases.any { lower.contains(it) }
    }

    private fun activateMathly() {
        isMathlyActive = true
        conversationMode = true
        updateAllUI("Mathly activated!", currentTranscription, true)
        speakResponse("Hello! I'm Mathly, your AI assistant. I can help with math calculations, navigation, and more. What would you like me to do?")
        startEnhancedWaveAnimation()
    }

    private fun deactivateMathly() {
        isMathlyActive = false
        conversationMode = false
        updateAllUI("Mathly deactivated", currentTranscription, false)
        speakResponse("Goodbye! I'll be here when you need me. Just say 'Hi Mathly' to activate again.")
        stopEnhancedWaveAnimation()
    }

    private fun handleGreeting(_text: String) {
        val responses = listOf(
            "I'm doing great! I'm here to help you with math calculations and navigation. What can I do for you?",
            "Hello! I'm ready to assist you. I can solve math problems, help you navigate the app, or just chat. What would you like?",
            "Hi there! I'm Mathly, your AI assistant. I can calculate math expressions, open different sections of the app, and more. How can I help?"
        )
        val response = responses.random()
        updateAllUI("Greeting received", currentTranscription, true)
        speakResponse(response)
    }

    private fun handleNavigationCommand(text: String) {
        val lowerText = text.lowercase(Locale.getDefault())
        val command = MATHLY_NAVIGATION_PHRASES.entries.find { lowerText.contains(it.key) }
        
        if (command != null) {
            when (command.value) {
                "calculator" -> {
                    updateAllUI("Opening calculator...", currentTranscription, true)
                    speakResponse("Opening the calculator for you.")
                    // Navigate to calculator (you'll need to implement this)
                }
                "history" -> {
                    updateAllUI("Opening history...", currentTranscription, true)
                    speakResponse("Opening the history section.")
                    // Navigate to history (you'll need to implement this)
                }
                "home" -> {
                    updateAllUI("Going to home...", currentTranscription, true)
                    speakResponse("Taking you to the home screen.")
                    // Navigate to home (you'll need to implement this)
                }
                "back" -> {
                    updateAllUI("Going back...", currentTranscription, true)
                    speakResponse("Going back to the previous screen.")
                    try {
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    } catch (e: Exception) {
                        updateAllUI("Navigation error", currentTranscription, true)
                        logError("Navigation error: ${e.message}", 19)
                    }
                }
            }
        }
    }

    private fun handleUnknownCommand(_text: String) {
        val responses = listOf(
            "I didn't understand that. You can ask me to solve math problems, open different sections of the app, or just say hello.",
            "I'm not sure what you mean. Try saying 'solve 5 plus 3' for math, or 'open calculator' for navigation.",
            "I didn't catch that. You can ask me to calculate something, navigate the app, or just chat with me."
        )
        val response = responses.random()
        updateAllUI("Unknown command", currentTranscription, true)
        speakResponse(response)
    }

    private fun solveMathExpression(spokenText: String) {
        coroutineScope.launch {
            try {
                val expression = extractMathExpression(spokenText)
                if (expression.isNotEmpty()) {
                    updateAllUI("Processing math expression...", currentTranscription, true)
                    
                    // Try AI-powered math solving first
                    val aiResult = tryAIMathSolving(expression)
                    if (aiResult != null) {
                        val response = "The answer is $aiResult for $expression. Is there anything else you'd like me to calculate?"
                        displayResult(expression, aiResult)
                        updateAllUI("Solved with AI!", currentTranscription, true)
                        speakResponse(response)
                    } else {
                        // Fallback to basic math
                        val result = evaluateMathExpression(expression)
                        val response = "The answer is $result for $expression. Is there anything else you'd like me to calculate?"
                        displayResult(expression, result)
                        updateAllUI("Solved!", currentTranscription, true)
                        speakResponse(response)
                    }
                } else {
                    updateAllUI("Couldn't understand the expression", currentTranscription, true)
                    speakResponse("I couldn't understand the math expression. Try saying 'solve 5 plus 3' or 'calculate 10 times 2'.")
                }
            } catch (e: Exception) {
                logError("Math solving error: ${e.message}", 12)
                updateAllUI("Math processing error", currentTranscription, true)
                speakResponse("Sorry, I encountered an error while processing the math expression. Please try again.")
            }
        }
    }

    private suspend fun tryAIMathSolving(expression: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Using a free math API (Wolfram Alpha style)
                val encodedExpression = URLEncoder.encode(expression, "UTF-8")
                val url = URL("https://api.mathjs.org/v4/?expr=$encodedExpression")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                
                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val result = reader.readText().trim()
                    reader.close()
                    connection.disconnect()
                    result
                } else {
                    null
                }
            } catch (e: Exception) {
                // Log.e("Mathly", "AI math solving failed: ${e.message}")
                null
            }
        }
    }

    private fun extractMathExpression(spokenText: String): String {
        var expression = spokenText
        MATHLY_SOLVE_PHRASES.forEach { phrase ->
            expression = expression.replace(phrase, "", ignoreCase = true)
        }
        
        expression = expression
            .replace("plus", "+", ignoreCase = true)
            .replace("minus", "-", ignoreCase = true)
            .replace("times", "*", ignoreCase = true)
            .replace("multiplied by", "*", ignoreCase = true)
            .replace("divided by", "/", ignoreCase = true)
            .replace("equals", "=", ignoreCase = true)
            .replace(" ", "")
        
        return expression
    }

    private fun evaluateMathExpression(expression: String): String {
        return try {
            when {
                expression.contains("+") -> {
                    val parts = expression.split("+")
                    val result = parts[0].toDouble() + parts[1].toDouble()
                    result.toString()
                }
                expression.contains("-") -> {
                    val parts = expression.split("-")
                    val result = parts[0].toDouble() - parts[1].toDouble()
                    result.toString()
                }
                expression.contains("*") -> {
                    val parts = expression.split("*")
                    val result = parts[0].toDouble() * parts[1].toDouble()
                    result.toString()
                }
                expression.contains("/") -> {
                    val parts = expression.split("/")
                    val result = parts[0].toDouble() / parts[1].toDouble()
                    result.toString()
                }
                else -> {
                    expression.toDouble().toString()
                }
            }
        } catch (e: Exception) {
            "Error"
        }
    }

    private fun startBackgroundListening() {
        if (!isAppActive || isManualStop) return
        
        if (speechRecognizer == null) {
            // Log.e("Mathly", "Speech recognizer is null, cannot start listening")
            updateAllUI("Speech recognizer not initialized", "", isMathlyActive)
            setupSpeechRecognizer()
            return
        }
        
        // Smart approach: Only start if we haven't started recently
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastRestartTime < 3000) {
            // Log.d("Mathly", "Skipping restart - too soon since last attempt")
            return
        }
        
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Listening for commands...")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1) // Reduce to 1 for better performance
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, requireContext().packageName)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 8000) // Balanced silence tolerance
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 6000) // Balanced silence tolerance
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500) // Reduced for better responsiveness
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true) // Prefer offline for better stability
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            }
            
            // Log.d("Mathly", "Starting speech recognition...")
            speechRecognizer?.startListening(intent)
            isListening = true
            lastRestartTime = currentTime
            updateAllUI("Listening...", "", isMathlyActive)
            startWaveAnimation()
            
            // Shorter session duration to prevent client errors
            micButton.postDelayed({
                if (isListening && !isManualStop && isAppActive) {
                    // Log.d("Mathly", "Session timeout, restarting listening")
                    restartListeningWithDelay()
                }
            }, 20000) // Reduced from 30s to 20s
            
        } catch (e: Exception) {
            // Log.e("Mathly", "Error starting speech recognition", e)
            updateAllUI("Error starting speech recognition", "", isMathlyActive)
            logError("Speech recognition start error: ${e.message}", 20)
            handleClientError()
        }
    }

    private fun startListening() {
        isManualStop = false
        startBackgroundListening()
        micButton.setImageResource(R.drawable.ic_stop)
        stopButton.visibility = View.VISIBLE
    }

    private fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
        micButton.setImageResource(R.drawable.ic_microphone)
        stopButton.visibility = View.GONE
        stopWaveAnimation()
        updateAllUI("Stopped listening", currentTranscription, isMathlyActive)
    }

    private fun manualStop() {
        isManualStop = true
        stopListening()
        updateAllUI("Tap mic to start listening again", currentTranscription, isMathlyActive)
    }

    private fun restartListeningWithDelay(delay: Long = RESTART_DELAY) {
        if (!isAppActive || isManualStop) return
        
        micButton.postDelayed({
            if (isAppActive && !isManualStop) {
                startBackgroundListening()
            }
        }, delay)
    }

    private fun updateAllUI(status: String, transcript: String, mathlyActive: Boolean) {
        try {
            statusText.text = status
            // Only show user's voice in transcription, not Mathly's responses
            if (transcript.isNotEmpty() && !isSpeaking) {
                liveTranscriptionText.text = transcript
            }
            mathlyStatus.text = if (mathlyActive) getString(R.string.voice_mathly_active) else getString(R.string.voice_mathly_inactive)
        } catch (e: Exception) {
            logError("UI update error: ${e.message}", 21)
        }
    }

    private fun displayResult(expression: String, result: String) {
        try {
            expressionText.text = "Expression: $expression"
            resultText.text = "Result: $result"
            resultContainer.visibility = View.VISIBLE
        } catch (e: Exception) {
            logError("Result display error: ${e.message}", 22)
        }
    }

    private fun startWaveAnimation() {
        try {
            waveContainer.visibility = View.VISIBLE
            waveContainer.alpha = 1.0f
            val waveAnimation1 = AnimationUtils.loadAnimation(requireContext(), R.anim.wave_scale_animation)
            waveAnimation1.repeatMode = Animation.RESTART
            waveAnimation1.repeatCount = Animation.INFINITE
            waveCircle1.startAnimation(waveAnimation1)
            val waveAnimation2 = AnimationUtils.loadAnimation(requireContext(), R.anim.wave_scale_animation)
            waveAnimation2.startOffset = 600
            waveAnimation2.repeatMode = Animation.RESTART
            waveAnimation2.repeatCount = Animation.INFINITE
            waveCircle2.startAnimation(waveAnimation2)
            val waveAnimation3 = AnimationUtils.loadAnimation(requireContext(), R.anim.wave_scale_animation)
            waveAnimation3.startOffset = 1200
            waveAnimation3.repeatMode = Animation.RESTART
            waveAnimation3.repeatCount = Animation.INFINITE
            waveCircle3.startAnimation(waveAnimation3)
        } catch (e: Exception) {
            logError("Wave animation error: ${e.message}", 23)
        }
    }

    private fun startEnhancedWaveAnimation() {
        startWaveAnimation()
        startMicPulseAnimation()
    }

    private fun stopEnhancedWaveAnimation() {
        stopWaveAnimation()
        stopMicPulseAnimation()
    }

    private fun stopWaveAnimation() {
        try {
            waveCircle1.clearAnimation()
            waveCircle2.clearAnimation()
            waveCircle3.clearAnimation()
            waveContainer.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    waveContainer.visibility = View.GONE
                    waveContainer.alpha = 1.0f
                }
                .start()
        } catch (e: Exception) {
            logError("Stop wave animation error: ${e.message}", 24)
        }
    }

    private fun startMicPulseAnimation() {
        try {
            val pulseAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.mic_pulse_animation)
            pulseAnimation.repeatMode = Animation.REVERSE
            pulseAnimation.repeatCount = Animation.INFINITE
            micButton.startAnimation(pulseAnimation)
        } catch (e: Exception) {
            logError("Mic pulse animation error: ${e.message}", 25)
        }
    }

    private fun stopMicPulseAnimation() {
        try {
            micButton.clearAnimation()
        } catch (e: Exception) {
            logError("Stop mic pulse animation error: ${e.message}", 26)
        }
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
                        redirectToHome()
                    } else {
                        hasPermission = false
                        updateAllUI("Microphone permission required", currentTranscription, isMathlyActive)
                    }
                }
            }
        }
    }
} 