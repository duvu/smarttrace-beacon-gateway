package io.smarttrace.beacon.net;

import android.location.Location;
import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import io.smarttrace.beacon.AppConfig;
import io.smarttrace.beacon.model.BT04Package;
import io.smarttrace.beacon.model.BroadcastEvent;
import io.smarttrace.beacon.model.CellTower;

/**
 * Created by beou on 3/9/18.
 */

public class DataUtil {
    private static final String SEP = "|";
    private static final String N = "\n";

    public static List<String> formatData1(BroadcastEvent broadcastEvent) {
        List<BT04Package> BT04PackageList = broadcastEvent.getBT04PackageList();
        Date curremtDate = (new Date());
        List<String> stringList = new ArrayList<>();
        for (BT04Package BT04Package : BT04PackageList) {
            StringBuffer sb = new StringBuffer();

            Location location = broadcastEvent.getLocation();


            //sb.append(broadcastEvent.getGatewayId() + BT04Package.getSerialNumber()).append(SEP);
            sb.append(BT04Package.getSerialNumber()).append(SEP);
            sb.append("AUT").append(SEP); //type
            sb.append(formatDate(curremtDate, null)).append(SEP).append(N);

            sb.append(BT04Package.getBatteryLevel()).append(SEP);
            sb.append(BT04Package.getTemperature()).append(SEP).append(N);

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
        List<BT04Package> BT04PackageList = broadcastEvent.getBT04PackageList();

        sb.append(broadcastEvent.getGatewayId()).append(SEP);
        sb.append(timestamp).append(SEP);
        if (location != null) {
            sb.append(location.getLatitude()).append(SEP);
            sb.append(location.getLongitude()).append(SEP);
            sb.append(location.getAltitude()).append(SEP);
            sb.append(location.getAccuracy()).append(SEP);
            sb.append(location.getSpeed()).append(SEP).append(N);
        } else {
            sb.append(0).append(SEP);
            sb.append(0).append(SEP);
            sb.append(0).append(SEP);
            sb.append(0).append(SEP);
            sb.append(0).append(SEP).append(N);
        }

        for (BT04Package BT04Package : BT04PackageList) {
            sb.append(BT04Package.getSerialNumber()).append(SEP);
            sb.append(BT04Package.getName()).append(SEP);
            sb.append(BT04Package.getTemperature()).append(SEP);
            sb.append(BT04Package.getHumidity()).append(SEP);
            sb.append(BT04Package.getRssi()).append(SEP);
            sb.append(BT04Package.getDistance()).append(SEP);
            sb.append(BT04Package.getBatteryLevel()).append(SEP);
            sb.append(BT04Package.getTimestamp()).append(SEP);
            sb.append(BT04Package.getModel()).append(SEP).append(N);
        }
        return sb.toString();
    }

    public static String timeOldPeriod(long timestamp) {
        long now = (new Date()).getTime();
        long period = (now - timestamp) /1000;

        return period + "seconds";
    }


    public static String formatDate(long timestamp, String format, TimeZone tz) {
        Date date = new Date(timestamp);
        return formatDate(date, format, tz);
    }

    public static String formatDate(long timestamp, String format, String tz) {
        Date date = new Date(timestamp);
        return formatDate(date, format, TimeZone.getTimeZone(tz));
    }

    private static String formatDate(Date date, String format) {
        return formatDate(date, format, null);
    }

    private static String formatDate(Date date, String format, TimeZone tz) {
        if (TextUtils.isEmpty(format)) {
            format = "yyyy/MM/dd HH:mm:ss";
        }
        if (tz == null) {
            tz = TimeZone.getTimeZone(AppConfig.DEFAULT_TIMEZONE_STR);
        }
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
        sdf.setTimeZone(tz);
        return sdf.format(date);
    }
}
