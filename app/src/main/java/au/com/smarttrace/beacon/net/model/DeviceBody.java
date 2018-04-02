package au.com.smarttrace.beacon.net.model;

public class DeviceBody {
    String lastReadingTimeISO;
    long lastShipmentId;
    String shipmentStatus;

    public String getLastReadingTimeISO() {
        return lastReadingTimeISO;
    }

    public void setLastReadingTimeISO(String lastReadingTimeISO) {
        this.lastReadingTimeISO = lastReadingTimeISO;
    }

    public long getLastShipmentId() {
        return lastShipmentId;
    }

    public void setLastShipmentId(long lastShipmentId) {
        this.lastShipmentId = lastShipmentId;
    }

    public String getShipmentStatus() {
        return shipmentStatus;
    }

    public void setShipmentStatus(String shipmentStatus) {
        this.shipmentStatus = shipmentStatus;
    }
}
