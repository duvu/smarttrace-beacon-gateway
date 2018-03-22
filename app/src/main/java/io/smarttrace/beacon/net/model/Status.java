package io.smarttrace.beacon.net.model;

/**
 * Created by beou on 3/21/18.
 */

public class Status {
    int code;
    String message;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}