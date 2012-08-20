package com.ti.fmapp.utils;

import android.util.Log;

/**
 * User: Pedro Veloso
 * Date: 8/20/12
 * Time: 5:04 PM
 */
public class Utils {

    private final static boolean DEBUG_ACTIVE = true;
    private final static String LOG_TAG = "FmRxApp";

    /**
     * @param message Message to display
     * @param type    [Log.Error, Log.Warn, ...]
     */
    public static void debugFunc(String message, int type) {
        // errors must always be displayed
        if (type == Log.ERROR) {
            Log.e(LOG_TAG, message);
        } else if (DEBUG_ACTIVE) {
            switch (type) {
                case Log.DEBUG:
                    Log.d(LOG_TAG, message);
                    break;
                case Log.INFO:
                    Log.i(LOG_TAG, message);
                    break;
                case Log.VERBOSE:
                    Log.v(LOG_TAG, message);
                    break;
                case Log.WARN:
                    Log.w(LOG_TAG, message);
                    break;
                default:
                    Log.v(LOG_TAG, message);
                    break;
            }
        }
    }
}
