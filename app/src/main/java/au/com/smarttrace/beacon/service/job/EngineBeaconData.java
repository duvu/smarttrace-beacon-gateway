package au.com.smarttrace.beacon.service.job;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.NetworkOnMainThreadException;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import au.com.smarttrace.beacon.AppConfig;
import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.model.BeaconPackage;
import au.com.smarttrace.beacon.model.BroadcastEvent;
import au.com.smarttrace.beacon.net.DataUtil;
import au.com.smarttrace.beacon.net.WebService;
import au.com.smarttrace.beacon.service.NetworkUtils;

public class EngineBeaconData {
    private Context mContext;

    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;

    private Location mLastKnownLocation;

    private BluetoothAdapter _BluetoothAdapter;
    private BroadcastService _BroadcastService;
    private boolean _IsInit = false;
    private List<Device> _DeviceList = new ArrayList<>();
    private Timer _Timer;

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
        mLocationRequest.setInterval(AppConfig.UPDATE_INTERVAL / 5);
        mLocationRequest.setFastestInterval(AppConfig.FASTEST_UPDATE_INTERVAL / 2);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(AppConfig.LOCATION_PROVIDERS_MIN_REFRESH_DISTANCE);

        try {
            if (_BroadcastService == null) {
                _BroadcastService = new BroadcastService();
            }
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            _BluetoothAdapter = bluetoothManager.getAdapter();
            if (_BroadcastService.Init(_BluetoothAdapter, _LocalBluetoothCallBack)) {
                _IsInit = true;
            } else {
                _IsInit = false;
                return;
            }
        } catch (Exception ex) {
        }

    }

    private void onUpdateLocation(Location lastLocation) {
        mLastKnownLocation = lastLocation;
    }

    @WorkerThread
    public boolean uploadData() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new NetworkOnMainThreadException();
        }

        startScanBLE();
        startScanLocation();

        Executors.newScheduledThreadPool(2).schedule(new Runnable() {
            @Override
            public void run() {
                broadcastToServer();
            }
        }, 5, TimeUnit.SECONDS);
        return true;
    }

    private void startScanBLE() {
        try {
            if(_Timer != null)
                _Timer.cancel();
            _Timer = new Timer();
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    try {
                        synchronized (this) {
                            if (_IsInit) {
                                _BroadcastService.StartScan();
                            }
                        }
                    } catch (Exception ex){}
                }
            };
            _Timer.schedule(timerTask, 1000, 500);
        } catch (Exception ex){
            return;
        }
    }

    private void startScanLocation() {
        HandlerThread hThread = new HandlerThread("background_job_smarttrace_io");
        hThread.start();
        Handler mHandler = new Handler(hThread.getLooper());
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                } catch (SecurityException unlikely) {
                    Logger.e("Lost location permission. Could not request update", unlikely);
                }
            }
        });
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
            bp.setDistance(0.0);
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

            lbp.add(bp);
        }
        be.setBeaconPackageList(lbp);
        WebService.sendEvent(DataUtil.formatData(be));
    }
}