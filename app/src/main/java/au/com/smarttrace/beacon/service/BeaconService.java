package au.com.smarttrace.beacon.service;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.NetworkOnMainThreadException;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.TZONE.Bluetooth.BLE;
import com.TZONE.Bluetooth.ILocalBluetoothCallBack;
import com.TZONE.Bluetooth.Temperature.BroadcastService;
import com.TZONE.Bluetooth.Temperature.Model.Device;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.service.ScanJobScheduler;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import au.com.smarttrace.beacon.App;
import au.com.smarttrace.beacon.AppConfig;
import au.com.smarttrace.beacon.GsonUtils;
import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.R;
import au.com.smarttrace.beacon.SharedPref;
import au.com.smarttrace.beacon.db.EventData;
import au.com.smarttrace.beacon.db.Locations;
import au.com.smarttrace.beacon.db.PhonePaired;
import au.com.smarttrace.beacon.db.PhonePaired_;
import au.com.smarttrace.beacon.db.SensorData;
import au.com.smarttrace.beacon.firebase.ToFirebase;
import au.com.smarttrace.beacon.jobs.LocationServiceWrapper;
import au.com.smarttrace.beacon.model.BeaconPackage;
import au.com.smarttrace.beacon.model.BroadcastEvent;
import au.com.smarttrace.beacon.model.CellTower;
import au.com.smarttrace.beacon.model.ExitEvent;
import au.com.smarttrace.beacon.model.UpdateEvent;
import au.com.smarttrace.beacon.model.UpdateToken;
import au.com.smarttrace.beacon.net.DataUtil;
import au.com.smarttrace.beacon.net.WebService;
import au.com.smarttrace.beacon.net.model.LatLng;
import au.com.smarttrace.beacon.net.model.LoginResponse;
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

    private LocationCallback mLocationCallback;
    private Handler mServiceHandler;
    private Location mLocation;

    private TimeZone userTimezone = null;

    boolean _mShouldCreateOnBoot = false;
    private boolean updatingToken = false;

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

    private BeaconManager mBeaconManager = BeaconManager.getInstanceForApplication(this);

    private Map<String, BeaconPackage> deviceMap = new ConcurrentHashMap<>();
//    private List<LocationBody> companyShipmentLocations = null;
    private List<Locations> companyShipmentLocations = null;

