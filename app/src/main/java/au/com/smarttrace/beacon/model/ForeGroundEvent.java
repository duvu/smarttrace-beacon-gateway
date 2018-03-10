package au.com.smarttrace.beacon.model;

/**
 * Created by beou on 3/9/18.
 */

public class ForeGroundEvent {
    private boolean foreground;

    public ForeGroundEvent(boolean foreground) {
        this.foreground = foreground;
    }

    public boolean isForeground() {
        return foreground;
    }

    public void setForeground(boolean foreground) {
        this.foreground = foreground;
    }
}
