package au.com.smarttrace.beacon.model;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.Identifier;

/**
 * Created by beou on 3/20/18.
 */

public class DataLogger {
    private String bluetoothAddress;
    private String name;
    private int rssi;
    private int type;
    private int batteryLevel;
    private Double temperature;
    private Double humidity;
    private Double distance;
    private String model;
    private String firmware;
    private String serialNumber;

    public DataLogger(Beacon beacon) {
        this.bluetoothAddress = beacon.getBluetoothAddress();
        this.name = beacon.getBluetoothName();
        this.rssi = beacon.getRssi();
        this.type = beacon.getBeaconTypeCode();
        this.batteryLevel = beacon.getTxPower();
        this.distance = beacon.getDistance();
        if (beacon.getDataFields().size() > 2) {
            this.temperature = beacon.getDataFields().get(1) / 100.00;
            this.humidity = beacon.getDataFields().get(2) / 100.00;
        }
        int hm = beacon.getIdentifier(0).toInt();
        switch (hm) {
            case 0x3901:
                this.model = "3901";
                break;
            case 0x3A01:
                this.model = "3A01";
                break;
            case 0x3C01:
                this.model = "3C01";
                break;
            case 0x3A04:
                this.model = "3A04";
                break;
        }
        this.firmware = beacon.getIdentifier(1).toHexString();
        this.serialNumber = beacon.getIdentifier(2).toHexString();
    }

    public String getBluetoothAddress() {
        return bluetoothAddress;
    }

    public void setBluetoothAddress(String bluetoothAddress) {
        this.bluetoothAddress = bluetoothAddress;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getHumidity() {
        return humidity;
    }

    public void setHumidity(Double humidity) {
        this.humidity = humidity;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getFirmware() {
        return firmware;
    }

    public void setFirmware(String firmware) {
        this.firmware = firmware;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }
}
