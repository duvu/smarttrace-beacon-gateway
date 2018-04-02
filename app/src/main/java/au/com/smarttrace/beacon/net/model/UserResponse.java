package au.com.smarttrace.beacon.net.model;

public class UserResponse {
    private Status status;
    UserBody response;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public UserBody getResponse() {
        return response;
    }

    public void setResponse(UserBody response) {
        this.response = response;
    }
}
