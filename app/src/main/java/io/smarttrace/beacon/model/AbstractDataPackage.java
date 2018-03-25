package io.smarttrace.beacon.model;

/**
 * Created by beou on 3/23/18.
 */

public abstract class AbstractDataPackage {
    private String bluetoothAddress;
    private String name;
    private int rssi;
    private int type;
    private int batteryLevel;
    private Long timestamp;

    public void setBluetoothAddress(String bluetoothAddress) {
        this.bluetoothAddress = bluetoothAddress;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getBluetoothAddress() {
        return bluetoothAddress;
    }

    public String getName() {
        return name;
    }

    public int getRssi() {
        return rssi;
    }

    public int getType() {
        return type;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public abstract Double getTemperature();

    public abstract Double getHumidity();

    public abstract Double getDistance();

    public abstract String getModel();

    public abstract String getFirmware();

    public abstract String getSerialNumber();
}
