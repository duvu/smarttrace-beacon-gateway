package io.smarttrace.beacon.model;

public class ProbeBeacon {
    private String serialNumber;

    public ProbeBeacon(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }
}
