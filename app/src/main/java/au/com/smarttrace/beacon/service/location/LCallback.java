package au.com.smarttrace.beacon.service.location;

import android.location.Location;

public interface LCallback {
    public void onLocationChanged(Location location);
}
