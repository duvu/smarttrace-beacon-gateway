package au.com.smarttrace.beacon.db;

import au.com.smarttrace.beacon.net.model.LocationBody;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class Locations {
    @Id
    private Long id;

    long locationId;
    String locationName;
    double latitude;
    double longitude;
    double radiusMeters;
    String startFlag;
    String interimFlag;
    String endFlag;

    public void updateFromRaw(LocationBody lb) {
        this.locationId = lb.getLocationId();
        this.locationName = lb.getLocationName();
        this.latitude = lb.getLocation()!=null? lb.getLocation().getLat():0;
        this.longitude = lb.getLocation()!=null?lb.getLocation().getLon():0;
        this.radiusMeters = lb.getRadiusMeters();
        this.startFlag = lb.getStartFlag();
        this.interimFlag = lb.getInterimFlag();
        this.endFlag = lb.getEndFlag();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
