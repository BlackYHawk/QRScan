package com.hawk.qrscan.decoding.qrcode

import android.graphics.Bitmap
import android.os.Handler
import android.os.Message
import android.util.Log
import com.google.zxing.Result
import com.hawk.qrscan.R
import com.hawk.qrscan.camera.CameraManager
import com.hawk.qrscan.interfaces.QRViewController
import com.hawk.qrscan.view.QRPointCallback


/**
 * Created by heyong on 2018/3/6.
 */
class QRHandler(private val controller: QRViewController) : Handler() {

    companion object {
        private val TAG = QRHandler::class.java.simpleName
    }

    private val decodeThread: DecodeThread = DecodeThread(controller, QRPointCallback(controller.getQRPreview()))
    private var state: State? = null

    private enum class State {
        PREVIEW,
        SUCCESS,
        DONE
    }

    init {
        decodeThread.start()
        state = State.SUCCESS
        // Start ourselves capturing previews and decoding.
        CameraManager.get()?.startPreview()
        restartPreviewAndDecode()
    }

    override fun handleMessage(message: Message) {
        when (message.what) {
            R.id.auto_focus ->
                //Log.d(TAG, "Got auto-focus message");
                // When one auto focus pass finishes, start another. This is the closest thing to
                // continuous AF. It does seem to hunt a bit, but I'm not sure what else to do.
                if (state == State.PREVIEW) {
                    CameraManager.get()?.requestAutoFocus(this, R.id.auto_focus)
                }
            R.id.restart_preview -> {
                Log.d(TAG, "Got restart preview message")
                restartPreviewAndDecode()
            }
            R.id.decode_succeeded -> {
                Log.d(TAG, "Got decode succeeded message")
                state = State.SUCCESS
                val bundle = message.getData()
                var barcode: Bitmap? = null

                if (bundle != null) {
                    barcode = bundle.getParcelable(DecodeThread.BARCODE_BITMAP)
                }

                controller.handleDecode(message.obj as Result, barcode)
            }
            R.id.decode_failed -> {
                // We're decoding as fast as possible, so when one decode fails, start another.
                state = State.PREVIEW

                if (decodeThread.getHandler() != null) {
                    CameraManager.get()?.requestPreviewFrame(decodeThread.getHandler()!!, R.id.decode)
                }
            }
        }
    }

    fun quitSynchronously() {
        state = State.DONE
        CameraManager.get()?.stopPreview()
        val quit = Message.obtain(decodeThread.getHandler(), R.id.quit)
        quit.sendToTarget()
        try {
            decodeThread.join()
        } catch (e: InterruptedException) {
            // continue
        }

        // Be absolutely sure we don't send any queued up messages
        removeMessages(R.id.decode_succeeded)
        removeMessages(R.id.decode_failed)
    }

    fun restartPreviewAndDecode() {
        if (state == State.SUCCESS) {
            state = State.PREVIEW

            if (decodeThread.getHandler() != null) {
                CameraManager.get()?.requestPreviewFrame(decodeThread.getHandler()!!, R.id.decode)
            }
            CameraManager.get()?.requestAutoFocus(this, R.id.auto_focus)
            controller.drawView()
        }
    }


}
