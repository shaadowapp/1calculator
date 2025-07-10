package com.shaadow.onecalculator.mathly

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.shaadow.onecalculator.R
import java.util.Locale
import kotlin.math.min
import android.content.res.Configuration
import com.google.android.material.switchmaterial.SwitchMaterial
import android.widget.EditText

class MathlyFragment : Fragment() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognizerIntent: Intent
    private lateinit var tts: TextToSpeech

    private lateinit var tvTranscription: TextView
    private lateinit var tvAnswer: TextView
    private lateinit var btnStop: Button
    private lateinit var listeningAnimation: View
    private lateinit var btnConfirm: Button
    private lateinit var btnEdit: Button
    private lateinit var editCorrection: EditText
    private lateinit var switchLearn: SwitchMaterial

    private var isListening = false
    private var isMathlyActive = false
    private var hasPermission = false

    private var silenceHandler: Handler? = null
    private var silenceRunnable: Runnable? = null
    private val defaultSilenceTimeoutMs = 5000L // 5 seconds
    private val trainingSilenceTimeoutMs = 15000L // 15 seconds
    private var silenceTimeoutMs = defaultSilenceTimeoutMs

    private var quietCounter = 0
    private var noiseCounter = 0
    private val rmsQuietThreshold = 2f
    private val rmsNoiseThreshold = 8f
    private val rmsCounterLimit = 30 // about 3s if onRmsChanged called every 100ms
    private val restartDelayMs = 4000L // 4 seconds

    private var isRecognizerActive = false
    private var consecutiveErrors = 0
    private val errorTipThreshold = 3

    private val correctionMap = mutableMapOf<String, String>()
    private var lastRecognized: String = ""

    private var recognizerErrorState: Int? = null

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_mathly, container, false)
        tvTranscription = root.findViewById(R.id.tvTranscription)
        tvAnswer = root.findViewById(R.id.tvAnswer)
        btnStop = root.findViewById(R.id.btnStop)
        listeningAnimation = root.findViewById(R.id.listeningAnimation)
        btnConfirm = root.findViewById(R.id.btnConfirm)
        btnEdit = root.findViewById(R.id.btnEdit)
        editCorrection = root.findViewById(R.id.editCorrection)
        switchLearn = root.findViewById(R.id.switchLearn)
        btnConfirm.visibility = View.GONE
        btnEdit.visibility = View.GONE
        editCorrection.visibility = View.GONE
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tts = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) tts.language = Locale.US
        }

        checkPermission()

        updateStopButton()
        btnStop.setOnClickListener {
            if (isListening) {
                stopListening()
            } else {
                recognizerErrorState = null
                tvTranscription.text = "Listening..."
                tvAnswer.text = ""
                startListening()
            }
            updateStopButton()
        }

        tvTranscription.setOnClickListener {
            if (!isListening && hasPermission) startListening()
        }

        val leftEdge = view.findViewById<View>(R.id.left_edge_gesture)
        val rightEdge = view.findViewById<View>(R.id.right_edge_gesture)
        val gestureDetectorLeft = android.view.GestureDetector(requireContext(), object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: android.view.MotionEvent?, e2: android.view.MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val deltaX = e2.x - e1.x
                if (deltaX < -200 && Math.abs(velocityX) > 800) {
                    // Swipe left: go to Home tab (index 0)
                    (requireActivity().findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.view_pager)).currentItem = 0
                    return true
                }
                return false
            }
        })
        val gestureDetectorRight = android.view.GestureDetector(requireContext(), object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: android.view.MotionEvent?, e2: android.view.MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val deltaX = e2.x - e1.x
                if (deltaX > 200 && Math.abs(velocityX) > 800) {
                    // Swipe right: go to Chat tab (index 2)
                    (requireActivity().findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.view_pager)).currentItem = 2
                    return true
                }
                return false
            }
        })
        leftEdge.setOnTouchListener { _, event ->
            gestureDetectorLeft.onTouchEvent(event)
            false
        }
        rightEdge.setOnTouchListener { _, event ->
            gestureDetectorRight.onTouchEvent(event)
            false
        }
    }

    private fun checkPermission() {
        hasPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        
        if (hasPermission) {
            setupSpeechRecognizer()
            startListening()
        } else {
            requestPermission()
        }
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.RECORD_AUDIO),
            PERMISSION_REQUEST_CODE
        )
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, getDeviceLanguage())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                tvTranscription.text = "Listening..."
                animatePulse(true)
                quietCounter = 0
                noiseCounter = 0
                isRecognizerActive = true
            }
            override fun onResults(results: Bundle?) {
                isRecognizerActive = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.firstOrNull().orEmpty()
                lastRecognized = spokenText
                tvTranscription.text = spokenText
                if (spokenText.count { it.isLetterOrDigit() } >= 2) {
                    btnConfirm.visibility = View.VISIBLE
                    btnEdit.visibility = View.VISIBLE
                    btnConfirm.text = "Confirm: $spokenText"
                    btnEdit.text = "Edit"
                    btnEdit.setOnClickListener {
                        editCorrection.visibility = View.VISIBLE
                        editCorrection.setText("")
                        btnEdit.visibility = View.GONE
                        btnConfirm.text = "Save Correction"
                        btnConfirm.setOnClickListener {
                            val correction = editCorrection.text.toString().trim()
                            if (correction.isNotBlank() && switchLearn.isChecked) {
                                correctionMap[spokenText] = correction
                            }
                            btnConfirm.visibility = View.GONE
                            editCorrection.visibility = View.GONE
                            btnEdit.visibility = View.GONE
                            handleTranscription(correction.ifBlank { spokenText })
                            Handler(Looper.getMainLooper()).postDelayed({
                                if (isListening && !isRecognizerActive) startListening()
                            }, restartDelayMs)
                        }
                    }
                    btnConfirm.setOnClickListener {
                        btnConfirm.visibility = View.GONE
                        btnEdit.visibility = View.GONE
                        editCorrection.visibility = View.GONE
                        consecutiveErrors = 0
                        val corrected = correctionMap[spokenText] ?: spokenText
                        handleTranscription(corrected)
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (isListening && !isRecognizerActive) startListening()
                        }, restartDelayMs)
                    }
                } else {
                    val msg = "Sorry, I didn't get that. Please try again."
                    tvAnswer.text = msg
                    speak(msg)
                    consecutiveErrors++
                    if (consecutiveErrors >= errorTipThreshold) {
                        tvAnswer.text = "Tip: Try speaking slowly and clearly."
                    }
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (isListening && !isRecognizerActive) startListening()
                    }, restartDelayMs)
                }
            }
            override fun onError(error: Int) {
                isRecognizerActive = false
                tvAnswer.text = ""
                consecutiveErrors++
                val msg = when (error) {
                    SpeechRecognizer.ERROR_CLIENT -> {
                        recognizerErrorState = 5
                        "Internal error (code: 5). Tap Start to try again."
                    }
                    7 -> {
                        recognizerErrorState = 7
                        "No internet connection (code: 7). Tap Start to retry."
                    }
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Didn't catch that. Try again. (code: $error)"
                    else -> "Error occurred (code: $error). Tap Start to retry."
                }
                tvTranscription.text = msg
                speak(msg)
                if (error == SpeechRecognizer.ERROR_CLIENT || error == 7) {
                    try { speechRecognizer.destroy() } catch (_: Exception) {}
                    isRecognizerActive = false
                    isListening = false
                    updateStopButton()
                    return
                }
                if (consecutiveErrors >= errorTipThreshold) {
                    tvAnswer.text = "Tip: Try speaking slowly and clearly."
                }
                Handler(Looper.getMainLooper()).postDelayed({
                    if (isListening && !isRecognizerActive && recognizerErrorState == null) startListening()
                }, restartDelayMs)
            }
            override fun onBeginningOfSpeech() {
                cancelSilenceTimer()
                tvTranscription.text = "Listening..."
                tvAnswer.text = ""
                btnConfirm.visibility = View.GONE
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                // Only start silence timer if not editing/correcting
                if (!isCorrectionActive()) {
                    startSilenceTimer()
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (isListening && !isRecognizerActive && recognizerErrorState == null) startListening()
                    }, restartDelayMs)
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partialText = partial?.firstOrNull().orEmpty()
                if (partialText.isNotBlank()) {
                    tvTranscription.text = "Heard: $partialText"
                }
            }
            override fun onRmsChanged(rmsdB: Float) {
                // Clamp rmsdB to a reasonable range for animation
                val minDb = 0f
                val maxDb = 10f
                val clamped = rmsdB.coerceIn(minDb, maxDb)
                // Map rmsdB to a scale factor (1.0 to 1.5)
                val scale = 1f + (clamped - minDb) / (maxDb - minDb) * 0.5f
                listeningAnimation.scaleX = scale
                listeningAnimation.scaleY = scale
                listeningAnimation.alpha = 0.7f + (clamped - minDb) / (maxDb - minDb) * 0.3f
                // Noise/quiet feedback (less aggressive)
                if (clamped < rmsQuietThreshold) {
                    quietCounter++
                    noiseCounter = 0
                    if (quietCounter > rmsCounterLimit) {
                        tvTranscription.text = "Please speak louder."
                    }
                } else if (clamped > rmsNoiseThreshold) {
                    noiseCounter++
                    quietCounter = 0
                    if (noiseCounter > rmsCounterLimit) {
                        tvTranscription.text = "Too much background noise."
                    }
                } else {
                    quietCounter = 0
                    noiseCounter = 0
                }
            }
        })
    }

    private fun startListening() {
        if (isRecognizerActive || recognizerErrorState != null) return
        isListening = true
        isRecognizerActive = true
        setupSpeechRecognizer()
        speechRecognizer.startListening(recognizerIntent)
        animatePulse(true)
        updateStopButton()
    }

    private fun stopListening() {
        isListening = false
        isRecognizerActive = false
        try { speechRecognizer.stopListening() } catch (_: Exception) {}
        try { speechRecognizer.destroy() } catch (_: Exception) {}
        animatePulse(false)
        Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null)
        btnConfirm.visibility = View.GONE
        btnEdit.visibility = View.GONE
        editCorrection.visibility = View.GONE
        tvTranscription.text = "Stopped. Tap Start to listen."
        tvAnswer.text = ""
        updateStopButton()
    }

    private fun animatePulse(active: Boolean) {
        if (active) {
            val scale = ScaleAnimation(
                1f, 1.2f, 1f, 1.2f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f
            )
            scale.duration = 500
            scale.repeatMode = ScaleAnimation.REVERSE
            scale.repeatCount = ScaleAnimation.INFINITE
            listeningAnimation.startAnimation(scale)
        } else {
            listeningAnimation.clearAnimation()
        }
    }

    private fun recreateSpeechRecognizer() {
        try { speechRecognizer.destroy() } catch (_: Exception) {}
        setupSpeechRecognizer()
    }

    private fun soundex(word: String): String {
        val map = mapOf(
            'b' to '1', 'f' to '1', 'p' to '1', 'v' to '1',
            'c' to '2', 'g' to '2', 'j' to '2', 'k' to '2', 'q' to '2', 's' to '2', 'x' to '2', 'z' to '2',
            'd' to '3', 't' to '3',
            'l' to '4',
            'm' to '5', 'n' to '5',
            'r' to '6'
        )
        val w = word.lowercase(Locale.getDefault()).filter { it.isLetter() }
        if (w.isEmpty()) return ""
        val first = w[0]
        val tail = w.drop(1).map { map[it] ?: '0' }
        val filtered = (listOf(first) + tail).filterIndexed { i, c -> i == 0 || c != tail.getOrNull(i - 1) }
        val code = filtered.joinToString("").padEnd(4, '0').take(4)
        return code
    }

    private fun isSimilarToMathly(word: String): Boolean {
        val target = "mathly"
        val w = word.lowercase(Locale.getDefault())
        // Levenshtein distance
        fun levenshtein(a: String, b: String): Int {
            val dp = Array(a.length + 1) { IntArray(b.length + 1) }
            for (i in 0..a.length) dp[i][0] = i
            for (j in 0..b.length) dp[0][j] = j
            for (i in 1..a.length) {
                for (j in 1..b.length) {
                    dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1]
                    else min(min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]) + 1
                }
            }
            return dp[a.length][b.length]
        }
        // Soundex phonetic match
        val soundexMatch = soundex(w) == soundex(target)
        return levenshtein(w, target) <= 2 || soundexMatch
    }

    private fun handleTranscription(text: String) {
        val corrected = if (switchLearn.isChecked && correctionMap.containsKey(text)) correctionMap[text]!! else text
        val lower = corrected.lowercase(Locale.getDefault())
        val mathlyVariants = listOf(
            "mathly", "matly", "matlee", "mathlee", "matli", "mathli", "matlii", "mathlii",
            "maithali", "maithly", "mathali", "mathily", "maithily", "maithalee", "maithalee"
        )
        val words = lower.split(" ", ",", ".", "?", "!")
        val containsMathly = mathlyVariants.any { lower.contains(it) } || words.any { isSimilarToMathly(it) }
        when {
            !isMathlyActive && containsMathly -> {
                isMathlyActive = true
                tvAnswer.text = "Hi! Ask me a math question."
                speak("Hi! Ask me a math question.")
            }
            !isMathlyActive && words.any { isSimilarToMathly(it) } -> {
                tvAnswer.text = "Did you mean 'Mathly'? Please say 'Mathly' to activate."
                speak("Did you mean Mathly? Please say Mathly to activate.")
            }
            isMathlyActive && (lower.contains("bye mathly") || lower.contains("by mathly")) -> {
                isMathlyActive = false
                tvAnswer.text = "Goodbye!"
                speak("Goodbye!")
            }
            isMathlyActive -> {
                val answer = MathlyUtils.solveMath(corrected)
                when (answer) {
                    "__OPEN_HISTORY__" -> {
                        tvAnswer.text = "Opening history... (navigation not implemented)"
                        speak("Opening history")
                    }
                    "__OPEN_CALCULATOR__" -> {
                        tvAnswer.text = "Opening calculator... (navigation not implemented)"
                        speak("Opening calculator")
                    }
                    "__OPEN_SETTINGS__" -> {
                        tvAnswer.text = "Opening settings... (navigation not implemented)"
                        speak("Opening settings")
                    }
                    "__CLEAR__" -> {
                        tvTranscription.text = ""
                        tvAnswer.text = ""
                        speak("Cleared")
                    }
                    else -> {
                        tvAnswer.text = answer
                        speak(answer)
                    }
                }
            }
            else -> {
                tvAnswer.text = "Say 'Mathly' to activate."
            }
        }
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun startSilenceTimer() {
        if (isCorrectionActive()) return // Don't start timer if editing
        cancelSilenceTimer()
        silenceHandler = Handler(Looper.getMainLooper())
        silenceRunnable = Runnable {
            // Stop listening after 5 seconds of silence
            stopListening()
            tvTranscription.text = "Stopped due to silence. Tap Start to listen."
            animatePulse(false)
        }
        silenceHandler?.postDelayed(silenceRunnable!!, silenceTimeoutMs)
    }

    private fun cancelSilenceTimer() {
        silenceHandler?.removeCallbacks(silenceRunnable!!)
    }

    private fun isCorrectionActive(): Boolean {
        return editCorrection.visibility == View.VISIBLE
    }

    override fun onPause() {
        super.onPause()
        stopListening()
    }

    override fun onResume() {
        super.onResume()
        if (!isListening) startListening()
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
                    setupSpeechRecognizer()
                    startListening()
                } else {
                    hasPermission = false
                    tvTranscription.text = "Microphone permission required to use Mathly"
                    tvAnswer.text = "Please grant microphone permission in settings"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cancelSilenceTimer()
        stopListening()
        tts.shutdown()
        speechRecognizer.destroy()
    }

    private fun getDeviceLanguage(): String {
        val locale = resources.configuration.locales.get(0)
        return locale.toLanguageTag()
    }

    private fun updateStopButton() {
        btnStop.text = if (isListening) "Stop" else "Start"
    }

    fun setTrainingMode(enabled: Boolean) {
        silenceTimeoutMs = if (enabled) trainingSilenceTimeoutMs else defaultSilenceTimeoutMs
    }
} 