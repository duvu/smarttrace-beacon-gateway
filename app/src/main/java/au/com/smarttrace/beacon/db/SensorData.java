package au.com.smarttrace.beacon.db;

import au.com.smarttrace.beacon.db.EventData;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;

@Entity
public class SensorData {
    @Id
    private Long id;

    private String serialNumber;
    private String name;
    private double temperature;
    private double humidity;
    private int rssi;
    private double distance;
    private float battery;
    private long lastScannedTime;
    private String hardwareModel;

    private ToOne<EventData> eventData;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getHumidity() {
        return humidity;
    }

    public void setHumidity(double humidity) {
        this.humidity = humidity;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public float getBattery() {
        return battery;
    }

    public void setBattery(float battery) {
        this.battery = battery;
    }

    public long getLastScannedTime() {
        return lastScannedTime;
    }

    public void setLastScannedTime(long lastScannedTime) {
        this.lastScannedTime = lastScannedTime;
    }

    public String getHardwareModel() {
        return hardwareModel;
    }

    public void setHardwareModel(String hardwareModel) {
        this.hardwareModel = hardwareModel;
    }

    public ToOne<EventData> getEventData() {
        return eventData;
    }

    public void setEventData(ToOne<EventData> eventData) {
        this.eventData = eventData;
    }
}
