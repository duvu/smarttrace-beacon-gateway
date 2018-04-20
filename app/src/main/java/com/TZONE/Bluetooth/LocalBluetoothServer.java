package com.TZONE.Bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Native Bluetooth service
 * Created by Forrest on 2016/6/1.
 */
public class LocalBluetoothServer {
    // Local Bluetooth adapter object
    private BluetoothAdapter _BluetoothAdapter = null;
    // Callback method
    private ILocalBluetoothCallBack _LocalBluetoothCallBack = null;
    // Scan callback
    private ScanCallbackEx _ScanCallbackEx = null;
    // Processing object
    private Handler _BluetoothHandler = null;
    // Device list
    public List<BLE> Devices = new ArrayList<BLE>();
    // Whether it is scanning
    private boolean IsScanning = false;
    // Start Time
    public Date StartTime = new Date();
    // Last active time
    public Date LastActiveTime = new Date();
    /**
     * Init
     * @param bluetoothAdapter
     * @return
     */
    public boolean Init(BluetoothAdapter bluetoothAdapter, ILocalBluetoothCallBack localBluetoothCallBack){
        try {
            this._BluetoothAdapter = bluetoothAdapter;
            this._LocalBluetoothCallBack = localBluetoothCallBack;
            this._ScanCallbackEx = new ScanCallbackEx();
            this._BluetoothHandler = new Handler();
            StartTime = new Date();
            LastActiveTime = new Date();

            if(_BluetoothAdapter!=null&& _BluetoothAdapter.isEnabled()){
                return true;
            }
        }catch (Exception ex){
            Log.e("BluetoothServer", "Init:" + ex.toString());
        }
        return false;
    }

