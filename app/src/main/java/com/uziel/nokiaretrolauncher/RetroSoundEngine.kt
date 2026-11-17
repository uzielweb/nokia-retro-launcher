package com.uziel.nokiaretrolauncher

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

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

object RetroSoundEngine {
    private val executor = Executors.newSingleThreadExecutor()
    private val mediaLock = Any()

    @Volatile
    private var soundPool: SoundPool? = null
    @Volatile
    private var activeMediaPlayer: MediaPlayer? = null
    @Volatile
    private var activeMediaSound: RetroSound? = null

    private val rawToSampleId = ConcurrentHashMap<Int, Int>()
    private val loadedSampleIds = Collections.newSetFromMap(ConcurrentHashMap<Int, Boolean>())
    private val initialized = AtomicBoolean(false)

    private val shortRawResources = listOf(
        R.raw.nokia_key_standard,
        R.raw.nokia_key_delete,
        R.raw.nokia_key_return,
        R.raw.nokia_key_space,
        R.raw.nokia_key_invalid,
        R.raw.nokia_nav_tick,
        R.raw.nokia_lock,
        R.raw.nokia_unlock
    )

    fun init(context: Context) {
        if (!initialized.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        executor.execute {
            runCatching {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
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

                for (rawRes in shortRawResources) {
                    val sampleId = pool.load(appContext, rawRes, 1)
                    rawToSampleId[rawRes] = sampleId
                }
            }
        }
    }

    fun play(context: Context, sound: RetroSound) {
        init(context)
        if (sound == RetroSound.STARTUP || sound == RetroSound.NOKIA_TUNE) {
            playLongAudio(context.applicationContext, sound)
            return
        }

        val rawRes = rawResFor(sound)
        val pool = soundPool
        val sampleId = rawToSampleId[rawRes]
        if (pool != null && sampleId != null && loadedSampleIds.contains(sampleId)) {
            val streamId = pool.play(sampleId, 1f, 1f, 1, 0, 1f)
            if (streamId != 0) return
        }
        playShortFallback(context.applicationContext, rawRes)
    }

    private fun rawResFor(sound: RetroSound): Int = when (sound) {
        RetroSound.KEY_1, RetroSound.KEY_2, RetroSound.KEY_3,
        RetroSound.KEY_4, RetroSound.KEY_5, RetroSound.KEY_6,
        RetroSound.KEY_7, RetroSound.KEY_8, RetroSound.KEY_9,
        RetroSound.KEY_STAR, RetroSound.KEY_0, RetroSound.KEY_HASH -> R.raw.nokia_key_standard
        RetroSound.NAV -> R.raw.nokia_nav_tick
        RetroSound.SELECT -> R.raw.nokia_key_return
        RetroSound.BACK -> R.raw.nokia_key_delete
        RetroSound.CALL -> R.raw.nokia_key_space
        RetroSound.END -> R.raw.nokia_lock
        RetroSound.TORCH -> R.raw.nokia_unlock
        RetroSound.SNAKE_EAT -> R.raw.nokia_unlock
        RetroSound.SNAKE_DIE -> R.raw.nokia_key_invalid
        RetroSound.STARTUP -> R.raw.nokia_startup
        RetroSound.NOKIA_TUNE -> R.raw.nokia_tune
    }

    private fun playLongAudio(appContext: Context, sound: RetroSound) {
        executor.execute {
            synchronized(mediaLock) {
                val existing = activeMediaPlayer
                val wasSameTune = sound == RetroSound.NOKIA_TUNE &&
                    activeMediaSound == RetroSound.NOKIA_TUNE &&
                    existing != null && runCatching { existing.isPlaying }.getOrDefault(false)

                runCatching { existing?.stop() }
                runCatching { existing?.release() }
                activeMediaPlayer = null
                activeMediaSound = null

                if (wasSameTune) return@execute

                val rawRes = rawResFor(sound)
                val mp = createRawMediaPlayer(appContext, rawRes) ?: return@execute
                activeMediaPlayer = mp
                activeMediaSound = sound
                mp.setOnCompletionListener { finished ->
                    synchronized(mediaLock) {
                        if (activeMediaPlayer === finished) {
                            activeMediaPlayer = null
                            activeMediaSound = null
                        }
                    }
                    runCatching { finished.release() }
                }
                runCatching { mp.start() }.onFailure {
                    synchronized(mediaLock) {
                        if (activeMediaPlayer === mp) {
                            activeMediaPlayer = null
                            activeMediaSound = null
                        }
                    }
                    runCatching { mp.release() }
                }
            }
        }
    }

    private fun playShortFallback(appContext: Context, rawRes: Int) {
        executor.execute {
            val mp = createRawMediaPlayer(appContext, rawRes) ?: return@execute
            mp.setOnCompletionListener { finished -> runCatching { finished.release() } }
            runCatching { mp.start() }.onFailure { runCatching { mp.release() } }
        }
    }

    private fun createRawMediaPlayer(appContext: Context, rawRes: Int): MediaPlayer? {
        return runCatching {
            val afd = appContext.resources.openRawResourceFd(rawRes) ?: return null
            val mp = MediaPlayer()
            try {
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                mp.prepare()
                mp
            } catch (t: Throwable) {
                runCatching { afd.close() }
                runCatching { mp.release() }
                throw t
            }
        }.getOrNull()
    }
}
