package com.hawk.qrscan.view

import com.google.zxing.ResultPoint
import com.google.zxing.ResultPointCallback

@Suppress("UNREACHABLE_CODE")
/**
 * Created by heyong on 2018/3/6.
 */
class QRPointCallback constructor(val qrPreview: QRPreview): ResultPointCallback {

    override fun foundPossibleResultPoint(point: ResultPoint?) {
        if (point != null) {
            qrPreview.addPossibleResultPoint(point)
        }
    }
}