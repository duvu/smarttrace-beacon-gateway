package io.smarttrace.beacon;

import android.annotation.SuppressLint;
import android.content.Context;
import android.telephony.TelephonyManager;

/**
 * Created by beou on 3/7/18.
 */

public class AppConfig {

    // Milliseconds per second
    private static final int MILLISECONDS_PER_SECOND = 1000;

    /**
     * Temperature unit
     * 0、Degrees Celsius
     * 1、Fahrenheit
     * 2、Kelvin
     * 3、Lambertian degree
     * 4、Liege degree
     */
    public static int TemperatureUnit = 0;
    static final String TAG = "Smarttrace-Beacon";
    public static final String BACKEND_URL = "https://smarttrace.com.au/web/vf/rest";
    public static final String BACKEND_URL_BT04 = "https://smarttrace.com.au/bt04";
    static final boolean LOG_DEBUG_ENABLED = true;
    // max "age" in ms of last location (default 120000).
    public static final long LAST_LOCATION_MAX_AGE = 30 * MILLISECONDS_PER_SECOND;
    // the minimum time interval for GPS notifications, in milliseconds (default 60000).
    public static final long UPDATE_INTERVAL = 20 * MILLISECONDS_PER_SECOND;
    public static final long UPDATE_PERIOD = 8 * MILLISECONDS_PER_SECOND;
    // the minimum distance interval for GPS notifications, in meters (default 20)
    public static final float LOCATION_PROVIDERS_MIN_REFRESH_DISTANCE = 20;

    public static String GATEWAY_ID = "";

    private Context context;

    @SuppressLint("MissingPermission")
    public static void populateSetting(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        GATEWAY_ID = telephonyManager != null ? telephonyManager.getDeviceId() : null;
    }
}
