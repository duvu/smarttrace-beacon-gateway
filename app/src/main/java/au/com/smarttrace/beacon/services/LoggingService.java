package au.com.smarttrace.beacon.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;

import com.TZONE.Bluetooth.BLE;
import com.TZONE.Bluetooth.ILocalBluetoothCallBack;
import com.TZONE.Bluetooth.Temperature.BroadcastService;
import com.TZONE.Bluetooth.Temperature.Model.Device;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import au.com.smarttrace.beacon.AppConfig;
import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.protocol.Net;
import au.com.smarttrace.beacon.ui.MainActivity;
import au.com.smarttrace.beacon.R;
import au.com.smarttrace.beacon.Systems;
import au.com.smarttrace.beacon.model.ForeGroundEvent;
import au.com.smarttrace.beacon.model.AdvancedDevice;

public class LoggingService extends Service {
    private final IBinder binder = new DataBinder();
    AlarmManager nextPointAlarmManager;
    Location lastKnownLocation;
    private Handler handler = new Handler();

    private LocationManager gpsLocationManager;
    private LocationManager passiveLocationManager;
    private LocationManager towerLocationManager;
    private GeneralLocationListener gpsLocationListener;
    private GeneralLocationListener towerLocationListener;
    private GeneralLocationListener passiveLocationListener;
    private TelephonyManager telephonyManager;

    //-- Bluetooth
    private BluetoothAdapter bluetoothAdapter;
    private BroadcastService broadcastService;
    private List<Device> deviceList = new ArrayList<>();
    private AdvancedDevice advancedDevice;
    private BluetoothManager bluetoothManager;
    private NotificationManager mNM;

    private static final String CHANNEL_ID = "MySmarttraceChannelId";

    private String imei;

    public LoggingService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        Logger.d("[LoggingService] onCreated");
        nextPointAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createNotificationChannel();
        registerEventBus();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mChannel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this

