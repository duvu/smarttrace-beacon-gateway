package au.com.smarttrace.beacon.service;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.gson.Gson;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import au.com.smarttrace.beacon.AppConfig;
import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.R;
import au.com.smarttrace.beacon.net.WebService;
import au.com.smarttrace.beacon.net.model.Device;
import au.com.smarttrace.beacon.net.model.DeviceResponse;
import au.com.smarttrace.beacon.net.model.LocationBody;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ServiceUtils {
    private static final String KEY_REQUESTING_LOCATION_UPDATES = "requesting_location_updates";

    public static boolean isGooglePlayServicesAvailable(Context ctx){
        int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(ctx);
        if (ConnectionResult.SUCCESS == resultCode) {
            return true;
        } else {
            return false;
        }

//        boolean isGooglePlayServicesAvailable=false;
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
//                || (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
//                || ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
//
//            int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(ctx);
//            if (ConnectionResult.SUCCESS == resultCode) {
//                isGooglePlayServicesAvailable=true;
//            }
//        }
//        return isGooglePlayServicesAvailable;
    }

    public static boolean requestingLocationUpdates(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_REQUESTING_LOCATION_UPDATES, true);
    }

    public static void setRequestingLocationUpdates(Context context, boolean requestingLocationUpdates) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(KEY_REQUESTING_LOCATION_UPDATES, requestingLocationUpdates).apply();
    }

    /**
     * Returns the {@code location} object as a human readable string.
     * @param location  The {@link Location}.
     */
    public static String getLocationText(Location location) {
        return location == null ? "SmartTraceIO is running" :
                "(" + location.getLatitude() + ", " + location.getLongitude() + ")";
    }

    public static String getLocationTitle(Context context) {
        return context.getString(R.string.location_updated,
                DateFormat.getDateTimeInstance().format(new Date()));
    }

    private static Cache<String, Device> deviceCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(AppConfig.DEVICE_CACHED_AGE, TimeUnit.MILLISECONDS)
            .build();

    LoadingCache<String, LocationBody> locationLoadingCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .build(
                    new CacheLoader<String, LocationBody>() {
                        @Override
                        public LocationBody load(String key) throws Exception {
                            return null;
                        }
                    }
            );
}
