package com.hawk.qrscan.util

import android.content.Context



/**
 * Created by heyong on 2018/3/6.
 */
class Utils {

    companion object {

        val Extra_Code = "Extra_Code"
        val Extra_Bitmap = "Extra_Bitmap"

        /**
         * 计算相对应的长度
         * @param dip
         * @return float
         */
        fun convertDip2Pixel(context: Context, dip: Int): Int {
            val scale = context.getResources().getDisplayMetrics().density

            return (dip * scale + 0.5f).toInt()
        }

        /**
         * 计算相对应的长度
         * @param px
         * @return dp
         */
        fun convertPixel2Dip(context: Context, px: Int): Int {
            val scale = context.getResources().getDisplayMetrics().density

            return (px / scale + 0.5f).toInt()
        }
    }

}