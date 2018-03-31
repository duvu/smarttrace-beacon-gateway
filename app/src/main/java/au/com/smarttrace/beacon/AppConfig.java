package au.com.smarttrace.beacon;

import android.content.Context;

/**
 * Created by beou on 3/7/18.
 */

public class AppConfig {

    // Milliseconds per second
    private static final int MILLISECONDS_PER_SECOND = 1000;
    public static int MAX_DATA_PACKAGES = 10;

    /**
     * Temperature unit
     * 0、Degrees Celsius
     * 1、Fahrenheit
     * 2、Kelvin
     * 3、Lambertian degree
     * 4、Liege degree
     */

    public static final boolean DEBUG_ENABLED = false;
    public static int TemperatureUnit = 0;
    static final String TAG = "Smarttrace-Beacon";
    public static final String BACKEND_URL = "https://smarttrace.com.au/web/vf/rest";
    public static final String BACKEND_URL_BT04_NEW = "https://smarttrace.com.au/bt04";
    public static final String BACKEND_URL_BT04 = "http://smarttrace.com.au:8080/data";
    // max "age" in ms of last location (default 120000).
    public static final long LAST_LOCATION_MAX_AGE = 30 * MILLISECONDS_PER_SECOND;
    // the minimum time interval for GPS notifications, in milliseconds (default 60000).
    public static final long UPDATE_INTERVAL = 600 * MILLISECONDS_PER_SECOND;
    public static final long UPDATE_INTERVAL_START = 30 * MILLISECONDS_PER_SECOND;
    public static final long UPDATE_PERIOD = 10 * MILLISECONDS_PER_SECOND;
    // the minimum distance interval for GPS notifications, in meters (default 20)
    public static final float LOCATION_PROVIDERS_MIN_REFRESH_DISTANCE = 20;

    public static String DEFAULT_TIMEZONE_STR = "GMT";

    public static String GATEWAY_ID = "";
    public static final int SMARTTRACE_NOTIFICATION_ID = 190584;

    private Context context;

    public static void populateSetting(Context context) {
        //GATEWAY_ID = "356024089973101";
//        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
//        GATEWAY_ID = telephonyManager != null ? telephonyManager.getDeviceId() : null;
    }
}
