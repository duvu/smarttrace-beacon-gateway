package io.smarttrace.beacon.net.model;

/**
 * Created by beou on 3/21/18.
 */

public abstract class CommonResponse {
    private Status status;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public abstract BaseBody getResponse();
    public abstract void setResponse(BaseBody response);
}
