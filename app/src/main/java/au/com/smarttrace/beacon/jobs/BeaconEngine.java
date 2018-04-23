package au.com.smarttrace.beacon.jobs;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Looper;
import android.os.NetworkOnMainThreadException;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.TZONE.Bluetooth.BLE;
import com.TZONE.Bluetooth.ILocalBluetoothCallBack;
import com.TZONE.Bluetooth.Temperature.BroadcastService;
import com.TZONE.Bluetooth.Temperature.Model.Device;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import au.com.smarttrace.beacon.App;
import au.com.smarttrace.beacon.AppConfig;
import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.db.PhonePaired;
import au.com.smarttrace.beacon.db.PhonePaired_;
import au.com.smarttrace.beacon.model.BeaconPackage;
import au.com.smarttrace.beacon.model.BroadcastEvent;
import au.com.smarttrace.beacon.model.MeasuringDistance;
import au.com.smarttrace.beacon.net.DataUtil;
import au.com.smarttrace.beacon.net.WebService;
import au.com.smarttrace.beacon.service.NetworkUtils;
import io.objectbox.Box;
import io.objectbox.query.Query;

public class BeaconEngine {
    private Context mContext;
    private Box<PhonePaired> pairedBox;

//    private LocationRequest mLocationRequest;
//    private LocationSettingsRequest mLocationSettingsRequest;
//    private SettingsClient mSettingsClient;
//    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private Location mCurrentLocation;
//    private Boolean mRequestingLocationUpdates = false;

//    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 8000;
//    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private LocationServiceWrapper locationWrapper;
    private int phoneBatteryLevel = 100;

    private BluetoothAdapter _BluetoothAdapter;
    private BroadcastService _BroadcastService;
    private List<Device> _DeviceList = new ArrayList<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    BeaconEngine(Context context) {
        mContext = context;
        createLocationCallback();
        locationWrapper = new LocationServiceWrapper(mContext, mLocationCallback);
        pairedBox = ((App) mContext.getApplicationContext()).getBoxStore().boxFor(PhonePaired.class);
    }

    /**
     * Creates a callback for receiving location events.
     */
    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onUpdateLocation(locationResult.getLastLocation());
            }
        };
    }

    private void onUpdateLocation(Location location) {
        Logger.d("[>_] onLocationChanged: " + location.getAccuracy());
        if (mCurrentLocation == null) {
            mCurrentLocation = location;
        } else {
            //1. check time age
            if (location.getTime() - mCurrentLocation.getTime() >= AppConfig.SCANNING_TIMEOUT) {
                mCurrentLocation = location;
            } else
            //2. check accuracy
            if (location.hasAccuracy() && (location.getAccuracy() < mCurrentLocation.getAccuracy())) {
                mCurrentLocation = location;
            }
        }
    }

    @WorkerThread
    public boolean scanAndUpload() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new NetworkOnMainThreadException();
        }

        startBLEScan(10);


        final ScheduledFuture locScheduledHandler = scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                locationWrapper.startLocationUpdates();
                Looper.loop();
            }
        }, 0, TimeUnit.MILLISECONDS);

        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                //stop location scan
                locationWrapper.stopLocationUpdates();
                locScheduledHandler.cancel(true);
                uploadToServer();
            }
        }, AppConfig.SCANNING_TIMEOUT, TimeUnit.MILLISECONDS);
        return true;
    }

    @WorkerThread
    private void startBLEScan(long timeout) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new NetworkOnMainThreadException();
        }

        final ScheduledFuture bleScheduledHandler = scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                Logger.d("[>_] startBLEScan ...");
                Looper.prepare();
                try {
                    if (_BroadcastService == null) {
                        _BroadcastService = new BroadcastService();
                    }
                    final BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
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

    //uploadToServer
    private void uploadToServer() {
        Logger.i("[>_] starting upload to server ...");

        updateBatteryLevel();

        BroadcastEvent be = new BroadcastEvent();
        be.setLocation(mCurrentLocation);
        be.setCellTowerList(NetworkUtils.getAllCellInfo());
        be.setGatewayId(NetworkUtils.getGatewayId());

        List<BeaconPackage> lbp = new ArrayList<>();
        for (Device d : _DeviceList) {
            BeaconPackage bp = new BeaconPackage();
            bp.setFirmware(d.Firmware);
            bp.setDistance(MeasuringDistance.calculateAccuracy(-60, d.RSSI));
            bp.setHumidity(d.Humidity);
            bp.setModel(d.HardwareModel);
            bp.setBatteryLevel(d.Battery);
            bp.setPhoneBatteryLevel(phoneBatteryLevel);
            bp.setTemperature(d.Temperature);
            bp.setSerialNumber(d.SN);
            bp.setBluetoothAddress(d.MacAddress);
            bp.setName(d.Name);
            bp.setRssi(d.RSSI);
            bp.setTimestamp(d.LastScanTime.getTime());
            if (isPaired(bp)) {
                lbp.add(bp);
            }
        }

        for (BeaconPackage bp: lbp) {
            Logger.d("[>_] SN: "+ bp.getSerialNumberString() + ", Last Reading: " + new Date(bp.getTimestamp()).toString() + " Temperature: " + bp.getTemperatureString() + " Humidity: " + bp.getHumidity());
        }

        be.setBeaconPackageList(lbp);
        //TODO: update to firestore
        //--
        WebService.sendEvent(DataUtil.formatData(be));
    }

    private boolean isPaired(BeaconPackage data) {
        Logger.i("[>] Checking if paired : " + data.getSerialNumberString());
        Query<PhonePaired> query = pairedBox.query()
                .equal(PhonePaired_.phoneImei, NetworkUtils.getGatewayId())
                .equal(PhonePaired_.beaconSerialNumber, data.getSerialNumberString())
                .build();
        Logger.i("[>... isPaired: ] " + (query.findFirst() != null));
        return query.findFirst() != null;
    }

    private void updateBatteryLevel() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        intentFilter.addAction(Intent.ACTION_BATTERY_LOW);
        intentFilter.addAction(Intent.ACTION_BATTERY_OKAY);
        Intent batteryStatus = mContext.registerReceiver(null, intentFilter);

        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            phoneBatteryLevel = Math.round(level / (float) scale);
        }
    }
}