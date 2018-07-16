package au.com.smarttrace.beacon.service.location;

import android.location.Location;

public interface LCallback {
    void onLocationChanged(Location location);
}
