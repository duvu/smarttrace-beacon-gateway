package au.com.smarttrace.beacon.protocol;

import android.location.Location;

import com.TZONE.Bluetooth.Temperature.Model.Device;
import com.TZONE.Bluetooth.Utils.MeasuringDistance;

import java.util.Date;
import java.util.List;

import au.com.smarttrace.beacon.model.AdvancedDevice;

/**
 * Created by beou on 3/9/18.
 */

public class DataUtil {
    private static final String SEP = "|";
    private static final String N = "\n";

    public static String formatData(AdvancedDevice advancedDevice) {
        StringBuffer sb = new StringBuffer();
        // phone-imei|epoch-time|latitude|longitude|altitude|accuracy|speedKPH|<\n>
        // SN|Name|Temperature|Humidity|RSSI|Distance|battery|LastScannedTime|HardwareModel|<\n>
        // SN|Name|Temperature|Humidity|RSSI|Distance|battery|LastScannedTime|HardwareModel|<\n>
        // SN|Name|Temperature|Humidity|RSSI|Distance|battery|LastScannedTime|HardwareModel|<\n>
        // SN|Name|Temperature|Humidity|RSSI|Distance|battery|LastScannedTime|HardwareModel|<\n>
        // SN|Name|Temperature|Humidity|RSSI|Distance|battery|LastScannedTime|HardwareModel|<\n>
        Long timestamp = (new Date()).getTime();
        Location location = advancedDevice.getLocation();
        List<Device> deviceList = advancedDevice.getDeviceList();

        sb.append(advancedDevice.getImei()).append(SEP);
        sb.append(timestamp).append(SEP);
        if (location != null) {
            sb.append(location.getLatitude()).append(SEP);
            sb.append(location.getLongitude()).append(SEP);
            sb.append(location.getAltitude()).append(SEP);
            sb.append(location.getAccuracy()).append(SEP);
            sb.append(location.getSpeed()).append(SEP).append(N);
        }

        for (Device device : deviceList) {
            sb.append(device.SN).append(SEP);
            sb.append(device.Name).append(SEP);
            sb.append(device.Temperature).append(SEP);
            sb.append(device.Humidity).append(SEP);
            sb.append(device.RSSI).append(SEP);
            sb.append(MeasuringDistance.calculateAccuracy(60, device.RSSI)).append(SEP);
            sb.append(device.Battery).append(SEP);
            sb.append(device.LastScanTime.getTime()).append(SEP);
            sb.append(device.HardwareModel).append(SEP).append(N);
        }
        return sb.toString();
    }
}