//    private LocationManager gpsLocationManager;
//    private LocationManager passiveLocationManager;
//    private LocationManager towerLocationManager;
//    private GeneralLocationListener locationListener;

    private Box<EventData> eventBox;
    private Box<PhonePaired> pairedBox;
    private Box<Locations> locationsBox;

    private final IBinder mBinder = new LocalBinder();
    private boolean hasGoogleClient = false;
    HandlerThread handlerThread = null;
    private String currentToken = null;

    private FirebaseAuth mAuth;
    FirebaseDatabase database;
    DatabaseReference ref;

    LocationServiceWrapper locationWrapper;
    private BluetoothAdapter _BluetoothAdapter;
    private BroadcastService _BroadcastService;
    private List<Device> _DeviceList = new ArrayList<>();

    public BeaconService() {
    }

    @Override
    public void onCreate() {
        Logger.i("[BeaconService] onCreated");
//        mBeaconManager.setForegroundBetweenScanPeriod(AppConfig.UPDATE_INTERVAL/10);
//        mBeaconManager.setForegroundScanPeriod(AppConfig.UPDATE_PERIOD);
//        mBeaconManager.bind(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onUpdateLocation(locationResult.getLastLocation());
            }
        };
        locationWrapper = new LocationServiceWrapper(this, mLocationCallback);

        handlerThread = new HandlerThread(AppConfig.TAG);
        handlerThread.start();
        mServiceHandler = new Handler(handlerThread.getLooper());
        mServiceHandler = new Handler();

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Android O requires a Notification Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            // Create the channel for the notification
            NotificationChannel channel = new NotificationChannel(getString(R.string.default_notification_channel_id), name, NotificationManager.IMPORTANCE_DEFAULT);

            //set the notification-channel for the Notification Manager
            mNotificationManager.createNotificationChannel(channel);
        }
        registerEventBus();
        String token = FirebaseInstanceId.getInstance().getToken();

        Logger.i("FirebaseToken: " + token);

        //-- init database;
        eventBox = ((App) getApplicationContext()).getBoxStore().boxFor(EventData.class);
        pairedBox = ((App) getApplicationContext()).getBoxStore().boxFor(PhonePaired.class);
        locationsBox = ((App) getApplicationContext()).getBoxStore().boxFor(Locations.class);

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        ref = database.getReference(NetworkUtils.getGatewayId());
        ref.child("TOKEN").setValue(token);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        intentFilter.addAction(Intent.ACTION_BATTERY_LOW);
        intentFilter.addAction(Intent.ACTION_BATTERY_OKAY);
        registerReceiver(mBatteryLevelReceiver, intentFilter);
    }

    //--Upload data control
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    private ScheduledFuture<?> sHandler;
    private final Runnable uploader = new Runnable() {
        @Override
        public void run() {
            broadcastData();
        }
    };

    public void uploadDataToServer() {
        Logger.i("[+] uploadDataToServer");
        if (sHandler != null) {
            sHandler.cancel(true);
        }

//        startFast();
        sHandler = scheduler.scheduleAtFixedRate(uploader, 30 * 1000 /*Wait10Sec*/, 10*1000, TimeUnit.MILLISECONDS);
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                stopUploadDataToServer();
//                stopFast();
            }
        }, AppConfig.SCHEDULED_RUNNING_FIRST - 60*1000 /*Finished1minEarlier*/, TimeUnit.MILLISECONDS);

        scheduler.scheduleAtFixedRate(uploader, AppConfig.SCHEDULED_RUNNING_FIRST, AppConfig.UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
    }

    private void stopUploadDataToServer() {
        if (sHandler != null) {
            sHandler.cancel(false);
        }
    }

    //--End
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.i("##Service Started: " + SharedPref.getToken());
        currentToken = SharedPref.getToken();
        startForeground(NOTIFICATION_ID, getNotification());
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        //-- load Shipment Location
        companyShipmentLocations = locationsBox.query().build().find();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null || currentUser.isAnonymous()) {
            Logger.i("LogginInFirebase");
            mAuth.signInWithEmailAndPassword("hoaivubk@gmail.com", "poiuyt01")
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            Logger.i("[>] logged in");
                        }
                    });
        }

        if (userTimezone == null) {
            TimeZone.getTimeZone(SharedPref.getUserTimezone());
        }

        checkIfNeedToCreateShipmentOnBoot();
        App.serviceStarted();
        startBLEAndLocationUpdate();
        //uploadDataToServer();
        return START_STICKY;
    }

    private void startBLEAndLocationUpdate() {
        startLocationUpdate();
        startBLEScan(10);

        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                broadcastData();
            }
        }, 30*1000, TimeUnit.MILLISECONDS);

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
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
        FirebaseCrash.log("Service was destroyed");
        mServiceHandler.removeCallbacks(null);
        //stopLocationUpdate();
        //stopBLEScan(true);

        App.serviceEnded();
        unregisterEventBus();
        unregisterReceiver(mBatteryLevelReceiver);

        if (handlerThread != null) {
            handlerThread.quitSafely();
        }
        super.onDestroy();
    }




    private void syncSettingsToService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ScanJobScheduler.getInstance().applySettingsToScheduledJob(this, mBeaconManager);
        }
    }

    private ScheduledFuture locationHandler;
    private void startLocationUpdate() {
        Logger.i("[+] startLocationUpdate: " + hasGoogleClient);
        locationHandler = scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                locationWrapper.startLocationUpdates();
                Looper.loop();
            }
        }, 0, TimeUnit.SECONDS);

        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                locationWrapper.stopLocationUpdates();
                locationHandler.cancel(true);
            }
        }, 30, TimeUnit.SECONDS);
    }

    private void broadcastData() {
        Logger.i("[+] broadcasting, isAtStartLocations #" + isAtStartLocations() + " _mShouldCreateOnBoot: " + _mShouldCreateOnBoot);

        checkAndCreateShipment(getAll());

        List<BeaconPackage> dataList = getDataPackageList();

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
        }

        if (dataList == null || dataList.size() == 0) {
            Logger.i("[-] no new data");
            return; // not update if no event data;
        }

        if (!isAtStartLocations()) {
            _mShouldCreateOnBoot = false;
            SharedPref.saveOnBoot(false);
        }
        List<CellTower> cellTowerList = NetworkUtils.getAllCellInfo();
        BroadcastEvent event = new BroadcastEvent();
        event.setBeaconPackageList(dataList);
        event.setLocation(mLocation);
        event.setGatewayId(NetworkUtils.getGatewayId());
        event.setCellTowerList(cellTowerList);

        Logger.i("[>] saving to firebase");
        DatabaseReference locRef = ref.child(System.currentTimeMillis()+"");
        ToFirebase tfb = ToFirebase.fromRaw(event);
        locRef.setValue(tfb);

        if (NetworkUtils.isInternetAvailable()) {
            Logger.i("[Online] Network is online");
            //1. upload old data
            List<EventData> evdtList = eventBox.getAll();
            for (EventData evdt : evdtList) {
                final long evId = evdt.getId();
                Logger.i("[*] check: " + evdt.toString());
                WebService.sendEvent(evdt.toString(), new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        // remove data from db;
                        eventBox.remove(evId);
                    }
                });
            }

            //2. upload new data
            String dataForUpload = DataUtil.formatData(event);
            WebService.sendEvent(dataForUpload);
        } else {
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

            for (BeaconPackage data : dataList) {
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
    }



    private List<BeaconPackage> getDataPackageList() {
        List<BeaconPackage> dataList = new ArrayList<>();
        for (Object o : deviceMap.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            String key = (String)entry.getKey();
            BeaconPackage data = (BeaconPackage) entry.getValue();
            if (isPaired(data) && !data.isShouldCreateOnBoot()) {
                dataList.add(data);
                deviceMap.get(data.getBluetoothAddress()).setShouldUpload(false); // to prevent repeat upload

            }
        }
        return dataList;
    }

    private List<BeaconPackage> getAll() {
        return new ArrayList<>(deviceMap.values());
    }

    private boolean isPaired(BeaconPackage data) {
        Logger.i("[>]Checking if paired");
        Query<PhonePaired> query = pairedBox.query()
                .equal(PhonePaired_.phoneImei, NetworkUtils.getGatewayId())
                .equal(PhonePaired_.beaconSerialNumber, data.getSerialNumberString())
                .build();
        Logger.i("[>... isPaired: ] " + (query.findFirst() != null));
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
        EventBus.getDefault().removeStickyEvent(BroadcastEvent.class);
        EventBus.getDefault().postSticky(event);
    }

    private void checkAndCreateShipment(List<BeaconPackage> dataList) {
        Logger.i("[+] checkAndCreateShipment: updatingToken: #" + updatingToken + "isConnected: #" + NetworkUtils.isInternetAvailable());
        if (updatingToken || !NetworkUtils.isInternetAvailable()) return;
        int idx = 0;
        for (final BeaconPackage data: dataList) {
            if ((data.isShouldCreateOnBoot() && isAtStartLocations() && isPaired(data))) {

                WebService.createNewAutoSthipment(data.getSerialNumberString(), currentToken, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Logger.i("Failed to create shipment");
                        //updateToken(3*1000);
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
                                .setContentText("ShipmentCreated")
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
            } else {
                deviceMap.get(data.getBluetoothAddress()).setForedCreateNew(false);
                deviceMap.get(data.getBluetoothAddress()).setShouldCreateShipment(false);
                deviceMap.get(data.getBluetoothAddress()).setShouldCreateOnBoot(false);
            }
        }
    }

    private boolean isAtStartLocations() {
        if (companyShipmentLocations == null){
            companyShipmentLocations = locationsBox.query().build().find();
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


    @Override
    public void onBeaconServiceConnect() {
        Logger.i("[+] onBeaconServiceConnect");
        mBeaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    checkIfNeedToCreateShipmentOnBoot();
                    for (Beacon beacon : beacons) {
                        Logger.i("[BLE #] " + beacon.getIdentifier(2).toHexString() + ", [+onBoot: ] " + _mShouldCreateOnBoot);
                        BeaconPackage bt04 = deviceMap.get(beacon.getBluetoothAddress());
                        if (bt04 == null) {
                            bt04 = BeaconPackage.fromBeacon(beacon);
                            //-- first reading
                            bt04.setShouldCreateOnBoot(_mShouldCreateOnBoot);
                        } else {
                            bt04.updateFromBeacon(beacon);
                        }
                        bt04.setPhoneBatteryLevel(mBatteryLevel);
                        deviceMap.put(beacon.getBluetoothAddress(), bt04);
                    }
                    broadCastAllBeacon();
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

    //
    private void onUpdateLocation(Location location) {
        Logger.d("[>_] onLocationChanged: " + location.getAccuracy());
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
                .setContentTitle(ServiceUtils.getLocationTitle(this))
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

    private void checkIfNeedToCreateShipmentOnBoot() {
        _mShouldCreateOnBoot = SharedPref.isOnBoot();
        //set flag to false
        //SharedPref.saveOnBoot(false);
        Logger.i("[+] checkIfNeedToCreateShipmentOnBoot: " + _mShouldCreateOnBoot);
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

    private void startBLEScan(long timeout) {
//        if (Looper.myLooper() == Looper.getMainLooper()) {
//            throw new NetworkOnMainThreadException();
//        }

        final ScheduledFuture bleScheduledHandler = scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                Logger.d("[>_] startBLEScan ...");
                Looper.prepare();
                try {
                    if (_BroadcastService == null) {
                        _BroadcastService = new BroadcastService();
                    }
                    final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                    _BluetoothAdapter = bluetoothManager.getAdapter();
                    _BroadcastService.Init(_BluetoothAdapter, _LocalBluetoothCallBack);
                } catch (Exception ex) {
                    Logger.d("BLE error: " + ex.getMessage());
                }


                synchronized (this) {
                    _BroadcastService.StartScan();
                }

                Looper.loop();
            }
        }, 1, TimeUnit.SECONDS);

        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                Logger.d("[>_] stopBLEScan ...");
                _BroadcastService.StopScan();
                bleScheduledHandler.cancel(true);
            }
        }, timeout + 1, TimeUnit.SECONDS);

    }

    private void AddOrUpdate(final BLE ble){
        try {
            Device d = new Device();
            d.fromScanData(ble);
            if(d.SN == null || d.SN.length() != 8) return;

            /*
                HardwareModel = "39"  BT04
                HardwareModel = "3A"  BT05
            */

            Logger.i("[BLE #] " + d.SN + ", [+onBoot: ] " + _mShouldCreateOnBoot);
            BeaconPackage bt04 = deviceMap.get(d.MacAddress);
            if (bt04 == null) {
                bt04 = BeaconPackage.fromBt04(d);
                //-- first reading
                bt04.setShouldCreateOnBoot(_mShouldCreateOnBoot);
            } else {
                bt04.updateFromBt04(d);
            }
            bt04.setPhoneBatteryLevel(mBatteryLevel);

            deviceMap.put(d.MacAddress, bt04);

            //--

            boolean flag = true;
            for (int i = 0; i < _DeviceList.size(); i++) {
                Device item = _DeviceList.get(i);
                if (item.SN.equals(d.SN)){
                    //update
                    item.fromScanData(ble);
                    _DeviceList.set(i, item);

                    flag = false;
                }
            }
            if(flag){
                _DeviceList.add(d);
            }
        }catch (Exception ex){
            Logger.e("AddOrUpdate:", ex);
        }
    }

    private ILocalBluetoothCallBack _LocalBluetoothCallBack = new ILocalBluetoothCallBack() {
        @Override
        public void OnEntered(BLE ble) {
            try {
                AddOrUpdate(ble);
            } catch (Exception ex) {
            }
        }

        @Override
        public void OnUpdate(BLE ble) {
            try {
                AddOrUpdate(ble);
            } catch (Exception ex) {
            }
        }

        @Override
        public void OnExited(final BLE ble) {
            try {

            } catch (Exception ex) {
            }
        }

        @Override
        public void OnScanComplete() {

        }
    };


    @Subscribe
    public void onExitEvent(ExitEvent exitEvent) {
        stopForeground(true);
        stopSelf();
    }

    @Subscribe
    public void onUpdateEvent(UpdateEvent updateEvent) {
//        stopBLEScan(false);
//        stopLocationUpdate();
        startBLEAndLocationUpdate();
    }
    @Subscribe
    public void onUpdateToken(UpdateToken updateToken) {
        // update token
        updateToken(1*1000, true);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equalsIgnoreCase(SharedPref.KEY_SMARRTRACE_IO_TOKEN)) {
            Logger.i("[+ Updating token ...]");
            currentToken = sharedPreferences.getString(SharedPref.KEY_SMARRTRACE_IO_TOKEN, "");
        }

        if (key.equalsIgnoreCase(SharedPref.KEY_ONBOOT)) {
            _mShouldCreateOnBoot = sharedPreferences.getBoolean(SharedPref.KEY_ONBOOT, false);
        }
    }

    //-- class LocalBinder --
    public class LocalBinder extends Binder {
        public BeaconService getService() {
            return BeaconService.this;
        }
    }

    private Runnable reLoginRunnable = new Runnable() {
        @Override
        public void run() {
//            stopBLEScan(false);
//            stopLocationUpdate();
            reLogin();
        }
    };
    private void updateToken(long timeMillisDelay, boolean cancelPrev) {
        if (cancelPrev) {
            stopUpdateToken();
            mServiceHandler.postDelayed(reLoginRunnable, timeMillisDelay);
            updatingToken = true;
        } else {
            if (!updatingToken && NetworkUtils.isInternetAvailable()) {
                Logger.i("[+] Token update in " + timeMillisDelay / 1000 + "s");
                updatingToken = true;
                mServiceHandler.postDelayed(reLoginRunnable, timeMillisDelay);

                mServiceHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startBLEAndLocationUpdate();
                    }
                }, timeMillisDelay + 5*1000);
            }
        }

    }

    private void stopUpdateToken() {
        mServiceHandler.removeCallbacks(reLoginRunnable);
        updatingToken = false;
    }

    private void reLogin() {
        WebService.login(SharedPref.getUserName(), SharedPref.getPassword(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                updateToken(2* 60 * 1000, false);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() != null) {
                    LoginResponse data = GsonUtils.getInstance().fromJson(response.body().string(), LoginResponse.class);
                    if (data != null && data.getResponse() != null) {
                        SharedPref.saveToken(data.getResponse().getToken());
                        SharedPref.saveExpiredStr(data.getResponse().getExpired());
                        SharedPref.saveTokenInstance(data.getResponse().getInstance());
                        stopUpdateToken();
                    } else {
                        updateToken(60 * 1000, false);
                    }
                } else {
                    updateToken(60 * 1000, false);
                }
            }
        });
    }
}
