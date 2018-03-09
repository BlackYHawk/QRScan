package com.hawk.qrscan.decoding.card

import android.content.Context
import android.os.Environment
import android.os.Handler
import android.os.Looper
import com.hawk.qrscan.interfaces.CardViewController
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch



/**
 * This thread does all the heavy lifting of decoding the images.
 * 解码线程
 */
internal class DecodeThread(context: Context, private val controller: CardViewController) : Thread() {

    companion object {
        val CARD_BITMAP = "card_bitmap"
        val DIR = "/qrscan"
    }

    private val sdPath: String = StringBuilder(Environment.getExternalStorageDirectory().path).append(DIR).toString()
    private var handler: Handler? = null
    private val handlerInitLatch: CountDownLatch = CountDownLatch(1)

    init {
        try {
            copyAssetFile(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
        handler = DecodeHandler(controller, sdPath.toString())
        handlerInitLatch.countDown()
        Looper.loop()
    }

    @Throws(Exception::class)
    private fun copyAssetFile(context: Context): Boolean {
        val dirName = "$sdPath/tessdata"
        val filePath = "$sdPath/tessdata/eng.traineddata"
        val dir = File(dirName)

        if (!dir.exists()) {
            dir.mkdirs()
        }

        val dataFile = File(filePath)

        if (dataFile.exists()) {
            return true// 文件存在
        } else {
            val stream = context.getAssets().open("eng.traineddata")

            val outFile = File(filePath)

            if (outFile.exists()) {
                outFile.delete();
            }
            val out = FileOutputStream(outFile)
            val buf = ByteArray(1024)
            var len: Int = stream.read(buf)

            while (len > 0) {
                out.write(buf, 0, len)
                len = stream.read(buf)
            }
            stream.close()
            out.close()
        }

        return false
    }

}