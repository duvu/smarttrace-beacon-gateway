package io.smarttrace.beacon.model;

import android.location.Location;

public class BeaconLocation {

    private double lat;
    private double lng;
    private float accuracy;
    private double altitude;
    private long timestamp;
    private String method;
    private Location location;

    public BeaconLocation() {

    }

    public BeaconLocation(Location loc) {
        if (loc != null) {
            this.lat = loc.getLatitude();
            this.lng = loc.getLongitude();
            this.accuracy = loc.getAccuracy();
            this.altitude = loc.getAltitude();
            this.timestamp = System.currentTimeMillis();
            this.location = loc;
        }
    }

    public BeaconLocation(Location loc, String method) {
        this(loc);
        if (loc != null) {
            this.method = method;
        }
    }


    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public long timestamp() {
        return this.timestamp;
    }

    @Override
    public String toString() {
        return "lat: " + lat + " - lng: " + lng + " - acc: "+accuracy + " - method: " + method;
    }

    public boolean isValid() {
        return (this.lat != 0 && this.lng != 0);
    }

    public float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public long getTimestamp() { return timestamp; }

    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public Location getLocation() { return location; }

    public void setLocation(Location location) { this.location = location; }

}

