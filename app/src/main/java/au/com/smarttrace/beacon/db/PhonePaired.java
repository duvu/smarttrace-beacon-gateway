package au.com.smarttrace.beacon.db;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class PhonePaired {
    @Id
    private Long id;

    private String phoneImei;
    private String beaconSerialNumber;

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

    public String getBeaconSerialNumber() {
        return beaconSerialNumber;
    }

    public void setBeaconSerialNumber(String beaconSerialNumber) {
        this.beaconSerialNumber = beaconSerialNumber;
    }
}
