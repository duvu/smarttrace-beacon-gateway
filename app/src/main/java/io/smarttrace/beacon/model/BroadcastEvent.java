package io.smarttrace.beacon.model;

import android.location.Location;

import java.util.List;

/**
 * Created by beou on 3/21/18.
 */

public class BroadcastEvent {
    private String gatewayId;
    private List<BT04Package> BT04PackageList;
    private Location location;
    private List<CellTower> cellTowerList;

    public String getGatewayId() {
        return gatewayId;
    }

    public void setGatewayId(String gatewayId) {
        this.gatewayId = gatewayId;
    }

    public List<BT04Package> getBT04PackageList() {
        return BT04PackageList;
    }

    public void setBT04PackageList(List<BT04Package> BT04PackageList) {
        this.BT04PackageList = BT04PackageList;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public List<CellTower> getCellTowerList() {
        return cellTowerList;
    }

    public void setCellTowerList(List<CellTower> cellTowerList) {
        this.cellTowerList = cellTowerList;
    }
}
