package com.hawk.qrscan.decoding

import android.app.Activity
import java.util.concurrent.*


/**
 * Created by heyong on 2018/3/6.
 */
/**
 * Finishes an activity after a period of inactivity.
 */
class InactivityTimer {

    companion object {
        private val INACTIVITY_DELAY_SECONDS = 5 * 60L;

    }

    private val inactivityTimer: ScheduledExecutorService =
            Executors.newSingleThreadScheduledExecutor(DaemonThreadFactory());
    private var activity: Activity? = null;
    private var inactivityFuture: ScheduledFuture<*>? = null;

    constructor(activity: Activity) {
        this.activity = activity;
        onActivity();
    }

    fun onActivity() {
        cancel();
        inactivityFuture = inactivityTimer.schedule(FinishListener(activity!!),
                INACTIVITY_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    private fun cancel() {
        if (inactivityFuture != null) {
            inactivityFuture?.cancel(true);
            inactivityFuture = null;
        }
    }

    fun shutdown() {
        cancel();
        inactivityTimer.shutdown();
    }

    private inner class DaemonThreadFactory: ThreadFactory {

        override fun newThread(runnable: Runnable): Thread {
            val thread: Thread = Thread(runnable);

            thread.setDaemon(true);
            return thread;
        }
    }

}
