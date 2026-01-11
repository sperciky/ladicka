package com.audiorecorder

import kotlin.math.*

/**
 * FrequencyAnalyzer uses FFT (Fast Fourier Transform) to analyze audio data
 * and detect the dominant frequency and musical note
 */
class FrequencyAnalyzer(private val sampleRate: Int) {

    data class AudioAnalysisResult(
        val frequency: Double,
        val note: String,
        val octave: Int,
        val centsOff: Double
    )

    /**
     * Analyzes audio buffer and returns the dominant frequency and note information
     */
    fun analyze(audioData: ShortArray): AudioAnalysisResult? {
        // Convert to double array and normalize
        val samples = DoubleArray(audioData.size) { audioData[it].toDouble() / Short.MAX_VALUE }

        // Apply Hamming window to reduce spectral leakage
        applyHammingWindow(samples)

        // Perform FFT
        val fftSize = findNextPowerOfTwo(samples.size)
        val fftResult = performFFT(samples, fftSize)

        // Find dominant frequency
        val frequency = findDominantFrequency(fftResult, sampleRate)

        if (frequency < 20.0 || frequency > 4000.0) {
            return null // Out of reasonable range
        }

        // Convert frequency to musical note
        val (note, octave, centsOff) = frequencyToNote(frequency)

        return AudioAnalysisResult(frequency, note, octave, centsOff)
    }

    /**
     * Applies Hamming window to reduce spectral leakage
     */
    private fun applyHammingWindow(samples: DoubleArray) {
        val n = samples.size
        for (i in samples.indices) {
            val window = 0.54 - 0.46 * cos(2.0 * PI * i / (n - 1))
            samples[i] *= window
        }
    }

    /**
     * Find next power of two for FFT
     */
    private fun findNextPowerOfTwo(n: Int): Int {
        var power = 1
        while (power < n) {
            power *= 2
        }
        return power
    }

    /**
     * Performs Fast Fourier Transform
     */
    private fun performFFT(samples: DoubleArray, fftSize: Int): DoubleArray {
        val real = DoubleArray(fftSize)
        val imag = DoubleArray(fftSize)

        // Copy samples and pad with zeros
        for (i in samples.indices.take(fftSize)) {
            real[i] = samples[i]
        }

        // Perform FFT
        fft(real, imag)

        // Calculate magnitude spectrum
        val magnitude = DoubleArray(fftSize / 2)
        for (i in magnitude.indices) {
            magnitude[i] = sqrt(real[i] * real[i] + imag[i] * imag[i])
        }

        return magnitude
    }

    /**
     * FFT implementation (Cooley-Tukey algorithm)
     */
    private fun fft(real: DoubleArray, imag: DoubleArray) {
        val n = real.size
        if (n <= 1) return

        // Bit reversal
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                var temp = real[i]
                real[i] = real[j]
                real[j] = temp

                temp = imag[i]
                imag[i] = imag[j]
                imag[j] = temp
            }

            var k = n / 2
            while (k <= j) {
                j -= k
                k /= 2
            }
            j += k
        }

        // Cooley-Tukey FFT
        var length = 2
        while (length <= n) {
            val angle = -2.0 * PI / length
            val wStepReal = cos(angle)
            val wStepImag = sin(angle)

            for (i in 0 until n step length) {
                var wReal = 1.0
                var wImag = 0.0

                for (j in 0 until length / 2) {
                    val tReal = wReal * real[i + j + length / 2] - wImag * imag[i + j + length / 2]
                    val tImag = wReal * imag[i + j + length / 2] + wImag * real[i + j + length / 2]

                    real[i + j + length / 2] = real[i + j] - tReal
                    imag[i + j + length / 2] = imag[i + j] - tImag
                    real[i + j] += tReal
                    imag[i + j] += tImag

                    val wNewReal = wReal * wStepReal - wImag * wStepImag
                    wImag = wReal * wStepImag + wImag * wStepReal
                    wReal = wNewReal
                }
            }
            length *= 2
        }
    }

    /**
     * Finds the dominant frequency from FFT magnitude spectrum
     */
    private fun findDominantFrequency(magnitude: DoubleArray, sampleRate: Int): Double {
        var maxMagnitude = 0.0
        var maxIndex = 0

        // Skip DC component (index 0) and find peak
        for (i in 1 until magnitude.size) {
            if (magnitude[i] > maxMagnitude) {
                maxMagnitude = magnitude[i]
                maxIndex = i
            }
        }

        // Convert bin to frequency
        val frequencyResolution = sampleRate.toDouble() / (magnitude.size * 2)
        return maxIndex * frequencyResolution
    }

    /**
     * Converts frequency to musical note
     * Returns (note name, octave, cents off from exact pitch)
     */
    private fun frequencyToNote(frequency: Double): Triple<String, Int, Double> {
        val notes = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

        // A4 = 440 Hz is our reference
        val a4 = 440.0
        val a4Index = 57 // A4 is the 58th key on piano (index 57)

        // Calculate semitones from A4
        val semitones = 12 * log2(frequency / a4)
        val noteNumber = (a4Index + semitones.roundToInt()) % 12
        val octave = ((a4Index + semitones.roundToInt()) / 12)

        // Calculate cents off (100 cents = 1 semitone)
        val exactSemitones = a4Index + semitones
        val nearestSemitone = exactSemitones.roundToInt()
        val centsOff = (exactSemitones - nearestSemitone) * 100

        return Triple(notes[noteNumber], octave, centsOff)
    }
}
