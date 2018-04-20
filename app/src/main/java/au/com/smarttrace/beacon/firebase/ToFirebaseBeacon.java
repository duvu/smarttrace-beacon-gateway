package au.com.smarttrace.beacon.firebase;

import au.com.smarttrace.beacon.model.BeaconPackage;

public class ToFirebaseBeacon {
    int batteryLevel;
    String bluetoothAddress;
    double distance;
    String firmware;
    String model;
    String modelString;
    String name;
    long readingCount;

    String serialNumber;
    String serialNumberString;

    int rssi;
    double temperature;
    double humidity;
    long lastReadingTime;

    public static ToFirebaseBeacon fromRaw(BeaconPackage raw) {
        ToFirebaseBeacon tfb = new ToFirebaseBeacon();
        tfb.batteryLevel = raw.getBatteryLevel();
        tfb.bluetoothAddress = raw.getBluetoothAddress();
        tfb.distance = raw.getDistance();
        tfb.firmware = raw.getFirmware();
        tfb.model = raw.getModel();
        tfb.modelString = raw.getModelString();
        tfb.name = raw.getName();
        tfb.readingCount = raw.getReadingCount();
        tfb.serialNumber = raw.getSerialNumber();
        tfb.serialNumberString = raw.getSerialNumberString();
        tfb.rssi = raw.getRssi();
        tfb.temperature = raw.getTemperature();
        tfb.humidity = raw.getHumidity();
        tfb.lastReadingTime = raw.getTimestamp();

        return tfb;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public String getBluetoothAddress() {
        return bluetoothAddress;
    }

    public void setBluetoothAddress(String bluetoothAddress) {
        this.bluetoothAddress = bluetoothAddress;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public String getFirmware() {
        return firmware;
    }

    public void setFirmware(String firmware) {
        this.firmware = firmware;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getModelString() {
        return modelString;
    }

    public void setModelString(String modelString) {
        this.modelString = modelString;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getReadingCount() {
        return readingCount;
    }

    public void setReadingCount(long readingCount) {
        this.readingCount = readingCount;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getSerialNumberString() {
        return serialNumberString;
    }

    public void setSerialNumberString(String serialNumberString) {
        this.serialNumberString = serialNumberString;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
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

    public long getLastReadingTime() {
        return lastReadingTime;
    }

    public void setLastReadingTime(long lastReadingTime) {
        this.lastReadingTime = lastReadingTime;
    }
}
