package au.com.smarttrace.beacon.services;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import au.com.smarttrace.beacon.Logger;

/**
 * Created by beou on 3/7/18.
 */

public class GeneralLocationListener2 implements LocationListener {
    private static String name;
    private static BeaconService loggingService;

    public GeneralLocationListener2(BeaconService activity, String name1) {
        loggingService = activity;
        name = name1;
    }

    @Override
    public void onLocationChanged(Location location) {
        loggingService.onLocationChanged(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        Logger.i("[onProviderEnabled] "+ provider);
    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
