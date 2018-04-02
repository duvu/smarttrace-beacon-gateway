package au.com.smarttrace.beacon.services;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import au.com.smarttrace.beacon.Logger;

/**
 * Created by beou on 3/7/18.
 */

public class GeneralLocationListener2 implements LocationListener {
    private static BeaconService loggingService;

    public GeneralLocationListener2(BeaconService activity) {
        loggingService = activity;
    }

    @Override
    public void onLocationChanged(Location location) {
        loggingService.onNewLocation(location);
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
