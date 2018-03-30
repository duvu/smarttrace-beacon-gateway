package au.com.smarttrace.beacon.model;

import org.altbeacon.beacon.Beacon;

import java.util.Date;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

/**
 * Created by beou on 3/20/18.
 */

@Entity
public class DataLogger {
    @Id
    private Long id;
    private String bluetoothAddress;
    private String name;
    private int rssi;
    private int type;
    private Long timestamp;
    private int batteryLevel;
    private Double temperature;
    private Double humidity;
    private Double distance;
    private String model;
    private String firmware;
    private String serialNumber;

    public DataLogger() {

    }

    public static DataLogger fromBeacon(Beacon beacon) {
        DataLogger dl = new DataLogger();
        dl.bluetoothAddress = beacon.getBluetoothAddress();
        dl.name = beacon.getBluetoothName();
        dl.rssi = beacon.getRssi();
        dl.type = beacon.getBeaconTypeCode();
        dl.timestamp = (new Date()).getTime();
        dl.batteryLevel = beacon.getTxPower();
        dl.distance = beacon.getDistance();
        if (beacon.getDataFields().size() > 2) {
            dl.temperature = beacon.getDataFields().get(1) / 100.00;
            dl.humidity = beacon.getDataFields().get(2) / 100.00;
        }
        int hm = beacon.getIdentifier(0).toInt();
        switch (hm) {
            case 0x3901:
                dl.model = "3901";
                break;
            case 0x3A01:
                dl.model = "3A01";
                break;
            case 0x3C01:
                dl.model = "3C01";
                break;
            case 0x3A04:
                dl.model = "3A04";
                break;
        }
        dl.firmware = beacon.getIdentifier(1).toHexString();
        dl.serialNumber = beacon.getIdentifier(2).toHexString();
        return dl;
    }

    public DataLogger updateFromBeacon(Beacon beacon) {
        this.bluetoothAddress = beacon.getBluetoothAddress();
        this.name = beacon.getBluetoothName();
        this.rssi = beacon.getRssi();
        this.type = beacon.getBeaconTypeCode();

        this.timestamp = (new Date()).getTime();

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
        return this;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
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
