package au.com.smarttrace.beacon.net.model;

/**
 * Created by beou on 3/21/18.
 */

public class LoginBody {
    private String token;
    private String expired;
    private Long expiredTimestamp;
    private String instance;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getExpired() {
        return expired;
    }

    public void setExpired(String expired) {
        this.expired = expired;
    }

    public Long getExpiredTimestamp() {
        return expiredTimestamp;
    }

    public void setExpiredTimestamp(Long expiredTimestamp) {
        this.expiredTimestamp = expiredTimestamp;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }
}
