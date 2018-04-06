package au.com.smarttrace.beacon.model;

import org.altbeacon.beacon.Beacon;

import java.util.Date;
import java.util.Locale;

import au.com.smarttrace.beacon.AppContants;

/**
 * Created by beou on 3/21/18.
 */

public class BT04Package extends AbstractDataPackage{
    private static final String IMEI_PREFIX = "bt04-";
    private static final String IMEI_SUFFIX = "x";
    private Double temperature;
    private Double humidity;
    private Double distance;
    private String model;
    private String firmware;
    private String serialNumber;

    private long readingCount;
    private boolean shouldCreateShipment;
    private boolean shouldUpload;
    private boolean foredCreateNew;
    private long readingAge;

    public static BT04Package fromBeacon(Beacon beacon) {
        BT04Package dl = new BT04Package();
        long now = (new Date()).getTime();
        dl.setBluetoothAddress(beacon.getBluetoothAddress());
        dl.setName(beacon.getBluetoothName());
        dl.setRssi(beacon.getRssi());
        dl.setType(beacon.getBeaconTypeCode());
        dl.setTimestamp(now);
        dl.readingAge = 0L;
        dl.setBatteryLevel(beacon.getTxPower());
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
        dl.serialNumber = beacon.getIdentifier(2).toHexString().substring(2);
        dl.readingCount = 0;
        dl.shouldCreateShipment = false;
        dl.shouldUpload = true;
        dl.foredCreateNew = false;
        return dl;
    }

    public BT04Package updateFromBeacon(Beacon beacon) {
        long now = (new Date()).getTime();
        this.setBluetoothAddress(beacon.getBluetoothAddress());
        this.setName(beacon.getBluetoothName());
        this.setRssi(beacon.getRssi());
        this.setType(beacon.getBeaconTypeCode());

        this.readingAge = now - this.getTimestamp();

        //-- update timestamp
        this.setTimestamp(now);
        this.setBatteryLevel(beacon.getTxPower());
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
        this.serialNumber = beacon.getIdentifier(2).toHexString().substring(2);
        this.readingCount++;
        this.shouldUpload = true;
        return this;
    }

    public Double getTemperature() {
        return temperature;
    }
    public String getTemperatureString() {
        return temperature + AppContants.DEGREE_CELSIUS;
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

    public Double getDistance() {
        return distance;
    }

    public String getDistanceString() {
        String units = "m";
        return String.format(Locale.US, "%.1f", distance + 0.1) + units;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public String getModel() {
        return model;
    }

    public String getModelString() {
        if (getModel().equals("3901"))
            return ("BT04 (v" + getFirmware() + ")");
        else if (getModel().equals("3C01"))
            return ("BT04B (v" + getFirmware() + ")");
        else if (getModel().equals("3A01"))
            return ("BT05 (v" + getFirmware() + ")");
        else if (getModel().equals("3A04"))
            return ("BT05B (v" + getFirmware() + ")");
        else
            return (getModel() + " (v" + getFirmware() + ")");

    }

    public String getModelStringShort() {
        if (getModel().equals("3901"))
            return ("BT04");
        else if (getModel().equals("3C01"))
            return ("BT04B");
        else if (getModel().equals("3A01"))
            return ("BT05");
        else if (getModel().equals("3A04"))
            return ("BT05B");
        else
            return (getModel() + " (v" + getFirmware() + ")");

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
    public String getSerialNumberString() {
        return "bt04-"+serialNumber+"x";
    }
    public String getSerialNumberStringX() {
        final StringBuilder sb = new StringBuilder(serialNumber);
        sb.append(IMEI_SUFFIX);
        //add zero symbols
        int len = 11 - sb.length() - IMEI_PREFIX.length();
        while (len > 0) {
            sb.insert(0, '0');
            len--;
        }
        return sb.toString();
    }
    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public long getReadingCount() {
        return readingCount;
    }

    public void setReadingCount(long readingCount) {
        this.readingCount = readingCount;
    }

    public boolean isShouldCreateShipment() {
        return shouldCreateShipment;
    }

    public void setShouldCreateShipment(boolean shouldCreateShipment) {
        this.shouldCreateShipment = shouldCreateShipment;
    }

    public boolean isShouldUpload() {
        return shouldUpload;
    }

    public void setShouldUpload(boolean shouldUpload) {
        this.shouldUpload = shouldUpload;
    }

    public boolean isForedCreateNew() {
        return foredCreateNew;
    }

    public void setForedCreateNew(boolean foredCreateNew) {
        this.foredCreateNew = foredCreateNew;
    }

    public long getReadingAge() {
        return readingAge;
    }

    public void setReadingAge(long readingAge) {
        this.readingAge = readingAge;
    }
}
