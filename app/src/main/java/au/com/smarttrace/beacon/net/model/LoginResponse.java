package au.com.smarttrace.beacon.net.model;

/**
 * Created by beou on 3/21/18.
 */

public class LoginResponse {
    private Status status;
    LoginBody response;

    public Status getStatus() {
        return status;
    }
    public void setStatus(Status status) {
        this.status = status;
    }


    public LoginBody getResponse() {
        return response;
    }
    public void setResponse(Object response) {
        this.response = (LoginBody) response;
    }
}
