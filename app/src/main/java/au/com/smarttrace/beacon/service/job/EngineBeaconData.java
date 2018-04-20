package au.com.smarttrace.beacon.service.job;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.NetworkOnMainThreadException;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
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

public class EngineBeaconData {
    private Context mContext;
    private Box<PhonePaired> pairedBox;

    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;

    private Location mLastKnownLocation;

    private BluetoothAdapter _BluetoothAdapter;
    private BroadcastService _BroadcastService;
    private List<Device> _DeviceList = new ArrayList<>();

    EngineBeaconData(Context context) {
        mContext = context;
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onUpdateLocation(locationResult.getLastLocation());
            }
        };
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(10*1000);
        mLocationRequest.setFastestInterval(5*1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        pairedBox = ((App) mContext.getApplicationContext()).getBoxStore().boxFor(PhonePaired.class);
    }

    private void onUpdateLocation(Location lastLocation) {
        mLastKnownLocation = lastLocation;
    }

    @WorkerThread
    public boolean uploadData() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new NetworkOnMainThreadException();
        }

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
        final ScheduledFuture scheduledFuture = scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                try {
                    if (_BroadcastService == null) {
                        _BroadcastService = new BroadcastService();
                    }
                    final BluetoothManager bluetoothManager =
                            (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
                    _BluetoothAdapter = bluetoothManager.getAdapter();
                    if (_BroadcastService.Init(_BluetoothAdapter, _LocalBluetoothCallBack)) {
                    } else {
                        return;
                    }
                } catch (Exception ex) {
                }

                _BroadcastService.StartScan();
                Looper.loop();
            }
        }, 0, TimeUnit.SECONDS);

        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                _BroadcastService.StopScan();
                scheduledFuture.cancel(true);
            }
        }, 10, TimeUnit.SECONDS);

        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                startScanLocation();
                Looper.loop();
            }
        }, 0, TimeUnit.SECONDS);

        Executors.newScheduledThreadPool(1).schedule(new Runnable() {
            @Override
            public void run() {
                broadcastToServer();
            }
        }, 30, TimeUnit.SECONDS);
        return true;
    }

    private void startScanLocation() {
        try {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        } catch (SecurityException unlikely) {
            Logger.e("Lost location permission. Could not request update", unlikely);
        }
        getLastLocation();
    }

    private void getLastLocation() {
        try {
            mFusedLocationClient.getLastLocation()
                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                mLastKnownLocation = task.getResult();
                            } else {
                                Logger.i("[GPS] Failed to get location.");
                            }
                        }
                    });
        } catch (SecurityException unlikely) {
            Logger.i("[GPS] Lost location permission." + unlikely);
        }
    }


    private void AddOrUpdate(final BLE ble){
        try {
            Device d = new Device();
            d.fromScanData(ble);
            if(d == null || d.SN == null || d.SN.length() != 8)
                return;

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
            Log.e("home", "AddOrUpdate:" + ex.toString());
        }
    }

    public ILocalBluetoothCallBack _LocalBluetoothCallBack = new ILocalBluetoothCallBack() {

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

    //broadcastToServer
    private void broadcastToServer() {
        Logger.i("[> BackgroundJob]");
        BroadcastEvent be = new BroadcastEvent();
        be.setLocation(mLastKnownLocation);
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
            bp.setPhoneBatteryLevel(d.Battery);
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
        be.setBeaconPackageList(lbp);
        WebService.sendEvent(DataUtil.formatData(be));
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
}