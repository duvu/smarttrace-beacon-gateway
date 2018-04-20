package au.com.smarttrace.beacon.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityManager;
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
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
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

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import au.com.smarttrace.beacon.firebase.ToFirebase;
import au.com.smarttrace.beacon.model.BeaconPackage;
import au.com.smarttrace.beacon.model.BroadcastEvent;
import au.com.smarttrace.beacon.model.CellTower;
import au.com.smarttrace.beacon.model.EventData;
import au.com.smarttrace.beacon.model.ExitEvent;
import au.com.smarttrace.beacon.model.SensorData;
import au.com.smarttrace.beacon.model.UpdateEvent;
import au.com.smarttrace.beacon.model.UpdateToken;
import au.com.smarttrace.beacon.net.DataUtil;
import au.com.smarttrace.beacon.net.WebService;
import au.com.smarttrace.beacon.net.model.Device;
import au.com.smarttrace.beacon.net.model.DeviceResponse;
import au.com.smarttrace.beacon.net.model.LatLng;
import au.com.smarttrace.beacon.net.model.LocationBody;
import au.com.smarttrace.beacon.net.model.LocationResponse;
import au.com.smarttrace.beacon.net.model.LoginResponse;
import au.com.smarttrace.beacon.net.model.PairedBeaconResponse;
import au.com.smarttrace.beacon.net.model.UserBody;
import au.com.smarttrace.beacon.net.model.UserResponse;
import au.com.smarttrace.beacon.ui.MainActivity;
import io.objectbox.Box;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static au.com.smarttrace.beacon.AppConfig.NOTIFICATION_ID;

