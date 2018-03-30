package au.com.smarttrace.beacon.net.model;

/**
 * Created by beou on 3/21/18.
 */

public class LoginResponse extends CommonResponse {
    LoginBody response;

    @Override
    public LoginBody getResponse() {
        return response;
    }

    @Override
    public void setResponse(BaseBody response) {
        this.response = (LoginBody) response;
    }
}
