package com.hawk.qrscan.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.Camera
import android.os.Build
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.view.SurfaceHolder
import com.hawk.qrscan.util.Utils
import java.io.IOException




/**
 * Created by heyong on 2018/3/6.
 */
class CameraManager {

    companion object {
        private val TAG = CameraManager::class.java.simpleName
        private var cameraManager : CameraManager? = null

        private val MIN_FRAME_WIDTH = 240
        private val MIN_FRAME_HEIGHT = 240
        private val MAX_FRAME_WIDTH = 480
        private val MAX_FRAME_HEIGHT = 360
        private var widthPixels: Int = 0;
        var SDK_INT: Int = 0 // Later we can use Build.VERSION.SDK_INT

        fun get() = cameraManager;

        fun init(context: Context) {
            if (cameraManager == null) {
                cameraManager = CameraManager(context)
            }
            widthPixels = context.getResources().getDisplayMetrics().widthPixels;
        }
    }

    private var context: Context? = null
    private var configManager: CameraConfigurationManager? = null
    private var camera: Camera? = null
    private var framingRect: Rect? = null
    private var framingRectInPreview: Rect? = null
    private var initialized: Boolean = false
    private var previewing: Boolean = false
    private var useOneShotPreviewCallback: Boolean = false
    /** 是否已经开启闪光灯  */
    private var isOpenFlash = false
    private val isSupportFlashCamera2 = false
    /**
     * Preview frames are delivered here, which we pass on to the registered handler. Make sure to
     * clear the handler so it will only receive one message.
     */
    private var previewCallback: PreviewCallback? = null
    /** Autofocus callbacks arrive here, and are dispatched to the Handler which requested them.  */
    private var autoFocusCallback: AutoFocusCallback? = null

    init {
        var sdkInt: Int
        try {
            sdkInt = Integer.parseInt(Build.VERSION.SDK)
        } catch (nfe: NumberFormatException) {
            // Just to be safe
            sdkInt = 10000
        }

        SDK_INT = sdkInt
    }

    private constructor(context: Context) {
        this.context = context
        this.configManager = CameraConfigurationManager(context)

        // Camera.setOneShotPreviewCallback() has a race condition in Cupcake, so we use the older
        // Camera.setPreviewCallback() on 1.5 and earlier. For Donut and later, we need to use
        // the more efficient one shot callback, as the older one can swamp the system and cause it
        // to run out of memory. We can't use SDK_INT because it was introduced in the Donut SDK.
        //useOneShotPreviewCallback = Integer.parseInt(Build.VERSION.SDK) > Build.VERSION_CODES.CUPCAKE;
        useOneShotPreviewCallback = Integer.parseInt(Build.VERSION.SDK) > 3 // 3 = Cupcake
        previewCallback = PreviewCallback(configManager!!, useOneShotPreviewCallback)
        autoFocusCallback = AutoFocusCallback()
    }

    /**
     * Opens the camera driver and initializes the hardware parameters.
     *
     * @param holder The surface object which the camera will draw preview frames into.
     * @throws IOException Indicates the camera driver failed to open.
     */
    @Throws(IOException::class)
    fun openDriver(holder: SurfaceHolder) {
        if (camera == null) {
            camera = Camera.open()

            if (camera == null) {
                throw IOException()
            }
            camera!!.setPreviewDisplay(holder)

            if (!initialized) {
                initialized = true
                configManager!!.initFromCameraParameters(camera!!)
            }
            configManager!!.setDesiredCameraParameters(camera!!)

            //FIXME
            //     SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            //是否使用前灯
            //      if (prefs.getBoolean(PreferencesActivity.KEY_FRONT_LIGHT, false)) {
            //        FlashlightManager.enableFlashlight();
            //      }
            FlashlightManager.enableFlashlight()
        }
    }

    /**
     * Closes the camera driver if still in use.
     */
    fun closeDriver() {
        if (camera != null) {
            FlashlightManager.disableFlashlight()
            camera!!.release()
            camera = null
        }
    }

    /**
     * Asks the camera hardware to begin drawing preview frames to the screen.
     */
    fun startPreview() {
        if (camera != null && !previewing) {
            camera!!.startPreview()
            previewing = true
        }
    }

