package com.audiorecorder

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*

/**
 * AudioRecorder manages audio recording and real-time frequency analysis
 */
class AudioRecorder(
    private val onAnalysisResult: (FrequencyAnalyzer.AudioAnalysisResult?) -> Unit,
    private val onError: (String) -> Unit
) {
    companion object {
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_MULTIPLIER = 2
    }

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingJob: Job? = null
    private val frequencyAnalyzer = FrequencyAnalyzer(SAMPLE_RATE)

    /**
     * Checks if recording is currently active
     */
    fun isRecording(): Boolean = isRecording

    /**
     * Starts audio recording and analysis
     */
    fun startRecording() {
        if (isRecording) return

        try {
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            ) * BUFFER_SIZE_MULTIPLIER

            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                onError("Failed to get buffer size")
                return
            }

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                onError("Failed to initialize AudioRecord")
                return
            }

            audioRecord?.startRecording()
            isRecording = true

            // Start recording in a coroutine
            recordingJob = CoroutineScope(Dispatchers.IO).launch {
                recordAudio(bufferSize)
            }

        } catch (e: SecurityException) {
            onError("Microphone permission not granted")
        } catch (e: Exception) {
            onError("Error starting recording: ${e.message}")
        }
    }

    /**
     * Stops audio recording
     */
    fun stopRecording() {
        if (!isRecording) return

        isRecording = false
        recordingJob?.cancel()

        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            onError("Error stopping recording: ${e.message}")
        }
    }

    /**
     * Records audio and performs frequency analysis
     */
    private suspend fun recordAudio(bufferSize: Int) {
        val audioBuffer = ShortArray(bufferSize)

        while (isRecording) {
            val readSize = audioRecord?.read(audioBuffer, 0, bufferSize) ?: 0

            if (readSize > 0) {
                // Analyze the audio data
                val result = frequencyAnalyzer.analyze(audioBuffer)

                // Post result on main thread
                withContext(Dispatchers.Main) {
                    onAnalysisResult(result)
                }

                // Add small delay to avoid overwhelming the UI
                delay(100)
            } else {
                // Handle read errors
                when (readSize) {
                    AudioRecord.ERROR_INVALID_OPERATION -> {
                        withContext(Dispatchers.Main) {
                            onError("Invalid operation during recording")
                        }
                        stopRecording()
                    }
                    AudioRecord.ERROR_BAD_VALUE -> {
                        withContext(Dispatchers.Main) {
                            onError("Bad value during recording")
                        }
                        stopRecording()
                    }
                }
            }
        }
    }

    /**
     * Releases all resources
     */
    fun release() {
        stopRecording()
    }
}
