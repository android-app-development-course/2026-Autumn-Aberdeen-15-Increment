package com.increment.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.view.SurfaceHolder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

class ClipPlayer(context: Context, private val lesson: Lesson, initial: Int) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(attributes).setOnAudioFocusChangeListener {
            if (it < 0) pause()
        }.build()
    private var player: MediaPlayer? = null
    private var stopAt = lesson.duration
    private var pendingPlay = false
    private var seeking = false
    private var queuedSeek: Int? = null
    var position by mutableStateOf(initial.coerceIn(0, lesson.duration))
        private set
    var ready by mutableStateOf(false)
        private set
    var playing by mutableStateOf(false)
        private set
    var error by mutableStateOf("")
        private set
    var playbackRate by mutableStateOf(1f)
        private set

    init {
        runCatching {
            val media = MediaPlayer()
            player = media
            media.setAudioAttributes(attributes)
            media.setDataSource(context, lesson.mediaUri(context))
            media.setOnPreparedListener {
                ready = true
                seek(position)
            }
            media.setOnSeekCompleteListener {
                seeking = false
                val queued = queuedSeek
                queuedSeek = null
                if (queued != null) {
                    seek(queued)
                } else if (pendingPlay) {
                    pendingPlay = false
                    start()
                }
            }
            media.setOnCompletionListener {
                position = lesson.duration
                pause()
            }
            media.setOnErrorListener { _, _, _ -> fail(); true }
            media.prepareAsync()
        }.onFailure { fail() }
    }

    private fun fail() {
        close()
        error = "无法播放媒体。若文件已移动或格式不受支持，请重新导入。"
    }

    fun display(holder: SurfaceHolder?) {
        runCatching { player?.setDisplay(holder) }
    }

    fun seek(ms: Int) {
        position = ms.coerceIn(0, lesson.duration)
        if (!ready) return
        if (seeking) {
            queuedSeek = position
            return
        }
        seeking = true
        runCatching {
            player?.seekTo((position + lesson.start).toLong(), MediaPlayer.SEEK_CLOSEST)
        }.onFailure { fail() }
    }

    fun play(from: Int = position, until: Int = lesson.duration) {
        if (!ready) return
        stopAt = until.coerceIn(1, lesson.duration)
        val desired = if (from >= stopAt) 0 else from
        if (desired != position) {
            pause()
            pendingPlay = true
            seek(desired)
        } else if (seeking) {
            pendingPlay = true
        } else {
            start()
        }
    }

    private fun start() {
        if (audioManager.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return
        runCatching {
            player?.let {
                it.playbackParams = it.playbackParams.setSpeed(playbackRate)
                it.start()
                playing = true
            }
        }.onFailure { fail() }
    }

    fun changeSpeed(speed: Float) {
        playbackRate = speed
        if (playing) runCatching {
            player?.let { it.playbackParams = it.playbackParams.setSpeed(speed) }
        }.onFailure { fail() }
    }

    fun tick() {
        if (!playing || seeking) return
        runCatching {
            val current = player?.currentPosition ?: return
            position = (current - lesson.start).coerceIn(0, lesson.duration)
            if (position >= stopAt) {
                pause()
                seek(stopAt)
            }
        }.onFailure { fail() }
    }

    fun pause() {
        pendingPlay = false
        if (playing) runCatching { player?.pause() }
        playing = false
        audioManager.abandonAudioFocusRequest(focus)
    }

    fun close() {
        pause()
        player?.release()
        player = null
        ready = false
    }
}

data class MediaInfo(val duration: Int, val video: Boolean)

fun inspectMedia(context: Context, uri: Uri): MediaInfo {
    val media = MediaPlayer()
    return try {
        media.setDataSource(context, uri)
        media.prepare()
        require(media.duration > 0) { "无法读取媒体时长" }
        require(media.trackInfo.any {
            it.trackType == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO
        }) { "请选择包含音轨的媒体" }
        MediaInfo(media.duration, media.videoWidth > 0)
    } finally {
        media.release()
    }
}

class LocalRecorder(private val context: Context, private val file: File) {
    private var recorder: MediaRecorder? = null
    private val temporary = File(file.parentFile, "${file.name}.tmp")

    fun start() {
        @Suppress("DEPRECATION")
        val media = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
        try {
            media.setAudioSource(MediaRecorder.AudioSource.MIC)
            media.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            media.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            media.setOutputFile(temporary.absolutePath)
            media.prepare()
            media.start()
            recorder = media
        } catch (error: Exception) {
            media.release()
            temporary.delete()
            throw error
        }
    }

    fun stop(): Boolean {
        val media = recorder ?: return false
        recorder = null
        return try {
            media.stop()
            temporary.renameTo(file)
        } catch (_: RuntimeException) {
            false
        } finally {
            media.release()
            temporary.delete()
        }
    }
}
