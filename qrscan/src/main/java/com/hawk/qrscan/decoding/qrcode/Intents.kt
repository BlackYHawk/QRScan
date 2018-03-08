package com.hawk.qrscan.decoding.qrcode

/**
 * Created by heyong on 2018/3/6.
 */
class Intents private constructor() {

    class Scan private constructor() {

        companion object {
            /**
             * Send this intent to open the Barcodes app in scanning mode, find a barcode, and return
             * the results.
             */
            val ACTION: String = "com.google.zxing.client.android.SCAN";

            /**
             * By default, sending Scan.ACTION will decode all barcodes that we understand. However it
             * may be useful to limit scanning to certain formats. Use Intent.putExtra(MODE, value) with
             * one of the values below ({@link #PRODUCT_MODE}, {@link #ONE_D_MODE}, {@link #QR_CODE_MODE}).
             * Optional.
             *
             * Setting this is effectively shorthnad for setting explicit formats with {@link #SCAN_FORMATS}.
             * It is overridden by that setting.
             */
            val MODE: String = "SCAN_MODE";

            /**
             * Comma-separated list of formats to scan for. The values must match the names of
             * {@link com.google.zxing.BarcodeFormat}s, such as {@link com.google.zxing.BarcodeFormat#EAN_13}.
             * Example: "EAN_13,EAN_8,QR_CODE"
             *
             * This overrides {@link #MODE}.
             */
            val SCAN_FORMATS: String = "SCAN_FORMATS";

            /**
             * @see com.google.zxing.DecodeHintType#CHARACTER_SET
             */
            val CHARACTER_SET: String = "CHARACTER_SET";

            /**
             * Decode only UPC and EAN barcodes. This is the right choice for shopping apps which get
             * prices, reviews, etc. for products.
             */
            val PRODUCT_MODE: String = "PRODUCT_MODE";

            /**
             * Decode only 1D barcodes (currently UPC, EAN, Code 39, and Code 128).
             */
            val ONE_D_MODE: String = "ONE_D_MODE";

            /**
             * Decode only QR codes.
             */
            val QR_CODE_MODE: String = "QR_CODE_MODE";

            /**
             * Decode only Data Matrix codes.
             */
            val DATA_MATRIX_MODE: String = "DATA_MATRIX_MODE";

            /**
             * If a barcode is found, Barcodes returns RESULT_OK to onActivityResult() of the app which
             * requested the scan via startSubActivity(). The barcodes contents can be retrieved with
             * intent.getStringExtra(RESULT). If the user presses Back, the result code will be
             * RESULT_CANCELED.
             */
            val RESULT: String = "SCAN_RESULT";

            /**
             * Call intent.getStringExtra(RESULT_FORMAT) to determine which barcode format was found.
             * See Contents.Format for possible values.
             */
            val RESULT_FORMAT: String = "SCAN_RESULT_FORMAT";

            /**
             * Setting this to false will not save scanned codes in the history.
             */
            val SAVE_HISTORY: String = "SAVE_HISTORY";
        }

    }

    class Encode private constructor() {

        companion object {
            /**
             * Send this intent to encode a piece of data as a QR code and display it full screen, so
             * that another person can scan the barcode from your screen.
             */
            val ACTION = "com.google.zxing.client.android.ENCODE";

            /**
             * The data to encode. Use Intent.putExtra(DATA, data) where data is either a String or a
             * Bundle, depending on the type and format specified. Non-QR Code formats should
             * just use a String here. For QR Code, see Contents for details.
             */
            val DATA = "ENCODE_DATA";

            /**
             * The type of data being supplied if the format is QR Code. Use
             * Intent.putExtra(TYPE, type) with one of Contents.Type.
             */
            val TYPE = "ENCODE_TYPE";

            /**
             * The barcode format to be displayed. If this isn't specified or is blank,
             * it defaults to QR Code. Use Intent.putExtra(FORMAT, format), where
             * format is one of Contents.Format.
             */
            val FORMAT = "ENCODE_FORMAT";
        }

    }

    class SearchBookContents private constructor() {

        companion object {
            /**
             * Use Google Book Search to search the contents of the book provided.
             */
            val ACTION = "com.google.zxing.client.android.SEARCH_BOOK_CONTENTS";

            /**
             * The book to search, identified by ISBN number.
             */
            val ISBN = "ISBN";

            /**
             * An optional field which is the text to search for.
             */
            val QUERY = "QUERY";
        }
    }

    class WifiConnect private constructor() {

        companion object {
            /**
             * Internal intent used to trigger connection to a wi-fi network.
             */
            val ACTION = "com.google.zxing.client.android.WIFI_CONNECT";

            /**
             * The network to connect to, all the configuration provided here.
             */
            val SSID = "SSID";

            /**
             * The network to connect to, all the configuration provided here.
             */
            val TYPE = "TYPE";

            /**
             * The network to connect to, all the configuration provided here.
             */
            val PASSWORD: String = "PASSWORD";
        }

    }

    class Share private constructor() {

        companion object {
            /**
             * Give the user a choice of items to encode as a barcode, then render it as a QR Code and
             * display onscreen for a friend to scan with their phone.
             */
            val ACTION: String = "com.google.zxing.client.android.SHARE";
        }

    }
}