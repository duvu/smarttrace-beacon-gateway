package au.com.smarttrace.beacon.service;

import android.content.Context;
import android.location.Location;
import android.os.Looper;
import android.os.NetworkOnMainThreadException;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.Timer;
import java.util.TimerTask;

import au.com.smarttrace.beacon.AppConfig;
import au.com.smarttrace.beacon.FireLogger;
import au.com.smarttrace.beacon.Logger;

public class LServiceWrapper {
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private SettingsClient mSettingsClient;
    private FusedLocationProviderClient mFusedLocationClient;
    private LCallback mLCallback;
    private LocationCallback mLocationCallback;
    private Location mCurrentLocation;
    private Boolean mRequestingLocationUpdates = false;

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 20000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 10;

    private static LServiceWrapper instance = null;
    public static LServiceWrapper instances(Context mContext, LCallback mLCallback) {
        if (instance == null) {
            instance = new LServiceWrapper(mContext, mLCallback);
        } else {
            instance.setContext(mContext);
            instance.setLCallback(mLCallback);
        }
        instance.startLocationUpdates();
        return instance;
    }

    public LServiceWrapper(Context context, LCallback callback) {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        mSettingsClient = LocationServices.getSettingsClient(context);

        // Kick off the process of building the LocationCallback, LocationRequest, and
        // LocationSettingsRequest objects.
        mLCallback = callback;

        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();
    }

    public LCallback getLCallback() {
        return mLCallback;
    }

    public void setLCallback(LCallback callback) {
        this.mLCallback = callback;
    }

    public void setContext(Context mContext) {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext);
        mSettingsClient = LocationServices.getSettingsClient(mContext);

    }

    /**
     * Creates a callback for receiving location events.
     */
    private void createLocationCallback() {
        if (mLocationCallback == null) {
            mLocationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    if (mLCallback != null) {
                        mLCallback.onLocationChanged(locationResult.getLastLocation());
                    }
                    onUpdateLocation(locationResult.getLastLocation());
                }
            };
        }
    }



    /**
     * Uses a {@link com.google.android.gms.location.LocationSettingsRequest.Builder} to build
     * a {@link com.google.android.gms.location.LocationSettingsRequest} that is used for checking
     * if a device has the needed location settings.
     */
    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);
        builder.setNeedBle(true);
        mLocationSettingsRequest = builder.build();
    }


    private void onUpdateLocation(Location location) {
        Logger.d("[>_] onUpdateLocation");
        if (mCurrentLocation == null) {
            mCurrentLocation = location;
        } else {
            //1. check time age
            if (location.getTime() - mCurrentLocation.getTime() >= AppConfig.SCANNING_TIMEOUT) {
                mCurrentLocation = location;
            } else
                //2. check accuracy
                if (location.hasAccuracy() && (location.getAccuracy() < mCurrentLocation.getAccuracy())) {
                    mCurrentLocation = location;
                }
        }
    }

    /**
     * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
     * runtime permission has been granted.
     */
    public void startLocationUpdates() {
        if (mRequestingLocationUpdates) {
            Logger.i("[>_] Location is updating");
        } else {
            // Begin by checking if the device has the necessary location settings.
            mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                    .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                        @Override
                        public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                            Logger.d("All location settings are satisfied.");
                            FireLogger.d("All location settings are satisfied.");
                            try {
                                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                mRequestingLocationUpdates = true;
                                            }
                                        });
                            } catch (SecurityException unlikely) {
                                Logger.d("Lost location permission. Could not request update");
                                FireLogger.d("Lost location permission. Could not request update");
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            int statusCode = ((ApiException) e).getStatusCode();
                            switch (statusCode) {
                                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                    Logger.d("Location settings are not satisfied. Attempting to upgrade location settings ");
                                    mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                                    mRequestingLocationUpdates = false;
                                    break;
                                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                    String errorMessage = "Location settings are inadequate, and cannot be fixed here. Fix in Settings.";
                                    Logger.d(errorMessage);
                                    mRequestingLocationUpdates = false;
                            }
                        }
                    });
        }
    }

    public void reset() {
        if (!mRequestingLocationUpdates) {
            Logger.d("stopLocationUpdates: updates never requested, no-op.");
            startLocationUpdates();
            return;
        } else {

            // It is a good practice to remove location requests when the activity is in a paused or
            // stopped state. Doing so helps battery performance and is especially
            // recommended in applications that request frequent location updates.
//            mFusedLocationClient.removeLocationUpdates(mLocationCallback)
//                    .addOnCompleteListener(new OnCompleteListener<Void>() {
//                        @Override
//                        public void onComplete(@NonNull Task<Void> task) {
//                            Logger.d("[>_] Removed Location Updates");
//                            mRequestingLocationUpdates = false;
//                            startLocationUpdates();
//                        }
//                    });
        }
    }
    /**
     * Removes location updates from the FusedLocationApi.
     */

    public void stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            Logger.d("stopLocationUpdates: updates never requested, no-op.");
            return;
        }

        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Logger.d("[>_] Removed Location Updates");
                        mRequestingLocationUpdates = false;
                    }
                });
    }

    public Location getCurrentLocation() {
        if (mCurrentLocation == null) {
            startLocationUpdates();
        }
        return mCurrentLocation;
    }
    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }
}
