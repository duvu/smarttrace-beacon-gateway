package au.com.smarttrace.beacon.service;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
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
import com.google.firebase.crash.FirebaseCrash;
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

import au.com.smarttrace.beacon.AppConfig;
import au.com.smarttrace.beacon.AppContants;
import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.App;
import au.com.smarttrace.beacon.R;
import au.com.smarttrace.beacon.SharedPref;
import au.com.smarttrace.beacon.model.BT04Package;
import au.com.smarttrace.beacon.model.BroadcastEvent;
import au.com.smarttrace.beacon.model.CellTower;
import au.com.smarttrace.beacon.model.ExitEvent;
import au.com.smarttrace.beacon.model.UpdateEvent;
import au.com.smarttrace.beacon.net.DataUtil;
import au.com.smarttrace.beacon.net.Http;
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
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static au.com.smarttrace.beacon.AppConfig.NOTIFICATION_ID;

public class BeaconService extends Service implements BeaconConsumer {
    private static final String PACKAGE_NAME    = "au.com.smarttrace.beacon";
    private static final String CHANNEL_ID      = "channel_01";

    private static final String EXTRA_STARTED_FROM_NOTIFICATION     = PACKAGE_NAME + ".started_from_notification";
    public static final String EXTRA_STARTED_FROM_BOOTSTRAP        = PACKAGE_NAME + ".started_from_bootstrap";

    public static final String ACTION_BROADCAST                     = PACKAGE_NAME + ".broadcast";
    public static final String EXTRA_LOCATION                       = PACKAGE_NAME + ".location";

    private boolean mChangingConfiguration      = false;
    private NotificationManager mNotificationManager;

    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private Handler mServiceHandler;

    private Location mLocation;

    private TimeZone userTimezone = null;

    private FirebaseAnalytics mFirebaseAnalytics;

    boolean _mShouldCreateOnBoot = false;

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
    Map<String, BT04Package> deviceMap = new ConcurrentHashMap<>();
    private Set<String> _paired_beacon = new HashSet<>();

    private List<LocationBody> companyShipmentLocations = null;

    private LocationManager gpsLocationManager;
    private LocationManager passiveLocationManager;
    private LocationManager towerLocationManager;
    private GeneralLocationListener locationListener;

    private TelephonyManager mTelephonyManager;

    private boolean hasValidBeaconData = false;
    private boolean hasValidLocationData = false;

    private final IBinder mBinder = new LocalBinder();

    private final Gson gson = new Gson();

    private int timesOfDataUpdated = 0;
    private boolean hasGoogleClient = false;

    ConnectivityManager connectivityManager;

    public BeaconService() {
    }

    @Override
    public void onCreate() {
        Logger.i("[BeaconService] onCreated");
        mBeaconManager.setBackgroundBetweenScanPeriod(AppConfig.UPDATE_INTERVAL_IN_MILLISECONDS);
        mBeaconManager.setBackgroundScanPeriod(AppConfig.UPDATE_PERIOD);
        mBeaconManager.setForegroundBetweenScanPeriod(AppConfig.UPDATE_INTERVAL_IN_MILLISECONDS);
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
        }

        if (mFusedLocationClient != null && mLocationCallback != null && mLocationRequest != null) {
            hasGoogleClient = true;
            getLastLocation();
        }

        //-- load Shipment Location
        updateNewShipmentLocations(true);
        if (userTimezone == null) {
            updateUserInformation();
        }

