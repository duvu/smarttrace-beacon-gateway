package au.com.smarttrace.beacon.service.jobs;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.NetworkOnMainThreadException;
import android.support.annotation.WorkerThread;

import com.TZONE.Bluetooth.BLE;
import com.TZONE.Bluetooth.ILocalBluetoothCallBack;
import com.TZONE.Bluetooth.Temperature.BroadcastService;
import com.TZONE.Bluetooth.Temperature.Model.Device;
import com.google.firebase.iid.FirebaseInstanceId;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import au.com.smarttrace.beacon.App;
import au.com.smarttrace.beacon.AppConfig;
import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.db.EventData;
import au.com.smarttrace.beacon.db.PhonePaired;
import au.com.smarttrace.beacon.db.PhonePaired_;
import au.com.smarttrace.beacon.db.SensorData;
import au.com.smarttrace.beacon.model.BeaconPackage;
import au.com.smarttrace.beacon.model.BroadcastEvent;
import au.com.smarttrace.beacon.model.MeasuringDistance;
import au.com.smarttrace.beacon.net.DataUtil;
import au.com.smarttrace.beacon.net.WebService;
import au.com.smarttrace.beacon.net.model.FcmMessage;
import au.com.smarttrace.beacon.service.LCallback;
import au.com.smarttrace.beacon.service.LServiceWrapper;
import au.com.smarttrace.beacon.service.NetworkUtils;
import io.objectbox.Box;
import io.objectbox.query.Query;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class BeaconEngine {
    private Context mContext;
    private Box<PhonePaired> pairedBox;
    private LCallback mCallback;
    private Location mCurrentLocation;
    private LServiceWrapper locationWrapper;
    private int phoneBatteryLevel = 100;

    private BluetoothAdapter _BluetoothAdapter;
    private BroadcastService _BroadcastService;
    private List<Device> _DeviceList = new ArrayList<>();
    private Timer _Timer;
    private boolean _IsInit = false;

    private Handler mHandler;
//    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    private Box<EventData> eventBox;

    BeaconEngine(Context context) {
        mContext = context;
        createLocationCallback();

        HandlerThread ht = new HandlerThread("beacon_engine");
        ht.start();
        mHandler = new Handler(ht.getLooper());

        locationWrapper = LServiceWrapper.instances(mContext, mCallback);
        pairedBox = ((App) mContext.getApplicationContext()).getBoxStore().boxFor(PhonePaired.class);
        eventBox = ((App) mContext.getApplicationContext()).getBoxStore().boxFor(EventData.class);

    }

    /**
     * Creates a callback for receiving location events.
     */
    private void createLocationCallback() {
        mCallback = new LCallback() {
            @Override
            public void onLocationChanged(Location location) {
                onUpdateLocation(location);
            }
        };
    }

    private void onUpdateLocation(Location location) {
        Logger.d("[>_] EngineLocation: " + location.getAccuracy());
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
    public Location scanAndUpload() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new NetworkOnMainThreadException();
        }

        mCurrentLocation = locationWrapper.getCurrentLocation();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //stop location scan
                locationWrapper.stopLocationUpdates();
                stopBLEScan();
                uploadToServer();
            }
        }, AppConfig.SCANNING_TIMEOUT);

        startBLEScan();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                locationWrapper.startLocationUpdates();
            }
        });

        return mCurrentLocation;
    }

    private void startBLEScan() {
        Logger.i("[+] startBLEScan");
        try {
            if(_Timer != null) {
                _Timer.cancel();
            }

            _Timer = new Timer();
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    Looper.prepare();
                    try {
                        if (_BroadcastService == null) {
                            _BroadcastService = new BroadcastService();
                        }
                        final BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
                        _BluetoothAdapter = bluetoothManager.getAdapter();
                        if (!_IsInit) {
                            _IsInit = _BroadcastService.Init(_BluetoothAdapter, _LocalBluetoothCallBack);
                        }
                    } catch (Exception ex) {
                        Logger.e("BLE Error#", ex);
                    }

                    try {
                        synchronized (this) {
                            _BroadcastService.StartScan();
                        }
                    } catch (Exception ex){
                        Logger.e("BLE Error#", ex);
                    }
                    Looper.loop();
                }
            };
            _Timer.schedule(timerTask, 10000);
        } catch (Exception ex){
            Logger.e("BLE Error#", ex);
        }
    }

    private void stopBLEScan() {
        Logger.d("[>_] StopBLEScan");
        try {
            _Timer.cancel();
            if(_BroadcastService!=null)
                _BroadcastService.StopScan();
        } catch (Exception ex){
            Logger.e("BLE Error#", ex);

        } finally {
            _Timer.purge();
            _Timer = null;
        }
    }


    private void AddOrUpdate(final BLE ble){
        try {
            Device d = new Device();
            d.fromScanData(ble);
            if(d.SN == null || d.SN.length() != 8) return;
            //Logger.i("[Engine #Scanning]" + d.SN);
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
                Logger.e("BLE Error#", ex);
            }
        }

        @Override
        public void OnUpdate(BLE ble) {
            try {
                AddOrUpdate(ble);
            } catch (Exception ex) {
                Logger.e("BLE Error#", ex);
            }
        }

        @Override
        public void OnExited(final BLE ble) {
            try {

            } catch (Exception ex) {
                Logger.e("BLE Error#", ex);
            }
        }

        @Override
        public void OnScanComplete() {

        }
    };

    //uploadToServer
    private void uploadToServer() {
        Logger.i("[Engine#] starting upload to server ...");
        FcmMessage fcmMessage = FcmMessage.create();
        fcmMessage.setFcmToken(FirebaseInstanceId.getInstance().getToken());
        fcmMessage.setPhoneImei(NetworkUtils.getGatewayId());
        fcmMessage.setFcmInstanceId(FirebaseInstanceId.getInstance().getId());
        fcmMessage.setExpectedTimeToReceive(System.currentTimeMillis());
        WebService.nextPoint(fcmMessage);

        updateBatteryLevel();
        BroadcastEvent be = new BroadcastEvent();
        be.setLocation(mCurrentLocation);
        be.setCellTowerList(NetworkUtils.getAllCellInfo());
        be.setGatewayId(NetworkUtils.getGatewayId());

        final List<BeaconPackage> lbp = new ArrayList<>();
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

        //2. upload new data
        String dataForUpload = DataUtil.formatData(be);
        WebService.sendEvent(dataForUpload, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // save to db
                saveDataToDB(lbp);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //try to upload old
                tryToSendOldData();
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
            if (mCurrentLocation != null) {
                evdt.setLatitude(mCurrentLocation.getLatitude());
                evdt.setLongitude(mCurrentLocation.getLongitude());
                evdt.setAltitude(mCurrentLocation.getAltitude());
                evdt.setAccuracy(mCurrentLocation.getAccuracy());
                evdt.setSpeedKPH(mCurrentLocation.getSpeed());
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

    private boolean isPaired(BeaconPackage data) {
        Query<PhonePaired> query = pairedBox.query()
                .equal(PhonePaired_.phoneImei, NetworkUtils.getGatewayId())
                .equal(PhonePaired_.beaconSerialNumber, data.getSerialNumberString())
                .build();
        Logger.i("[>... paired? ] " + data.getSerialNumberString() + " :#" + (query.findFirst() != null));
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


    public void closeBox() {
        if (pairedBox != null) {
            pairedBox.closeThreadResources();
        }
        if (eventBox != null) {
            eventBox.closeThreadResources();
        }
    }
}