package com.hawk.qrscan.decoding.card

import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.googlecode.tesseract.android.TessBaseAPI
import com.hawk.qrscan.R
import com.hawk.qrscan.camera.CameraManager
import com.hawk.qrscan.interfaces.CardViewController
import com.hawk.qrscan.util.IdcardUtils
import java.io.ByteArrayOutputStream


/**
 * Created by heyong on 2018/3/6.
 */

internal class DecodeHandler(private val controller: CardViewController, private val sdPath: String) : Handler() {

    companion object {
        private val TAG = DecodeHandler::class.java.simpleName
    }

    private var baseApi: TessBaseAPI? = null

    init {
        try {
            baseApi = TessBaseAPI()
            baseApi!!.init(sdPath, "eng")
            baseApi!!.setVariable("tessedit_char_whitelist", "0123456789Xx")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun handleMessage(message: Message) {
        when (message.what) {
            R.id.decode ->
                //Log.d(TAG, "Got decode message");
                decode(message.obj as ByteArray, message.arg1, message.arg2)
            R.id.quit -> {
                baseApi!!.end()
                Looper.myLooper()!!.quit()
            }
        }
    }

    /**
     * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
     * reuse the same reader objects from one decode to the next.
     *
     * @param data   The YUV preview frame.
     * @param width  The width of the preview frame.
     * @param height The height of the preview frame.
     */
    private fun decode(data: ByteArray, width: Int, height: Int) {
        val start = System.currentTimeMillis()
        var bitmap: Bitmap? = null
        var rawImage: ByteArray? = null
        val yuvimage = YuvImage(data, ImageFormat.NV21, width, height, null)
        val baos = ByteArrayOutputStream()

        yuvimage.compressToJpeg(Rect(0, 0, width, height), 100, baos)// 80--JPG图片的质量[0-100],100最高
        rawImage = baos.toByteArray()
        //将rawImage转换成bitmap
        val options = BitmapFactory.Options()

        options.inPreferredConfig = Bitmap.Config.RGB_565
        val bitmapOrigin = BitmapFactory.decodeByteArray(rawImage, 0, rawImage.size, options)
        val matrix = Matrix()

        matrix.postRotate(90f)
        bitmap = Bitmap.createBitmap(bitmapOrigin, 0, 0, bitmapOrigin.width, bitmapOrigin.height, matrix, true)

        if (bitmap == null) {
            return;
        }
        else {
            val rect = CameraManager.get()?.getCardFramingRectInPreview() ?: return

            val bitmapWidth = rect.right - rect.left
            val bitmapHeight = rect.bottom - rect.top
            val bitmap1 = Bitmap.createBitmap(bitmap, rect.left, rect.top,
                    bitmapWidth, bitmapHeight)

            val x = (bitmapWidth * 0.340).toInt()
            val y = (bitmapHeight * 0.800).toInt()
            val w = (bitmapWidth * 0.6 + 0.5f).toInt()
            val h = (bitmapHeight * 0.12 + 0.5f).toInt()
            val bit_hm = Bitmap.createBitmap(bitmap1, x, y, w, h)
            val id = doOcr(bit_hm)

            if (id != null && IdcardUtils.validateCard(id)) {
                val end = System.currentTimeMillis()
                Log.d(TAG, "Found card (" + (end - start) + " ms):\n" + id)

                val message = Message.obtain(controller.getHandler(), R.id.decode_succeeded, id)
                val bundle = Bundle()
                bundle.putParcelable(DecodeThread.CARD_BITMAP, bitmap1)
                message.data = bundle
                message.sendToTarget()
            }
            else {
                val message = Message.obtain(controller.getHandler(), R.id.decode_failed)
                message.sendToTarget()
            }

            bitmapOrigin.recycle()
            bitmap.recycle()
            bit_hm.recycle()
        }
    }

    private fun doOcr(bitmap: Bitmap): String? {
        val bitmapCopy = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        baseApi?.setImage(bitmapCopy)

        val text = baseApi?.getUTF8Text()

        baseApi?.clear()
        return text
    }

}