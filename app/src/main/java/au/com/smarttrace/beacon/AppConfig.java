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
    private static final int MILLISECONDS = 1000;

    // private static final String KEY_*
     private static final String KEY_WEB_SERVICE_URL            = "key_web_service_url";
     private static final String KEY_BACKEND_URL_BT04_NEW       = "key_backend_url_bt04_new";
     private static final String KEY_PLAY_SERVICE_AVAILABILITY  = "key_play_service_availability";

    public static final String TAG = "Smarttrace-Beacon";
    //ensure update startLocations for 1hours. default value is 12.
    public static final int COUNT_FOR_UPDATE_SHIPMENT_LOCATIONS = 1;

    public static final boolean DEBUG_ENABLED                           = false;
    public static int TemperatureUnit = 0;

    public static final String SMARTTRACE_URL                           = "https://smarttrace.com.au";
    public static final String WEB_SERVICE_URL                          = SMARTTRACE_URL +"/web/vf/rest";
    public static final String BACKEND_URL_BT04_NEW                     = SMARTTRACE_URL + "/bt04";

    // max "age" in ms of last location (default 120000).
    public static final long UPDATE_INTERVAL                            = (DEBUG_ENABLED ? 60 : 600) * MILLISECONDS;
    public static final long SCHEDULED_RUNNING_FIRST                    = (DEBUG_ENABLED ? 300 : 600) * MILLISECONDS;
    public static final long LAST_LOCATION_MAX_AGE                      = 60 * MILLISECONDS;
    public static final long FASTEST_UPDATE_INTERVAL                    = UPDATE_INTERVAL / 5;
    public static final long DEVICE_MAX_AGE                             = 144 * UPDATE_INTERVAL + 10 * MILLISECONDS; //1 day
    public static final long SHIPMENT_MAX_AGE                           = 12 * UPDATE_INTERVAL + 10 * MILLISECONDS; //2 hours
    public static final long DEVICE_CACHED_AGE                          = 3 * UPDATE_INTERVAL;
    public static final long UPDATE_PERIOD                              = 10 * MILLISECONDS;

    // the minimum distance interval for GPS notifications, in meters (default 20)
    public static final float LOCATION_PROVIDERS_MIN_REFRESH_DISTANCE = DEBUG_ENABLED ? 0 : 0;

    public static String TIMEZONE_STR = "GMT";

    public static String GATEWAY_ID = "";
    public static final int NOTIFICATION_ID = 190584999;

    private Context context;

    public static void populateSetting(Context context) {
    }
}
