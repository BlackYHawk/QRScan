package com.hawk.qrscan.activity

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Vibrator
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.SurfaceHolder
import android.view.Window
import android.view.WindowManager
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
        private val Tag = 20
    }
    private var senserManager: SensorManager? = null
    private var audioService: AudioManager? = null
    private var vibrator: Vibrator? = null
    protected var inactivityTimer: InactivityTimer? = null
    private var mediaPlayer: MediaPlayer? = null
    private var camera: Boolean = false
    private var vibrate: Boolean = false
    protected var flashlight: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //取消标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //取消状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        senserManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        audioService = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        CameraManager.init(application);

        inactivityTimer = InactivityTimer(this);
        initBeepSound()
        registerSensor()
    }

    private fun registerSensor() {
        val sensor = senserManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
        senserManager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun unregisterSensor() {
        senserManager?.unregisterListener(listener)
    }

    protected open fun openCamera(surfaceHolder: SurfaceHolder) {
        try {
            camera = true
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
            camera = false
            CameraManager.get()?.closeDriver()
        } catch (ioe: IOException) {
            return
        } catch (e: RuntimeException) {
            return
        }
    }

    private fun initBeepSound() {
        if (mediaPlayer == null) {
            volumeControlStream = AudioManager.STREAM_MUSIC
            mediaPlayer = MediaPlayer()
            mediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
            mediaPlayer!!.setOnCompletionListener(beepListener)

            val file = resources.openRawResourceFd(R.raw.beep)
            try {
                mediaPlayer!!.setDataSource(file.fileDescriptor, file.startOffset, file.length)
                mediaPlayer!!.setVolume(BEEP_VOLUME, BEEP_VOLUME)
                mediaPlayer!!.prepare()
            } catch (e: IOException) {
                mediaPlayer = null
            } finally {
                file.close()
            }
        }
    }

    protected fun playBeepSoundAndVibrate() {
        if (mediaPlayer != null && !(mediaPlayer!!.isPlaying)) {
            mediaPlayer!!.start()
        }
        if (vibrate) {
            vibrator?.vibrate(VIBRATE_DURATION)
        }
        turnOnFlash(false)
    }

    protected open fun turnOnFlash(on: Boolean) {
        if (on && !flashlight) {
            flashlight = true
            CameraManager.get()?.turnOnFlash(true)
        }
        else if(!on && flashlight) {
            flashlight = false
            CameraManager.get()?.turnOnFlash(false)
        }
    }

    override fun onDestroy() {
        inactivityTimer?.shutdown();
        unregisterSensor()
        super.onDestroy()
    }

    /**
     * When the beep has finished playing, rewind to queue up another one.
     */
    private val beepListener = MediaPlayer.OnCompletionListener { mediaPlayer -> mediaPlayer.seekTo(0) }

    private var listener: SensorEventListener = object : SensorEventListener {

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            //当传感器精度发生变化时
        }

        override fun onSensorChanged(event: SensorEvent) {
            //当传感器监测到的数值发生变化时
            val value = event.values[0]
            Log.e("test", ""+value)

            if (value < Tag && camera) {
                turnOnFlash(true)
                unregisterSensor()
            }
        }
    }
}