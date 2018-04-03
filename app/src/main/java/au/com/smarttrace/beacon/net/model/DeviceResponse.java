package au.com.smarttrace.beacon.net.model;

public class DeviceResponse {
    private Status status;
    Device response;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Device getResponse() {
        return response;
    }

    public void setResponse(Device response) {
        this.response = response;
    }
}
