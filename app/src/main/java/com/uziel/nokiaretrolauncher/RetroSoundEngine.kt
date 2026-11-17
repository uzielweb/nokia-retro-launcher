package com.uziel.nokiaretrolauncher

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.sin

enum class RetroSound {
    KEY_1, KEY_2, KEY_3,
    KEY_4, KEY_5, KEY_6,
    KEY_7, KEY_8, KEY_9,
    KEY_STAR, KEY_0, KEY_HASH,
    NAV,
    SELECT,
    BACK,
    CALL,
    END,
    TORCH,
    STARTUP,
    NOKIA_TUNE,
    SNAKE_EAT,
    SNAKE_DIE;

    companion object {
        fun forDigit(digit: String): RetroSound = when (digit) {
            "1" -> KEY_1
            "2" -> KEY_2
            "3" -> KEY_3
            "4" -> KEY_4
            "5" -> KEY_5
            "6" -> KEY_6
            "7" -> KEY_7
            "8" -> KEY_8
            "9" -> KEY_9
            "*" -> KEY_STAR
            "0" -> KEY_0
            "#" -> KEY_HASH
            else -> SELECT
        }
    }
}

private data class ToneStep(val freqHz: Double, val durationMs: Int, val gapMs: Int = 0)

object RetroSoundEngine {
    private const val SAMPLE_RATE = 44100
    private val executor = Executors.newSingleThreadExecutor()
    private val playExecutor = Executors.newCachedThreadPool()

    @Volatile
    private var soundPool: SoundPool? = null
    private val soundIds = ConcurrentHashMap<RetroSound, Int>()
    private val loadedSampleIds = ConcurrentHashMap.newKeySet<Int>()
    private val initialized = AtomicBoolean(false)

