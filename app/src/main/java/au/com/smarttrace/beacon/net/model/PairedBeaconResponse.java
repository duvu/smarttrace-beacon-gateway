package au.com.smarttrace.beacon.net.model;

import java.util.List;
import java.util.Set;

public class PairedBeaconResponse {
    private Status status;
    Set<String> response;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Set<String> getResponse() {
        return response;
    }

    public void setResponse(Set<String> response) {
        this.response = response;
    }
}
