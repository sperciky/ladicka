package com.audiorecorder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import android.widget.TextView
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    companion object {
        private const val RECORD_AUDIO_PERMISSION_CODE = 1001
    }

    private lateinit var recordButton: MaterialButton
    private lateinit var statusText: TextView
    private lateinit var frequencyText: TextView
    private lateinit var toneText: TextView
    private lateinit var noteText: TextView

    private var audioRecorder: AudioRecorder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        recordButton = findViewById(R.id.recordButton)
        statusText = findViewById(R.id.statusText)
        frequencyText = findViewById(R.id.frequencyText)
        toneText = findViewById(R.id.toneText)
        noteText = findViewById(R.id.noteText)

        // Initialize AudioRecorder
        audioRecorder = AudioRecorder(
            onAnalysisResult = { result ->
                handleAnalysisResult(result)
            },
            onError = { error ->
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                stopRecording()
            }
        )

        // Set up button click listener
        recordButton.setOnClickListener {
            if (audioRecorder?.isRecording() == true) {
                stopRecording()
            } else {
                startRecording()
            }
        }
    }

    /**
     * Starts audio recording after checking permissions
     */
    private fun startRecording() {
        // Check for microphone permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION_CODE
            )
            return
        }

        // Start recording
        audioRecorder?.startRecording()
        updateUIForRecording(true)
    }

    /**
     * Stops audio recording
     */
    private fun stopRecording() {
        audioRecorder?.stopRecording()
        updateUIForRecording(false)
    }

    /**
     * Updates UI based on recording state
     */
    private fun updateUIForRecording(isRecording: Boolean) {
        if (isRecording) {
            recordButton.text = getString(R.string.stop_button)
            recordButton.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
            statusText.text = getString(R.string.recording)
        } else {
            recordButton.text = getString(R.string.record_button)
            recordButton.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_500))
            statusText.text = getString(R.string.no_data)
        }
    }

    /**
     * Handles analysis results and updates UI
     */
    private fun handleAnalysisResult(result: FrequencyAnalyzer.AudioAnalysisResult?) {
        if (result != null) {
            // Update frequency display
            frequencyText.text = String.format("%.2f Hz", result.frequency)

            // Update tone display
            val toneName = "${result.note}${result.octave}"
            toneText.text = toneName

            // Update note details with cents information
            val centsText = if (abs(result.centsOff) < 5) {
                "In tune ✓"
            } else {
                val direction = if (result.centsOff > 0) "sharp" else "flat"
                String.format("%.1f cents %s", abs(result.centsOff), direction)
            }
            noteText.text = centsText

            // Update status
            statusText.text = getString(R.string.recording)
        } else {
            // No clear frequency detected
            frequencyText.text = getString(R.string.no_data)
            toneText.text = getString(R.string.no_data)
            noteText.text = "No clear tone detected"
        }
    }

    /**
     * Handles permission request result
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            RECORD_AUDIO_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, start recording
                    startRecording()
                } else {
                    // Permission denied
                    Toast.makeText(
                        this,
                        getString(R.string.permission_required),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * Clean up resources when activity is destroyed
     */
    override fun onDestroy() {
        super.onDestroy()
        audioRecorder?.release()
    }

    /**
     * Stop recording when activity is paused
     */
    override fun onPause() {
        super.onPause()
        if (audioRecorder?.isRecording() == true) {
            stopRecording()
        }
    }
}
