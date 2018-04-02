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

import java.text.DateFormat;
import java.util.Date;

import au.com.smarttrace.beacon.R;

public class ServiceUtils {
    static final String KEY_REQUESTING_LOCATION_UPDATES = "requesting_location_updates";

    public static boolean isGooglePlayServicesAvailable(Context ctx){
        boolean isGooglePlayServicesAvailable=false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(ctx);
            if (ConnectionResult.SUCCESS == resultCode) {
                isGooglePlayServicesAvailable=true;
            }
        }
        return isGooglePlayServicesAvailable;
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
        return location == null ? "Unknown location" :
                "(" + location.getLatitude() + ", " + location.getLongitude() + ")";
    }

    public static String getLocationTitle(Context context) {
        return context.getString(R.string.location_updated,
                DateFormat.getDateTimeInstance().format(new Date()));
    }
}
