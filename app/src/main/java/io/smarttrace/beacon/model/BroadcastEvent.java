package io.smarttrace.beacon.model;

import android.location.Location;

import java.util.List;

/**
 * Created by beou on 3/21/18.
 */

public class BroadcastEvent {
    private String gatewayId;
    private List<DataPackage> dataPackageList;
    private Location location;
    private List<CellTower> cellTowerList;

    public String getGatewayId() {
        return gatewayId;
    }

    public void setGatewayId(String gatewayId) {
        this.gatewayId = gatewayId;
    }

    public List<DataPackage> getDataPackageList() {
        return dataPackageList;
    }

    public void setDataPackageList(List<DataPackage> dataPackageList) {
        this.dataPackageList = dataPackageList;
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
