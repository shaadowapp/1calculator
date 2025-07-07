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

class MathlyFragment : Fragment() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var recognizerIntent: Intent
    private lateinit var tts: TextToSpeech

    private lateinit var tvTranscription: TextView
    private lateinit var tvAnswer: TextView
    private lateinit var btnStop: Button
    private lateinit var listeningAnimation: View

    private var isListening = false
    private var isMathlyActive = false
    private var hasPermission = false

    private var silenceHandler: Handler? = null
    private var silenceRunnable: Runnable? = null
    private val silenceTimeoutMs = 5000L // 5 seconds

    private var quietCounter = 0
    private var noiseCounter = 0
    private val rmsQuietThreshold = 2f
    private val rmsNoiseThreshold = 8f
    private val rmsCounterLimit = 30 // about 3s if onRmsChanged called every 100ms
    private val restartDelayMs = 2000L // 2 seconds

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_mathly, container, false)
        tvTranscription = root.findViewById(R.id.tvTranscription)
        tvAnswer = root.findViewById(R.id.tvAnswer)
        btnStop = root.findViewById(R.id.btnStop)
        listeningAnimation = root.findViewById(R.id.listeningAnimation)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tts = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) tts.language = Locale.US
        }

        checkPermission()

        btnStop.setOnClickListener {
            stopListening()
            tvTranscription.text = "Stopped. Tap to restart."
            animatePulse(false)
        }

        tvTranscription.setOnClickListener {
            if (!isListening && hasPermission) startListening()
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
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                tvTranscription.text = "Listening..."
                animatePulse(true)
                quietCounter = 0
                noiseCounter = 0
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.firstOrNull().orEmpty()
                tvTranscription.text = spokenText
                if (spokenText.isNotBlank()) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        handleTranscription(spokenText)
                        if (isListening) startListening()
                    }, restartDelayMs)
                } else {
                    tvAnswer.text = ""
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (isListening) startListening()
                    }, restartDelayMs)
                }
            }
            override fun onError(error: Int) {
                tvAnswer.text = "" // Clear answer field on error
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        tvTranscription.text = "Didn't catch that. Try again. (code: $error)"
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (isListening) startListening()
                        }, restartDelayMs)
                    }
                    else -> {
                        tvTranscription.text = "Error occurred (code: $error). Tap to retry."
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (isListening) startListening()
                        }, restartDelayMs)
                    }
                }
            }
            override fun onBeginningOfSpeech() {
                cancelSilenceTimer()
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                // Start silence timer
                startSilenceTimer()
                // Add a 2s delay before restarting listening (if needed)
                Handler(Looper.getMainLooper()).postDelayed({
                    if (isListening) startListening()
                }, restartDelayMs)
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partialText = partial?.firstOrNull().orEmpty()
                tvTranscription.text = partialText
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
        isListening = true
        speechRecognizer.startListening(recognizerIntent)
        animatePulse(true)
    }

    private fun stopListening() {
        isListening = false
        speechRecognizer.stopListening()
        animatePulse(false)
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
        return levenshtein(w, target) <= 2
    }

    private fun handleTranscription(text: String) {
        val lower = text.lowercase(Locale.getDefault())
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
                val answer = MathlyUtils.solveMath(text)
                when (answer) {
                    "__OPEN_HISTORY__" -> {
                        tvAnswer.text = "Opening history... (navigation not implemented)"
                        speak("Opening history")
                        // TODO: Implement navigation to history screen
                    }
                    "__OPEN_CALCULATOR__" -> {
                        tvAnswer.text = "Opening calculator... (navigation not implemented)"
                        speak("Opening calculator")
                        // TODO: Implement navigation to calculator screen
                    }
                    "__OPEN_SETTINGS__" -> {
                        tvAnswer.text = "Opening settings... (navigation not implemented)"
                        speak("Opening settings")
                        // TODO: Implement navigation to settings screen
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
        cancelSilenceTimer()
        silenceHandler = Handler(Looper.getMainLooper())
        silenceRunnable = Runnable {
            // Stop listening after 5 seconds of silence
            stopListening()
            tvTranscription.text = "Stopped due to silence. Tap to restart."
            animatePulse(false)
        }
        silenceHandler?.postDelayed(silenceRunnable!!, silenceTimeoutMs)
    }

    private fun cancelSilenceTimer() {
        silenceHandler?.removeCallbacks(silenceRunnable!!)
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
} 