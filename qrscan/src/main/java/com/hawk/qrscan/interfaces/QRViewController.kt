package com.hawk.qrscan.interfaces

import android.graphics.Bitmap
import com.google.zxing.Result
import com.hawk.qrscan.view.QRPreview

/**
 * Created by heyong on 2018/3/8.
 */
interface QRViewController : ViewController {

    fun getQRPreview() : QRPreview

    fun handleDecode(result: Result, barcode: Bitmap?)

}