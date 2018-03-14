package com.hawk.qrscan.decoding.card

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Message
import android.util.Log
import com.hawk.qrscan.R
import com.hawk.qrscan.camera.CameraManager
import com.hawk.qrscan.interfaces.CardViewController

/**
 * Created by heyong on 2018/3/6.
 */
class CardHandler(context: Context, private val controller: CardViewController, private val front: Boolean) : Handler() {

    companion object {
        private val TAG = CardHandler::class.java.simpleName
    }

    private val decodeThread: DecodeThread = DecodeThread(context, controller, front)
    private var state: State? = null

    private enum class State {
        PREVIEW,
        SUCCESS,
        DONE
    }

    init {
        decodeThread.start()
        state = State.SUCCESS
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
                Log.d(TAG, "Got decodeFront succeeded message")
                state = State.SUCCESS
                val bundle = message.data
                var barcode: Bitmap? = null

                if (bundle != null) {
                    barcode = bundle.getParcelable(DecodeThread.CARD_BITMAP)
                }

                controller.handleDecode(message.obj as String, barcode)
            }
            R.id.decode_failed -> {
                // We're decoding as fast as possible, so when one decodeFront fails, start another.
                state = State.PREVIEW

                if (decodeThread.getHandler() != null) {
                    if (front) {
                        CameraManager.get()?.requestPreviewFrame(decodeThread.getHandler()!!, R.id.decodeFront)
                    } else {
                        CameraManager.get()?.requestPreviewFrame(decodeThread.getHandler()!!, R.id.decodeBack)
                    }
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
                if (front) {
                    CameraManager.get()?.requestPreviewFrame(decodeThread.getHandler()!!, R.id.decodeFront)
                } else {
                    CameraManager.get()?.requestPreviewFrame(decodeThread.getHandler()!!, R.id.decodeBack)
                }
            }
            CameraManager.get()?.requestAutoFocus(this, R.id.auto_focus)
            controller.drawView()
        }
    }

}