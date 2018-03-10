package com.TZONE.Bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Periphery Bluetooth Service
 * Created by Forrest on 2017/11/7.
 */
public class PeripheryService {
    public final static String TAG = "Periphery";
    // Local Bluetooth adapter object
    protected BluetoothAdapter _BluetoothAdapter = null;
    // Context
    protected Context _Context;
    // Peripheral device Mac address
    private String _MacAddress;
    // Connection timed out
    private long _Timeout = 30 * 1000;
    // Reconnection number of times
    private int _Count = 3;
    // Peripheral
    public BluetoothDevice Periphery = null;
    // BLE device GATT server object
    protected BluetoothGatt _BluetoothGatt;
    // Peripheral BLE service collection
    public List<BLEGattService> BLEGattServiceList = new ArrayList<>();
    // IsRunning
    public boolean IsRunning = false;

    public int BLE_STATE = 0;
    public int BLE_STATE_CONNECTING = 1;
    public int BLE_STATE_CONNECTED = 2;
    public int BLE_STATE_DISCONNECTED = 3;

    /**
     *
     * @param bluetoothAdapter
     * @param context
     * @param macAddress
     * @param timeout Connection timed out
     * @param count Reconnection number of times
     * @throws Exception
     */
    public PeripheryService(BluetoothAdapter bluetoothAdapter, Context context, String macAddress, long timeout, final int count) throws Exception {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled())
            throw new Exception("bluetoothAdapter object can not be empty, or bluetooth shutdown");

        _BluetoothAdapter = bluetoothAdapter;
        _Context = context;
        _MacAddress = macAddress;
        _Timeout = timeout;
        _Count = count;

        IsRunning = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                int j = 0;
                while (IsRunning){
                    try {
                        if(BLE_STATE == BLE_STATE_CONNECTED)
                            break;
                        if(j > _Count)
                            break;
                        if(BLE_STATE == BLE_STATE_CONNECTING){
                            if(i > (_Timeout / 1000)){
                                Connect();
                                i = 0;
                                j ++ ;
                                continue;
                            }
                            i ++;
                        } else {
                            i = 0;
                        }
                        Thread.sleep(1000);
                    }catch (Exception ex){}
                }
            }
        }).start();

    }

    private final BluetoothGattCallback _GattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS){
                if (newState == BluetoothProfile.STATE_CONNECTED){
                    BLE_STATE = BLE_STATE_CONNECTED;
                    Log.i(TAG, "Connected to GATT server.");
                    Log.i(TAG, "Attempting to start service discovery:" + _BluetoothGatt.discoverServices());
                    OnConnected();
                    try {
                        if (_BluetoothGatt.getDevice().getBondState() == BluetoothDevice.BOND_BONDED){
                            //Url : https://github.com/NordicSemiconductor/Android-DFU-Library/blob/release/dfu/src/main/java/no/nordicsemi/android/dfu/DfuBaseService.java
                            Log.i("onConnectionStateChange","Waiting 1600 ms for a possible Service Changed indication...");
                            // Connection successfully sleep 1600ms, the connection success rate will increase
                            Thread.sleep(1600);
                        }
                    } catch (Exception e){}
                    // Discover service
                    _BluetoothGatt.discoverServices();

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED){
                    BLE_STATE = BLE_STATE_DISCONNECTED;
                    Close();
                    Log.i(TAG, "Disconnected from GATT server.");
                    OnDisConnected();
                }
            }else{
                Log.d(TAG, "onConnectionStateChange received: " + status);
                BLE_STATE = BLE_STATE_DISCONNECTED;
                Close();
                Connect();
            }
            super.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
        }
    };

    /**
     * connect
     * @return
     */
    public boolean Connect() {
        if(!IsRunning)
            return false;

        final String address = _MacAddress;
        if (_BluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized.");
            return false;
        }
        if (address == null) {
            Log.w(TAG, "Unspecified address.");
        }
        if (_BluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (_BluetoothGatt.connect()) {
                BLE_STATE = BLE_STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }
        final BluetoothDevice device = _BluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found. Unable to connect.");
            return false;
        }
        _BluetoothGatt = device.connectGatt(_Context, false, _GattCallback);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            _BluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
        Log.d(TAG, "Trying to create a new connection.");
        Log.d(TAG, "BluetoothDeviceAddress is :" + _MacAddress);
        BLE_STATE = BLE_STATE_CONNECTING;
        return true;
    }

    /**
     * Close
     */
    public void Close(){
        if(_BluetoothGatt == null)
            return;
        Log.w(TAG, "_BluetoothGatt closed");
        _BluetoothGatt.close();
        _BluetoothGatt = null;
    }

    /**
     * Dispose
     */
    public void Dispose(){
        if(_BluetoothGatt == null)
            return;
        Log.w(TAG, "Dispose");
        _MacAddress = null;
        _BluetoothGatt.disconnect();
        if(_BluetoothGatt.getDevice().getBondState() == BluetoothDevice.BOND_NONE){
            // There is a refresh() method in BluetoothGatt class but for now it's hidden. We will call it using reflections.
            try {
                final Method refresh = _BluetoothGatt.getClass().getMethod("refresh");
                if (refresh != null) {
                    boolean success = (Boolean) refresh.invoke(_BluetoothGatt);
                    Log.i(TAG,"gatt.refresh:" + success);
                }
            } catch (Exception ex) {
                Log.e(TAG,"gatt.refresh:" + ex.toString());
            }
        }
        _BluetoothGatt.close();
        _BluetoothGatt = null;
        IsRunning = false;
    }

    public void OnConnected(){

    }

    public void OnDisConnected(){

    }

}