public class BeaconService extends Service implements BeaconConsumer, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String PACKAGE_NAME = "au.com.smarttrace.beacon";
    //private static final String CHANNEL_ID = "channel_01";

    private static final String EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME + ".started_from_notification";
    public static final String EXTRA_STARTED_FROM_BOOTSTRAP = PACKAGE_NAME + ".started_from_bootstrap";

    public static final String ACTION_BROADCAST = PACKAGE_NAME + ".broadcast";
    public static final String EXTRA_LOCATION = PACKAGE_NAME + ".location";

    private NotificationManager mNotificationManager;

    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private Handler mServiceHandler;
    private Handler mHandler = new Handler();
    private AlarmManager nextPointAlarmManager;
    private Location mLocation;

    private TimeZone userTimezone = null;

    private FirebaseAnalytics mFirebaseAnalytics;

    boolean _mShouldCreateOnBoot = false;
    private boolean updatingToken = false;
    private boolean updatingPairedList = false;
    private boolean isConnected = true;

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

    private Set<String> _paired_beacon = new HashSet<>();

    private List<LocationBody> companyShipmentLocations = null;

    private LocationManager gpsLocationManager;
    private LocationManager passiveLocationManager;
    private LocationManager towerLocationManager;
    private GeneralLocationListener locationListener;

    private TelephonyManager mTelephonyManager;

    private Box<EventData> eventBox;

    private final IBinder mBinder = new LocalBinder();

    private int timesOfDataUpdated = 0;
    private boolean hasGoogleClient = false;

    ConnectivityManager connectivityManager;

    HandlerThread handlerThread = null;

    private String currentToken = null;

    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    DatabaseReference ref;

    public BeaconService() {
    }

    @Override
    public void onCreate() {
        Logger.i("[BeaconService] onCreated");

        nextPointAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        mBeaconManager.setBackgroundBetweenScanPeriod(AppConfig.UPDATE_INTERVAL / 5);
        mBeaconManager.setBackgroundScanPeriod(AppConfig.UPDATE_PERIOD);
        mBeaconManager.setForegroundBetweenScanPeriod(AppConfig.UPDATE_INTERVAL / 5);
        mBeaconManager.setForegroundScanPeriod(AppConfig.UPDATE_PERIOD);
        mBeaconManager.bind(this);

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        if (ServiceUtils.isGooglePlayServicesAvailable(this)) {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            mLocationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    onUpdateLocation(locationResult.getLastLocation());
                }
            };
            createLocationRequest();
            hasGoogleClient = true;
        }

        Logger.i("Starting foreground service");
        startForeground(NOTIFICATION_ID, getNotification());

        handlerThread = new HandlerThread(AppConfig.TAG);
        handlerThread.start();
        mServiceHandler = new Handler(handlerThread.getLooper());
        mServiceHandler = new Handler();
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Android O requires a Notification Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            // Create the channel for the notification
            NotificationChannel channel = new NotificationChannel(getString(R.string.default_notification_channel_id), name, NotificationManager.IMPORTANCE_DEFAULT);

            //set the notification-channel for the Notification Manager
            mNotificationManager.createNotificationChannel(channel);
        }
        registerEventBus();
        NetworkUtils.init(this);
        String token = FirebaseInstanceId.getInstance().getToken();
        Logger.i("TOKEN: " + token);

        //-- init database;
        eventBox = ((App) getApplicationContext()).getBoxStore().boxFor(EventData.class);
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        ref = database.getReference(getGatewayId());

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        intentFilter.addAction(Intent.ACTION_BATTERY_LOW);
        intentFilter.addAction(Intent.ACTION_BATTERY_OKAY);
        //intentFilter.addAction(Inte);
        registerReceiver(mBatteryLevelReceiver, intentFilter);
    }

    //--Upload data control
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> sHandler;
    private final Runnable uploader = new Runnable() {
        @Override
        public void run() {
            broadcastData();
        }
    };

    private void uploadDataToServer() {
        Logger.d("[+] uploadDataToServer");
        if (sHandler != null) {
            sHandler.cancel(true);
        }
        startScanFast();
        sHandler = scheduler.scheduleAtFixedRate(uploader, 30 * 1000, 10*1000, TimeUnit.MILLISECONDS);
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                stopUploadDataToServer();
                stopScanFast();
            }
        }, 10 * 60, TimeUnit.SECONDS);

        scheduler.scheduleAtFixedRate(uploader, 10*60*1000, AppConfig.UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
    }

    private void stopUploadDataToServer() {
        if (sHandler != null) {
            sHandler.cancel(true);
        }
    }

    private void startScanFast() {
        //BLE
        mBeaconManager.setBackgroundMode(false);
        mBeaconManager.setForegroundBetweenScanPeriod(10*1000);
        mBeaconManager.setForegroundScanPeriod(6*1000);
        syncSettingsToService();

        //Location
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(10*1000);
        mLocationRequest.setFastestInterval(2*1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(0);
        stopLocationUpdate();
        startLocationUpdate();
    }

    private void stopScanFast() {
        //BLE
        mBeaconManager.setBackgroundMode(false);
        mBeaconManager.setForegroundBetweenScanPeriod(AppConfig.UPDATE_INTERVAL/5);
        mBeaconManager.setForegroundScanPeriod(AppConfig.UPDATE_PERIOD);
        syncSettingsToService();
        stopLocationUpdate();
        createLocationRequest();
        startLocationUpdate();
    }

    //--End

    @TargetApi(Build.VERSION_CODES.M)
    private void setExactAndAllowWhileIdle(PendingIntent pendingIntent) {
        nextPointAlarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 10 * 1000, pendingIntent);
    }
    //----

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.i("##Service Started: " + SharedPref.getToken());
        FirebaseUser currentUser = mAuth.getCurrentUser();
        Logger.d("LogginInFirebase: " + currentUser.getEmail());
        if (currentUser == null || currentUser.isAnonymous()) {
            Logger.d("LogginInFirebase");
            mAuth.signInWithEmailAndPassword("hoaivubk@gmail.com", "poiuyt01")
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            Logger.d("[>] logged in");
                        }
                    });
        }

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        currentToken = SharedPref.getToken();

        boolean startFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false);
        if (startFromNotification) {
            stopLocationUpdate();
            stopBLEScan(true);
            stopSelf();
        }

        //-- load Shipment Location
        updateNewShipmentLocations(true);
        if (userTimezone == null) {
            updateUserInformation();
        }

        if (hasGoogleClient) {
            getLastLocation();
        }

        checkIfNeedToCreateShipmentOnBoot();
        isConnected = NetworkUtils.isInternetAvailable();
        App.serviceStarted();
        startBLEAndLocationUpdate();
        uploadDataToServer();
        return START_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Logger.i("onBind");
        //stopForeground(true);
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        // Called when a client returns to the foreground and binds once again with this service. The
        // service should cease to be a foreground service when that happends.
        //stopForeground(true);
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Logger.i("onUnbind");

        // Called when the last client unibinds from this service. If this method is called due to a
        // configuration change in MainActivity, we do nothing. Otherwise, we make this service a foreground service.
