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

}
