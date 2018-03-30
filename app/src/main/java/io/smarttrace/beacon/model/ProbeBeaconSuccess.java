package io.smarttrace.beacon.model;

public class ProbeBeaconSuccess {
    private boolean success;

    public ProbeBeaconSuccess(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
