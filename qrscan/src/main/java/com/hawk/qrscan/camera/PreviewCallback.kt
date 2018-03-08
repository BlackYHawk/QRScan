package com.hawk.qrscan.camera

import android.hardware.Camera
import android.os.Handler
import android.os.Message
import android.util.Log


@Suppress("UNREACHABLE_CODE")
/**
 * Created by heyong on 2018/3/6.
 */
class PreviewCallback constructor(val configManager: CameraConfigurationManager, val useOneShotPreviewCallback: Boolean)
    : Camera.PreviewCallback {

    companion object {
        private val TAG = PreviewCallback::class.java.simpleName
    }

    private var previewHandler : Handler? = null;
    private var previewMessage : Int = 0

    fun setHandler(previewHandler: Handler?, previewMessage: Int) {
        this.previewHandler = previewHandler;
        this.previewMessage = previewMessage;
    }

    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
        val cameraResolution = configManager.getCameraResolution();

        if (!useOneShotPreviewCallback && camera != null) {
            camera.setPreviewCallback(null);
        }
        if (previewHandler != null && cameraResolution != null) {
            val message : Message = previewHandler!!.obtainMessage(previewMessage, cameraResolution.x,
                    cameraResolution.y, data);
            message.sendToTarget();
            previewHandler = null;
        } else {
            Log.d(TAG, "Got preview callback, but no handler for it");
        }
    }
}