    /**
     * ScanLeDevice
     * @param enable
     */
    private void ScanLeDevice(final boolean enable) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            ScanLeDevice_LOLLIPOP(enable);
        else
            ScanLeDevice_JELLY_BEAN_MR2(enable);
    }

    /**
     * Scan Device
     * Android 4.3 Api
     * @param enable
     */
    private void ScanLeDevice_JELLY_BEAN_MR2(final boolean enable){
        if (enable) {
            if (IsScanning){
                return;
            }
            _BluetoothHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    IsScanning = false;
                    _BluetoothAdapter.stopLeScan(_ScanCallbackEx.mLeScanCallback);
                    IsBleExited();// Analyze the Ble state in the collection
                    if(_LocalBluetoothCallBack != null)
                        _LocalBluetoothCallBack.OnScanComplete();// State change
                }
            }, AppConfig.ScanRunTime);

            IsScanning = true;
            LastActiveTime = new Date();
            _BluetoothAdapter.startLeScan(_ScanCallbackEx.mLeScanCallback);
        } else {
            IsScanning = false;
            _BluetoothAdapter.stopLeScan(_ScanCallbackEx.mLeScanCallback);
        }
    }


    BluetoothLeScanner _Scanner = null;
    /**
     * Scan Device
     * Android 5.0 Api
     * @param enable
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void ScanLeDevice_LOLLIPOP(final boolean enable){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            return;
        if(_Scanner == null)
            _Scanner = _BluetoothAdapter.getBluetoothLeScanner();
        if (enable) {
            if (IsScanning){
                return;
            }
            _BluetoothHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    IsScanning = false;
                    _Scanner.stopScan(_ScanCallbackEx.mLeScanCallback_LOLLIPOP);
                    IsBleExited();//Analyze the Ble state in the collection
                    if(_LocalBluetoothCallBack != null)
                        _LocalBluetoothCallBack.OnScanComplete();// State change
                }
            }, AppConfig.ScanRunTime);

            IsScanning = true;
            LastActiveTime = new Date();
            //scanner.startScan(_ScanCallbackEx.mLeScanCallback_LOLLIPOP);
            _Scanner.startScan(null,new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(0).build(),_ScanCallbackEx.mLeScanCallback_LOLLIPOP);
        } else {
            IsScanning = false;
            _Scanner.stopScan(_ScanCallbackEx.mLeScanCallback_LOLLIPOP);
        }
    }

    // Scan callback
    public class ScanCallbackEx{
        // 4.3 Bluetooth scan callback object
        public BluetoothAdapter.LeScanCallback mLeScanCallback = null;
        // 5.0 Bluetooth scan callback object
        public ScanCallback mLeScanCallback_LOLLIPOP = null;

        public ScanCallbackEx(){
            mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    try {
                        final BLE ble = new BLE();
                        ble.Name = device.getName();
                        ble.RSSI = rssi;
                        ble.ScanData = scanRecord;
                        ble.MacAddress = device.getAddress().toUpperCase();
                        ble.LastScanTime = new Date();
                        IsBleNewEnter(ble);
                    }catch (Exception ex){}
                }

            };
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mLeScanCallback_LOLLIPOP = new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        super.onScanResult(callbackType, result);
                        try {
                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                ScanRecord scanRecord = result.getScanRecord();
                                BluetoothDevice device = result.getDevice();
                                final BLE ble = new BLE();
                                ble.Name = device.getName();
                                ble.RSSI = result.getRssi();
                                ble.ScanData = scanRecord.getBytes();
                                ble.MacAddress = device.getAddress().toUpperCase();
                                ble.LastScanTime = new Date();
                                IsBleNewEnter(ble);
                            }
                        }catch (Exception ex){}
                    }
                    @Override
                    public void onScanFailed(int errorCode){
                        super.onScanFailed(errorCode);
                        if(_LocalBluetoothCallBack != null)
                            _LocalBluetoothCallBack.OnScanComplete();//状态改变
                    }
                };
            }

        }
    }

    /**
     * Whether it is new to join
     * Is New Enter
     * @param ble
     */
    private void IsBleNewEnter(BLE ble){
        try{
            // The maximum received only 100
           /* if(Devices.size() > 100)
                return;*/

            if(ble != null) {
                boolean isExist = false;
                for (int i = 0; i < Devices.size(); i++) {
                    BLE item = Devices.get(i);
                    if (item.MacAddress.equals(ble.MacAddress)) {
                        item.Name = ble.Name;
                        item.RSSI = ble.RSSI;
                        item.ScanData = ble.ScanData;
                        item.MacAddress = ble.MacAddress;
                        item.LastScanTime = new Date();
                        isExist = true;
                    }
                }
                if (!isExist){
                    // Added to the collection
                    Devices.add(ble);
                    if(this._LocalBluetoothCallBack!=null)
                        _LocalBluetoothCallBack.OnEntered(ble);
                }else {
                    if(this._LocalBluetoothCallBack!=null)
                        _LocalBluetoothCallBack.OnUpdate(ble);
                }
            }
        }catch (Exception ex){
            Log.e("BluetoothServer","IsNewEnter："+ex.toString());
        }
    }

    /**
     * Whether I will be able to scan the analysis set
     * More than 10 minutes to not scan, to leave
     */
    private void IsBleExited(){
        try {
            long exitedTime =  AppConfig.ExitedTime;
            Date now = new Date();
            for (int i = 0; i < Devices.size(); i++) {
                BLE item = Devices.get(i);
                long TotalMinutes = (now.getTime() - item.LastScanTime.getTime())
                        / (1000 * 60);
                if(TotalMinutes > exitedTime){
                    if(this._LocalBluetoothCallBack!=null)
                        _LocalBluetoothCallBack.OnExited(item);
                    Devices.remove(i);
                }
            }
        }
        catch (Exception ex){
            Log.e("BluetoothServer","IsBleExited："+ex.toString());
        }
    }

    /**
     * Scan
     */
    public void StartScan(){
        ScanLeDevice(true);
    }

    /**
     * Stop Scan
     */
    public void StopScan(){
        ScanLeDevice(false);
    }
}
