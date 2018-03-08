package com.hawk.qrscan.camera

import android.os.IBinder
import android.util.Log
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method


/**
 * Created by heyong on 2018/3/6.
 */
class FlashlightManager private constructor() {

    companion object {
        private val TAG = FlashlightManager::class.java.simpleName
        private var iHardwareService: Any? = null
        private var setFlashEnabledMethod: Method? = null;

        init {
            iHardwareService = getHardwareService();
            setFlashEnabledMethod = getSetFlashEnabledMethod(iHardwareService);

            if (iHardwareService == null) {
                Log.v(TAG, "This device does supports control of a flashlight");
            } else {
                Log.v(TAG, "This device does not support control of a flashlight");
            }
        }

        fun enableFlashlight() {
            setFlashlight(false);
        }

        fun disableFlashlight() {
            setFlashlight(false);
        }

        private fun getHardwareService() : Any? {
            val serviceManagerClass : Class<*>?  = maybeForName("android.os.ServiceManager");
            if (serviceManagerClass == null) {
                return null;
            }

            val getServiceMethod : Method? = maybeGetMethod(serviceManagerClass, "getService", String::class.java);
            if (getServiceMethod == null) {
                return null;
            }

            val hardwareService : Any? = invoke(getServiceMethod, null, "hardware");
            if (hardwareService == null) {
                return null;
            }

            val iHardwareServiceStubClass : Class<*>? = maybeForName("android.os.IHardwareService@Stub");
            if (iHardwareServiceStubClass == null) {
                return null;
            }

            val asInterfaceMethod : Method? = maybeGetMethod(iHardwareServiceStubClass, "asInterface", IBinder::class.java);
            if (asInterfaceMethod == null) {
                return null;
            }

            return invoke(asInterfaceMethod, null, hardwareService);
        }

        private fun getSetFlashEnabledMethod(iHardwareService: Any?) : Method? {
            if (iHardwareService == null) {
                return null;
            }
            val proxyClass : Class<*> = iHardwareService::class.java;

            return maybeGetMethod(proxyClass, "setFlashlightEnabled", Boolean::class.java)
        }

        private fun maybeForName(name: String) : Class<*>? {
            try {
                return Class.forName(name);
            } catch (cnfe: ClassNotFoundException ) {
                // OK
                return null;
            } catch (re: RuntimeException) {
                Log.w(TAG, "Unexpected error while finding class " + name, re);
                return null;
            }
        }

        private fun maybeGetMethod(clazz: Class<*>, name: String, vararg argClasses: Class<*>) : Method? {
            try {
                return clazz.getMethod(name, *argClasses);
            } catch (nsme: NoSuchMethodException) {
                // OK
                return null;
            } catch (re: RuntimeException) {
                Log.w(TAG, "Unexpected error while finding method " + name, re);
                return null;
            }
        }

        private fun setFlashlight(active: Boolean) {
            if (setFlashEnabledMethod != null && iHardwareService != null) {
                invoke(setFlashEnabledMethod!!, iHardwareService!!, active);
            }
        }

        private fun invoke(method: Method, instance: Any?, vararg args: Any) : Any? {
            try {
                return method.invoke(instance, *args);
            } catch (e: IllegalAccessException) {
                Log.w(TAG, "Unexpected error while invoking " + method, e);
                return null;
            } catch (e: InvocationTargetException) {
                Log.w(TAG, "Unexpected error while invoking " + method, e.cause);
                return null;
            } catch (re: RuntimeException) {
                Log.w(TAG, "Unexpected error while invoking " + method, re);
                return null;
            }
        }
    }

}