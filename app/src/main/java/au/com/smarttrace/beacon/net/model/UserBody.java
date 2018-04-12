package au.com.smarttrace.beacon.net.model;

public class UserBody {
    String timeZone;
    long internalCompanyId;

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public long getInternalCompanyId() {
        return internalCompanyId;
    }

    public void setInternalCompanyId(long internalCompanyId) {
        this.internalCompanyId = internalCompanyId;
    }
}
