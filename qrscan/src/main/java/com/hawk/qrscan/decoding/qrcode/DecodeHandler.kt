package com.hawk.qrscan.decoding.qrcode

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.hawk.qrscan.R
import com.hawk.qrscan.camera.CameraManager
import com.hawk.qrscan.interfaces.QRViewController
import java.util.*


/**
 * Created by heyong on 2018/3/6.
 */

internal class DecodeHandler(private val controller: QRViewController, hints: Hashtable<DecodeHintType, Any>) : Handler() {

    companion object {
        private val TAG = DecodeHandler::class.java.simpleName
    }

    private val multiFormatReader: MultiFormatReader = MultiFormatReader()

    init {
        multiFormatReader.setHints(hints)
    }

    override fun handleMessage(message: Message) {
        when (message.what) {
            R.id.decodeFront ->
                //Log.d(TAG, "Got decodeFront message");
                decode(message.obj as ByteArray, message.arg1, message.arg2)
            R.id.quit -> Looper.myLooper()!!.quit()
        }
    }

    /**
     * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
     * reuse the same reader objects from one decodeFront to the next.
     *
     * @param data   The YUV preview frame.
     * @param width  The width of the preview frame.
     * @param height The height of the preview frame.
     */
    private fun decode(data: ByteArray, width: Int, height: Int) {
        var width = width
        var height = height
        val start = System.currentTimeMillis()
        var rawResult: Result? = null

        //modify here
        val rotatedData = ByteArray(data.size)
        for (y in 0 until height) {
            for (x in 0 until width)
                rotatedData[x * height + height - y - 1] = data[x + y * width]
        }
        val tmp = width // Here we are swapping, that's the difference to #11
        width = height
        height = tmp

        val source = CameraManager.get()?.buildLuminanceSource(rotatedData, width, height)
        val bitmap = BinaryBitmap(HybridBinarizer(source))

        try {
            rawResult = multiFormatReader.decodeWithState(bitmap)
        } catch (re: ReaderException) {
            // continue
        } finally {
            multiFormatReader.reset()
        }

        if (rawResult != null) {
            val end = System.currentTimeMillis()
            Log.d(TAG, "Found barcode (" + (end - start) + " ms):\n" + rawResult!!.toString())
            val message = Message.obtain(controller.getHandler(), R.id.decode_succeeded, rawResult)
            val bundle = Bundle()
            bundle.putParcelable(DecodeThread.BARCODE_BITMAP, source?.renderCroppedGreyscaleBitmap())
            message.setData(bundle)
            //Log.d(TAG, "Sending decodeFront succeeded message...");
            message.sendToTarget()
        } else {
            val message = Message.obtain(controller.getHandler(), R.id.decode_failed)
            message.sendToTarget()
        }
    }

}