        HandlerThread handlerThread = new HandlerThread(AppConfig.TAG);
        handlerThread.start();
        mServiceHandler = new Handler(handlerThread.getLooper());
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Android O requires a Notification Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            // Create the channel for the notification
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);

            //set the notification-channel for the Notification Manager
            mNotificationManager.createNotificationChannel(channel);
        }
        registerEventBus();
        NetworkUtils.init(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        intentFilter.addAction(Intent.ACTION_BATTERY_LOW);
        intentFilter.addAction(Intent.ACTION_BATTERY_OKAY);
        //intentFilter.addAction(Inte);
        registerReceiver(mBatteryLevelReceiver, intentFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.i("##Service Started");
        boolean startFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false);
        if (startFromNotification) {
            removeLocationUpdates();
            stopSelf();
        }

        boolean startFromBoot = intent.getBooleanExtra(EXTRA_STARTED_FROM_BOOTSTRAP, false);
        if (startFromBoot) {
            startForeground(NOTIFICATION_ID, getNotification());
        }

        App.serviceStarted();
        requestUpdateData();
        return START_NOT_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mChangingConfiguration = true;
    }
    @Override
    public IBinder onBind(Intent intent) {
        Logger.i("onBind");
        stopForeground(true);
        mChangingConfiguration = false;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        // Called when a client returns to the foreground and binds once again with this service. The
        // service should cease to be a foreground service when that happends.
        stopForeground(true);
        mChangingConfiguration = false;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Logger.i("onUnbind: " + mChangingConfiguration);

        // Called when the last client unibinds from this service. If this method is called due to a
        // configuration change in MainActivity, we do nothing. Otherwise, we make this service a foreground service.
        Logger.i("Starting foreground service");
        startForeground(NOTIFICATION_ID, getNotification());
        return true;
    }

    @Override
    public void onDestroy() {
        FirebaseCrash.log("Service was destroyed");
        mServiceHandler.removeCallbacksAndMessages(null);
        removeLocationUpdates();
        stopBLEScan(true);

        App.serviceEnded();
        unregisterEventBus();
        unregisterReceiver(mBatteryLevelReceiver);
        super.onDestroy();
    }



    private void requestUpdateData() {
        Logger.i("[starting scan ble and location] ...");
        requestLocationUpdates();
        startBLEScan();

        //startAbsoluteTimer();
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

    private void requestLocationUpdates() {
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
                            AppConfig.UPDATE_INTERVAL_IN_MILLISECONDS,
                            AppConfig.LOCATION_PROVIDERS_MIN_REFRESH_DISTANCE,
                            locationListener);
                }

                if (towerLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    towerLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                            AppConfig.FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS,
                            AppConfig.LOCATION_PROVIDERS_MIN_REFRESH_DISTANCE,
                            locationListener);
                }
            }
        }
    }

    private void broadcastData() {
        Logger.i("Send Data to server");
        if (hasValidLocationData && hasValidBeaconData) {

            List<BT04Package> dataList = getDataPackageList();
            checkAndCreateShipment(dataList);

            List<CellTower> cellTowerList = getAllCellInfo();
            BroadcastEvent event = new BroadcastEvent();
            event.setBT04PackageList(dataList);
            event.setLocation(mLocation);
            event.setGatewayId(getGatewayId());
            event.setCellTowerList(cellTowerList);
            //send data bundle - new protocol
            //Logger.i(DataUtil.formatData(event));
            Logger.i("NetworkConnected? : " + NetworkUtils.isConnected());
            String dataForUpload = DataUtil.formatData(event);
            //upload current data
            Http.getIntance().post(AppConfig.BACKEND_URL_BT04_NEW, dataForUpload, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Logger.i("[Http] failed " + e.getMessage());
                    //Try to re-get token
                    updateToken();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Logger.i("[Http] success " + response.toString());
                }
            });

            broadCastAllBeacon();
        }
    }

    private void broadCastAllBeacon() {
        List<BT04Package> dataList = getAll();
        List<CellTower> cellTowerList = getAllCellInfo();
        BroadcastEvent event = new BroadcastEvent();
        event.setBT04PackageList(dataList);
        event.setLocation(mLocation);
        event.setGatewayId(getGatewayId());
        event.setCellTowerList(cellTowerList);
        EventBus.getDefault().removeStickyEvent(BroadcastEvent.class);
        EventBus.getDefault().postSticky(event);
    }

    private synchronized List<BT04Package> getDataPackageList() {
        //only get device that has data in 10 minute back
        long now = (new Date()).getTime();

        List<BT04Package> dataList = new ArrayList<>();
        for (Object o : deviceMap.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            String key = (String)entry.getKey();
            BT04Package data = (BT04Package) entry.getValue();
            if ((data.isShouldUpload() || data.isShouldCreateShipment()) && isPaired(data)) {
                dataList.add(data);
                deviceMap.get(data.getBluetoothAddress()).setShouldUpload(false); // to prevent repeat upload
            }
        }

        return dataList;
    }

    private List<BT04Package> getAll() {
        return new ArrayList<>(deviceMap.values());
    }

    private synchronized boolean isPaired(BT04Package data) {
        Logger.d("isPaired: " + _paired_beacon.size());
        return _paired_beacon.contains(data.getSerialNumberString());
    }

    private synchronized void updatePairedList() {
        WebService.getPairedBeacons(getGatewayId(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String strBody = response.body() != null ? response.body().string() : null;
                if (!TextUtils.isEmpty(strBody)) {
                    PairedBeaconResponse pbr = gson.fromJson(strBody, PairedBeaconResponse.class);
                    Logger.d("Paired-Beacons: " + (pbr.getResponse()!=null ? pbr.getResponse().size() : "null"));
                    _paired_beacon.addAll(pbr.getResponse());


                }
            }
        });
    }

    private synchronized void refineDeviceMap() {
        long now = (new Date()).getTime();
        Iterator it = deviceMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            BT04Package data = (BT04Package) entry.getValue();
            if (now - data.getTimestamp() >= AppConfig.DEVICE_MAX_AGE) {
                it.remove();
            } else {
                updateDeviceStateInMap(data);
            }

            if (_paired_beacon != null && _paired_beacon.contains(data.getSerialNumberString())) {
                deviceMap.get(data.getBluetoothAddress()).setPaired(true);
            }
        }
    }

    private void updateDeviceStateInMap(final BT04Package data) {
        //deviceMap.get(data.getBluetoothAddress()).setShouldCreateOnBoot(_mShouldCreateOnBoot);

        final Date now = new Date();
        WebService.getDevice(data.getSerialNumberString(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //
                Logger.e("Failure to get device", e);
                FirebaseCrash.report(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() != null) {
                    String strData = response.body().string();
                    DeviceResponse deviceResponse = TextUtils.isEmpty(strData) ? null : gson.fromJson(strData, DeviceResponse.class);
                    if (deviceResponse == null) {
                        // no device for this beacon
                        // do remove from map
                        deviceMap.get(data.getBluetoothAddress()).setReadingCount(0);
                    } else {
                        Device device = deviceResponse.getResponse();
                        if (device != null) {
                            //[+]
                            Logger.i("[+] got device");
                            String lastReadingTimeISO = device.getLastReadingTimeISO();
                            String lastShipmentStatus = device.getShipmentStatus();

                            if (TextUtils.isEmpty(lastShipmentStatus)|| TextUtils.isEmpty(lastReadingTimeISO)) {
                                deviceMap.get(data.getBluetoothAddress()).setForedCreateNew(true);
                            } else if (lastShipmentStatus.equalsIgnoreCase("Ended")) {
                                deviceMap.get(data.getBluetoothAddress()).setForedCreateNew(true);
                            } else {
                            }
                        }
                    }
                }
            }
        });
    }

    private synchronized void checkAndCreateShipment(List<BT04Package> dataList) {
        for (final BT04Package data: dataList) {
            Logger.i("[" + data.getSerialNumber() +"] count: " + data.getReadingCount() + ", isStartLocation: " + isAtStartLocations() + ", isShouldCreate: " + data.isShouldCreateShipment() + ", [+] foreCreate: " + data.isForedCreateNew() + ", onBoot: " + data.isShouldCreateOnBoot());
            if ((data.isShouldCreateOnBoot() && isAtStartLocations()) || (data.isForedCreateNew() && isAtStartLocations())) {
                    WebService.createNewAutoSthipment(data.getSerialNumberString(), new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Logger.i("Failed to create shipment");
                        updateToken();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
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
        mBeaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    Logger.i("[BLE] " + beacons.size());
                    hasValidBeaconData = true;
                    _shouldCreateShipmentOnBoot();
                    for (Beacon beacon : beacons) {
                        BT04Package bt04 = deviceMap.get(beacon.getBluetoothAddress());
                        if (bt04 == null) {
                            bt04 = BT04Package.fromBeacon(beacon);
                            //-- first reading

                            bt04.setShouldCreateOnBoot(_mShouldCreateOnBoot);
                        } else {
                            bt04.updateFromBeacon(beacon);
                        }
                        bt04.setPhoneBatteryLevel(mBatteryLevel);
                        deviceMap.put(beacon.getBluetoothAddress(), bt04);
                    }
                }
                refineDeviceMap(); //remove not update device from map
                updatePairedList(); // update paired list from server
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
        mLocationRequest.setInterval(AppConfig.UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(AppConfig.FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(AppConfig.LOCATION_PROVIDERS_MIN_REFRESH_DISTANCE);
    }

    private void getLastLocation() {
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
    }

    public void onUpdateLocation(Location location) {
        Logger.i("[+] Location" + location);
        if (mLocation == null) {
            mLocation = location;
        } else {
            if (location.getTime() - mLocation.getTime() > AppConfig.LAST_LOCATION_MAX_AGE) {
                mLocation = location;
            } else if (location.hasAccuracy() && location.getAccuracy() < mLocation.getAccuracy()) {
                mLocation = location;
            }
        }

        //_shouldCreateShipmentOnBoot();
        if (!isAtStartLocations() && (mLocation != null)) {
            _updateBootCompleted();
        }
        hasValidLocationData = true;
        updateNewShipmentLocations(false);
        refineDeviceMap();
        //Notify anyone listening for broadcasting about the new location
        Intent intent = new Intent(ACTION_BROADCAST);
        intent.putExtra(EXTRA_LOCATION, location);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

        if (serviceIsRunningInForeground(this)) {
            //mNotificationManager.notify(NOTIFICATION_ID, getNotification());
        }

        //send data to server
        broadcastData();
    }
    /**
     * Removes location updates. Note that in this sample we merely log the
     * {@link SecurityException}.
     */
    public void removeLocationUpdates() {
        Logger.i("[GPS] Removing location updates");

        try {
            if (ServiceUtils.isGooglePlayServicesAvailable(this)) {
                mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            } else {
                if (locationListener != null) {
                    gpsLocationManager.removeUpdates(locationListener);
                    towerLocationManager.removeUpdates(locationListener);
                }
            }
            stopSelf();
        } catch (SecurityException unlikely) {
            Logger.i("[GPS] Lost location permission. Could not remove updates. " + unlikely);
        }

    }

    private Notification getNotification() {
        Intent intent = new Intent(this, BeaconService.class);
        CharSequence text = ServiceUtils.getLocationText(mLocation);

        // Extra to help us figure out if we arrived in onStartCommand via the notification or not
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);

        // The PendingIntent that leads to a call to onStartCommand() in this service
        PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // The PendingIntent to launch activity
        PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .addAction(R.drawable.ic_launch, getString(R.string.launch_activity), activityPendingIntent)
                .addAction(R.drawable.ic_cancel, getString(R.string.cancel_service), servicePendingIntent)
                .setContentText(text)
                .setContentTitle(ServiceUtils.getLocationTitle(this))
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.notification)
                .setTicker(text)
                .setWhen(System.currentTimeMillis());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID);
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

    public void wipeAllDataOut() {
        Iterator it = deviceMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            BT04Package data = (BT04Package) entry.getValue();
            data.setForedCreateNew(true);
        }
    }

    private void _shouldCreateShipmentOnBoot() {
        _mShouldCreateOnBoot = SharedPref.isOnBoot();
        Logger.d("[+] _shouldCreateShipmentOnBoot: " + _mShouldCreateOnBoot);
    }
    private void _updateBootCompleted() {
        Logger.d("[+] changedBootStatus");
        _mShouldCreateOnBoot = false;
        SharedPref.saveOnBoot(false);
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

    @Subscribe
    public void onUpdateEvent(UpdateEvent updateEvent) {
        stopBLEScan(false);
        removeLocationUpdates();
        //stopAbsoluteTimer();
        requestUpdateData();
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
        if (timesOfDataUpdated >= AppConfig.COUNT_FOR_UPDATE_SHIPMENT_LOCATIONS || isFirst) {
            WebService.getLocations(1, 1000, null, null, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    updateToken();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String bodyStr = (response.body() != null) ? response.body().string() : null;
                    if (!TextUtils.isEmpty(bodyStr)) {
                        LocationResponse response1 = gson.fromJson(bodyStr, LocationResponse.class);
                        updateShipmentLocations(response1.getResponse());
                    }
                }
            });
            //reset
            timesOfDataUpdated = 0;
        } else {
            timesOfDataUpdated++;
        }
    }

    private void updateToken() {
        WebService.login(SharedPref.getUserName(), SharedPref.getPassword(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                FirebaseCrash.log("Not able to login");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() != null) {
                    LoginResponse data = gson.fromJson(response.body().string(), LoginResponse.class);
                    if (data.getResponse() != null) {
                        SharedPref.saveToken(data.getResponse().getToken());
                        SharedPref.saveExpiredStr(data.getResponse().getExpired());
                        SharedPref.saveTokenInstance(data.getResponse().getInstance());
                    }
                }
            }
        });
    }

    private void updateUserInformation() {
        //
        FirebaseCrash.log("[FirebaseCrash] + getTimezone");
        WebService.getUser(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Logger.i("Not able get user");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() != null) {
                    UserResponse userResponse = gson.fromJson(response.body().string(), UserResponse.class);
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
                }
            }
        });
    }

}
