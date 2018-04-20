package au.com.smarttrace.beacon.firebase;

import android.location.Location;

import java.util.ArrayList;
import java.util.List;

import au.com.smarttrace.beacon.model.BeaconPackage;
import au.com.smarttrace.beacon.model.BroadcastEvent;

public class ToFirebase {
    double latitude;
    double longitude;
    double accuracy;
    double speedKph;
    String provider;

    List<ToFirebaseBeacon> beacons;

    public static ToFirebase fromRaw(BroadcastEvent raw) {
        ToFirebase tf = new ToFirebase();
        Location loc = raw.getLocation();
        if (loc != null) {
            tf.latitude = loc.getLatitude();
            tf.longitude = loc.getLongitude();
            tf.accuracy = loc.getAccuracy();
            tf.speedKph = loc.getSpeed();
            tf.provider = loc.getProvider();
        } else {
            tf.latitude = 0;
            tf.longitude = 0;
            tf.accuracy = 0;
            tf.speedKph = 0;
            tf.provider = "noprovider";
        }
        List<BeaconPackage> lb = raw.getBeaconPackageList();
        if (lb != null && lb.size() > 0) {
            tf.beacons = new ArrayList<>();
            for (BeaconPackage bp : lb) {
                ToFirebaseBeacon tfb = ToFirebaseBeacon.fromRaw(bp);
                tf.beacons.add(tfb);
            }
        }
        return tf;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public double getSpeedKph() {
        return speedKph;
    }

    public void setSpeedKph(double speedKph) {
        this.speedKph = speedKph;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public List<ToFirebaseBeacon> getBeacons() {
        return beacons;
    }

    public void setBeacons(List<ToFirebaseBeacon> beacons) {
        this.beacons = beacons;
    }
}
