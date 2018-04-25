package au.com.smarttrace.beacon.service;

import com.TZONE.Bluetooth.Temperature.Model.Device;

public interface BLECallback {
    public void onUpdateBLE(Device device);
}
