package com.hawk.qrscan.activity

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Vibrator
import android.support.v7.app.AppCompatActivity
import android.view.SurfaceHolder
import com.hawk.qrscan.R
import com.hawk.qrscan.camera.CameraManager
import com.hawk.qrscan.decoding.InactivityTimer
import java.io.IOException

/**
 * Created by heyong on 2018/3/6.
 */
abstract class AbstractCameraActivity : AppCompatActivity() {
    companion object {
        private val VIBRATE_DURATION = 200L
        private val BEEP_VOLUME = 0.10f
    }

    protected var inactivityTimer: InactivityTimer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var playBeep: Boolean = false
    private var vibrate: Boolean = false
    private var flashlight = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CameraManager.init(application);
        inactivityTimer = InactivityTimer(this);
    }

    protected open fun openCamera(surfaceHolder: SurfaceHolder) {
        try {
            CameraManager.get()?.openDriver(surfaceHolder)
            CameraManager.get()?.startPreview()
        } catch (ioe: IOException) {
            return
        } catch (e: RuntimeException) {
            return
        }
    }

    protected fun closeCamera() {
        try {
            CameraManager.get()?.closeDriver()
        } catch (ioe: IOException) {
            return
        } catch (e: RuntimeException) {
            return
        }
    }

    private fun initBeepSound() {
        if (playBeep && mediaPlayer == null) {
            // The volume on STREAM_SYSTEM is not adjustable, and users found it
            // too loud,
            // so we now play on the music stream.
            volumeControlStream = AudioManager.STREAM_MUSIC
            mediaPlayer = MediaPlayer()
            mediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
            mediaPlayer!!.setOnCompletionListener(beepListener)

            val file = resources.openRawResourceFd(R.raw.beep)
            try {
                mediaPlayer!!.setDataSource(file.fileDescriptor,
                        file.startOffset, file.length)
                file.close()
                mediaPlayer!!.setVolume(BEEP_VOLUME, BEEP_VOLUME)
                mediaPlayer!!.prepare()
            } catch (e: IOException) {
                mediaPlayer = null
            }
        }
    }

    protected fun playBeepSoundAndVibrate() {
        if (playBeep && mediaPlayer != null) {
            mediaPlayer!!.start()
        }
        if (vibrate) {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(VIBRATE_DURATION)
        }
        if (flashlight) {
            flashlight = false
            CameraManager.get()?.turnOnFlash(false)
        }
    }

    override fun onResume() {
        super.onResume()
        playBeep = true
        val audioService = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioService.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            playBeep = false
        }
        initBeepSound()
        vibrate = true
    }

    override fun onDestroy() {
        inactivityTimer?.shutdown();
        super.onDestroy()
    }

    /**
     * When the beep has finished playing, rewind to queue up another one.
     */
    private val beepListener = MediaPlayer.OnCompletionListener { mediaPlayer -> mediaPlayer.seekTo(0) }

}