package com.hawk.qrscan.decoding.qrcode

import android.content.Intent
import android.net.Uri
import com.google.zxing.BarcodeFormat
import java.util.*
import java.util.regex.Pattern


/**
 * Created by heyong on 2018/3/6.
 */
class DecodeFormatManager private constructor() {

    companion object {
        private val COMMA_PATTERN: Pattern = Pattern.compile(",");
        internal var PRODUCT_FORMATS: Vector<BarcodeFormat>? = null;
        internal var ONE_D_FORMATS: Vector<BarcodeFormat>? = null ;
        internal var QR_CODE_FORMATS: Vector<BarcodeFormat>? = null ;
        internal var DATA_MATRIX_FORMATS: Vector<BarcodeFormat>? = null;

        init {
            PRODUCT_FORMATS = Vector<BarcodeFormat>(5);
            PRODUCT_FORMATS?.add(BarcodeFormat.UPC_A);
            PRODUCT_FORMATS?.add(BarcodeFormat.UPC_E);
            PRODUCT_FORMATS?.add(BarcodeFormat.EAN_13);
            PRODUCT_FORMATS?.add(BarcodeFormat.EAN_8);
            PRODUCT_FORMATS?.add(BarcodeFormat.RSS_14);
            ONE_D_FORMATS = Vector<BarcodeFormat>(PRODUCT_FORMATS!!.size + 4)
            ONE_D_FORMATS?.addAll(PRODUCT_FORMATS!!);
            ONE_D_FORMATS?.add(BarcodeFormat.CODE_39);
            ONE_D_FORMATS?.add(BarcodeFormat.CODE_93);
            ONE_D_FORMATS?.add(BarcodeFormat.CODE_128);
            ONE_D_FORMATS?.add(BarcodeFormat.ITF);
            QR_CODE_FORMATS = Vector<BarcodeFormat>(1);
            QR_CODE_FORMATS?.add(BarcodeFormat.QR_CODE);
            DATA_MATRIX_FORMATS = Vector<BarcodeFormat>(1);
            DATA_MATRIX_FORMATS?.add(BarcodeFormat.DATA_MATRIX);
        }

        fun parseDecodeFormats(intent: Intent) : Vector<BarcodeFormat>? {
            var scanFormats: List<String>? = null;
            val scanFormatsString: String? = intent.getStringExtra(Intents.Scan.SCAN_FORMATS);

            if (scanFormatsString != null) {
                scanFormats = Arrays.asList(*COMMA_PATTERN.split(scanFormatsString));
            }
            return parseDecodeFormats(scanFormats, intent.getStringExtra(Intents.Scan.MODE));
        }

        fun parseDecodeFormats(inputUri: Uri) : Vector<BarcodeFormat>? {
            var formats: List<String>? = inputUri.getQueryParameters(Intents.Scan.SCAN_FORMATS);

            if (formats != null && formats.size == 1){
                formats = Arrays.asList(*COMMA_PATTERN.split(formats.get(0)));
            }
            return parseDecodeFormats(formats, inputUri.getQueryParameter(Intents.Scan.MODE));
        }

        private fun parseDecodeFormats(scanFormats: Iterable<String>?, decodeMode: String?): Vector<BarcodeFormat>? {
            if (scanFormats != null) {
                val formats: Vector<BarcodeFormat> = Vector<BarcodeFormat>();

                try {
                    for (format in scanFormats) {
                        formats.add(BarcodeFormat.valueOf(format));
                    }
                    return formats;
                } catch (iae: IllegalArgumentException) {
                    // ignore it then
                }
            }
            if (decodeMode != null) {
                if (Intents.Scan.PRODUCT_MODE.equals(decodeMode)) {
                    return PRODUCT_FORMATS!!;
                }
                if (Intents.Scan.QR_CODE_MODE.equals(decodeMode)) {
                    return QR_CODE_FORMATS!!;
                }
                if (Intents.Scan.DATA_MATRIX_MODE.equals(decodeMode)) {
                    return DATA_MATRIX_FORMATS!!;
                }
                if (Intents.Scan.ONE_D_MODE.equals(decodeMode)) {
                    return ONE_D_FORMATS!!;
                }
            }
            return null;
        }
    }

}