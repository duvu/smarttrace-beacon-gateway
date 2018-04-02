package au.com.smarttrace.beacon.net.model;

import java.util.List;

public class LocationBody {
    long locationId;
    String locationName;
    LatLng location;
    double radiusMeters;
    String startFlag;
    String interimFlag;
    String endFlag;

    public long getLocationId() {
        return locationId;
    }

    public void setLocationId(long locationId) {
        this.locationId = locationId;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public LatLng getLocation() {
        return location;
    }

    public void setLocation(LatLng location) {
        this.location = location;
    }

    public double getRadiusMeters() {
        return radiusMeters;
    }

    public void setRadiusMeters(double radiusMeters) {
        this.radiusMeters = radiusMeters;
    }

    public String getStartFlag() {
        return startFlag;
    }

    public void setStartFlag(String startFlag) {
        this.startFlag = startFlag;
    }

    public String getInterimFlag() {
        return interimFlag;
    }

    public void setInterimFlag(String interimFlag) {
        this.interimFlag = interimFlag;
    }

    public String getEndFlag() {
        return endFlag;
    }

    public void setEndFlag(String endFlag) {
        this.endFlag = endFlag;
    }
}
