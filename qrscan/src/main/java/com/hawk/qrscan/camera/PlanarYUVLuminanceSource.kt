package com.hawk.qrscan.camera

import android.graphics.Bitmap
import com.google.zxing.LuminanceSource
import kotlin.experimental.and


@Suppress("UNREACHABLE_CODE")
/**
 * Created by heyong on 2018/3/6.
 */
class PlanarYUVLuminanceSource : LuminanceSource {
    private val yuvData: ByteArray;
    private val dataWidth: Int;
    private val dataHeight: Int;
    private val left: Int;
    private val top: Int;

    constructor(yuvData: ByteArray, dataWidth: Int, dataHeight: Int, left: Int, top: Int,
                                 width: Int, height: Int) : super(width, height) {
        if (left + width > dataWidth || top + height > dataHeight) {
            throw IllegalArgumentException("Crop rectangle does not fit within image data.")
        }

        this.yuvData = yuvData
        this.dataWidth = dataWidth
        this.dataHeight = dataHeight
        this.left = left
        this.top = top
    }

    override fun getRow(y: Int, row: ByteArray?): ByteArray {
        if (y < 0 || y >= getHeight()) {
            throw IllegalArgumentException("Requested row is outside the image: " + y);
        }

        val width = getWidth();
        var size = 0;

        if (row == null || row.size < width) {
            size = width;
        }
        else {
            size = row.size;
        }
        val copyRow = ByteArray(size);

        val offset = (y + top) * dataWidth + left;
        System.arraycopy(yuvData, offset, copyRow, 0, width);

        return copyRow;
    }

    override fun getMatrix(): ByteArray {
        val width = getWidth();
        val height = getHeight();

        // If the caller asks for the entire underlying image, save the copy and give them the
        // original data. The docs specifically warn that result.length must be ignored.
        if (width == dataWidth && height == dataHeight) {
            return yuvData;
        }

        val area = width * height;
        val matrix = ByteArray(area);
        var inputOffset = top * dataWidth + left;

        // If the width matches the full width of the underlying data, perform a single copy.
        if (width == dataWidth) {
            System.arraycopy(yuvData, inputOffset, matrix, 0, area);
            return matrix;
        }

        // Otherwise copy one cropped row at a time.
        val yuv = yuvData;
        var y = 0;
        while (y < height) {
            val outputOffset = y * width;
            System.arraycopy(yuv, inputOffset, matrix, outputOffset, width);
            inputOffset += dataWidth;
            y++;
        }
        return matrix;
    }

    override fun isCropSupported(): Boolean {
        return true;
    }

    fun getDataWidth(): Int {
        return dataWidth;
    }

    fun getDataHeight(): Int {
        return dataHeight;
    }

    fun renderCroppedGreyscaleBitmap(): Bitmap {
        val width = getWidth();
        val height = getHeight();
        val pixels: IntArray = kotlin.IntArray(width * height);
        val yuv: ByteArray = yuvData;
        var inputOffset = top * dataWidth + left;
        var y = 0;

        while (y < height) {
            val outputOffset = y * width;
            var x = 0;

            while (x < width) {
                val grey = yuv[inputOffset + x] and 0xff.toByte();
                pixels[outputOffset + x] = (0xFF000000 or ((grey * 0x00010101).toLong())).toInt();
                x++
            }
            inputOffset += dataWidth;
            y++
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }
}