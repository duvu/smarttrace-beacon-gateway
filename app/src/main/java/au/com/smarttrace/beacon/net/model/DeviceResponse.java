package au.com.smarttrace.beacon.net.model;

public class DeviceResponse {
    private Status status;
    DeviceBody response;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public DeviceBody getResponse() {
        return response;
    }

    public void setResponse(DeviceBody response) {
        this.response = response;
    }
}
