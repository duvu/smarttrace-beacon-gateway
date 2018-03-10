package au.com.smarttrace.beacon.model;

import android.location.Location;

import com.TZONE.Bluetooth.Temperature.Model.Device;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by beou on 3/8/18.
 */

public class AdvancedDevice {
    private List<Device> deviceList;
    private Location location;
    private List<CellTower> cellTowerList;
    private String imei;

    public AdvancedDevice() {
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

    public List<CellTower> getCellTowerList() {
        return cellTowerList;
    }

    public void setCellTowerList(List<CellTower> cellTowerList) {
        this.cellTowerList = cellTowerList;
    }

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public void addCellTower(CellTower cell) {
        if (cellTowerList == null) {
            cellTowerList = new ArrayList<>();
        }
        this.cellTowerList.add(cell);
    }

    public void addDevice(Device device) {
        if (deviceList == null) {
            deviceList = new ArrayList<>();
        }
        deviceList.add(device);
    }
}
