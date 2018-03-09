package com.hawk.qrscan.decoding.qrcode

import android.os.Handler
import android.os.Looper
import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.ResultPointCallback
import com.hawk.qrscan.interfaces.QRViewController
import java.util.*
import java.util.concurrent.CountDownLatch


/**
 * Created by heyong on 2018/3/6.
 */
/**
 * This thread does all the heavy lifting of decoding the images.
 * 解码线程
 */
internal class DecodeThread(private val controller: QRViewController,
                            resultPointCallback: ResultPointCallback) : Thread() {

    companion object {
        val BARCODE_BITMAP = "barcode_bitmap"
    }

    private val hints: Hashtable<DecodeHintType, Any> = Hashtable<DecodeHintType, Any>(3)
    private var handler: Handler? = null
    private val handlerInitLatch: CountDownLatch = CountDownLatch(1)

    init {
        val decodeFormats = Vector<BarcodeFormat>()

        decodeFormats.addAll(DecodeFormatManager.ONE_D_FORMATS!!)
        decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS!!)
        decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS!!)
        hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats)
      //  hints.put(DecodeHintType.CHARACTER_SET, "utf-8")
        hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK, resultPointCallback)
    }

    fun getHandler(): Handler? {
        try {
            handlerInitLatch.await()
        } catch (ie: InterruptedException) {
            // continue?
        }

        return handler
    }

    override fun run() {
        Looper.prepare()
        handler = DecodeHandler(controller, hints)
        handlerInitLatch.countDown()
        Looper.loop()
    }

}