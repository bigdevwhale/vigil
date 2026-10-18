package app.vigil.data

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/** The reason, in your own voice: recorded once when the vow is sealed, played back when it's about to break. */
class Voice(private val context: Context) {
    private val dir = File(context.filesDir, "voice").apply { mkdirs() }
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null

    fun file(name: String) = File(dir, name)

    fun exists(name: String) = name.isNotBlank() && file(name).length() > 0

    /** Starts recording into a new file and returns its name, or null if the microphone is busy. */
    fun start(): String? {
        stopPlaying()
        val name = "voice_${System.currentTimeMillis()}.m4a"
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        return try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(64_000)
            r.setAudioSamplingRate(44_100)
            r.setOutputFile(file(name).absolutePath)
            r.prepare()
            r.start()
            recorder = r
            name
        } catch (_: Exception) {
            r.release()
            null
        }
    }

    /** Stops recording; false if nothing usable was caught (too short). */
    fun stop(): Boolean {
        val r = recorder ?: return false
        recorder = null
        return try {
            r.stop()
            true
        } catch (_: RuntimeException) {
            false
        } finally {
            r.release()
        }
    }

    fun play(name: String, onDone: () -> Unit = {}) {
        stopPlaying()
        if (!exists(name)) return onDone()
        val p = MediaPlayer()
        try {
            p.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            p.setDataSource(file(name).absolutePath)
            p.setOnCompletionListener { stopPlaying(); onDone() }
            p.prepare()
            p.start()
            player = p
        } catch (_: Exception) {
            p.release()
            onDone()
        }
    }

    /** Length of a recording in milliseconds, 0 if unknown. */
    fun duration(name: String): Int {
        if (!exists(name)) return 0
        val p = MediaPlayer()
        return try {
            p.setDataSource(file(name).absolutePath)
            p.prepare()
            p.duration
        } catch (_: Exception) {
            0
        } finally {
            p.release()
        }
    }

    fun stopPlaying() {
        player?.let {
            runCatching { it.stop() }
            it.release()
        }
        player = null
    }

    fun delete(name: String) {
        if (name.isNotBlank()) file(name).delete()
    }
}
