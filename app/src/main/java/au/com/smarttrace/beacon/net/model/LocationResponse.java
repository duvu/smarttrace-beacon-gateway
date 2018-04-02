package au.com.smarttrace.beacon.net.model;

import java.util.List;

public class LocationResponse {
    private Status status;
    List<LocationBody> response;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<LocationBody> getResponse() {
        return response;
    }

    public void setResponse(List<LocationBody> response) {
        this.response = response;
    }
}
