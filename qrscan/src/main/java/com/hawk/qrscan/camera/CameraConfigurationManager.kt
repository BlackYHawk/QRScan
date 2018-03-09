package com.hawk.qrscan.camera

import android.content.Context
import android.graphics.Point
import android.hardware.Camera
import android.os.Build
import android.util.Log
import android.view.WindowManager
import java.lang.reflect.Method
import java.util.regex.Pattern




/**
 * Created by heyong on 2018/3/6.
 */
class CameraConfigurationManager constructor(val context : Context) {

    companion object {
        private val TAG = CameraConfigurationManager::class.java.simpleName

        private val COMMA_PATTERN : Pattern = Pattern.compile(",");
        private val TEN_DESIRED_ZOOM = 27;
        private val DESIRED_SHARPNESS = 30;

        fun getDesiredSharpness() = DESIRED_SHARPNESS
    }

    private var screenResolution : Point? = null;
    private var cameraResolution : Point? = null;
    private var previewFormat = 0;
    private var previewFormatString = "";


    /**
     * Reads, one time, values from the camera that are needed by the app.
     */
    fun initFromCameraParameters(camera : Camera) {
        val parameters : Camera.Parameters = camera.getParameters();
        previewFormat = parameters.getPreviewFormat();
        previewFormatString = parameters.get("preview-format");
        Log.d(TAG, "Default preview format: " + previewFormat + '/' + previewFormatString);
        val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager;
        val display = manager.getDefaultDisplay();
        screenResolution = Point(display.getWidth(), display.getHeight());
        Log.d(TAG, "Screen resolution: " + screenResolution);
        cameraResolution = getCameraResolution(parameters, screenResolution!!);
        Log.d(TAG, "Camera resolution: " + cameraResolution);
    }

     /**
     * Sets the camera up to take preview images which are used for both preview and decoding.
     * We detect the preview format here so that buildLuminanceSource() can build an appropriate
     * LuminanceSource subclass. In the future we may want to force YUV420SP as it's the smallest,
     * and the planar Y can be used for barcode scanning without a copy in some cases.
     */
    fun setDesiredCameraParameters(camera : Camera) {
        val parameters = camera.getParameters();
        Log.d(TAG, "Setting preview size: " + cameraResolution);
        parameters.setPreviewSize(cameraResolution!!.x, cameraResolution!!.y);
        setFlash(parameters);
        setZoom(parameters);
        //setSharpness(parameters);
        //modify here

       // camera.setDisplayOrientation(90);
        //兼容2.1
        setDisplayOrientation(camera, 90);
        camera.setParameters(parameters);
    }

    fun getCameraResolution() : Point? {
        return cameraResolution;
    }

    fun getScreenResolution() : Point? {
        return screenResolution;
    }

    fun getPreviewFormat() : Int {
        return previewFormat;
    }

    fun getPreviewFormatString() : String {
        return previewFormatString;
    }

    private fun getCameraResolution(parameters : Camera.Parameters, screenResolution: Point) : Point {
        var previewSizeValueString = parameters.get("preview-size-values");
        // saw this on Xperia
        if (previewSizeValueString == null) {
            previewSizeValueString = parameters.get("preview-size-value");
        }

        var cameraResolution : Point? = null

        if (previewSizeValueString != null) {
            Log.d(TAG, "preview-size-values parameter: " + previewSizeValueString);
            cameraResolution = findBestPreviewSizeValue(previewSizeValueString, screenResolution);
        }

        if (cameraResolution == null) {
            // Ensure that the camera resolution is a multiple of 8, as the screen may not be.
            cameraResolution = Point((screenResolution.x.shr(3)).shl(3),
                    (screenResolution.y.shr(3)).shl(3));
        }

        return cameraResolution;
    }

    private fun findBestPreviewSizeValue(previewSizeValueString: CharSequence, screenResolution: Point): Point? {
        var bestX = 0
        var bestY = 0
        var diff = Float.MAX_VALUE
        val targetRatio = screenResolution.x / screenResolution.y

        for (previewSize in COMMA_PATTERN.split(previewSizeValueString)) {
            val previewSizeTrim = previewSize.trim { it <= ' ' }
            val dimPosition = previewSizeTrim.indexOf('x')

            if (dimPosition < 0) {
                Log.w(TAG, "Bad preview-size: " + previewSizeTrim)
                continue
            }

            var newX: Int
            var newY: Int
            try {
                newX = Integer.parseInt(previewSizeTrim.substring(0, dimPosition))
                newY = Integer.parseInt(previewSizeTrim.substring(dimPosition + 1))
            } catch (nfe: NumberFormatException) {
                Log.w(TAG, "Bad preview-size: " + previewSizeTrim)
                continue
            }

            val ratio = if (newX > newY) newY.toFloat()/newX else newX.toFloat()/newY

            val newDiff = Math.abs(ratio - targetRatio)

            if (newDiff < diff) {
                bestX = newX
                bestY = newY
                diff = newDiff
            }
        }

        return if (bestX > 0 && bestY > 0) {
            Point(bestX, bestY)
        } else null
    }

