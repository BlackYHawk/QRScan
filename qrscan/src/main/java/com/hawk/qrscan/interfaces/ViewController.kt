package com.hawk.qrscan.interfaces

import android.os.Handler

/**
 * Created by heyong on 2018/3/8.
 */
interface ViewController {

    fun drawView()

    fun getHandler(): Handler?

}