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
import java.io.File
import java.io.FileOutputStream


/**
 * Created by heyong on 2018/3/6.
 */

internal class DecodeHandler(private val controller: CardViewController, private val sdPath: String, front: Boolean) : Handler() {

    companion object {
        private val TAG = DecodeHandler::class.java.simpleName
    }

    private var baseApi: TessBaseAPI? = null

    init {
        try {
            baseApi = TessBaseAPI()

            if (front) {
                baseApi!!.init(sdPath, "eng")
                baseApi!!.setVariable("tessedit_char_whitelist", "0123456789Xx")
            }
            else {
                baseApi!!.init(sdPath, "chi_sim")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun handleMessage(message: Message) {
        when (message.what) {
            R.id.decodeFront ->
                //Log.d(TAG, "Got decodeFront message");
                decodeFront(message.obj as ByteArray, message.arg1, message.arg2)
            R.id.decodeBack ->
                decodeBack(message.obj as ByteArray, message.arg1, message.arg2)
            R.id.quit -> {
                baseApi!!.end()
                Looper.myLooper()!!.quit()
            }
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
    private fun decodeFront(data: ByteArray, width: Int, height: Int) {
        val start = System.currentTimeMillis()
        val bitmap: Bitmap? = takePicture(data, width, height)

        if (bitmap != null) {
            val x = (bitmap.width * 0.340).toInt()
            val y = (bitmap.height * 0.800).toInt()
            val w = (bitmap.width * 0.6 + 0.5f).toInt()
            val h = (bitmap.height * 0.12 + 0.5f).toInt()
            val bit_hm = Bitmap.createBitmap(bitmap, x, y, w, h)
            val id = doOcr(bit_hm)

            if (id != null && IdcardUtils.validateCard(id)) {
                val end = System.currentTimeMillis()
                Log.d(TAG, "Found card (" + (end - start) + " ms):\n" + id)
                val message = Message.obtain(controller.getHandler(), R.id.decode_succeeded, id)
                val bundle = Bundle()
                bundle.putParcelable(DecodeThread.CARD_BITMAP, bitmap)
                message.data = bundle
                message.sendToTarget()
            }
            else {
                val message = Message.obtain(controller.getHandler(), R.id.decode_failed)
                message.sendToTarget()
            }
            bit_hm.recycle()
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
    private fun decodeBack(data: ByteArray, width: Int, height: Int) {
        val start = System.currentTimeMillis()
        val bitmap: Bitmap? = takePicture(data, width, height)

        if (bitmap != null) {
            val x = (bitmap.width * 0.29).toInt()
            val y = (bitmap.height * 0.10).toInt()
            val w = (bitmap.width * 0.58 + 0.5f).toInt()
            val h = (bitmap.height * 0.37 + 0.5f).toInt()
            val bit_hm = Bitmap.createBitmap(bitmap, x, y, w, h)
            val id = doOcr(bit_hm)

            if (id != null && id.contains("中华人民")) {
                saveBitmap(bit_hm, sdPath, start.toString());
                val end = System.currentTimeMillis()
                Log.d(TAG, "Found card (" + (end - start) + " ms):\n" + id)
                val message = Message.obtain(controller.getHandler(), R.id.decode_succeeded, id)
                val bundle = Bundle()
                bundle.putParcelable(DecodeThread.CARD_BITMAP, bitmap)
                message.data = bundle
                message.sendToTarget()
            }
            else {
                val message = Message.obtain(controller.getHandler(), R.id.decode_failed)
                message.sendToTarget()
            }
            bit_hm.recycle()
        }
    }

    private fun takePicture(data: ByteArray, width: Int, height: Int) : Bitmap? {
        var bitmap: Bitmap? = null
        var bitmap1: Bitmap? = null
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
            return null;
        }
        else {
            val rect = CameraManager.get()?.getCardFramingRectInPreview() ?: return null

            val bitmapWidth = rect.right - rect.left
            val bitmapHeight = rect.bottom - rect.top
            bitmap1 = Bitmap.createBitmap(bitmap, rect.left, rect.top,
                    bitmapWidth, bitmapHeight);
        }
        bitmapOrigin.recycle()
        bitmap.recycle()

        return bitmap1
    }

    private fun doOcr(bitmap: Bitmap): String? {
        val bitmapCopy = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        baseApi?.setImage(bitmapCopy)

        val text = baseApi?.getUTF8Text()

        baseApi?.clear()
        return text
    }

    /**
     * 保存图片
     *
     * @param bm
     * @param dir
     * @param picName
     */
    fun saveBitmap(bm: Bitmap, dir: String, picName: String): Boolean {
        try {
            val file = File(dir, picName)

            if (file.exists()) {
                file.delete()
            }

            val out = FileOutputStream(file)
            bm.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
            out.close()
            Log.e("test", "保存图片文件成功")
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

}