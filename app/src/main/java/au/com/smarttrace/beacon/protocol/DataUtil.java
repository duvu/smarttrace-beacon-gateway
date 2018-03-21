package au.com.smarttrace.beacon.protocol;

import android.location.Location;

import java.util.Date;
import java.util.List;

import au.com.smarttrace.beacon.model.BroadcastEvent;
import au.com.smarttrace.beacon.model.Device;

/**
 * Created by beou on 3/9/18.
 */

public class DataUtil {
    private static final String SEP = "|";
    private static final String N = "\n";

    public static String formatData(BroadcastEvent broadcastEvent) {
        StringBuffer sb = new StringBuffer();
        // phone-imei|epoch-time|latitude|longitude|altitude|accuracy|speedKPH|<\n>
        // SN|Name|Temperature|Humidity|RSSI|Distance|battery|LastScannedTime|HardwareModel|<\n>
        // SN|Name|Temperature|Humidity|RSSI|Distance|battery|LastScannedTime|HardwareModel|<\n>
        // SN|Name|Temperature|Humidity|RSSI|Distance|battery|LastScannedTime|HardwareModel|<\n>
        // SN|Name|Temperature|Humidity|RSSI|Distance|battery|LastScannedTime|HardwareModel|<\n>
        // SN|Name|Temperature|Humidity|RSSI|Distance|battery|LastScannedTime|HardwareModel|<\n>
        Long timestamp = (new Date()).getTime();
        Location location = broadcastEvent.getLocation();
        List<Device> deviceList = broadcastEvent.getDeviceList();

        sb.append(broadcastEvent.getGatewayId()).append(SEP);
        sb.append(timestamp).append(SEP);
        if (location != null) {
            sb.append(location.getLatitude()).append(SEP);
            sb.append(location.getLongitude()).append(SEP);
            sb.append(location.getAltitude()).append(SEP);
            sb.append(location.getAccuracy()).append(SEP);
            sb.append(location.getSpeed()).append(SEP).append(N);
        }

        for (Device device : deviceList) {
            sb.append(device.getSerialNumber()).append(SEP);
            sb.append(device.getName()).append(SEP);
            sb.append(device.getTemperature()).append(SEP);
            sb.append(device.getHumidity()).append(SEP);
            sb.append(device.getRssi()).append(SEP);
            sb.append(device.getDistance()).append(SEP);
            sb.append(device.getBatteryLevel()).append(SEP);
            sb.append(device.getTimestamp()).append(SEP);
            sb.append(device.getModel()).append(SEP).append(N);
        }
        return sb.toString();
    }
}
