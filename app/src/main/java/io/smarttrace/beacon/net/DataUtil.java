package io.smarttrace.beacon.net;

import android.location.Location;
import android.text.TextUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import io.smarttrace.beacon.model.BroadcastEvent;
import io.smarttrace.beacon.model.CellTower;
import io.smarttrace.beacon.model.DataPackage;

/**
 * Created by beou on 3/9/18.
 */

public class DataUtil {
    private static final String SEP = "|";
    private static final String N = "\n";

    public static List<String> formatData1(BroadcastEvent broadcastEvent) {
        List<DataPackage> dataPackageList = broadcastEvent.getDataPackageList();
        Date curremtDate = (new Date());
        List<String> stringList = new ArrayList<>();
        for (DataPackage dataPackage : dataPackageList) {
            StringBuffer sb = new StringBuffer();

            Location location = broadcastEvent.getLocation();


            //sb.append(broadcastEvent.getGatewayId() + dataPackage.getSerialNumber()).append(SEP);
            sb.append(dataPackage.getSerialNumber()).append(SEP);
            sb.append("AUT").append(SEP); //type
            sb.append(formatDate(curremtDate, null)).append(SEP).append(N);

            sb.append(dataPackage.getBatteryLevel()).append(SEP);
            sb.append(dataPackage.getTemperature()).append(SEP).append(N);

            List<CellTower> cellTowerList = broadcastEvent.getCellTowerList();
            for (CellTower cell : cellTowerList) {
                sb.append(cell.getMcc()).append(SEP);
                sb.append(cell.getMnc()).append(SEP);
                sb.append(cell.getLac()).append(SEP);
                sb.append(cell.getCid()).append(SEP);
                sb.append(cell.getRxlev()).append(SEP).append(N);
            }
            stringList.add(sb.toString());
        }

        return stringList;
    }


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
        List<DataPackage> dataPackageList = broadcastEvent.getDataPackageList();

        sb.append(broadcastEvent.getGatewayId()).append(SEP);
        sb.append(timestamp).append(SEP);
        if (location != null) {
            sb.append(location.getLatitude()).append(SEP);
            sb.append(location.getLongitude()).append(SEP);
            sb.append(location.getAltitude()).append(SEP);
            sb.append(location.getAccuracy()).append(SEP);
            sb.append(location.getSpeed()).append(SEP).append(N);
        }

        for (DataPackage dataPackage : dataPackageList) {
            sb.append(dataPackage.getSerialNumber()).append(SEP);
            sb.append(dataPackage.getName()).append(SEP);
            sb.append(dataPackage.getTemperature()).append(SEP);
            sb.append(dataPackage.getHumidity()).append(SEP);
            sb.append(dataPackage.getRssi()).append(SEP);
            sb.append(dataPackage.getDistance()).append(SEP);
            sb.append(dataPackage.getBatteryLevel()).append(SEP);
            sb.append(dataPackage.getTimestamp()).append(SEP);
            sb.append(dataPackage.getModel()).append(SEP).append(N);
        }
        return sb.toString();
    }

    private static String formatDate(Date date, String format) {
        if (TextUtils.isEmpty(format)) {
            format = "yyyy/MM/dd HH:mm:ss";
        }
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(date);
    }
}