    /**
     * Tells the camera to stop drawing preview frames.
     */
    fun stopPreview() {
        if (camera != null && previewing) {
            if (!useOneShotPreviewCallback) {
                camera!!.setPreviewCallback(null)
            }
            camera!!.stopPreview()

            if (previewCallback != null) {
                previewCallback!!.setHandler(null, 0)
            }
            if (autoFocusCallback != null) {
                autoFocusCallback!!.setHandler(null, 0)
            }
            previewing = false
        }
    }

    /**
     * A single preview frame will be returned to the handler supplied. The data will arrive as byte[]
     * in the message.obj field, with width and height encoded as message.arg1 and message.arg2,
     * respectively.
     *
     * @param handler The handler to send the message to.
     * @param message The what field of the message to be sent.
     */
    fun requestPreviewFrame(handler: Handler, message: Int) {
        if (camera != null && previewing) {
            if (previewCallback != null) {
                previewCallback!!.setHandler(handler, message)
            }
            if (useOneShotPreviewCallback) {
                camera!!.setOneShotPreviewCallback(previewCallback)
            } else {
                camera!!.setPreviewCallback(previewCallback)
            }
        }
    }

    /**
     * Asks the camera hardware to perform an autofocus.
     *
     * @param handler The Handler to notify when the autofocus completes.
     * @param message The message to deliver.
     */
    fun requestAutoFocus(handler: Handler, message: Int) {
        if (camera != null && previewing) {
            if (autoFocusCallback != null) {
                autoFocusCallback!!.setHandler(handler, message)
            }
            //Log.d(TAG, "Requesting auto-focus callback");
            try {
                camera!!.autoFocus(autoFocusCallback)
            } catch (e: RuntimeException) {
                e.printStackTrace()
            }

        }
    }

    /**
     * Calculates the framing rect which the UI should draw to show the user where to place the
     * barcode. This target helps with alignment as well as forces the user to hold the device
     * far enough away to ensure the image will be in focus.
     *
     * @return The rectangle to draw on screen in window coordinates.
     */
    fun getQRFramingRect(): Rect? {
        val screenResolution = configManager?.getScreenResolution()

        if (framingRect == null && screenResolution != null) {
            if (camera == null) {
                return null
            }
            val width = widthPixels * 2 / 3
            val leftOffset = (screenResolution.x - width) / 2
            val topOffset = (screenResolution.y - width) / 2 - Utils.convertDip2Pixel(context!!, 50)
            framingRect = Rect(leftOffset, topOffset, leftOffset + width, topOffset + width)
            Log.d(TAG, "Calculated framing rect: " + framingRect)
        }
        return framingRect
    }

    /**
     * Calculates the framing rect which the UI should draw to show the user where to place the
     * Card. This target helps with alignment as well as forces the user to hold the device
     * far enough away to ensure the image will be in focus.
     *
     * @return The rectangle to draw on screen in window coordinates.
     */
    fun getCardFramingRect(): Rect? {
        val screenResolution = configManager?.getScreenResolution()

        if (framingRect == null && screenResolution != null) {
            if (camera == null) {
                return null
            }
            val width = widthPixels * 7 / 8
            val height = (width * 0.63).toInt()
            val leftOffset = (screenResolution.x - width) / 2
            val topOffset = (screenResolution.y - width) / 2 - Utils.convertDip2Pixel(context!!, 50)
            framingRect = Rect(leftOffset, topOffset, leftOffset + width, topOffset + height)
            Log.d(TAG, "Calculated framing rect: " + framingRect)
        }
        return framingRect
    }

    /**
     * Like [.getQRFramingRect] but coordinates are in terms of the preview frame,
     * not UI / screen.
     */
    fun getFramingRectInPreview(type: Int): Rect? {
        if (framingRectInPreview == null) {
            val rect: Rect?

            if (type == 0) {
                rect = Rect(getQRFramingRect())
            } else{
                rect = Rect(getCardFramingRect())
            }
            val cameraResolution = configManager?.getCameraResolution()
            val screenResolution = configManager?.getScreenResolution()

            if (cameraResolution != null && screenResolution != null) {
                rect.left = rect.left * cameraResolution.y / screenResolution.x
                rect.right = rect.right * cameraResolution.y / screenResolution.x
                rect.top = rect.top * cameraResolution.x / screenResolution.y
                rect.bottom = rect.bottom * cameraResolution.x / screenResolution.y
                framingRectInPreview = rect
            }
        }
        return framingRectInPreview
    }

