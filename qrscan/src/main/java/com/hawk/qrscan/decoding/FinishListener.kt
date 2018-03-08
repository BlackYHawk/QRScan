package com.hawk.qrscan.decoding

import android.app.Activity
import android.content.DialogInterface



/**
 * Created by heyong on 2018/3/6.
 */
/**
 * Simple listener used to exit the app in a few cases.
 *
 */
class FinishListener constructor(val activityToFinish: Activity) : DialogInterface.OnClickListener,
        DialogInterface.OnCancelListener, Runnable {

    override fun onCancel(dialogInterface: DialogInterface) {
        run();
    }

    override fun onClick(dialogInterface: DialogInterface, i: Int) {
        run();
    }

    override fun run() {
        activityToFinish.finish();
    }

}
