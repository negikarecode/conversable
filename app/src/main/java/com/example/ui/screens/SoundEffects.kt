package com.example.ui.screens

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack

object SoundEffects {
    private fun playTone(frequencies: List<Double>, durationsMs: List<Int>, volume: Float = 0.5f) {
        Thread {
            try {
                val sampleRate = 44100
                val totalSamples = durationsMs.sumOf { (it * sampleRate) / 1000 }
                val buffer = ShortArray(totalSamples)
                
                var sampleIndex = 0
                for (i in frequencies.indices) {
                    val freq = frequencies[i]
                    val durationMs = durationsMs[i]
                    val samplesForTone = (durationMs * sampleRate) / 1000
                    
                    for (j in 0 until samplesForTone) {
                        val t = j.toDouble() / sampleRate
                        val raw = Math.sin(2.0 * Math.PI * freq * t)
                        // Apply linear envelope to prevent click/pop artifacts
                        val envelope = if (j < 150) {
                            j / 150.0
                        } else if (j > samplesForTone - 150) {
                            (samplesForTone - j) / 150.0
                        } else {
                            1.0
                        }
                        val shortVal = (raw * 32767.0 * volume * envelope).toInt().toShort()
                        if (sampleIndex < totalSamples) {
                            buffer[sampleIndex++] = shortVal
                        }
                    }
                }
                
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                
                val audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    Math.max(minBufferSize, buffer.size * 2),
                    AudioTrack.MODE_STATIC
                )
                
                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                
                // Wait for playback to finish
                val playDurationMs = durationsMs.sum()
                Thread.sleep(playDurationMs.toLong() + 30)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    fun playRapportUp(context: android.content.Context) {
        // Soft rising chime: C5 (523.25 Hz) then E5 (659.25 Hz)
        playTone(listOf(523.25, 659.25), listOf(80, 120), volume = 0.35f)
    }

    fun playRapportDown(context: android.content.Context) {
        // Soft thud: 150 Hz descending to 100 Hz
        playTone(listOf(150.0, 100.0), listOf(100, 120), volume = 0.45f)
    }

    fun playBadgeUnlock(context: android.content.Context) {
        // Celebratory arpeggio: C5 (523.25 Hz), E5 (659.25 Hz), G5 (783.99 Hz), C6 (1046.50 Hz)
        playTone(listOf(523.25, 659.25, 783.99, 1046.50), listOf(70, 70, 70, 180), volume = 0.35f)
    }

    fun playMessageSent(context: android.content.Context) {
        // Subtle clean pop: 850 Hz for 15ms
        playTone(listOf(850.0), listOf(15), volume = 0.25f)
    }
}
