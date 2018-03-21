package au.com.smarttrace.beacon.services;

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
import android.telephony.TelephonyManager;

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

import au.com.smarttrace.beacon.AppConfig;
import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.MyApplication;
import au.com.smarttrace.beacon.R;
import au.com.smarttrace.beacon.Systems;
import au.com.smarttrace.beacon.model.BroadcastEvent;
import au.com.smarttrace.beacon.model.DataLogger;
import au.com.smarttrace.beacon.model.Device;
import au.com.smarttrace.beacon.model.ExitEvent;
import au.com.smarttrace.beacon.net.DataUtil;
import au.com.smarttrace.beacon.net.Http;
import au.com.smarttrace.beacon.ui.MainActivity;
import io.objectbox.Box;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static au.com.smarttrace.beacon.AppConfig.UPDATE_INTERVAL;

public class BeaconService extends Service implements BeaconConsumer, BootstrapNotifier{
    private static final String CHANNEL_ID = "MySmarttraceChannelId";

    private BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);
    private RegionBootstrap regionBootstrap;
    AlarmManager nextPointAlarmManager;
    Location lastKnownLocation;
    Map<String, Device> deviceMap = new HashMap<>();

    private LocationManager gpsLocationManager;
    private LocationManager passiveLocationManager;
    private LocationManager towerLocationManager;
    private GeneralLocationListener2 gpsLocationListener;
    private GeneralLocationListener2 towerLocationListener;
    private GeneralLocationListener2 passiveLocationListener;
    private TelephonyManager telephonyManager;

    private final IBinder binder = new DataBinder();

    private Region myRegion = new Region("myRangingUniqueId", null, null, null);

    Box<DataLogger> dataLoggerBox;
    Notification notification;
    private Handler handler = new Handler();
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
        //--
        dataLoggerBox = ((MyApplication) getApplication()).getBoxStore().boxFor(DataLogger.class);

        //init
        notification = Util.createNotification(this.getApplicationContext(), CHANNEL_ID);
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
        return START_NOT_STICKY;
    }

    private void handle() {
        Logger.i("[starting scan ble and location] ...");
        startGpsManager();
        startBLEScan();
        startAbsoluteTimer();
    }

    private void setAlarmForNextPoint() {
        Logger.i("[### Waiting for next point in " + AppConfig.UPDATE_INTERVAL + " milliseconds] ...");
        Intent intent = new Intent(this, BeaconService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
        nextPointAlarmManager.cancel(pendingIntent);

        if (Systems.isDozing(this)) {
            setExactAndAllowWhileIdle(pendingIntent);
        } else {
            nextPointAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + AppConfig.UPDATE_INTERVAL, pendingIntent);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void setExactAndAllowWhileIdle(PendingIntent pendingIntent) {
        nextPointAlarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + AppConfig.UPDATE_INTERVAL, pendingIntent);
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
            gpsLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_INTERVAL, AppConfig.LOCATION_PROVIDERS_MIN_REFRESH_DISTANCE, gpsLocationListener);

            towerLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, UPDATE_INTERVAL / 4, AppConfig.LOCATION_PROVIDERS_MIN_REFRESH_DISTANCE, towerLocationListener);
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
        BroadcastEvent event = new BroadcastEvent();
        event.setDeviceList(getDeviceList());
        event.setLocation(lastKnownLocation);
        event.setGatewayId(AppConfig.GATEWAY_ID);
        Http.getIntance().post(AppConfig.BACKEND_URL_BT04, DataUtil.formatData(event), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Logger.d("[Http] failed " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Logger.d("[Http] success " + response.toString());
            }
        });
        EventBus.getDefault().postSticky(event);
    }

    private List<Device> getDeviceList() {
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
        notification.contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
        startForeground(R.string.local_service_started, notification);
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    for (Beacon beacon : beacons) {
                        Device existedDevice = deviceMap.get(beacon.getBluetoothAddress());
                        if (existedDevice != null) {
                            //update
                            existedDevice.updateFromBeacon(beacon);
                            deviceMap.put(beacon.getBluetoothAddress(), existedDevice);
                        } else {
                            deviceMap.put(beacon.getBluetoothAddress(), Device.fromBeacon(beacon));
                        }
                    }
                }
            }

        });
        try {
            beaconManager.startRangingBeaconsInRegion(myRegion);
        } catch (RemoteException e) {   }

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
