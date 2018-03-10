/*******************************************************************************
 * Created by Carlos Yaconi
 * Copyright 2015 Prey Inc. All rights reserved.
 * License: GPLv3
 * Full license at "/LICENSE"
 ******************************************************************************/
package au.com.smarttrace.beacon;

import android.util.Log;

public class Logger {

    public static void d(String message) {
        if (AppConfig.LOG_DEBUG_ENABLED)
            Log.d(AppConfig.TAG, message);
    }

    public static void i(String message) {
        Log.i(AppConfig.TAG, message);
    }

    public static void e(final String message, Throwable e) {
        if (e != null)
            Log.e(AppConfig.TAG, message, e);
        else
            Log.e(AppConfig.TAG, message);
    }

}