package au.com.smarttrace.beacon.net.model;

public class PairedRequest {
      private String pairedPhoneID;
      private boolean active;
      private String pairedPhoneIMEI;
      private long company;
      private String description;
      private String pairedBeaconID;

    public String getPairedPhoneID() {
        return pairedPhoneID;
    }

    public void setPairedPhoneID(String pairedPhoneID) {
        this.pairedPhoneID = pairedPhoneID;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getPairedPhoneIMEI() {
        return pairedPhoneIMEI;
    }

    public void setPairedPhoneIMEI(String pairedPhoneIMEI) {
        this.pairedPhoneIMEI = pairedPhoneIMEI;
    }

    public long getCompany() {
        return company;
    }

    public void setCompany(long company) {
        this.company = company;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPairedBeaconID() {
        return pairedBeaconID;
    }

    public void setPairedBeaconID(String pairedBeaconID) {
        this.pairedBeaconID = pairedBeaconID;
    }
}
