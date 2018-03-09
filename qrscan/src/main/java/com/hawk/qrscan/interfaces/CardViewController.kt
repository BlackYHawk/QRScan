package com.hawk.qrscan.interfaces

import android.graphics.Bitmap
import com.hawk.qrscan.view.CardPreview

/**
 * Created by heyong on 2018/3/8.
 */
interface CardViewController : ViewController {

    fun getCardPreview() : CardPreview

    fun handleDecode(result: String, barcode: Bitmap?)

}