//        Logger.i("Starting foreground service");
//        startForeground(NOTIFICATION_ID, getNotification());
        return true;
    }

    @Override
    public void onDestroy() {
        FirebaseCrash.log("Service was destroyed");
        mServiceHandler.removeCallbacks(null);
        stopLocationUpdate();
        stopBLEScan(true);

        App.serviceEnded();
        unregisterEventBus();
        unregisterReceiver(mBatteryLevelReceiver);

        if (handlerThread != null) {
            handlerThread.quitSafely();
        }
        super.onDestroy();
    }


    private void startBLEAndLocationUpdate() {
        Logger.i("[+] startBLEAndLocationUpdate ...");
        startLocationUpdate();
        startBLEScan();
    }

    private void startBLEScan() {
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            BluetoothAdapter.getDefaultAdapter().enable();
        }

        mBeaconManager.setBackgroundMode(false);
        syncSettingsToService();

    }

    private void stopBLEScan(boolean exit) {
        if (exit) {
            mBeaconManager.unbind(this);
        } else {
            mBeaconManager.setBackgroundMode(true);
        }
    }

    private void syncSettingsToService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ScanJobScheduler.getInstance().applySettingsToScheduledJob(this, mBeaconManager);
        }
    }

    private void startLocationUpdate() {
        Logger.d("[+] startLocationUpdate: " + hasGoogleClient);
//        if (handlerThread == null) {
//            handlerThread = new HandlerThread(AppConfig.TAG);
//            handlerThread.start();
//        }

        //Looper.prepare();
        if (hasGoogleClient) {
            try {
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            } catch (SecurityException unlikely) {
                Logger.e("Lost location permission. Could not request update", unlikely);
            }
        } else {
            if (locationListener == null) {
                locationListener = new GeneralLocationListener(this);
            }

            gpsLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            towerLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                    (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {

                if (gpsLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    gpsLocationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            AppConfig.FASTEST_UPDATE_INTERVAL/5,
                            AppConfig.LOCATION_PROVIDERS_MIN_REFRESH_DISTANCE,
                            locationListener,Looper.myLooper());
                }

                if (towerLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    towerLocationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            AppConfig.FASTEST_UPDATE_INTERVAL/5,
                            AppConfig.LOCATION_PROVIDERS_MIN_REFRESH_DISTANCE,
                            locationListener,
                            Looper.myLooper());
                }
            }
        }
    }

    private void getLastLocation() {
        if (hasGoogleClient) {
            try {
                mFusedLocationClient.getLastLocation()
                        .addOnCompleteListener(new OnCompleteListener<Location>() {
                            @Override
                            public void onComplete(@NonNull Task<Location> task) {
                                if (task.isSuccessful() && task.getResult() != null) {
                                    mLocation = task.getResult();
                                } else {
                                    Logger.i("[GPS] Failed to get location.");
                                }
                            }
                        });
            } catch (SecurityException unlikely) {
                Logger.i("[GPS] Lost location permission." + unlikely);
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            gpsLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            towerLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        }
    }

    private void broadcastData() {
        Logger.i("[+] broadcasting, isAtStartLocations #" + isAtStartLocations() + " _mShouldCreateOnBoot: " + _mShouldCreateOnBoot);
        //FirebaseDatabase.getInstance().getReference().setValue("[+] broadcasting, isAtStartLocations #" + isAtStartLocations() + " _mShouldCreateOnBoot: " + _mShouldCreateOnBoot);
        List<BeaconPackage> dataList = getDataPackageList();

        if (dataList == null || dataList.size() == 0 || mLocation == null) {
            Logger.d("[-] no new data");
            return; // not update if no event data;
        }

        if (!isAtStartLocations()) {
            Logger.d("Can you run here?");
            _mShouldCreateOnBoot = false;
            SharedPref.saveOnBoot(false);
        }
        checkAndCreateShipment(dataList);

        List<CellTower> cellTowerList = getAllCellInfo();
        BroadcastEvent event = new BroadcastEvent();
        event.setBeaconPackageList(dataList);
        event.setLocation(mLocation);
        event.setGatewayId(getGatewayId());
        event.setCellTowerList(cellTowerList);

        Logger.d("[>] saving to firebase");
        DatabaseReference locRef = ref.child(System.currentTimeMillis() + "");
        locRef.setValue(ToFirebase.fromRaw(event));

        isConnected = NetworkUtils.isInternetAvailable();
        if (NetworkUtils.isInternetAvailable()) {
            Logger.d("[Online] Network is online");
            //1. upload old data
            List<EventData> evdtList = eventBox.getAll();
            for (EventData evdt : evdtList) {
                final long evId = evdt.getId();
                Logger.d("[*] check: " + evdt.toString());
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
            Logger.d("[Offline] Network is offline, going to store data");
            EventData evdt = new EventData();
            evdt.setPhoneImei(getGatewayId());
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
            Logger.d("[+] stored #" + id);
        }
    }



    private synchronized List<BeaconPackage> getDataPackageList() {
        //only get device that has data in 10 minute back
        List<BeaconPackage> dataList = new ArrayList<>();

        for (Object o : deviceMap.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            String key = (String)entry.getKey();
            BeaconPackage data = (BeaconPackage) entry.getValue();
            if (data.isShouldUpload() && isPaired(data)) {
                dataList.add(data);
                deviceMap.get(data.getBluetoothAddress()).setShouldUpload(false); // to prevent repeat upload

            }
        }

        return dataList;
    }

    private List<BeaconPackage> getAll() {
        return new ArrayList<>(deviceMap.values());
    }

    private synchronized boolean isPaired(BeaconPackage data) {
        Logger.d("isPaired: " + _paired_beacon.size());
        return _paired_beacon.contains(data.getSerialNumberString());
    }

    private void updatePairedList() {
        Logger.d("[+PairedList]  updatingToken: " + updatingToken + ", isConnected: " + NetworkUtils.isInternetAvailable() + ", updatingPairedList: " + updatingPairedList);
        if (updatingToken || !NetworkUtils.isInternetAvailable() || updatingPairedList) return;
        updatingPairedList = true;
        WebService.getPairedBeacons(getGatewayId(), currentToken,  new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //updateToken(3600*1000);
                updatingPairedList = false;
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String strBody = response.body() != null ? response.body().string() : null;
                Logger.d("WhyNull: " + strBody);
                if (!TextUtils.isEmpty(strBody)) {
                    PairedBeaconResponse pbr = GsonUtils.getInstance().fromJson(strBody, PairedBeaconResponse.class);
                    Logger.d("Paired-Beacons: " + (pbr.getResponse()!=null ? pbr.getResponse().size() : "null"));
                    if (pbr != null && pbr.getStatus().getCode() == 0) {
                        _paired_beacon.addAll(pbr.getResponse());
                        refineDeviceMap();
                    } else {
                        updateToken(3*1000, false);
                    }
                } else {
                    updateToken(3*1000, false);
                }
                updatingPairedList = false;
            }
        });

        WebService.getPairedPhones(currentToken, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Logger.d("GetPairedPhones" + response.body().string());
                //{
                //  "status": {
                //    "code": 0,
                //    "message": "Success"
                //  },
                //  "response": [
                //    {
                //      "pairedPhoneID": 195,
                //      "active": true,
                //      "pairedPhoneIMEI": "imei1",
                //      "company": 1464,
                //      "description": null,
                //      "pairedBeaconID": "b2"
                //    },
                //    {
                //      "pairedPhoneID": 194,
                //      "active": true,
                //      "pairedPhoneIMEI": "imei2",
                //      "company": 1464,
                //      "description": null,
                //      "pairedBeaconID": "b1"
                //    }
                //  ],
                //  "totalCount": 2
                //}
            }
        });

    }

    private synchronized void refineDeviceMap() {
        long now = (new Date()).getTime();
        Iterator it = deviceMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            BeaconPackage data = (BeaconPackage) entry.getValue();
//            if (now - data.getTimestamp() >= AppConfig.DEVICE_MAX_AGE) {
            if (data.getReadingAge() >= AppConfig.DEVICE_MAX_AGE) {
                it.remove();
            } else {
                updateDeviceStateInMap(data);
            }

            if (_paired_beacon != null && _paired_beacon.contains(data.getSerialNumberString())) {
                setPaired(data.getBluetoothAddress(), true);
            }
        }
        broadCastAllBeacon();
    }

    private void broadCastAllBeacon() {
        List<BeaconPackage> dataList = getAll();
        List<CellTower> cellTowerList = getAllCellInfo();
        BroadcastEvent event = new BroadcastEvent();
        event.setBeaconPackageList(dataList);
        event.setLocation(mLocation);
        event.setGatewayId(getGatewayId());
        event.setCellTowerList(cellTowerList);
        EventBus.getDefault().removeStickyEvent(BroadcastEvent.class);
        EventBus.getDefault().postSticky(event);
    }

    private void setPaired(String id, boolean paired) {
        if (deviceMap.containsKey(id)) {
            deviceMap.get(id).setPaired(paired);
        }
    }

    private void updateDeviceStateInMap(final BeaconPackage data) {
        if (updatingToken || !NetworkUtils.isInternetAvailable()) return;
        WebService.getDevice(data.getSerialNumberString(), currentToken,  new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                updateToken(60*1000, false);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() != null) {
                    String strData = response.body() != null ? response.body().string() : "";

//                    DeviceResponse deviceResponse = TextUtils.isEmpty(strData) ? null : gson.fromJson(strData, DeviceResponse.class);
                    DeviceResponse deviceResponse = TextUtils.isEmpty(strData) ? null : GsonUtils.getInstance().fromJson(strData, DeviceResponse.class);
                    if (deviceResponse != null) {
                        if (deviceResponse.getStatus() != null && deviceResponse.getStatus().getCode() == 0) {
                            Device device = deviceResponse.getResponse();
                            if (device != null) {
                                //[+]
                                //Logger.i("[+] got device");
                                String lastReadingTimeISO = device.getLastReadingTimeISO();
                                String lastShipmentStatus = device.getShipmentStatus();

                                if (TextUtils.isEmpty(lastShipmentStatus)||
                                        TextUtils.isEmpty(lastReadingTimeISO) ||
                                        lastShipmentStatus.equalsIgnoreCase("null")) {
                                    deviceMap.get(data.getBluetoothAddress()).setForedCreateNew(true);
                                } else if (lastShipmentStatus.equalsIgnoreCase("Ended")) {
                                    deviceMap.get(data.getBluetoothAddress()).setForedCreateNew(true);
                                } else {
                                }
                            }
                        } else {
                            //deviceMap.get(data.getBluetoothAddress()).setReadingCount(0);
                            updateToken(10 * 1000, false);
                        }
                    }
                } else {
                    updateToken(60*1000, false);
                }
            }
        });
    }