    /**
     * A factory method to build the appropriate LuminanceSource object based on the format
     * of the preview buffers, as described by Camera.Parameters.
     *
     * @param data A preview frame.
     * @param width The width of the image.
     * @param height The height of the image.
     * @return A PlanarYUVLuminanceSource instance.
     */
    fun buildLuminanceSource(data: ByteArray, width: Int, height: Int): PlanarYUVLuminanceSource {
        val rect = getFramingRectInPreview(0)
        val previewFormat = configManager?.getPreviewFormat()
        val previewFormatString = configManager?.getPreviewFormatString()

        if (rect != null) {
            when (previewFormat) {
            // This is the standard Android format which all devices are REQUIRED to support.
            // In theory, it's the only one we should ever care ac_ui_about.
                PixelFormat.YCbCr_420_SP,
                    // This format has never been seen in the wild, but is compatible as we only care
                    // ac_ui_about the Y channel, so allow it.
                PixelFormat.YCbCr_422_SP -> return PlanarYUVLuminanceSource(data, width, height, rect.left,
                        rect.top, rect.width(), rect.height())
                else ->
                    // The Samsung Moment incorrectly uses this variant instead of the 'sp' version.
                    // Fortunately, it too has all the Y data up front, so we can read it.
                    if ("yuv420p" == previewFormatString) {
                        return PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top,
                                rect.width(), rect.height())
                    }
            }
        }
        throw IllegalArgumentException("Unsupported picture format: " +
                previewFormat + '/' + previewFormatString)
    }

    fun turnOnFlash(open: Boolean) {
        if (camera == null) {
            return
        }
        if (open) {
            turnOn()
        } else {
            turnOff()
        }
    }

    /**
     * 开启闪光灯
     */
    fun turnOn() {
        if (!isSupportFlash()) {
            return
        }
        if (ActivityCompat.checkSelfPermission(context!!, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        if (isOpenFlash) {
            return
        }

        turnLightOnCamera(camera!!)
    }

    /**
     * 关闭闪光灯
     */
    fun turnOff() {
        if (!isSupportFlash()) {
            return
        }
        if (ActivityCompat.checkSelfPermission(context!!, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        if (!isOpenFlash) {
            return
        }
        turnLightOffCamera(camera!!)
        isOpenFlash = false
    }

    /**
     * 通过设置Camera打开闪光灯
     *
     * @param mCamera
     */
    fun turnLightOnCamera(mCamera: Camera) {
        val parameters = mCamera.parameters
        val flashModes = parameters.supportedFlashModes
        val flashMode = parameters.flashMode
        if (Camera.Parameters.FLASH_MODE_TORCH != flashMode) {
            // 开启闪光灯
            if (flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                parameters.flashMode = Camera.Parameters.FLASH_MODE_TORCH
                mCamera.parameters = parameters
            }
        }
        isOpenFlash = true
    }

    /**
     * 通过设置Camera关闭闪光灯
     *
     * @param mCamera
     */
    fun turnLightOffCamera(mCamera: Camera) {
        val parameters = mCamera.parameters
        val flashModes = parameters.supportedFlashModes
        val flashMode = parameters.flashMode
        if (Camera.Parameters.FLASH_MODE_OFF != flashMode) {
            // 关闭闪光灯
            if (flashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                parameters.flashMode = Camera.Parameters.FLASH_MODE_OFF
                mCamera.parameters = parameters
            }
        }
        isOpenFlash = false
    }

    /**
     * 判断Android系统版本是否 >= LOLLIPOP(API21)
     *
     * @return boolean
     */
    private fun isLOLLIPOP(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
    }

    /**
     * 判断设备是否支持闪光灯
     *
     * @return boolean
     */
    fun isSupportFlash(): Boolean {
        val pm = context?.getPackageManager()
        val features = pm?.systemAvailableFeatures

        if (features != null) {
            for (f in features) {
                // 判断设备是否支持闪光灯
                if (PackageManager.FEATURE_CAMERA_FLASH == f.name) {
                    return true
                }
            }
        }
        // 判断是否支持闪光灯,方式二
        // Camera.Parameters parameters = camera.getParameters();
        // if (parameters == null) {
        // return false;
        // }
        // List<String> flashModes = parameters.getSupportedFlashModes();
        // if (flashModes == null) {
        // return false;
        // }
        return false
    }

    fun getContext(): Context? {
        return context
    }

}