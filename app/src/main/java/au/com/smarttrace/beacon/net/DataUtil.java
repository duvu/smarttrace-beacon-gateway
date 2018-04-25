package au.com.smarttrace.beacon.net;

import android.location.Location;
import android.text.TextUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import au.com.smarttrace.beacon.AppConfig;
import au.com.smarttrace.beacon.AppContants;
import au.com.smarttrace.beacon.model.BeaconPackage;
import au.com.smarttrace.beacon.model.BroadcastEvent;
import au.com.smarttrace.beacon.model.CellTower;

/**
 * Created by beou on 3/9/18.
 */

public class DataUtil {
    public static List<String> formatData1(BroadcastEvent broadcastEvent) {
        List<BeaconPackage> BeaconPackageList = broadcastEvent.getBeaconPackageList();
        Date curremtDate = (new Date());
        List<String> stringList = new ArrayList<>();
        for (BeaconPackage BeaconPackage : BeaconPackageList) {
            StringBuffer sb = new StringBuffer();

            Location location = broadcastEvent.getLocation();


            //sb.append(broadcastEvent.getGatewayId() + BeaconPackage.getSerialNumber()).append(SEP);
            sb.append(BeaconPackage.getSerialNumber()).append(AppContants.SEPARATE);
            sb.append("AUT").append(AppContants.SEPARATE); //type
            sb.append(formatDate(curremtDate, null)).append(AppContants.SEPARATE).append(AppContants.N);

            sb.append(BeaconPackage.getBatteryLevel()).append(AppContants.SEPARATE);
            sb.append(BeaconPackage.getTemperature()).append(AppContants.SEPARATE).append(AppContants.N);

            List<CellTower> cellTowerList = broadcastEvent.getCellTowerList();
            for (CellTower cell : cellTowerList) {
                sb.append(cell.getMcc()).append(AppContants.SEPARATE);
                sb.append(cell.getMnc()).append(AppContants.SEPARATE);
                sb.append(cell.getLac()).append(AppContants.SEPARATE);
                sb.append(cell.getCid()).append(AppContants.SEPARATE);
                sb.append(cell.getRxlev()).append(AppContants.SEPARATE).append(AppContants.N);
            }
            stringList.add(sb.toString());
        }

        return stringList;
    }


    public static String formatData(BroadcastEvent broadcastEvent) {
        StringBuffer sb = new StringBuffer();
        // phone-imei|epoch-time|latitude|longitude|altitude|accuracy|speedKPH|VERSION|VERSION_CODE|<\n>
        // SN|Name|Temperature|Humidity|RSSI|Distance|battery|LastScannedTime|HardwareModel|<\n>
        // SN|Name|Temperature|Humidity|RSSI|Distance|battery|LastScannedTime|HardwareModel|<\n>
        // SN|Name|Temperature|Humidity|RSSI|Distance|battery|LastScannedTime|HardwareModel|<\n>
        // SN|Name|Temperature|Humidity|RSSI|Distance|battery|LastScannedTime|HardwareModel|<\n>
        // SN|Name|Temperature|Humidity|RSSI|Distance|battery|LastScannedTime|HardwareModel|<\n>
        Long timestamp = (new Date()).getTime();
        Location location = broadcastEvent.getLocation();
        List<BeaconPackage> BeaconPackageList = broadcastEvent.getBeaconPackageList();

        sb.append(broadcastEvent.getGatewayId()).append(AppContants.SEPARATE);
        sb.append(timestamp).append(AppContants.SEPARATE);
        if (location != null) {
            sb.append(location.getLatitude()).append(AppContants.SEPARATE);
            sb.append(location.getLongitude()).append(AppContants.SEPARATE);
            sb.append(location.getAltitude()).append(AppContants.SEPARATE);
            sb.append(location.getAccuracy()).append(AppContants.SEPARATE);
            sb.append(location.getSpeed()).append(AppContants.SEPARATE);
            sb.append(AppConfig.getVersionName()).append(AppContants.SEPARATE);
            sb.append(AppConfig.getVersionCode()).append(AppContants.SEPARATE);
            sb.append(AppContants.N);
        } else {
            sb.append(0).append(AppContants.SEPARATE);
            sb.append(0).append(AppContants.SEPARATE);
            sb.append(0).append(AppContants.SEPARATE);
            sb.append(0).append(AppContants.SEPARATE);
            sb.append(0).append(AppContants.SEPARATE);
            sb.append(AppConfig.getVersionName()).append(AppContants.SEPARATE);
            sb.append(AppConfig.getVersionCode()).append(AppContants.SEPARATE);
            sb.append(AppContants.N);
        }

        for (BeaconPackage data : BeaconPackageList) {
            sb.append(data.getSerialNumberString()).append(AppContants.SEPARATE);
            sb.append(data.getName()).append(AppContants.SEPARATE);
            sb.append(data.getTemperature()).append(AppContants.SEPARATE);
            sb.append(data.getHumidity()).append(AppContants.SEPARATE);
            sb.append(data.getRssi()).append(AppContants.SEPARATE);
            sb.append(data.getDistance()).append(AppContants.SEPARATE);
            sb.append(battPercentToVolt(data.getPhoneBatteryLevel()*100)).append(AppContants.SEPARATE);
            sb.append(data.getTimestamp()).append(AppContants.SEPARATE);
            sb.append(data.getModel()).append(AppContants.SEPARATE).append(AppContants.N);
        }
        return sb.toString();
    }

    public static float battPercentToVolt (float battLevel) {
        //min 3194.3
        //max 4200.0

        if (battLevel <= 1) {
            return 3194;
        } else
        if (battLevel <=2) {
            return 3241;
        } else
        if (battLevel <= 3) {
            return 3288;
        } else if (battLevel <= 4) {
            return 3336;
        } else if (battLevel <= 5) {
            return 3383;
        } else if (battLevel <= 6) {
            return 3430;
        } else if (battLevel <= 7) {
            return 3478;
        } else if (battLevel <= 8) {
            return 3525;
        } else if (battLevel <= 9) {
            return 3572;
        } else if (battLevel <= 10) {
            return 3620;
        } else if (battLevel <= 20) {
            return 3695;
        } else if (battLevel <= 30) {
            return 3770;
        } else if (battLevel <= 40) {
            return 3845;
        } else if (battLevel <= 50) {
            return 3895;
        } else if (battLevel <= 60) {
            return 3925;
        } else if (battLevel <= 70) {
            return 3975;
        } else if (battLevel <= 80) {
            return 4025;
        } else if (battLevel <= 90) {
            return 4075;
        } else if (battLevel <= 95) {
            return 4125;
        } else {
            return 4200;
        }
    }



    public static Date getUserDate(final String strDate, final TimeZone timeZone) {
        if (TextUtils.isEmpty(strDate)) return null;
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        if (timeZone != null) {
            sdf.setTimeZone(timeZone);
        } else sdf.setTimeZone(TimeZone.getDefault());
        try {
            return sdf.parse(strDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
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
            tz = TimeZone.getTimeZone(AppConfig.TIMEZONE_STR);
        }
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
        sdf.setTimeZone(tz);
        return sdf.format(date);
    }
}
