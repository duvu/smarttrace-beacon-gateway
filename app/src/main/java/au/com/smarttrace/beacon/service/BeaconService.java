package au.com.smarttrace.beacon.service;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.service.ScanJobScheduler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import au.com.smarttrace.beacon.App;
import au.com.smarttrace.beacon.AppConfig;
import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.R;
import au.com.smarttrace.beacon.SharedPref;
import au.com.smarttrace.beacon.Systems;
import au.com.smarttrace.beacon.db.EventData;
import au.com.smarttrace.beacon.db.Locations;
import au.com.smarttrace.beacon.db.Locations_;
import au.com.smarttrace.beacon.db.PhonePaired;
import au.com.smarttrace.beacon.db.PhonePaired_;
import au.com.smarttrace.beacon.db.SensorData;
import au.com.smarttrace.beacon.model.BeaconPackage;
import au.com.smarttrace.beacon.model.BroadcastEvent;
import au.com.smarttrace.beacon.model.CellTower;
import au.com.smarttrace.beacon.model.ExitEvent;
import au.com.smarttrace.beacon.model.UpdateEvent;
import au.com.smarttrace.beacon.model.WakeUpEvent;
import au.com.smarttrace.beacon.net.DataUtil;
import au.com.smarttrace.beacon.net.WebService;
import au.com.smarttrace.beacon.net.model.LatLng;
import au.com.smarttrace.beacon.service.location.LCallback;
import au.com.smarttrace.beacon.service.location.LServiceWrapper;
import au.com.smarttrace.beacon.ui.MainActivity;
import io.objectbox.Box;
import io.objectbox.query.Query;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static au.com.smarttrace.beacon.AppConfig.NOTIFICATION_ID;

