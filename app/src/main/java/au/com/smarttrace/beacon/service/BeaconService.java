package au.com.smarttrace.beacon.service;

import android.app.AlarmManager;
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
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.TZONE.Bluetooth.BLE;
import com.TZONE.Bluetooth.ILocalBluetoothCallBack;
import com.TZONE.Bluetooth.Temperature.BroadcastService;
import com.TZONE.Bluetooth.Temperature.Model.Device;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
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

public class BeaconService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String PACKAGE_NAME = "au.com.smarttrace.beacon";
    public static final String EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME + ".started_from_notification";
    public static final String EXTRA_STARTED_FROM_BOOTSTRAP = PACKAGE_NAME + ".started_from_bootstrap";

    public static final String ACTION_BROADCAST = PACKAGE_NAME + ".broadcast";
    public static final String EXTRA_LOCATION = PACKAGE_NAME + ".location";

    public static final String GET_NEXT_POINT = "getnextpoint";

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

    private final Map<String, BeaconPackage> deviceMap = new ConcurrentHashMap<>();
    private List<Locations> companyShipmentLocations = null;

    private Box<EventData> eventBox;
    private Box<PhonePaired> pairedBox;
    private Box<Locations> locationsBox;

    private final IBinder mBinder = new LocalBinder();
    HandlerThread handlerThread = null;
    private String currentToken = null;

    private FirebaseAuth mAuth;
    FirebaseDatabase database;
    DatabaseReference ref;

    LServiceWrapper locationWrapper;
    private LCallback lCallback;

    AlarmManager nextPointAlarmManager;


    private BluetoothAdapter _BluetoothAdapter;
    private BroadcastService _BroadcastService;
    private Timer _Timer;
    private boolean _IsInit = false;

    public BeaconService() {
    }

    @Override
    public void onCreate() {
        Logger.i("[BeaconService] onCreated");
        nextPointAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        lCallback = new LCallback() {
            @Override
            public void onLocationChanged(Location location) {
                onUpdateLocation(location);
            }
        };

        try {
            if (_BroadcastService == null) {
                _BroadcastService = new BroadcastService();
            }
            final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            _BluetoothAdapter = bluetoothManager.getAdapter();
            if (!_IsInit) {
                _IsInit = _BroadcastService.Init(_BluetoothAdapter, _LocalBluetoothCallBack);
            }
        } catch (Exception ex) {

        }

        locationWrapper = LServiceWrapper.instances(this, lCallback);

        handlerThread = new HandlerThread(AppConfig.TAG);
        handlerThread.start();
        mServiceHandler = new Handler(handlerThread.getLooper());
        mServiceHandler = new Handler();

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            NotificationChannel channel = new NotificationChannel(getString(R.string.default_notification_channel_id), name, NotificationManager.IMPORTANCE_DEFAULT);
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
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
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
        start();
        return START_NOT_STICKY;
    }

    private void start() {
        final ScheduledFuture handler = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                startBLEAndLocationUpdate();
            }
        }, 0, 10, TimeUnit.SECONDS);

        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                handler.cancel(true);
                //stopSelf();
            }
        }, 9*60, TimeUnit.SECONDS);

        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                (new BeaconEngine(BeaconService.this)).scanAndUpload();
            }
        }, 10*60, 60, TimeUnit.SECONDS);
    }

    boolean scanningBLEAndLocation = false;
    private void startBLEAndLocationUpdate() {
        Logger.i("[+] startLocationUpdate");
        mLocation = locationWrapper.getCurrentLocation();

        if (!scanningBLEAndLocation) {
            mServiceHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    locationWrapper.stopLocationUpdates();
                    stopBLEScan();
                    broadcastData();
                    scanningBLEAndLocation = false;
                }
            }, AppConfig.SCANNING_TIMEOUT); //30s for several try location update

            startBLEScan();
            mServiceHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanningBLEAndLocation = true;
                    locationWrapper.startLocationUpdates();
                }
            }, 0);
        }
    }
    private void startBLEScan() {
        Logger.i("[>_] startBLEScan");
        try {
            if(_Timer != null) {
                _Timer.cancel();
            }
            _Timer = new Timer();
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    try {
                        synchronized (this) {
                            _BroadcastService.StartScan();
                        }
                    } catch (Exception ex){}
                }
            };

            _Timer.schedule(timerTask, 100);
        } catch (Exception ex){
            Logger.e("[>_] BLErError #", ex);
        }
    }

    private void stopBLEScan() {
        Logger.d("[>_] StopBLEScan");
        try {
            _Timer.cancel();
            if(_BroadcastService!=null)
                _BroadcastService.StopScan();
        } catch (Exception ex){
            Logger.e("#", ex);
        } finally {
            _Timer.purge();
            broadCastAllBeacon();
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
        FirebaseCrash.log("Service was destroyed");
        mServiceHandler.removeCallbacks(null);
        App.serviceEnded();
        unregisterEventBus();
        unregisterReceiver(mBatteryLevelReceiver);
        stopForeground(false);
        if (handlerThread != null) {
            handlerThread.quitSafely();
        }
        scheduler.shutdown();
        super.onDestroy();
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

//        UploadDataAsync uploadDataAsync = new UploadDataAsync(eventBox);
//        uploadDataAsync.setEvent(event);
//        uploadDataAsync.setCurrentLocation(mLocation);
//        uploadDataAsync.execute();

//        if (NetworkUtils.isConnected()) {
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
//        } else {
//            Logger.i("[Offline] Network is offline, going to store data");
//            EventData evdt = new EventData();
//            evdt.setPhoneImei(NetworkUtils.getGatewayId());
//            Long timestamp = (new Date()).getTime();
//            evdt.setTimestamp(timestamp);
//            if (mLocation != null) {
//                evdt.setLatitude(mLocation.getLatitude());
//                evdt.setLongitude(mLocation.getLongitude());
//                evdt.setAltitude(mLocation.getAltitude());
//                evdt.setAccuracy(mLocation.getAccuracy());
//                evdt.setSpeedKPH(mLocation.getSpeed());
//            } else {
//                evdt.setLatitude(0);
//                evdt.setLongitude(0);
//                evdt.setAltitude(0);
//                evdt.setAccuracy(0);
//                evdt.setSpeedKPH(0);
//            }
//
//            for (BeaconPackage data : dataList) {
//                SensorData sd = new SensorData();
//                sd.setSerialNumber(data.getSerialNumberString());
//                sd.setName(data.getName());
//                sd.setTemperature(data.getTemperature());
//                sd.setHumidity(data.getHumidity());
//                sd.setRssi(data.getRssi());
//                sd.setDistance(data.getDistance());
//                sd.setBattery(DataUtil.battPercentToVolt(data.getPhoneBatteryLevel()*100));
//                sd.setLastScannedTime(data.getTimestamp());
//                sd.setHardwareModel(data.getModel());
//
//                evdt.getSensorDataList().add(sd);
//            }
//            long id = eventBox.put(evdt);
//            Logger.i("[+] stored #" + id);
//        }
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
        Query<PhonePaired> query = pairedBox.query()
                .equal(PhonePaired_.phoneImei, NetworkUtils.getGatewayId())
                .equal(PhonePaired_.beaconSerialNumber, data.getSerialNumberString())
                .build();
        Logger.i("[>... paired? ] " + data.getSerialNumberString() + " :#" + (query.findFirst() != null));
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
        Logger.i("[+] checkAndCreateShipment: updatingToken: #" + updatingToken);
        if (updatingToken) return;
        for (final BeaconPackage data: dataList) {

            if ((data.isShouldCreateOnBoot() && isAtStartLocations() && isPaired(data))) {
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
    }


    private void AddOrUpdate(final BLE ble){
        synchronized (deviceMap) {
            try {
                Device d = new Device();
                d.fromScanData(ble);
                if (d.SN == null || d.SN.length() != 8) return;
                //Logger.i("[Service #Scanning] " + d.SN + ", [+onBoot: ] " + _mShouldCreateOnBoot);
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
            } catch (Exception ex) {
                Logger.e("AddOrUpdate:", ex);
            }
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
        stopSelf();
    }

    @Subscribe
    public void onUpdateEvent(UpdateEvent updateEvent) {
        startBLEAndLocationUpdate();
    }
    @Subscribe
    public void onUpdateToken(UpdateToken updateToken) {
        // update token
        //updateToken(1000, true);
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

//    private Runnable reLoginRunnable = new Runnable() {
//        @Override
//        public void run() {
//            reLogin();
//        }
//    };
//    private void updateToken(long timeMillisDelay, boolean cancelPrev) {
//        if (cancelPrev) {
//            stopUpdateToken();
//            mServiceHandler.postDelayed(reLoginRunnable, timeMillisDelay);
//            updatingToken = true;
//        } else {
//            if (!updatingToken && NetworkUtils.isInternetAvailable()) {
//                Logger.i("[+] Token update in " + timeMillisDelay / 1000 + "s");
//                updatingToken = true;
//                mServiceHandler.postDelayed(reLoginRunnable, timeMillisDelay);
//
//                mServiceHandler.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        startBLEAndLocationUpdate();
//                    }
//                }, timeMillisDelay + 5*1000);
//            }
//        }
//
//    }
//
//    private void stopUpdateToken() {
//        mServiceHandler.removeCallbacks(reLoginRunnable);
//        updatingToken = false;
//    }
//
//    private void reLogin() {
//        WebService.login(SharedPref.getUserName(), SharedPref.getPassword(), new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                updateToken(2* 60 * 1000, false);
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                if (response.body() != null) {
//                    LoginResponse data = GsonUtils.getInstance().fromJson(response.body().string(), LoginResponse.class);
//                    if (data != null && data.getResponse() != null) {
//                        SharedPref.saveToken(data.getResponse().getToken());
//                        SharedPref.saveExpiredStr(data.getResponse().getExpired());
//                        SharedPref.saveTokenInstance(data.getResponse().getInstance());
//                        stopUpdateToken();
//                    } else {
//                        updateToken(60 * 1000, false);
//                    }
//                } else {
//                    updateToken(60 * 1000, false);
//                }
//            }
//        });
//    }

    public class UpdateTokenAsync extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            WebService.login(SharedPref.getUserName(), SharedPref.getPassword(), new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    //updateToken(2* 60 * 1000, false);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.body() != null) {
                        LoginResponse data = GsonUtils.getInstance().fromJson(response.body().string(), LoginResponse.class);
                        if (data != null && data.getResponse() != null) {
                            SharedPref.saveToken(data.getResponse().getToken());
                            SharedPref.saveExpiredStr(data.getResponse().getExpired());
                            SharedPref.saveTokenInstance(data.getResponse().getInstance());
                            //stopUpdateToken();
                        } else {
                            //updateToken(60 * 1000, false);
                        }
                    } else {
                        //updateToken(60 * 1000, false);
                    }
                }
            });
            return true;
        }
        @Override
        protected void onPostExecute(final Boolean success) {
            //updateTokenAsync = null;
        }
    }

}
