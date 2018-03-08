package com.hawk.demo.qrscan

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.hawk.qrscan.activity.AbstractCameraActivity
import com.hawk.qrscan.decoding.qrcode.QRHandler
import com.hawk.qrscan.interfaces.QRViewController
import com.hawk.qrscan.view.QRPreview

/**
 * Created by heyong on 2018/3/8.
 */
class QRPreviewActivity : AbstractCameraActivity(), QRViewController {
    private var handler: QRHandler? = null
    private var qrPreview: QRPreview? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_qr_t)
        qrPreview = findViewById(R.id.qrPreview)

        handler = QRHandler(this)
    }

    override fun handleDecode(result: com.google.zxing.Result, barcode: Bitmap?) {
        Log.i("sean3", "^^^^^^^@")
        inactivityTimer?.onActivity()
        playBeepSoundAndVibrate()
    }

    override fun drawView() {
        qrPreview!!.drawViewfinder()
    }

    override fun getHandler(): Handler? {
        return handler
    }

    override fun getQRPreview(): QRPreview {
        return qrPreview!!
    }

    override fun onPause() {
        if (handler != null) {
            handler!!.quitSynchronously();
            handler = null;
        }
        super.onPause()
    }
}