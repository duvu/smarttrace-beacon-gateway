package au.com.smarttrace.beacon.model;

import android.location.Location;

import java.util.List;

/**
 * Created by beou on 3/21/18.
 */

public class BroadcastEvent {
    private String gatewayId;
    private List<Device> deviceList;
    private Location location;

    public String getGatewayId() {
        return gatewayId;
    }

    public void setGatewayId(String gatewayId) {
        this.gatewayId = gatewayId;
    }

    public List<Device> getDeviceList() {
        return deviceList;
    }

    public void setDeviceList(List<Device> deviceList) {
        this.deviceList = deviceList;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
