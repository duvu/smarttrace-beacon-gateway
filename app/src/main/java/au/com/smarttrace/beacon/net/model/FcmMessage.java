package au.com.smarttrace.beacon.net.model;

import java.util.Date;

public class FcmMessage {
    private long expectedTimeToReceive;
    private String message;
    private String fcmToken;
    private String fcmInstanceId;
    private String phoneImei;

    public long getExpectedTimeToReceive() {
        return expectedTimeToReceive;
    }

    public void setExpectedTimeToReceive(long expectedTimeToReceive) {
        this.expectedTimeToReceive = expectedTimeToReceive;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public String getFcmInstanceId() {
        return fcmInstanceId;
    }

    public void setFcmInstanceId(String fcmInstanceId) {
        this.fcmInstanceId = fcmInstanceId;
    }

    public String getPhoneImei() {
        return phoneImei;
    }

    public void setPhoneImei(String phoneImei) {
        this.phoneImei = phoneImei;
    }

    public static FcmMessage create() {
        return new FcmMessage();
    }
}
