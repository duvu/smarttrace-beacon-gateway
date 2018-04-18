package au.com.smarttrace.beacon.net.model;

/**
 * Created by beou on 3/21/18.
 */

public class Common {
    private Status status;
    private Object response;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Object getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = response;
    }
}
