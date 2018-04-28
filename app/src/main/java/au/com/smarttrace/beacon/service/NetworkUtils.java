package au.com.smarttrace.beacon.service;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.PowerManager;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import au.com.smarttrace.beacon.AppConfig;
import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.model.BroadcastEvent;
import au.com.smarttrace.beacon.model.CellTower;

public class NetworkUtils {
    private static ConnectivityManager connectivityManager;
    private static TelephonyManager mTelephonyManager;
    public static void init(Context context) {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    public static boolean isConnected() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public static boolean isWifi() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI);
    }

    public static boolean isMobile() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE);
    }

    public static boolean isInternetAvailable() {
        try {
            InetAddress ipAddr = InetAddress.getByName("smarttrace.com.au");
            return !ipAddr.toString().equals("");
        } catch (Exception e) {
            Logger.e("[Network] + ", e);
            return false;
        }
    }

    /**
     * Returns true if the device is in Doze/Idle mode. Should be called before checking the network connection because
     * the ConnectionManager may report the device is connected when it isn't during Idle mode.
     * https://github.com/yigit/android-priority-jobqueue/blob/master/jobqueue/src/main/java/com/path/android/jobqueue/network/NetworkUtilImpl.java#L60
     */
    @TargetApi(23)
    public static boolean isDozing(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return powerManager.isDeviceIdleMode() &&
                    !powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
        } else {
            return false;
        }
    }

    @SuppressLint("MissingPermission")
    public static String getGatewayId() {
        if (TextUtils.isEmpty(AppConfig.GATEWAY_ID) || "unknownImei".equalsIgnoreCase(AppConfig.GATEWAY_ID)) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (mTelephonyManager.getPhoneType() ==  TelephonyManager.PHONE_TYPE_CDMA) {
                        AppConfig.GATEWAY_ID = mTelephonyManager.getMeid();
                    } else {
                        // GSM
                        AppConfig.GATEWAY_ID = mTelephonyManager.getImei();
                    }
                } else {
                    AppConfig.GATEWAY_ID = mTelephonyManager.getDeviceId();
                }
            } catch (Exception e) {
                AppConfig.GATEWAY_ID = "unknownImei";
            }
        }
        return AppConfig.GATEWAY_ID;
    }

    @SuppressLint("MissingPermission")
    public static List<CellTower> getAllCellInfo() {
        Logger.i("getAllCellInfo");
        List<CellTower> cellTowerList = new ArrayList<>();

        String networkOperator = mTelephonyManager.getNetworkOperator();
        int mcc = 0;
        int mnc = 0;
        if (networkOperator != null && networkOperator.length() >=3) {
            mcc = Integer.parseInt(networkOperator.substring(0, 3));
            mnc = Integer.parseInt(networkOperator.substring(3));
        }

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            List<NeighboringCellInfo> neighboringCellInfoList = mTelephonyManager.getNeighboringCellInfo();
            for (NeighboringCellInfo info : neighboringCellInfoList) {
                CellTower cellTower = new CellTower();
                cellTower.setLac(info.getLac());
                cellTower.setCid(info.getCid());
                cellTower.setMcc(mcc);
                cellTower.setMnc(mnc);
                cellTower.setRxlev(info.getRssi());
                cellTowerList.add(cellTower);
            }
        } else {
            List<CellInfo> cellInfos = mTelephonyManager.getAllCellInfo();
            if (cellInfos != null) {
                Logger.i("CellinfosList: " + cellInfos.size());
                for (CellInfo info : cellInfos) {
                    CellTower cellTower = new CellTower();

                    if (info instanceof CellInfoGsm) {
                        CellSignalStrengthGsm gsm = ((CellInfoGsm) info).getCellSignalStrength();
                        CellIdentityGsm identityGsm = ((CellInfoGsm) info).getCellIdentity();
                        cellTower.setLac(identityGsm.getLac());
                        cellTower.setCid(identityGsm.getCid());
                        cellTower.setMcc(identityGsm.getMcc());
                        cellTower.setMnc(identityGsm.getMnc());
                        cellTower.setRxlev(gsm.getAsuLevel());
                        cellTowerList.add(cellTower);

                    } else if (info instanceof CellInfoLte) {
                        CellSignalStrengthLte lte = ((CellInfoLte) info).getCellSignalStrength();
                        CellIdentityLte identityLte = ((CellInfoLte) info).getCellIdentity();
                        cellTower.setLac(identityLte.getTac());
                        cellTower.setCid(identityLte.getCi());
                        cellTower.setMcc(identityLte.getMcc());
                        cellTower.setMnc(identityLte.getMnc());
                        cellTower.setRxlev(lte.getAsuLevel());
                        cellTowerList.add(cellTower);
                    } else if (info instanceof CellInfoWcdma) {
                        CellSignalStrengthWcdma wcdma = ((CellInfoWcdma) info).getCellSignalStrength();
                        CellIdentityWcdma identityWcdma = ((CellInfoWcdma) info).getCellIdentity();

                        cellTower.setLac(identityWcdma.getLac());
                        cellTower.setCid(identityWcdma.getCid());
                        cellTower.setMnc(identityWcdma.getMnc());
                        cellTower.setMcc(identityWcdma.getMcc());
                        cellTower.setRxlev(wcdma.getAsuLevel());
                        cellTowerList.add(cellTower);
                    } else if (info instanceof CellInfoCdma) {
                        CellSignalStrengthCdma cdma = ((CellInfoCdma) info).getCellSignalStrength();
                        CellIdentityCdma identityCdma = ((CellInfoCdma) info).getCellIdentity();
                        //
                    }
                }
            }
        }
        return cellTowerList;
    }
}
