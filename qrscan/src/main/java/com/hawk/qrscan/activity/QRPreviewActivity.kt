package com.hawk.qrscan.activity

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.hawk.qrscan.R
import com.hawk.qrscan.decoding.qrcode.QRHandler
import com.hawk.qrscan.interfaces.QRViewController
import com.hawk.qrscan.util.Utils
import com.hawk.qrscan.view.QRPreview

/**
 * Created by heyong on 2018/3/8.
 */
class QRPreviewActivity : AbstractCameraActivity(), SurfaceHolder.Callback, QRViewController {
    private var handler: QRHandler? = null
    private var preview: SurfaceView? = null
    private var qrPreview: QRPreview? = null
    private var surfaceHolder: SurfaceHolder? = null
    private var hasSurface: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_qr)
        preview = findViewById(R.id.previewView)
        qrPreview = findViewById(R.id.qrPreview)
        surfaceHolder = preview?.holder

        handler = QRHandler(this)
        surfaceHolder?.addCallback(this)
        surfaceHolder?.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }

    override fun handleDecode(result: com.google.zxing.Result, barcode: Bitmap?) {
        Log.i("sean3", "^^^^^^^@" + result.text)
        inactivityTimer?.onActivity()
        playBeepSoundAndVibrate()

        val data = Intent()
        data.putExtra(Utils.Extra_Code, result.text)
        data.putExtra(Utils.Extra_Bitmap, barcode)
        setResult(Activity.RESULT_OK, data)
        finish()
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

    override fun openCamera(surfaceHolder: SurfaceHolder) {
        super.openCamera(surfaceHolder)
        handler?.restartPreviewAndDecode()
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        if (!hasSurface) {
            hasSurface = true;
            openCamera(holder!!);
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        hasSurface = false
    }

    override fun onResume() {
        super.onResume()
        if (hasSurface && surfaceHolder != null) {
            openCamera(surfaceHolder!!)
        }
    }

    override fun onPause() {
        if (handler != null) {
            handler!!.quitSynchronously();
            handler = null;
        }
        closeCamera()
        super.onPause()
    }
}