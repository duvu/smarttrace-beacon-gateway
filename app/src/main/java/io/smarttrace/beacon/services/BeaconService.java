package io.smarttrace.beacon.services;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
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

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.service.ScanJobScheduler;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.objectbox.Box;
import io.smarttrace.beacon.AppConfig;
import io.smarttrace.beacon.Logger;
import io.smarttrace.beacon.MyApplication;
import io.smarttrace.beacon.R;
import io.smarttrace.beacon.Systems;
import io.smarttrace.beacon.model.BT04Package;
import io.smarttrace.beacon.model.BroadcastEvent;
import io.smarttrace.beacon.model.CellTower;
import io.smarttrace.beacon.model.DataLogger;
import io.smarttrace.beacon.model.ExitEvent;
import io.smarttrace.beacon.net.DataUtil;
import io.smarttrace.beacon.net.Http;
import io.smarttrace.beacon.ui.MainActivity;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class BeaconService extends Service implements BeaconConsumer, BootstrapNotifier{
    private static final String CHANNEL_ID = "MySmarttraceChannelId";

    private BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);
    private RegionBootstrap regionBootstrap;
    AlarmManager nextPointAlarmManager;
    Location lastKnownLocation;
    Map<String, BT04Package> deviceMap = new HashMap<>();

    private LocationManager gpsLocationManager;
    private LocationManager passiveLocationManager;
    private LocationManager towerLocationManager;
    private GeneralLocationListener2 gpsLocationListener;
    private GeneralLocationListener2 towerLocationListener;
    private GeneralLocationListener2 passiveLocationListener;
    private TelephonyManager telephonyManager;

    private boolean isDataExisted = false;

    private final IBinder binder = new DataBinder();

    private Region myRegion = new Region("myRangingUniqueId", null, null, null);

    Box<DataLogger> dataLoggerBox;
    private final Handler handler = new Handler();
    public BeaconService() {
    }
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        Logger.d("[BeaconService] onCreated");
        nextPointAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        regionBootstrap = new RegionBootstrap(this, myRegion);
        beaconManager.bind(this);
        dataLoggerBox = ((MyApplication) getApplication()).getBoxStore().boxFor(DataLogger.class);

        NotificationUtil.init(this.getApplicationContext());
        startForeground();
        registerEventBus();
    }

    @Override
    public void onDestroy() {
        Logger.i("Destroying by OS");
        stopGpsManager();
        stopBLEScan(true);
        unregisterEventBus();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handle();
        return START_STICKY;
    }

    private void handle() {
        Logger.i("[starting scan ble and location] ...");
        startGpsManager();
        startBLEScan();
        startAbsoluteTimer();
    }

    private void setAlarmForNextPoint() {
        Logger.i("[### Waiting for next point in " + getInterval() + " milliseconds] ...");
        Intent intent = new Intent(this, BeaconService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
        nextPointAlarmManager.cancel(pendingIntent);

        if (Systems.isDozing(this)) {
            setExactAndAllowWhileIdle(pendingIntent);
        } else {
            nextPointAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + getInterval(), pendingIntent);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void setExactAndAllowWhileIdle(PendingIntent pendingIntent) {
        nextPointAlarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + getInterval(), pendingIntent);
    }

    private long getInterval() {
        if (!isDataExisted || AppConfig.DEBUG_ENABLED) {
            return AppConfig.UPDATE_INTERVAL_START;
        } else {
            return AppConfig.UPDATE_INTERVAL;
        }
    }

    private void startBLEScan() {
        beaconManager.setBackgroundMode(false);
        syncSettingsToService();
    }

    private void stopBLEScan(boolean exit) {
        if (exit) {
            beaconManager.unbind(this);
        } else {
            beaconManager.setBackgroundMode(true);
        }
    }

    private void syncSettingsToService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ScanJobScheduler.getInstance().applySettingsToScheduledJob(this, beaconManager);
        }
    }

    private void startGpsManager() {

        if (gpsLocationListener == null) {
            gpsLocationListener = new GeneralLocationListener2(this, "GPS");
        }

        if (towerLocationListener == null) {
            towerLocationListener = new GeneralLocationListener2(this, "CELL");
        }

        gpsLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        towerLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {

            if (gpsLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                gpsLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, getInterval(), AppConfig.LOCATION_PROVIDERS_MIN_REFRESH_DISTANCE, gpsLocationListener);
            }

            if (towerLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                towerLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, getInterval() / 4, AppConfig.LOCATION_PROVIDERS_MIN_REFRESH_DISTANCE, towerLocationListener);
            }
        }
    }

    private void stopGpsManager() {
        if (towerLocationListener != null) {
            towerLocationManager.removeUpdates(towerLocationListener);
        }
        if (gpsLocationListener != null) {
            gpsLocationManager.removeUpdates(gpsLocationListener);
        }
    }

    private void startAbsoluteTimer() {
        handler.postDelayed(stopManagerRunnable, AppConfig.UPDATE_PERIOD);
    }

    private void stopAbsoluteTimer() {
        handler.removeCallbacks(stopManagerRunnable);
    }

    private Runnable stopManagerRunnable = new Runnable() {
        @Override
        public void run() {
            Logger.i("[TIME-OUT] resetting ...");
            stopManagerAndResetAlarm();
        }
    };

    private void stopManagerAndResetAlarm() {
        Logger.i("[--] RESET");
        stopBLEScan(false);
        stopGpsManager();
        stopAbsoluteTimer();
        broadcastData();
        setAlarmForNextPoint();
    }

    private void broadcastData() {
        Logger.d("broadcasting data");
        List<CellTower> cellTowerList = getAllCellInfo();
//        for (CellTower tower: cellTowerList) {
//            Logger.d("Tower - Cid: " + tower.getCid());
//            Logger.d("Tower - Lac: " + tower.getLac());
//            Logger.d("Tower - Mcc: " + tower.getMcc());
//            Logger.d("Tower - Mnc: " + tower.getMnc());
//        }

            isDataExisted = true;
            List<BT04Package> BT04PackageList = getDataPackageList();
            BroadcastEvent event = new BroadcastEvent();
            event.setBT04PackageList(getDataPackageList());
            event.setLocation(lastKnownLocation);
            event.setGatewayId(getGatewayId());
            event.setCellTowerList(cellTowerList);

            for (BT04Package bt04: BT04PackageList) {
                Notification notification = NotificationUtil.createNotification(CHANNEL_ID,
                        bt04.getSerialNumber() + "(" + bt04.getModelStringShort() + ")",
                        "Temperature: " + bt04.getTemperature() + "Humidity: " + bt04.getHumidity() + "Distance: " + bt04.getDistanceString());
                NotificationUtil.notify(Integer.parseInt(bt04.getSerialNumber()), notification);
            }
            //old protocol
//            List<String> stringList = DataUtil.formatData1(event);

//            for (String str : stringList) {
//                Logger.d("[Http-Old]: " + str);
//                Http.getIntance().post(AppConfig.BACKEND_URL_BT04, str, new Callback() {
//                    @Override
//                    public void onFailure(Call call, IOException e) {
//                        Logger.d("[Http-Old] failed " + e.getMessage());
//                    }
//
//                    @Override
//                    public void onResponse(Call call, Response response) throws IOException {
//                        Logger.d("[Http-Old] success");
//                    }
//                });
//            }

            //send data bundle - new protocol
            Logger.d(DataUtil.formatData(event));
            Http.getIntance().post(AppConfig.BACKEND_URL_BT04_NEW, DataUtil.formatData(event), new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Logger.d("[Http] failed " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Logger.d("[Http] success " + response.toString());
                }
            });

            EventBus.getDefault().removeStickyEvent(BroadcastEvent.class);
            EventBus.getDefault().postSticky(event);
    }

    private List<BT04Package> getDataPackageList() {
        return new ArrayList<>(deviceMap.values());
    }

    public void onLocationChanged(Location newLocation) {
        Logger.i("[BeaconService] updating location ..." + newLocation.getLatitude() + "/" + newLocation.getLongitude());
        if (lastKnownLocation == null) {
            lastKnownLocation = newLocation;
        } else {
            if (newLocation.getTime() - lastKnownLocation.getTime() > AppConfig.LAST_LOCATION_MAX_AGE) {
                lastKnownLocation = newLocation;
            } else if (newLocation.hasAccuracy() && newLocation.getAccuracy() < lastKnownLocation.getAccuracy()) {
                lastKnownLocation = newLocation;
            }
        }
    }

    private void startForeground() {
        Notification notification = NotificationUtil.createNotification(CHANNEL_ID);
        notification.contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
        startForeground(R.string.smarttrace_io, notification);
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    for (Beacon beacon : beacons) {
                        BT04Package bt04 = deviceMap.get(beacon.getBluetoothAddress());
                        if (bt04 == null) {
                            bt04 = BT04Package.fromBeacon(beacon);
                        } else {
                            bt04.updateFromBeacon(beacon);
                        }
                        deviceMap.put(beacon.getBluetoothAddress(), bt04);
                    }
                }
            }

        });
        try {
            beaconManager.startRangingBeaconsInRegion(myRegion);
        } catch (RemoteException e) {   }

    }

    @SuppressLint("MissingPermission")
    private String getGatewayId() {
        if (TextUtils.isEmpty(AppConfig.GATEWAY_ID)) {
            if (telephonyManager == null) {
                telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (telephonyManager.getPhoneType() ==  TelephonyManager.PHONE_TYPE_CDMA) {
                    AppConfig.GATEWAY_ID = telephonyManager.getMeid();
                } else {
                    // GSM
                    AppConfig.GATEWAY_ID = telephonyManager.getImei();
                }
            } else {
                AppConfig.GATEWAY_ID = telephonyManager.getDeviceId();
            }
        }
        return AppConfig.GATEWAY_ID;
    }

    @SuppressLint("MissingPermission")
    private List<CellTower> getAllCellInfo() {
        Logger.d("getAllCellInfo");
        if (telephonyManager == null) {
            telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        }
        List<CellTower> cellTowerList = new ArrayList<>();

        String networkOperator = telephonyManager.getNetworkOperator();
        int mcc = 0;
        int mnc = 0;
        if (networkOperator != null && networkOperator.length() >=3) {
            mcc = Integer.parseInt(networkOperator.substring(0, 3));
            mnc = Integer.parseInt(networkOperator.substring(3));
        }

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            List<NeighboringCellInfo> neighboringCellInfoList = telephonyManager.getNeighboringCellInfo();
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
            List<CellInfo> cellInfos = telephonyManager.getAllCellInfo();
            Logger.d("CellinfosList: " + cellInfos.size());
            for (CellInfo info: cellInfos) {
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
        return cellTowerList;
    }


    // EventBus
    private void registerEventBus() {
        EventBus.getDefault().register(this);
    }

    private void unregisterEventBus(){
        try {
            EventBus.getDefault().unregister(this);
        } catch (Throwable t){
            //this may crash if registration did not go through. just be safe
        }
    }

    @Subscribe
    public void onExitEvent(ExitEvent exitEvent) {
        stopForeground(true);
        stopSelf();
    }
    @Override
    public void didEnterRegion(Region region) {
        //Logger.d("[+] didEnterRegion");
    }

    @Override
    public void didExitRegion(Region region) {
        //Logger.d("[+] didExitRegion");
    }

    @Override
    public void didDetermineStateForRegion(int i, Region region) {
        //Logger.d("[+] didDetermineStateForRegion");
    }

    //-- class DataBinder --
    public class DataBinder extends Binder {
        public BeaconService getService() {
            return BeaconService.this;
        }
    }
}