            mNM.createNotificationChannel(mChannel);
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        handleIntent(intent);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Logger.i("Destroying by OS");
        stopManagerAndResetAlarm();
        unregisterEventBus();
        super.onDestroy();
    }

    private void handleIntent(Intent intent) {
        startGpsManager();
        startBluetoothScan2();
    }

    @SuppressLint("MissingPermission")
    @TargetApi(Build.VERSION_CODES.M)
    private void setAlarmForNextPoint() {
        //send data before set next alarm
        if (advancedDevice == null) {
            advancedDevice = new AdvancedDevice();
        }
        advancedDevice.setDeviceList(deviceList);
        getImei();

        advancedDevice.setImei(imei);
        if (lastKnownLocation != null) {
            advancedDevice.setLocation(lastKnownLocation);
        }

        try {
            Net.postBundle(advancedDevice);
        } catch (IOException e) {
            Logger.d("[PostData] posting data" + e.getMessage());
        }

        EventBus.getDefault().postSticky(advancedDevice);

        //---

        Intent intent = new Intent(this, LoggingService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, 0);
        nextPointAlarmManager.cancel(pendingIntent);

        Logger.i("NEXT-POINT ...");
        if (Systems.isDozing(this)) {
            nextPointAlarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 10 * 1000, pendingIntent);
        } else {
            nextPointAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 10 * 1000, pendingIntent);
        }
    }

    private void startGpsManager() {

        if (gpsLocationListener == null) {
            gpsLocationListener = new GeneralLocationListener(this, "GPS");
        }

        if (towerLocationListener == null) {
            towerLocationListener = new GeneralLocationListener(this, "CELL");
        }

        gpsLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        towerLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || (
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            gpsLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, AppConfig.UPDATE_INTERVAL, AppConfig.LOCATION_PROVIDERS_MIN_REFRESH_DISTANCE, gpsLocationListener);

            towerLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, AppConfig.UPDATE_INTERVAL / 4, AppConfig.LOCATION_PROVIDERS_MIN_REFRESH_DISTANCE, towerLocationListener);

            startAbsoluteTimer();
        }

    }

    private void startAbsoluteTimer() {
        handler.postDelayed(stopManagerRunnable, 10 * 1000);
    }

    private void stopAbsoluteTimer() {
        handler.removeCallbacks(stopManagerRunnable);
    }

    private Runnable stopManagerRunnable = new Runnable() {
        @Override
        public void run() {
            Logger.i("[TIME-OUT] resetting ...");
            stopBluetoothScan();
            stopManagerAndResetAlarm();
        }
    };

    private void stopManagerAndResetAlarm() {
        Logger.i("[--] RESET");
        stopAbsoluteTimer();
        setAlarmForNextPoint();
    }

    @Override
    public void onLowMemory() {
        Logger.d("Android is low on memory!");
        super.onLowMemory();
    }

    private void setNewLocation(Location newLocation) {
        Logger.i("[" + newLocation.getProvider() + "] Fix found!. Accuracy: [" + newLocation.getAccuracy() + "]");

        if (lastKnownLocation == null) {

            lastKnownLocation = newLocation;
        } else {
            if (newLocation.getTime() - lastKnownLocation.getTime() > AppConfig.LAST_LOCATION_MAX_AGE) {
                //Last registered fix was set more that 2 minutes ago. It's older so must be updated!
                lastKnownLocation = newLocation;
            } else if (newLocation.hasAccuracy() && (newLocation.getAccuracy() < lastKnownLocation.getAccuracy())) {
                //New location is more accurate than the previous one. Win!
                lastKnownLocation = newLocation;
            }
        }
    }

    public void onLocationChanged(Location loc) {
        // update location
        Logger.i("[LoggingService] updating location ...");
        setNewLocation(loc);
    }

    //-- class DataBinder --
    public class DataBinder extends Binder {
        public LoggingService getService() {
            return LoggingService.this;
        }
    }

    private void startBluetoothScan2() {
        try {
            if (broadcastService == null) {
                broadcastService = new BroadcastService();
            }
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
            if (broadcastService.Init(bluetoothAdapter, bluetoothCallback)) {
                broadcastService.StartScan();
            }
        } catch (Exception exi) {
            Logger.e("[Exception] internal", exi);
        }
    }

    private void stopBluetoothScan() {
        try {
            if (broadcastService != null) {
                broadcastService.StopScan();
            }
        } catch (Exception ex) {
            Logger.e("[Exception]", ex);
        }
    }

    private void addOrUpdate(BLE ble) {
        try {
            Device device = new Device();
            device.fromScanData(ble);
            if (device.SN == null || device.SN.length() != 8) {
                return;
            }

            boolean flag = true;
            for (int i = 0; i < deviceList.size(); i++) {
                Device ditem = deviceList.get(i);
                if (ditem.SN.equals(device.SN)) {
                    flag = false;
                    //update data
                    ditem.fromScanData(ble);
                }
            }
            if (flag) {
                deviceList.add(device);
            }
            //Logger.i("SN:" + device.SN +" Temperature:" + (device.Temperature != - 1000 ? device.Temperature : "--") +"℃  Humidity:" + (device.Humidity != -1000 ? device.Humidity : "--") + "% Battery:"+device.Battery+"%");

//            Date now = new Date();
//            long totalTime = now.getTime() - lastUpdateTime.getTime();
//
//            if (totalTime > 10000) {
//                // build data
//                for (int i = 0; i < deviceList.size(); i++) {
//                    Logger.i("" + (i+1) + "、SN:" + deviceList.get(i).SN +" Temperature:" + (deviceList.get(i).Temperature != - 1000 ? deviceList.get(i).Temperature : "--") +"℃  Humidity:" + (deviceList.get(i).Humidity != -1000 ? deviceList.get(i).Humidity : "--") + "% Battery:"+deviceList.get(i).Battery+"%");
//                }
//            }
        } catch (Exception ex) {
            Logger.i("[Exception] " + ex.getMessage());
        }
    }

    public ILocalBluetoothCallBack bluetoothCallback = new ILocalBluetoothCallBack() {
        @Override
        public void OnEntered(BLE ble) {
            try {
                addOrUpdate(ble);
            } catch (Exception ignored) {
            }
        }

        @Override
        public void OnUpdate(BLE ble) {
            try {
                addOrUpdate(ble);
            } catch (Exception ignored) {
            }
        }

        @Override
        public void OnExited(BLE ble) {

        }

        @Override
        public void OnScanComplete() {
            Logger.d("[Bluetooth] scan completed");
            stopManagerAndResetAlarm();
        }
    };

    private void getImei() {
        if (imei == null) {
            TelephonyManager c = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            imei = c.getDeviceId();
        }
    }

    // EventBus
    @Subscribe
    public void onForegroundEvent(ForeGroundEvent fge) {
        if (fge.isForeground()) {
            //start this service in foreground
            CharSequence text = getText(R.string.local_service_started);
            // The PendingIntent to launch our activity if the user selects this notification
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.notification)  // the status icon
                    .setTicker(text)  // the status text
                    //.setWhen(System.currentTimeMillis())  // the time stamp
                    .setContentTitle(getText(R.string.local_service_label))  // the label of the entry
                    .setContentText(text)  // the contents of the entry
                    .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                    .setOngoing(true)
                    .build();
            startForeground(R.string.local_service_started, notification);
        } else {
            //start this service in background
            stopForeground(true);
        }
    }
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
}
