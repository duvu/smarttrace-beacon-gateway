package au.com.smarttrace.beacon.service;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
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
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;

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
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import au.com.smarttrace.beacon.App;
import au.com.smarttrace.beacon.AppConfig;
import au.com.smarttrace.beacon.FireLogger;
import au.com.smarttrace.beacon.GsonUtils;
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
import au.com.smarttrace.beacon.firebase.ToFirebase;
import au.com.smarttrace.beacon.model.BeaconPackage;
import au.com.smarttrace.beacon.model.BroadcastEvent;
import au.com.smarttrace.beacon.model.CellTower;
import au.com.smarttrace.beacon.model.ExitEvent;
import au.com.smarttrace.beacon.model.UpdateEvent;
import au.com.smarttrace.beacon.net.DataUtil;
import au.com.smarttrace.beacon.net.WebService;
import au.com.smarttrace.beacon.net.model.FcmMessage;
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
    public static final String ACTION_WAKEUP_BROADCAST = PACKAGE_NAME + ".wakeup.broadcast";
    public static final String EXTRA_LOCATION = PACKAGE_NAME + ".location";

    public static final String GET_NEXT_POINT = "getnextpoint";


    private Location mLocation;

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

    private FirebaseAuth mAuth;
    FirebaseDatabase database;
    DatabaseReference ref;

    LServiceWrapper locationWrapper;
    private LCallback lCallback;

    AlarmManager nextPointAlarmManager;

    PowerManager mPowerManager;
    PowerManager.WakeLock mWakeLokc;

    private PendingIntent wakeupIntent;
//    private final Runnable heartbeat = new Runnable() {
//        @Override
//        public void run() {
//            try {
////                if (Systems.isDozing(BeaconService.this)) {
//                    try {
//                        wakeupIntent.send();
//                    } catch (SecurityException | PendingIntent.CanceledException e) {
//                        e.printStackTrace();
//                        Logger.e("Heartbeat location manager keep-alive failed", e);
//                    }
////                }
//            } finally {
//                if (handler != null) {
//                    handler.postDelayed(this, 60*1000);
//                }
//            }
//        }
//    };


    public BeaconService() {
        super();
    }

    @Override
    public void onCreate() {
        Logger.i("[BeaconService] onCreated");
        nextPointAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);

        //--heartbeat
//        handlerThread = new HandlerThread(AppConfig.TAG);
//        handlerThread.start();

        handler = new Handler();

        handler = new Handler();
        wakeupIntent = PendingIntent.getBroadcast(getBaseContext(), 0, new Intent("com.android.internal.location.ALARM_WAKEUP"), 0);
