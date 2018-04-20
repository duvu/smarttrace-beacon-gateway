package au.com.smarttrace.beacon.db;

import java.util.List;

import au.com.smarttrace.beacon.AppContants;
import io.objectbox.annotation.Backlink;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToMany;

@Entity
public class EventData {

    @Id
    private Long id;

    private String phoneImei;
    private long timestamp;
    private double latitude;
    private double longitude;
    private double altitude;
    private double accuracy;
    private double speedKPH;

    @Backlink
    private ToMany<SensorData> sensorDataList;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPhoneImei() {
        return phoneImei;
    }

    public void setPhoneImei(String phoneImei) {
        this.phoneImei = phoneImei;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public double getSpeedKPH() {
        return speedKPH;
    }

    public void setSpeedKPH(double speedKPH) {
        this.speedKPH = speedKPH;
    }

    public ToMany<SensorData> getSensorDataList() {
        return sensorDataList;
    }

    public void setSensorDataList(ToMany<SensorData> sensorDataList) {
        this.sensorDataList = sensorDataList;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        // phone-imei|epoch-time|latitude|longitude|altitude|accuracy|speedKPH|<\n>
        // SN|Name|Temperature|Humidity|RSSI|Distance|battery|LastScannedTime|HardwareModel|<\n>
        // SN|Name|Temperature|Humidity|RSSI|Distance|battery|LastScannedTime|HardwareModel|<\n>
        // SN|Name|Temperature|Humidity|RSSI|Distance|battery|LastScannedTime|HardwareModel|<\n>
        // SN|Name|Temperature|Humidity|RSSI|Distance|battery|LastScannedTime|HardwareModel|<\n>
        // SN|Name|Temperature|Humidity|RSSI|Distance|battery|LastScannedTime|HardwareModel|<\n>


        sb.append(getPhoneImei()).append("|");
        sb.append(timestamp).append(AppContants.SEPARATE);
        sb.append(getLatitude()).append(AppContants.SEPARATE);
        sb.append(getLongitude()).append(AppContants.SEPARATE);
        sb.append(getAltitude()).append(AppContants.SEPARATE);
        sb.append(getAccuracy()).append(AppContants.SEPARATE);
        sb.append(getSpeedKPH()).append(AppContants.SEPARATE).append(AppContants.N);

        for (SensorData data : getSensorDataList()) {
            sb.append(data.getSerialNumber()).append(AppContants.SEPARATE);
            sb.append(data.getName()).append(AppContants.SEPARATE);
            sb.append(data.getTemperature()).append(AppContants.SEPARATE);
            sb.append(data.getHumidity()).append(AppContants.SEPARATE);
            sb.append(data.getRssi()).append(AppContants.SEPARATE);
            sb.append(data.getDistance()).append(AppContants.SEPARATE);
            sb.append(data.getBattery()).append(AppContants.SEPARATE);
            sb.append(data.getLastScannedTime()).append(AppContants.SEPARATE);
            sb.append(data.getHardwareModel()).append(AppContants.SEPARATE).append(AppContants.N);
        }
        return sb.toString();
    }
}