    fun init(context: Context) {
        if (!initialized.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        executor.execute {
            runCatching {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                val pool = SoundPool.Builder()
                    .setMaxStreams(6)
                    .setAudioAttributes(attrs)
                    .build()
                pool.setOnLoadCompleteListener { _, sampleId, status ->
                    if (status == 0) {
                        loadedSampleIds.add(sampleId)
                    }
                }
                soundPool = pool

                val dir = File(appContext.cacheDir, "nokia_sfx_v2").apply { mkdirs() }
                for (sound in RetroSound.entries) {
                    val wavFile = File(dir, "${sound.name.lowercase()}.wav")
                    if (!wavFile.exists() || wavFile.length() <= 44L) {
                        val pcm = synthesizePcm(stepsFor(sound))
                        writeWavFile(wavFile, pcm)
                    }
                    val id = pool.load(wavFile.absolutePath, 1)
                    soundIds[sound] = id
                }
            }
        }
    }

    fun play(context: Context, sound: RetroSound) {
        init(context)
        val pool = soundPool
        val sampleId = soundIds[sound]
        if (pool != null && sampleId != null && loadedSampleIds.contains(sampleId)) {
            val streamId = pool.play(sampleId, 0.9f, 0.9f, 1, 0, 1f)
            if (streamId != 0) return
        }
        playFallbackTrack(sound)
    }

    private fun playFallbackTrack(sound: RetroSound) {
        playExecutor.execute {
            runCatching {
                val pcm = synthesizePcm(stepsFor(sound))
                if (pcm.isEmpty()) return@runCatching
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                val format = AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
                val track = AudioTrack.Builder()
                    .setAudioAttributes(attrs)
                    .setAudioFormat(format)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(pcm.size)
                    .build()
                try {
                    track.write(pcm, 0, pcm.size)
                    track.play()
                    val durationMs = (pcm.size * 1000L) / (SAMPLE_RATE * 2L)
                    Thread.sleep(durationMs + 45L)
                } finally {
                    runCatching { track.stop() }
                    track.release()
                }
            }
        }
    }

    private fun stepsFor(sound: RetroSound): List<ToneStep> = when (sound) {
        RetroSound.KEY_1 -> listOf(ToneStep(1174.7, 55))
        RetroSound.KEY_2 -> listOf(ToneStep(1318.5, 55))
        RetroSound.KEY_3 -> listOf(ToneStep(1480.0, 55))
        RetroSound.KEY_4 -> listOf(ToneStep(1568.0, 55))
        RetroSound.KEY_5 -> listOf(ToneStep(1760.0, 55))
        RetroSound.KEY_6 -> listOf(ToneStep(1975.5, 55))
        RetroSound.KEY_7 -> listOf(ToneStep(2093.0, 55))
        RetroSound.KEY_8 -> listOf(ToneStep(2349.3, 55))
        RetroSound.KEY_9 -> listOf(ToneStep(2637.0, 55))
        RetroSound.KEY_STAR -> listOf(ToneStep(987.8, 55))
        RetroSound.KEY_0 -> listOf(ToneStep(1046.5, 55))
        RetroSound.KEY_HASH -> listOf(ToneStep(2793.8, 55))
        RetroSound.NAV -> listOf(ToneStep(1568.0, 28))
        RetroSound.SELECT -> listOf(ToneStep(1318.5, 38, 10), ToneStep(1760.0, 55))
        RetroSound.BACK -> listOf(ToneStep(1760.0, 38, 10), ToneStep(1174.7, 55))
        RetroSound.CALL -> listOf(ToneStep(1318.5, 45, 8), ToneStep(1661.2, 45, 8), ToneStep(1975.5, 80))
        RetroSound.END -> listOf(ToneStep(987.8, 60, 10), ToneStep(784.0, 90))
        RetroSound.TORCH -> listOf(ToneStep(1174.7, 40, 8), ToneStep(2349.3, 65))
        RetroSound.STARTUP -> listOf(
            ToneStep(1318.5, 110, 12),
            ToneStep(1174.7, 110, 12),
            ToneStep(740.0, 200, 12),
            ToneStep(830.6, 240)
        )
        RetroSound.NOKIA_TUNE -> listOf(
            ToneStep(1318.5, 140, 18),
            ToneStep(1174.7, 140, 18),
            ToneStep(740.0, 280, 20),
            ToneStep(830.6, 280, 20),
            ToneStep(1108.7, 140, 18),
            ToneStep(987.8, 140, 18),
            ToneStep(587.3, 280, 20),
            ToneStep(659.3, 280, 20),
            ToneStep(987.8, 140, 18),
            ToneStep(880.0, 140, 18),
            ToneStep(554.4, 280, 20),
            ToneStep(659.3, 280, 20),
            ToneStep(880.0, 620)
        )
        RetroSound.SNAKE_EAT -> listOf(ToneStep(1568.0, 36, 8), ToneStep(2093.0, 52))
        RetroSound.SNAKE_DIE -> listOf(ToneStep(659.3, 95, 14), ToneStep(493.9, 110, 14), ToneStep(329.6, 210))
    }

    private fun synthesizePcm(steps: List<ToneStep>): ByteArray {
        val out = ByteArrayOutputStream()
        val attackSamples = (SAMPLE_RATE * 0.0025).toInt().coerceAtLeast(1)
        val releaseSamples = (SAMPLE_RATE * 0.008).toInt().coerceAtLeast(1)

        for (step in steps) {
            val totalSamples = (SAMPLE_RATE * step.durationMs) / 1000
            val w = 2.0 * PI * step.freqHz / SAMPLE_RATE
            val buf = ByteBuffer.allocate(totalSamples * 2).order(ByteOrder.LITTLE_ENDIAN)

            for (i in 0 until totalSamples) {
                val env = when {
                    i < attackSamples -> i.toDouble() / attackSamples
                    i > totalSamples - releaseSamples -> ((totalSamples - i).coerceAtLeast(0)).toDouble() / releaseSamples
                    else -> 1.0
                }
                val wave = 0.68 * sin(w * i) +
                    0.20 * sin(3.0 * w * i) +
                    0.08 * sin(5.0 * w * i) +
                    0.04 * sin(7.0 * w * i)
                val sample = (wave * env * 0.85 * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                buf.putShort(sample.toShort())
            }
            out.write(buf.array())

            if (step.gapMs > 0) {
                val gapBytes = ((SAMPLE_RATE * step.gapMs) / 1000) * 2
                out.write(ByteArray(gapBytes))
            }
        }
        return out.toByteArray()
    }

    private fun writeWavFile(file: File, pcmData: ByteArray) {
        val byteRate = SAMPLE_RATE * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(36 + pcmData.size)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1)
            putShort(1)
            putInt(SAMPLE_RATE)
            putInt(byteRate)
            putShort(2)
            putShort(16)
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(pcmData.size)
        }.array()

        FileOutputStream(file).use { fos ->
            fos.write(header)
            fos.write(pcmData)
        }
    }
}