//        handler.postDelayed(heartbeat, 10*1000);
        mWakeLokc = mPowerManager.newWakeLock(PowerManager.ON_AFTER_RELEASE |PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "myWakeLockTag");

        if (!mWakeLokc.isHeld()) {
            mWakeLokc.acquire(14* 24 * 60 * 60 * 1000); // 14 days
        }

        mBeaconManager = BeaconManager.getInstanceForApplication(this.getApplicationContext());
        mBeaconManager.setForegroundBetweenScanPeriod(10*1000);
        mBeaconManager.setForegroundScanPeriod(10*1000);
        mBeaconManager.bind(this);
        mBeaconManager.setBackgroundMode(false);
        syncSettingsToService();

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
        registerEventBus();

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
        stopAbsoluteTimer();
        locationWrapper.stopLocationUpdates();
        //stopBLE();
        broadcastData();

    }
    @TargetApi(23)
    private void setAlarmForNextPoint() {
        Logger.d("[>_] Set alarm in: " + getNotificationTimeOut() + "seconds");

        Intent i = new Intent(this, BeaconService.class);
        i.putExtra(GET_NEXT_POINT, true);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        nextPointAlarmManager.cancel(pi);

        if(Systems.isDozing(this)){
            //Only invoked once per 15 minutes in doze mode
            Logger.d("Device is dozing, using infrequent alarm");
            nextPointAlarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, getNextPointTimestamp(), pi);
        }
        else {
            nextPointAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, getNextPointTimestamp(), pi);
        }
    }

    private long getNextPointTimestamp() {
        if (isAtStartLocations()) {
            return SystemClock.elapsedRealtime() + 10 * 1000;
        } else {
            return SystemClock.elapsedRealtime() + 10 * 60 * 1000;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.i("##Service Started: " + SharedPref.getToken());

        String token = FirebaseInstanceId.getInstance().getToken();
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        ref = database.getReference(NetworkUtils.getGatewayId());
        ref.child("TOKEN").setValue(token);
        currentToken = SharedPref.getToken();

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        //-- load Shipment Location
        loadLocations();

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

        checkIfNeedToCreateShipmentOnBoot();
        App.serviceStarted();
        start();
        return START_NOT_STICKY;
    }

    public void start() {
        Logger.d("[>>] Start scanning");
        FireLogger.d("[>>] Start scanning");
        try {
            locationWrapper.startLocationUpdates();
            startBLE();
            startAbsoluteTimer();
        } finally {
        }
    }

    private void stopBLE() {
        if (!mBeaconManager.getBackgroundMode()) {
            mBeaconManager.setForegroundBetweenScanPeriod(60 * 1000);
            syncSettingsToService();
        }
    }

    private void startBLE() {
        FireLogger.d("[>_] startBLE #backgroundMode: " + mBeaconManager.getBackgroundMode());
        if (mBeaconManager.getBackgroundMode()) {
            mBeaconManager.setForegroundBetweenScanPeriod(10 * 1000);
            mBeaconManager.setBackgroundMode(false);
            syncSettingsToService();
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
        unregisterEventBus();
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
        Logger.i("[+] broadcasting, isAtStartLocations #" + isAtStartLocations() + " _mShouldCreateOnBoot: " + _mShouldCreateOnBoot);
        createNotification();

        if (checkIfNewData()) {
            FcmMessage fcmMessage = new FcmMessage();
            fcmMessage.setExpectedTimeToReceive((System.currentTimeMillis())); // for 10 minute
            fcmMessage.setFcmInstanceId(FirebaseInstanceId.getInstance().getId());
            fcmMessage.setFcmToken(FirebaseInstanceId.getInstance().getToken());
            fcmMessage.setPhoneImei(NetworkUtils.getGatewayId());
            fcmMessage.setMessage("WAKEUP");
            WebService.nextPoint(fcmMessage);
        }

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
        }

        if (dataList == null || dataList.size() == 0) {
            Logger.i("[-] no new data");
            setAlarmForNextPoint();
            return; // not update if no event data;
        }

        if (!isAtStartLocations()) {
            _mShouldCreateOnBoot = false;
            SharedPref.saveOnBoot(false);
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

        Logger.i("[>] saving to firebase");
        DatabaseReference locRef = ref.child(System.currentTimeMillis()+"");
        ToFirebase tfb = ToFirebase.fromRaw(event);
        locRef.setValue(tfb);


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
            for (EventData evdt : evdtList) {
                final long evId = evdt.getId();
                Logger.i("[*] check: " + evdt.toString());
                WebService.sendEvent(evdt.toString(), new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        //noop
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        // remove data from db;
                        eventBox.remove(evId);
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
            String key = (String)entry.getKey();
            BeaconPackage data = (BeaconPackage) entry.getValue();
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
            if (isPaired(data) && data.isShouldUpload()) {
                return true;
            }
        }
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
        FireLogger.d("[+_] onLocationChanged: " + location.getAccuracy());
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

    private long getNotificationTimeOut() {
        if (isAtStartLocations()) {
            return 12*1000;
        } else {
            return 12*60*1000;
        }
    }

    private void createNotification() {
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("beacon_engine_tag", "Scanning Jobs", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Scanning Beacon job");
            this.getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, "beacon_engine_tag")
                .setContentTitle("Scanning Beacon ...")
                .setContentText(" Scanning Jobs are running ")
                .setAutoCancel(true)
                .setTimeoutAfter(getNotificationTimeOut())
                .setChannelId("beacon_engine_tag")
                .setSound(null)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.notification)
                .setShowWhen(true)
                .setColor(Color.GREEN)
                .setLocalOnly(true)
                .build();

        NotificationManagerCompat.from(this).notify(new Random().nextInt(), notification);
    }

    private void checkIfNeedToCreateShipmentOnBoot() {
        _mShouldCreateOnBoot = SharedPref.isOnBoot();
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
        stopSelf();
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
                    checkAndCreateShipment(getAll());
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

    @Subscribe (threadMode = ThreadMode.MAIN, priority = 0)
    public void onUpdate(UpdateEvent event) {
        start();
    }

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
