package au.com.smarttrace.beacon.service;

import android.location.Location;

public class ServiceUtils {
    private static final String KEY_REQUESTING_LOCATION_UPDATES = "requesting_location_updates";

    /**
     * Returns the {@code location} object as a human readable string.
     * @param location  The {@link Location}.
     */
    public static String getLocationText(Location location) {
        return location == null ? "SmartTraceIO is running" :
                "(" + location.getLatitude() + ", " + location.getLongitude() + ")";
    }

}
