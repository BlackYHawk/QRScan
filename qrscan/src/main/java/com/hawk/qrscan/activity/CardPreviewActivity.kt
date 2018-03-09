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
import com.hawk.qrscan.decoding.card.CardHandler
import com.hawk.qrscan.interfaces.CardViewController
import com.hawk.qrscan.util.Utils.Companion.Extra_Bitmap
import com.hawk.qrscan.util.Utils.Companion.Extra_Code
import com.hawk.qrscan.view.CardPreview

/**
 * Created by heyong on 2018/3/8.
 */
class CardPreviewActivity : AbstractCameraActivity(), SurfaceHolder.Callback, CardViewController {
    private var handler: CardHandler? = null
    private var preview: SurfaceView? = null
    private var cardPreview: CardPreview? = null
    private var surfaceHolder: SurfaceHolder? = null
    private var hasSurface: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_card)
        preview = findViewById(R.id.previewView)
        cardPreview = findViewById(R.id.cardPreview)
        surfaceHolder = preview?.holder

        handler = CardHandler(this, this)
        surfaceHolder?.addCallback(this)
        surfaceHolder?.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }

    override fun handleDecode(result: String, barcode: Bitmap?) {
        Log.i("sean3", "^^^^^^^@" + result)
        inactivityTimer?.onActivity()
        playBeepSoundAndVibrate()

        val data = Intent()
        data.putExtra(Extra_Code, result)
        data.putExtra(Extra_Bitmap, barcode)
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    override fun drawView() {
        cardPreview!!.drawViewfinder()
    }

    override fun getHandler(): Handler? {
        return handler
    }

    override fun getCardPreview(): CardPreview {
        return cardPreview!!
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