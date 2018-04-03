package au.com.smarttrace.beacon;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by beou on 3/7/18.
 *
 * Temperature unit
 * 0、Degrees Celsius
 * 1、Fahrenheit
 * 2、Kelvin
 * 3、Lambertian degree
 * 4、Liege degree
 */

public class AppConfig {

    // Milliseconds per second
    private static final int MILLISECONDS_PER_SECOND = 1000;

    // private static final String KEY_*
     private static final String KEY_WEB_SERVICE_URL            = "key_web_service_url";
     private static final String KEY_BACKEND_URL_BT04_NEW       = "key_backend_url_bt04_new";
     private static final String KEY_PLAY_SERVICE_AVAILABILITY  = "key_play_service_availability";

    public static final String TAG = "Smarttrace-Beacon";
    //ensure update startLocations for 1hours. default value is 12.
    public static final int COUNT_FOR_UPDATE_SHIPMENT_LOCATIONS = 24;

    public static final boolean DEBUG_ENABLED = true;
    public static int TemperatureUnit = 0;

    public static String WEB_SERVICE_URL = "https://smarttrace.com.au/web/vf/rest";
    public static String BACKEND_URL_BT04_NEW = "https://smarttrace.com.au/bt04";

    // max "age" in ms of last location (default 120000).
    public static final long LAST_LOCATION_MAX_AGE = 30 * MILLISECONDS_PER_SECOND;
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = (DEBUG_ENABLED ? 10 : 300) * MILLISECONDS_PER_SECOND;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    public static final long DEVICE_MAX_AGE = 7 * UPDATE_INTERVAL_IN_MILLISECONDS; //1 hours
    public static final long SHIPMENT_MAX_AGE = 12 * UPDATE_INTERVAL_IN_MILLISECONDS; //1 hours
    public static final long DEVICE_CACHED_AGE = 3 * UPDATE_INTERVAL_IN_MILLISECONDS;
    public static final long UPDATE_PERIOD = 10 * MILLISECONDS_PER_SECOND;

    // the minimum distance interval for GPS notifications, in meters (default 20)
    public static final float LOCATION_PROVIDERS_MIN_REFRESH_DISTANCE = 0;

    public static String TIMEZONE_STR = "GMT";

    public static String GATEWAY_ID = "";
    public static final int NOTIFICATION_ID = 190584999;

    private Context context;

    public static void populateSetting(Context context) {
        //GATEWAY_ID = "356024089973101";
//        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
//        GATEWAY_ID = telephonyManager != null ? telephonyManager.getDeviceId() : null;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static void setPlayServiceAvailability(Context context, boolean avail) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(KEY_PLAY_SERVICE_AVAILABILITY, avail).apply();
    }
    public static boolean getPlayServiceAvailability(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_PLAY_SERVICE_AVAILABILITY, false);
    }
}