public class BeaconService extends Service implements BeaconConsumer, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String PACKAGE_NAME = "au.com.smarttrace.beacon";
    public static final String EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME + ".started_from_notification";
    public static final String EXTRA_STARTED_FROM_BOOTSTRAP = PACKAGE_NAME + ".started_from_bootstrap";

    public static final String ACTION_BROADCAST = PACKAGE_NAME + ".broadcast";
    public static final String EXTRA_LOCATION = PACKAGE_NAME + ".location";

    public static final String GET_NEXT_POINT = "getnextpoint";


    private Location mLocation;

    private final long start_up_time = System.currentTimeMillis();
    private long last_ble_event;
    private boolean last_changed = false;

    // phone battery
    private float mBatteryLevel = 0.0f;
    private BroadcastReceiver mBatteryLevelReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                mBatteryLevel = 0.0f;
            } else {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                int scal = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0);
                mBatteryLevel = level / (float) scal;
            }
        }
    };
    private BeaconManager mBeaconManager;// = BeaconManager.getInstanceForApplication(this.getApplicationContext());

    private final Map<String, BeaconPackage> deviceMap = new ConcurrentHashMap<>();

    private List<Locations> companyShipmentLocations = null;

    private Box<EventData> eventBox;
    private Box<PhonePaired> pairedBox;
    private Box<Locations> locationsBox;

    private final IBinder mBinder = new LocalBinder();
    private Handler handler;
    private HandlerThread handlerThread = null;
    private String currentToken = null;

    LServiceWrapper locationWrapper;
    private LCallback lCallback;

    AlarmManager nextPointAlarmManager;

    PowerManager mPowerManager;
    public BeaconService() {
        super();
    }

    @Override
    public void onCreate() {
        Logger.i("[BeaconService] onCreated");
        nextPointAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);

        mBeaconManager = BeaconManager.getInstanceForApplication(this.getApplicationContext());
        mBeaconManager.setForegroundBetweenScanPeriod(10*1000);
        mBeaconManager.setForegroundScanPeriod(5*1000);
        mBeaconManager.bind(this);
        mBeaconManager.setBackgroundMode(false);
        syncSettingsToService();

        //--heartbeat
        handlerThread = new HandlerThread(AppConfig.TAG);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        lCallback = new LCallback() {
            @Override
            public void onLocationChanged(Location location) {
                onUpdateLocation(location);
            }
        };

        locationWrapper = LServiceWrapper.instances(this, lCallback);


        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            NotificationChannel channel = new NotificationChannel(getString(R.string.default_notification_channel_id), name, NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationManager.createNotificationChannel(channel);
        }


        //-- init database;
        eventBox = ((App) getApplicationContext()).getBoxStore().boxFor(EventData.class);
        pairedBox = ((App) getApplicationContext()).getBoxStore().boxFor(PhonePaired.class);
        locationsBox = ((App) getApplicationContext()).getBoxStore().boxFor(Locations.class);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        intentFilter.addAction(Intent.ACTION_BATTERY_LOW);
        intentFilter.addAction(Intent.ACTION_BATTERY_OKAY);
        registerReceiver(mBatteryLevelReceiver, intentFilter);

        startForeground(NOTIFICATION_ID, getNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.d("##Service Started: " + SharedPref.getToken());



        currentToken = SharedPref.getToken();

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        //-- load Shipment Location
        loadLocations();
        //checkIfNeedToCreateShipmentOnBoot();
        App.serviceStarted();
        start();
        return START_NOT_STICKY;
    }

    private void startAbsoluteTimer() {
        handler.postDelayed(stopManagerRunnable, AppConfig.SCANNING_TIMEOUT); //working for 30 seconds
    }


    private Runnable stopManagerRunnable = new Runnable() {
        @Override
        public void run() {
            Logger.d("[-->] Absolute timeout reached!");
            stopManagerAndResetAlarm();
        }
    };

    private void stopAbsoluteTimer() {
        handler.removeCallbacks(stopManagerRunnable);
    }

    private void stopManagerAndResetAlarm() {
        Logger.d("[>_] stopManagerAndResetAlarm");
        stopAbsoluteTimer();
        locationWrapper.stopLocationUpdates();
        stopBLE();
        checkIfNewData();
        checkAndCreateShipment(getAll());
        broadcastData();
    }

    @TargetApi(23)
    private void setAlarmForNextPoint() {
        Logger.d("[>_] Set alarm in: " + getNotificationTimeOut() + " seconds");

        Intent i = new Intent(this, BeaconService.class);
        i.putExtra(GET_NEXT_POINT, true);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        nextPointAlarmManager.cancel(pi);

        if(Systems.isDozing(this)){
            //Only invoked once per 15 minutes in doze mode
            Logger.d("Device is dozing, using infrequent alarm");
            nextPointAlarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, getNextPointElapsedRealtime(), pi);
        }
        else {
            nextPointAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, getNextPointElapsedRealtime(), pi);
        }
    }

    private long getNextPointElapsedRealtime() {
        if (mLocation == null || !last_changed) {
            return SystemClock.elapsedRealtime() + 10 * 1000;
        } else
        if (isAtStartLocations() && (System.currentTimeMillis() < (start_up_time + 10*60*1000))) {
            return SystemClock.elapsedRealtime() + 11 * 1000;
        } else {
            return SystemClock.elapsedRealtime() + 10 * 60 * 1000;
        }
    }

    public void start() {
        Logger.d("[>>] Start scanning");
            locationWrapper.startLocationUpdates();
            startBLE();
            startAbsoluteTimer();
    }

    private void startBLE() {
        Logger.d("[>_] startBLE #backgroundMode: " + mBeaconManager.getBackgroundMode());
        toggleBluetoothIfNeed();
        if (mBeaconManager.getBackgroundMode()) {

            if (mBeaconManager.getForegroundBetweenScanPeriod() > 1100) {
                mBeaconManager.setForegroundBetweenScanPeriod(1100);
            }

            mBeaconManager.setBackgroundMode(false);
            try {
                mBeaconManager.updateScanPeriods();
            } catch (RemoteException e) {
                Logger.e("Cannot update period", e);
            }
            syncSettingsToService();
        }
    }

    private void stopBLE() {
        if (!mBeaconManager.getBackgroundMode()) {
            mBeaconManager.setBackgroundMode(true);
            try {
                mBeaconManager.updateScanPeriods();
            } catch (RemoteException e) {
                Logger.e("Cannot update period", e);
            }
            syncSettingsToService();
        }
    }

    private void toggleBluetoothIfNeed() {
        if ((last_ble_event + TimeUnit.MINUTES.toMillis(8)) <= System.currentTimeMillis()) {
            final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.disable();
            }
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    bluetoothAdapter.enable();
                }
            }, TimeUnit.SECONDS.toMillis(5));
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Logger.i("onBind");
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Logger.i("onUnbind");
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Logger.d("[>_] Service is destroying");
        handler.removeCallbacks(null);
        App.serviceEnded();
        unregisterReceiver(mBatteryLevelReceiver);
        locationsBox.closeThreadResources();
        eventBox.closeThreadResources();
        pairedBox.closeThreadResources();
        stopForeground(false);
        if (handlerThread != null) {
            handlerThread.quitSafely();
        }
        mBeaconManager.unbind(this);
    }

    public void broadcastData() {
        Logger.i("[+] broadcasting, isAtStartLocations #" + isAtStartLocations() + " isBoot: " + App.isBoot() + "[>Imei<] " + NetworkUtils.getGatewayId());
        createNotification();

        final List<BeaconPackage> dataList = getDataToUpload();

        //warning if no location:
        if (mLocation == null) {
            Notification notification = new NotificationCompat.Builder(this, getString(R.string.default_notification_channel_id))
                    .setContentTitle("No Location!")
                    .setContentText("No Location Fixed")
                    .setChannelId(getString(R.string.default_notification_channel_id))
                    .setSound(null)
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.notification)
                    .setShowWhen(true)
                    .setColor(Color.GREEN)
                    .setLocalOnly(true)
                    .build();

            NotificationManagerCompat.from(this).notify(new Random().nextInt(), notification);
            setAlarmForNextPoint();
            return;
        }

        if (dataList == null || dataList.size() == 0) {
            Logger.i("[-] no new data");
            setAlarmForNextPoint();
            return; // not update if no event data;
        }

        if (!isAtStartLocations()) {
            App.onBoot(false);
        }
        BroadcastEvent event = new BroadcastEvent();
        try {
            List<CellTower> cellTowerList = NetworkUtils.getAllCellInfo();
            event.setBeaconPackageList(dataList);
            event.setLocation(mLocation);
            event.setGatewayId(NetworkUtils.getGatewayId());
            event.setCellTowerList(cellTowerList);

        } catch (Exception ex) {
            Logger.d("[Exception ...]");
            setAlarmForNextPoint();
        }

        //2. upload new data
        String dataForUpload = DataUtil.formatData(event);
        WebService.sendEvent(dataForUpload, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // save to db
                saveDataToDB(dataList);
                setAlarmForNextPoint();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //try to upload old
                tryToSendOldData();
                setAlarmForNextPoint();
            }
        });
    }

    private void tryToSendOldData() {
            //1. upload old data
            List<EventData> evdtList = eventBox.getAll();
            for (final EventData evdt : evdtList) {
                Logger.i("[*] check: " + evdt.toString());
                WebService.sendEvent(evdt.toString(), new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        //noop
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        // remove data from db;
                        eventBox.remove(evdt);
                    }
                });
            }
    }

    private void saveDataToDB(List<BeaconPackage> lbp) {
        Logger.i("[Offline] Network is offline, going to store data");
        EventData evdt = new EventData();
        evdt.setPhoneImei(NetworkUtils.getGatewayId());
        Long timestamp = (new Date()).getTime();
        evdt.setTimestamp(timestamp);
        if (mLocation != null) {
            evdt.setLatitude(mLocation.getLatitude());
            evdt.setLongitude(mLocation.getLongitude());
            evdt.setAltitude(mLocation.getAltitude());
            evdt.setAccuracy(mLocation.getAccuracy());
            evdt.setSpeedKPH(mLocation.getSpeed());
        } else {
            evdt.setLatitude(0);
            evdt.setLongitude(0);
            evdt.setAltitude(0);
            evdt.setAccuracy(0);
            evdt.setSpeedKPH(0);
        }

        for (BeaconPackage data : lbp) {
            SensorData sd = new SensorData();
            sd.setSerialNumber(data.getSerialNumberString());
            sd.setName(data.getName());
            sd.setTemperature(data.getTemperature());
            sd.setHumidity(data.getHumidity());
            sd.setRssi(data.getRssi());
            sd.setDistance(data.getDistance());
            sd.setBattery(DataUtil.battPercentToVolt(data.getPhoneBatteryLevel()*100));
            sd.setLastScannedTime(data.getTimestamp());
            sd.setHardwareModel(data.getModel());
            evdt.getSensorDataList().add(sd);
        }
        long id = eventBox.put(evdt);
        Logger.i("[+] stored #" + id);
    }

    private List<BeaconPackage> getDataToUpload() {
        List<BeaconPackage> dataList = new ArrayList<>();
        for (Object o : deviceMap.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            BeaconPackage data = (BeaconPackage) entry.getValue();
            //Logger.d("[>_] getDataToUpload #" + data.getSerialNumberString());
            if (isPaired(data) && !data.isShouldCreateOnBoot()) {
                dataList.add(data);
                synchronized (deviceMap) {
                    deviceMap.get(data.getBluetoothAddress()).setShouldUpload(false); // to prevent repeat upload
                }
            }
        }
        return dataList;
    }

    private boolean checkIfNewData() {
        for (Object o : deviceMap.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            String key = (String)entry.getKey();
            BeaconPackage data = (BeaconPackage) entry.getValue();
            Logger.d("[>_] CheckIfNewData #" + data.getSerialNumberString());
            if (isPaired(data) && data.isShouldUpload()) {
                last_changed = true;
                return true;
            }
        }
        Logger.d("[>_] No new data");
        last_changed = false;
        return false;
    }

    private List<BeaconPackage> getAll() {
        return new ArrayList<>(deviceMap.values());
    }

    private boolean isPaired(BeaconPackage data) {
        Query<PhonePaired> query = pairedBox.query()
                .equal(PhonePaired_.phoneImei, NetworkUtils.getGatewayId())
                .equal(PhonePaired_.beaconSerialNumber, data.getSerialNumberString())
                .build();
        return query.findFirst() != null;
    }

    private void broadCastAllBeacon() {
        List<BeaconPackage> dataList = getAll();
        List<CellTower> cellTowerList = NetworkUtils.getAllCellInfo();
        BroadcastEvent event = new BroadcastEvent();
        event.setBeaconPackageList(dataList);
        event.setLocation(mLocation);
        event.setGatewayId(NetworkUtils.getGatewayId());
        event.setCellTowerList(cellTowerList);
    }

    private void checkAndCreateShipment(List<BeaconPackage> dataList) {
        Logger.i("[+] checkAndCreateShipment [_>]" + dataList.size());
        for (final BeaconPackage data: dataList) {
            if (!isPaired(data) || !isAtStartLocations()) {
                continue;
            }

            Logger.i("[>_] should create on boot: " + data.isShouldCreateOnBoot());

            if (data.isShouldCreateOnBoot()) {
                WebService.createNewAutoSthipment(data.getSerialNumberString(), currentToken, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Logger.i("Failed to create shipment");
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        //--
                        deviceMap.get(data.getBluetoothAddress()).setForedCreateNew(false);
                        deviceMap.get(data.getBluetoothAddress()).setShouldCreateShipment(false);
                        deviceMap.get(data.getBluetoothAddress()).setShouldCreateOnBoot(false);
                        Logger.i("[ShipmentCreated+] " + data.getSerialNumber());


                        Notification notification = new NotificationCompat.Builder(BeaconService.this, getString(R.string.default_notification_channel_id))
                                .setContentTitle(data.getSerialNumberString() + "-Shipment++")
                                .setChannelId(getString(R.string.default_notification_channel_id))
                                .setSound(null)
                                .setTimeoutAfter(60*1000)
                                .setSmallIcon(R.drawable.notification)
                                .setShowWhen(true)
                                .setColor(Color.GREEN)
                                .setLocalOnly(true)
                                .build();

                        NotificationManagerCompat.from(BeaconService.this).notify(new Random().nextInt(), notification);
                    }
                });
            }
        }
    }

    private boolean isAtStartLocations() {
        if (companyShipmentLocations == null) {
            loadLocations();
        }
        if (mLocation == null) {
            return false;
        }
        for (Locations loc : companyShipmentLocations) {
            LatLng ll = new LatLng(loc.getLatitude(), loc.getLongitude());
            float[] result = new float[3];
            Location.distanceBetween(mLocation.getLatitude(), mLocation.getLongitude(), ll.getLat(), ll.getLon(), result);
            if (result[0] < loc.getRadiusMeters() && loc.getStartFlag().equalsIgnoreCase("y")) {
                return true;
            }
        }
        return false;
    }

    private void loadLocations() {
        Query<Locations> query = locationsBox.query().equal(Locations_.startFlag, "y").build();
        companyShipmentLocations = query.find();
    }

    private void onUpdateLocation(Location location) {
        Logger.d("[+_] onLocationChanged: " + location.getAccuracy());
        if (mLocation == null) {
            mLocation = location;
        } else {
            //1. check time age
            if (location.getTime() - mLocation.getTime() >= AppConfig.SCANNING_TIMEOUT) {
                mLocation = location;
            } else
                //2. check accuracy
                if (location.hasAccuracy() && (location.getAccuracy() < mLocation.getAccuracy())) {
                    mLocation = location;
                }
        }

        //Notify anyone listening for broadcasting about the new locatio
        Intent intent = new Intent(ACTION_BROADCAST);
        intent.putExtra(EXTRA_LOCATION, location);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private Notification getNotification() {

        CharSequence text = ServiceUtils.getLocationText(mLocation);
        // The PendingIntent to launch activity
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);
        PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .addAction(R.drawable.ic_launch, getString(R.string.launch_activity), activityPendingIntent)
                //.addAction(R.drawable.ic_cancel, getString(R.string.cancel_service), servicePendingIntent)
                .setContentText(text)
                .setContentTitle("SmartTraceIO")
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.notification)
                .setTicker(text)
                .setWhen(System.currentTimeMillis());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(getString(R.string.default_notification_channel_id));
        }
        return builder.build();
    }

    private long getNotificationTimeOut() {
        if (isAtStartLocations() || mLocation == null || !last_changed) {
            return 10*1000;
        } else {
            return 10*60*1000;
        }
    }

    private void createNotification() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("beacon_engine_tag", "Scanning Jobs", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Scanning Beacon job");
            this.getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, "beacon_engine_tag")
                .setContentTitle("Scanning Beacon ...")
                .setContentText(" Scanning Jobs are running ")
                .setAutoCancel(true)
                .setOngoing(false)
                .setTimeoutAfter(getNotificationTimeOut())
                .setChannelId("beacon_engine_tag")
                .setSound(null)
                .setSmallIcon(R.drawable.notification)
                .setShowWhen(true)
                .setColor(Color.GREEN)
                .setLocalOnly(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();

        NotificationManagerCompat.from(this).notify(1234567, notification);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equalsIgnoreCase(SharedPref.KEY_SMARRTRACE_IO_TOKEN)) {
            Logger.i("[+ Updating token ...]");
            currentToken = sharedPreferences.getString(SharedPref.KEY_SMARRTRACE_IO_TOKEN, "");
        }

        if (key.equalsIgnoreCase(SharedPref.KEY_ONBOOT)) {
//            _mShouldCreateOnBoot = sharedPreferences.getBoolean(SharedPref.KEY_ONBOOT, false);
        }
    }

    @Override
    public void onBeaconServiceConnect() {
        Logger.i("[>_] onBeaconServiceConnect ...");
        mBeaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    for (Beacon beacon : beacons) {
                        Logger.i("[BLE #] " + beacon.getIdentifier(2).toHexString() + ", [+onBoot: ] " + App.isBoot());
                        BeaconPackage bt04 = deviceMap.get(beacon.getBluetoothAddress());
                        if (bt04 == null || TextUtils.isEmpty(bt04.getName())) {
                            bt04 = BeaconPackage.fromBeacon(beacon);
                            //-- first reading
                            bt04.setShouldCreateOnBoot(App.isBoot());
                        } else {
                            bt04.updateFromBeacon(beacon);
                            deviceMap.remove(beacon.getBluetoothAddress());

                        }
                        bt04.setPhoneBatteryLevel(mBatteryLevel);
                        deviceMap.put(beacon.getBluetoothAddress(), bt04);
                    }
                    broadCastAllBeacon();
                    last_ble_event = System.currentTimeMillis();
                }
            }
        });
        try {
            Region myRegion = new Region("ranging_unique_id", null, null, null);
            mBeaconManager.startRangingBeaconsInRegion(myRegion);
        } catch (RemoteException e) {
            Logger.i("[BLE] error!");
        }
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void syncSettingsToService() {
        ScanJobScheduler.getInstance().applySettingsToScheduledJob(this, mBeaconManager);
    }

    //-- class LocalBinder --
    public class LocalBinder extends Binder {
        public BeaconService getService() {
            return BeaconService.this;
        }
    }
}
