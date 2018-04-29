package au.com.smarttrace.beacon.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import com.TZONE.Bluetooth.BLE;
import com.TZONE.Bluetooth.ILocalBluetoothCallBack;
import com.TZONE.Bluetooth.Temperature.BroadcastService;
import com.TZONE.Bluetooth.Temperature.Model.Device;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.service.location.LServiceWrapper;

public class BLEServiceWrapper {
    LServiceWrapper locationWrapper;
    private BluetoothAdapter _BluetoothAdapter;
    private BroadcastService _BroadcastService;
    private boolean _IsInit;
    private BLECallback _Callback;
    private ILocalBluetoothCallBack _LocalBluetoothCallBack;

    private Timer _Timer;

    private final Map<String, Device> deviceMap = new ConcurrentHashMap<>();

    private static BLEServiceWrapper instance = null;
    public static BLEServiceWrapper instances(Context context, BLECallback callback) {
        if (instance == null) {
            instance = new BLEServiceWrapper(context, callback);
        } else {
            instance._IsInit = false;
            instance.initBLE(context);
            instance.set_Callback(callback);
        }
        return instance;
    }

    public BLEServiceWrapper(Context context, BLECallback callback) {
        initBLE(context);
        set_Callback(callback);
    }

    private void initBLE(Context context) {
        try {
            createLocalBluetoothCallback();
            if (_BroadcastService == null) {
                _BroadcastService = new BroadcastService();
            }
            final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            _BluetoothAdapter = bluetoothManager.getAdapter();
            if (!_IsInit) {
                _IsInit = _BroadcastService.Init(_BluetoothAdapter, _LocalBluetoothCallBack);
            }
        } catch (Exception ex) {

        }
    }

    public void start() {
        try {
            if(_Timer != null) {
                _Timer.cancel();
            }
            _Timer = new Timer();
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    Logger.d("[>_] ble+");
                    try {
                        synchronized (this) {
                            _BroadcastService.StartScan();
                        }
                    } catch (Exception ex){}
                }
            };
            _Timer.schedule(timerTask, 1000);
        } catch (Exception ex){
            Logger.d("[>_] BLErError #" + ex.getMessage());
        }
    }
    public void stop() {
        Logger.d("[>_] StopBLEScan");
        try {
            _Timer.cancel();
            if(_BroadcastService!=null)
                _BroadcastService.StopScan();
        } catch (Exception ex){
            Logger.e("#", ex);
        } finally {
            _Timer.purge();
        }
    }

    public BLECallback get_Callback() {
        return _Callback;
    }

    public void set_Callback(BLECallback _Callback) {
        this._Callback = _Callback;
    }

    private void createLocalBluetoothCallback() {
        _LocalBluetoothCallBack = new ILocalBluetoothCallBack() {
            @Override
            public void OnEntered(BLE ble) {
                onBLEUpdate(ble);
            }

            @Override
            public void OnUpdate(BLE ble) {
                onBLEUpdate(ble);
            }

            @Override
            public void OnExited(BLE ble) {
                //--noop
            }

            @Override
            public void OnScanComplete() {
                //--noop
            }
        };
    }

    private void onBLEUpdate(BLE ble) {
        Device device = new Device();
        device.fromScanData(ble);
        //TODO Update internal device list
        try {
            if (device.SN == null || device.SN.length() != 8) return;
            deviceMap.put(device.MacAddress, device);
        } catch (Exception ex) {
            Logger.e("AddOrUpdate:", ex);
        }
        //--
        _Callback.onUpdateBLE(device);
    }
}