//    private long startTime = System.currentTimeMillis();
    private void checkAndCreateShipment(List<BeaconPackage> dataList) {
//        if (System.currentTimeMillis() - startTime <= 30 * 1000) { // 30 sec
//            return;
//        } else {
//            startTime = System.currentTimeMillis();
//        }

        Logger.d("[+] checkAndCreateShipment: updatingToken: #" + updatingToken + "isConnected: #" + NetworkUtils.isInternetAvailable());
        if (updatingToken || !NetworkUtils.isInternetAvailable()) return;
        int idx = 0;
        for (final BeaconPackage data: dataList) {
            Logger.d("ShipmentCreated #" + (idx++));
            Logger.i("[" + data.getSerialNumber() +"] count: " + data.getReadingCount() + ", isStartLocation: " + isAtStartLocations() + ", isShouldCreate: " + data.isShouldCreateShipment() + ", [+] foreCreate: " + data.isForedCreateNew() + ", onBoot: " + data.isShouldCreateOnBoot());
            //if ((data.isShouldCreateOnBoot() && isAtStartLocations()) || (data.isForedCreateNew() && isAtStartLocations())) {
            if ((data.isShouldCreateOnBoot() && isAtStartLocations())) {

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
        if (companyShipmentLocations == null) return false;

        if (mLocation == null) {
            return false;
        }

        for (LocationBody loc : companyShipmentLocations) {
            LatLng ll = loc.getLocation();
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
        Logger.d("[+] onBeaconServiceConnect");
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
                        Logger.d("[+] paired-list");
                        deviceMap.put(beacon.getBluetoothAddress(), bt04);
                    }

                    Logger.d("[+] prepare updateing paired-list");
                    updatePairedList(); // update paired list from server
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

    @SuppressLint("MissingPermission")
    private String getGatewayId() {
        if (TextUtils.isEmpty(AppConfig.GATEWAY_ID)) {
            if (mTelephonyManager == null) {
                mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            }
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
        }
        return AppConfig.GATEWAY_ID;
    }

    @SuppressLint("MissingPermission")
    private List<CellTower> getAllCellInfo() {
        Logger.i("getAllCellInfo");
        if (mTelephonyManager == null) {
            mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        }
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

    /**
     * Sets the location request parameters.
     */
    private void createLocationRequest() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(AppConfig.UPDATE_INTERVAL/5);
        mLocationRequest.setFastestInterval(AppConfig.FASTEST_UPDATE_INTERVAL/5);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setSmallestDisplacement(AppConfig.LOCATION_PROVIDERS_MIN_REFRESH_DISTANCE);
    }

    public void onUpdateLocation(Location location) {
        Logger.i("[+] onUpdateLocation: " + location);
//        if (mLocation == null) {
//            mLocation = location;
//        } else {
//            if (location.getTime() - mLocation.getTime() > AppConfig.LAST_LOCATION_MAX_AGE) {
//                mLocation = location;
//            } else if (location.hasAccuracy() && location.getAccuracy() < mLocation.getAccuracy()) {
//                mLocation = location;
//            }
//        }

        mLocation = location;
        updateNewShipmentLocations(false);

        //Notify anyone listening for broadcasting about the new locatio
        Intent intent = new Intent(ACTION_BROADCAST);
        intent.putExtra(EXTRA_LOCATION, location);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

//        if (serviceIsRunningInForeground(this)) {
//            mNotificationManager.notify(NOTIFICATION_ID, getNotification());
//        }
    }
    /**
     * Removes location updates. Note that in this sample we merely log the
     * {@link SecurityException}.
     */
    public void stopLocationUpdate() {
        Logger.i("[+] stopLocationUpdate");

        try {
            if (hasGoogleClient) {
                mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            }

            if (locationListener != null) {
                gpsLocationManager.removeUpdates(locationListener);
                towerLocationManager.removeUpdates(locationListener);
            }
        } catch (SecurityException unlikely) {
            Logger.i("[GPS] Lost location permission. Could not remove updates. " + unlikely);
        }

    }

    private Notification getNotification() {
        Intent intent = new Intent(this, BeaconService.class);
        CharSequence text = ServiceUtils.getLocationText(mLocation);

//        if (exit) {
            // Extra to help us figure out if we arrived in onStartCommand via the notification or not
//             intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);
//        }
        // The PendingIntent that leads to a call to onStartCommand() in this service
//        PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // The PendingIntent to launch activity
        PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

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

    // Return true if this is a foreground service
    public boolean serviceIsRunningInForeground(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo serviceInfo : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (getClass().getName().equals(serviceInfo.service.getClassName())) {
                if (serviceInfo.foreground) {
                    return true;
                }
            }
        }
        return false;
    }

    private void checkIfNeedToCreateShipmentOnBoot() {
        _mShouldCreateOnBoot = SharedPref.isOnBoot();
        //set flag to false
        //SharedPref.saveOnBoot(false);
        Logger.d("[+] checkIfNeedToCreateShipmentOnBoot: " + _mShouldCreateOnBoot);
    }
//    private void _updateBootCompleted() {
//        Logger.d("[+] changedBootStatus");
//        _mShouldCreateOnBoot = false;
//        SharedPref.saveOnBoot(false);
//    }

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

    @Subscribe
    public void onUpdateEvent(UpdateEvent updateEvent) {
        stopBLEScan(false);
        stopLocationUpdate();
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
            Logger.d("[+ Updating token ...]");
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

    private void updateShipmentLocations(List<LocationBody> shipmentLocations) {
        if (shipmentLocations != null) {
            Logger.i("[ShipmentLocations]: " + shipmentLocations.size());
            this.companyShipmentLocations = shipmentLocations;
        }
    }
    private void updateNewShipmentLocations(boolean isFirst) {
        if (updatingToken || !NetworkUtils.isInternetAvailable()) return;
        if (timesOfDataUpdated >= AppConfig.COUNT_FOR_UPDATE_SHIPMENT_LOCATIONS || isFirst) {
            WebService.getLocations(1, 1000, null, null, currentToken, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    updateToken(60*1000, false);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String bodyStr = (response.body() != null) ? response.body().string() : null;
                    if (!TextUtils.isEmpty(bodyStr)) {
                        LocationResponse response1 = GsonUtils.getInstance().fromJson(bodyStr, LocationResponse.class);
                        if (response1!= null && response1.getStatus() != null && response1.getStatus().getCode() == 0) {
                            updateShipmentLocations(response1.getResponse());
                        } else {
                            updateToken(10 *1000, false);
                        }
                    } else {
                        updateToken(10 * 1000, false);
                    }
                }
            });
            //reset
            timesOfDataUpdated = 0;
        } else {
            timesOfDataUpdated++;
        }
    }

    private Runnable reLoginRunnable = new Runnable() {
        @Override
        public void run() {
            stopBLEScan(false);
            stopLocationUpdate();
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
                Logger.d("[+] Token update in " + timeMillisDelay / 1000 + "s");
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
                        //startBLEAndLocationUpdate();
                    } else {
                        updateToken(60 * 1000, false);
                    }
                } else {
                    updateToken(60 * 1000, false);
                }
            }
        });
    }

    private void updateUserInformation() {
        //
        FirebaseCrash.log("[FirebaseCrash] + getTimezone");
        if (updatingToken || !NetworkUtils.isInternetAvailable()) return;
        WebService.getUser(currentToken, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Logger.i("Not able get user");
                //updateToken(60*1000);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String strBody = response.body() != null ? response.body().string() : "";
                if (!TextUtils.isEmpty(strBody)) {
                    Logger.d("+User: " + strBody);
                    UserResponse userResponse = GsonUtils.getInstance().fromJson(strBody, UserResponse.class);
                    if (userResponse != null && userResponse.getStatus() != null && userResponse.getStatus().getCode() == 0) {
                        UserBody body = null;
                        if (userResponse != null) {
                            body = userResponse.getResponse();
                        }

                        String tzId = null;
                        long coId = 0;
                        if (body != null) {
                            tzId = body.getTimeZone();
                            coId = body.getInternalCompanyId();
                        }

                        if (TextUtils.isEmpty(tzId)) tzId = "GMT";
                        userTimezone = TimeZone.getTimeZone(tzId);
                        SharedPref.saveUserTimezone(tzId);
                        SharedPref.saveCompanyId(coId);
                    } else {
                        updateToken(3 * 1000, false);
                    }
                } else {
                    updateToken(3 * 1000, false);
                }
            }
        });
    }

}