    private fun findBestMotZoomValue(stringValues: CharSequence, tenDesiredZoom: Int): Int {
        var tenBestValue = 0
        for (stringValue in COMMA_PATTERN.split(stringValues)) {
            val stringValueTrim = stringValue.trim { it <= ' ' }
            val value: Double

            try {
                value = java.lang.Double.parseDouble(stringValueTrim)
            } catch (nfe: NumberFormatException) {
                return tenDesiredZoom
            }

            val tenValue = (10.0 * value).toInt()
            if (Math.abs(tenDesiredZoom - value) < Math.abs(tenDesiredZoom - tenBestValue)) {
                tenBestValue = tenValue
            }
        }
        return tenBestValue
    }

    private fun setFlash(parameters : Camera.Parameters) {
        // FIXME: This is a hack to turn the flash off on the Samsung Galaxy.
        // And this is a hack-hack to work around a different value on the Behold II
        // Restrict Behold II check to Cupcake, per Samsung's advice
        //if (Build.MODEL.contains("Behold II") &&
        //    CameraManager.SDK_INT == Build.VERSION_CODES.CUPCAKE) {
        if (Build.MODEL.contains("Behold II") && CameraManager.SDK_INT == 3) { // 3 = Cupcake
            parameters.set("flash-value", 1);
        } else {
            parameters.set("flash-value", 2);
        }
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
    }

    private fun setZoom(parameters : Camera.Parameters ) {
        val zoomSupportedString = parameters.get("zoom-supported");

        if (zoomSupportedString != null && !zoomSupportedString.toBoolean()) {
            return;
        }

        var tenDesiredZoom = TEN_DESIRED_ZOOM;
        val maxZoomString = parameters.get("max-zoom");

        if (maxZoomString != null) {
            try {
                val tenMaxZoom : Int = (10.0 * maxZoomString.toDouble()).toInt();

                if (tenDesiredZoom > tenMaxZoom) {
                    tenDesiredZoom = tenMaxZoom;
                }
            } catch (nfe : NumberFormatException) {
                Log.w(TAG, "Bad max-zoom: " + maxZoomString);
            }
        }

        val takingPictureZoomMaxString = parameters.get("taking-picture-zoom-max");

        if (takingPictureZoomMaxString != null) {
            try {
                val tenMaxZoom = Integer.parseInt(takingPictureZoomMaxString);

                if (tenDesiredZoom > tenMaxZoom) {
                    tenDesiredZoom = tenMaxZoom;
                }
            } catch (nfe : NumberFormatException) {
                Log.w(TAG, "Bad taking-picture-zoom-max: " + takingPictureZoomMaxString);
            }
        }

        val motZoomValuesString = parameters.get("mot-zoom-values");

        if (motZoomValuesString != null) {
            tenDesiredZoom = findBestMotZoomValue(motZoomValuesString, tenDesiredZoom);
        }

        val motZoomStepString = parameters.get("mot-zoom-step");

        if (motZoomStepString != null) {
            try {
                val motZoomStep = motZoomStepString.trim().toDouble();
                val tenZoomStep = (10.0 * motZoomStep).toInt();

                if (tenZoomStep > 1) {
                    tenDesiredZoom -= tenDesiredZoom % tenZoomStep;
                }
            } catch (nfe : NumberFormatException) {
                // continue
            }
        }

        // Set zoom. This helps encourage the user to pull back_dark.
        // Some devices like the Behold have a zoom parameter
        if (maxZoomString != null || motZoomValuesString != null) {
            parameters.set("zoom", (tenDesiredZoom / 10.0).toString());
        }

        // Most devices, like the Hero, appear to expose this zoom parameter.
        // It takes on values like "27" which appears to mean 2.7x zoom
        if (takingPictureZoomMaxString != null) {
            parameters.set("taking-picture-zoom", tenDesiredZoom);
        }
    }

    /**
     * compatible  1.6
     * @param camera
     * @param angle
     */
    protected fun setDisplayOrientation(camera : Camera, angle : Int){
        val downPolymorphic : Method;

        try {
            downPolymorphic = camera.javaClass.getMethod("setDisplayOrientation", Int::class.java)

            if (downPolymorphic != null)
                downPolymorphic.invoke(camera, angle);
        }
        catch (e : Exception) {}
    }
}