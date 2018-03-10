package com.TZONE.Bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.TZONE.Bluetooth.Utils.StringConvertUtil;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Periphery Bluetooth Service
 * Created by Forrest on 2016/6/1.
 */
public class PeripheryBluetoothService {
    // Local Bluetooth adapter object
    protected BluetoothAdapter _BluetoothAdapter = null;
    protected Context _Context;
    // Peripheral device Mac address
    private String _MacAddress;
    // Time out
    private long _Timeout = 30 * 1000;
    // Peripheral
    public BluetoothDevice Periphery = null;
    // BLE device GATT server object
    protected BluetoothGatt _BluetoothGatt;
    // Peripheral BLE service collection
    public List<BLEGattService> BLEGattServiceList = new ArrayList<>();
    // Connection Status
    protected ConnectionStatus _ConnectionStatus = ConnectionStatus.NoConnect;
    // Operation callback
    protected IPeripheryBluetoothCallBack _PeripheryBluetoothServiceCallBack;

    public PeripheryBluetoothService(BluetoothAdapter bluetoothAdapter,Context context,String macAddress,IPeripheryBluetoothCallBack peripheryBluetoothServiceCallBack) throws Exception{
        this(bluetoothAdapter,context,macAddress,AppConfig.ConnectTimeout,peripheryBluetoothServiceCallBack);
    }

    public PeripheryBluetoothService(BluetoothAdapter bluetoothAdapter,Context context,String macAddress,long timeout,IPeripheryBluetoothCallBack peripheryBluetoothServiceCallBack) throws Exception{
        this._BluetoothAdapter = bluetoothAdapter;
        this._Context = context;
        this._MacAddress = macAddress;
        this._Timeout = timeout;
        this._ConnectionStatus = ConnectionStatus.NoConnect;
        this._PeripheryBluetoothServiceCallBack = peripheryBluetoothServiceCallBack;

        if(_BluetoothAdapter == null || !_BluetoothAdapter.isEnabled())
            throw new Exception("bluetoothAdapter object can not be empty, or bluetooth shutdown");

        if (_BluetoothAdapter.getRemoteDevice(macAddress) == null){
            throw new Exception("unable to find" + _MacAddress + " device");
        }
    }

    Date _ConnectStartTime = new Date();
    int _ConnectIndex = 0;
    /**
     * Connect
     */
    public boolean Connect(){
        try{
            if(_ConnectionStatus == ConnectionStatus.Connected || _ConnectionStatus == ConnectionStatus.Connecting){
                //There is already a connection without having to connect again ...
                Log.i("Connect", "There is already a connection without having to connect again ...");
                return true;
            }
            if(_BluetoothAdapter == null){
                // BluetoothAdapter is empty
                Log.e("Connect", "BluetoothAdapter is empty.");
                return false;
            }
            if (_MacAddress == null || _MacAddress.equals("")) {
                // MacAddress is empty
                Log.e("Connect", "MacAddress is empty");
                return false;
            }
            if(Periphery != null && Periphery.getAddress().equals(_MacAddress) && _BluetoothGatt != null){
                // Trying to re-connect ...
                Log.d("Connect", "Trying to re-connect ...");
                _ConnectionStatus = ConnectionStatus.Connecting;
                _ConnectIndex ++ ;
                if (_BluetoothGatt.connect()) {
                    return true;
                } else {
                    _ConnectionStatus = ConnectionStatus.DisConnected;
                    return false;
                }
            }
            BluetoothDevice device = _BluetoothAdapter.getRemoteDevice(_MacAddress);
            if (device == null){
                // Unable to connect device
                Log.e("Connect", "Unable to connect " + _MacAddress + " device");
                return false;
            }
            Periphery = device;
            // Connecting
            Log.i("Connect", "Connecting...");
            _ConnectionStatus = ConnectionStatus.Connecting;
            if(_ConnectIndex == 0)
                _ConnectStartTime = new Date();
            _ConnectIndex ++ ;
            _BluetoothGatt = device.connectGatt(_Context,false,_GattCallback);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                _BluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int i = 0;
                        while (_ConnectionStatus == ConnectionStatus.Connecting) {
                            int maxD = 15;
                            // sony phone connection takes 25 to 33 seconds to connect
                            if(_Timeout > 50 * 1000)
                                maxD = 40;
                            if(i > maxD){
                                Close();
                                _ConnectionStatus = ConnectionStatus.DisConnected;
                                Thread.sleep(3000);
                                if(_Timeout - (new Date().getTime() - _ConnectStartTime.getTime()) > 0) {
                                    //Overtime, try to reconnect ...
                                    Log.i("Connect", "Overtime, try to reconnect ...");
                                    Connect();
                                }
                                break;
                            }
                            i++;
                            Thread.sleep(1000);
                        }
                    }catch (Exception ex){
                        Log.e("Connect",ex.toString());
                    }
                }
            }).start();

            return true;
        }catch (Exception ex){
            Log.e("Connect",ex.toString());
            return false;
        }
    }
    /**
     * DisConnect
     */
    public void Close(){
        try{
            if(_BluetoothGatt == null){
                return;
            }
            Log.i("Close","Disconnecting ...");
            if(_ConnectionStatus == ConnectionStatus.Connected)
                _ConnectionStatus = ConnectionStatus.DisConnecting;
            Periphery = null;
            _BluetoothGatt.disconnect();
            if(_BluetoothGatt.getDevice().getBondState() == BluetoothDevice.BOND_NONE){
                // There is a refresh() method in BluetoothGatt class but for now it's hidden. We will call it using reflections.
                try {
                    final Method refresh = _BluetoothGatt.getClass().getMethod("refresh");
                    if (refresh != null) {
                        boolean success = (Boolean) refresh.invoke(_BluetoothGatt);
                        Log.i("Close","gatt.refresh:" + success);
                    }
                } catch (Exception ex) {
                    Log.e("Close","gatt.refresh:" + ex.toString());
                }
            }
            _BluetoothGatt.close();
            _BluetoothGatt = null;

        }catch (Exception ex){
            Log.e("Close",ex.toString());
        }

    }

    /**
     * Get Characteristic
     * @param uuid
     * @return
     */
    public BluetoothGattCharacteristic GetCharacteristic(String uuid){
        try{
            if(_ConnectionStatus == ConnectionStatus.Connected) {
                BluetoothGattCharacteristic characteristic = null;
                for (int i = 0; i < BLEGattServiceList.size(); i++) {
                    BLEGattService theService = BLEGattServiceList.get(i);
                    for (int j = 0; j < theService.CharacterList.size(); j++) {
                        BLEGattService.BLEGattCharacteristic theCharacteristic = theService.CharacterList.get(j);
                        if(uuid.toLowerCase().equals(theCharacteristic.GattCharacteristic.getUuid().toString().toLowerCase())){
                            characteristic = theCharacteristic.GattCharacteristic;
                            break;
                        }
                    }
                }
                if (characteristic != null) {
                    return characteristic;
                }
            }
        }catch (Exception ex){}
        return null;
    }

    /**
     * Is Exist Characteristic
     * @param uuid
     * @return
     */
    public boolean IsExistCharacteristic(String uuid){
        boolean isExist = false;
        if(GetCharacteristic(uuid)!=null)
            isExist = true;
        Log.i("Write", "Check for the presence of the feature UUID：" + uuid+" -> "+isExist);
        return isExist;
    }

    /**
     * Write Characteristic
     * @param uuid
     * @param bytes
     */
    public boolean WriteCharacteristic(String uuid,byte[] bytes){
        boolean isWrite = false;
        try{
            BluetoothGattCharacteristic characteristic = GetCharacteristic(uuid);
            if(characteristic!=null){
                if (bytes != null && characteristic != null) {
                    characteristic.setValue(bytes);
                    _BluetoothGatt.writeCharacteristic(characteristic);
                    isWrite = true;
                    Log.i("Write", "Write feature ：" + uuid + " --> " + StringConvertUtil.bytesToHexString(bytes));
                }
            }
        }catch (Exception ex){}
        return isWrite;
    }
    /**
     * Read Characteristic
     * @param uuid
     */
    public boolean ReadCharacteristic(String uuid){
        boolean isRead = false;
        try{
            BluetoothGattCharacteristic characteristic = GetCharacteristic(uuid);
            if (characteristic != null) {
                _BluetoothGatt.readCharacteristic(characteristic);
                isRead = true;
                Log.i("Write", "Read the feature：" + uuid);
            }
        }catch (Exception ex){}
        return isRead;
    }

    /**
     * Set up notifications
     * @param uuid
     * @param enabled
     * @return
     */
    public boolean EnableNotification(String uuid,boolean enabled) {
        boolean success = false;
        BluetoothGattCharacteristic characteristic = GetCharacteristic(uuid);
        if (characteristic != null) {
            success = _BluetoothGatt.setCharacteristicNotification(characteristic, true);
            if (success) {
                // 来源：http://stackoverflow.com/questions/38045294/oncharacteristicchanged-not-called-with-ble
                for(BluetoothGattDescriptor dp: characteristic.getDescriptors()){
                    if (dp != null) {
                        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                            if(enabled)
                                dp.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            else
                                dp.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                        } else if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                            dp.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                        }
                        _BluetoothGatt.writeDescriptor(dp);
                    }
                }
                Log.i("Notification", "Set up notifications：" + uuid + " --> " +enabled);
            }
        }
        return success;
    }

    // Different types of callback methods through the BLE API
    private final BluetoothGattCallback _GattCallback = new BluetoothGattCallback(){
        /**
         * The connection status has changed
         * @param gatt
         * @param status
         * @param newState
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            Log.i("onConnectionStateChange", "The connection status has changed. status:" + status + " newState:" + newState + "");
            try {
                if(status == BluetoothGatt.GATT_SUCCESS) {
                    switch (newState) {
                        // Has been connected to the device
                        case BluetoothProfile.STATE_CONNECTED:
                            Log.i("onConnectionStateChange", "Has been connected to the device");
                            if(_ConnectionStatus == ConnectionStatus.Connecting) {
                                _ConnectionStatus = ConnectionStatus.Connected;
                                try {
                                    if (_PeripheryBluetoothServiceCallBack != null)
                                        _PeripheryBluetoothServiceCallBack.OnConnected();
                                    if (gatt.getDevice().getBondState() == BluetoothDevice.BOND_BONDED){
                                        //Url : https://github.com/NordicSemiconductor/Android-DFU-Library/blob/release/dfu/src/main/java/no/nordicsemi/android/dfu/DfuBaseService.java
                                        Log.i("onConnectionStateChange","Waiting 1600 ms for a possible Service Changed indication...");
                                        // Connection successfully sleep 1600ms, the connection success rate will increase
                                        Thread.sleep(1600);
                                    }
                                } catch (Exception e){}
                                // Discover service
                                gatt.discoverServices();
                            }
                            break;
                        // The connection has been disconnected
                        case BluetoothProfile.STATE_DISCONNECTED:
                            Log.i("onConnectionStateChange", "The connection has been disconnected");
                            if(_ConnectionStatus == ConnectionStatus.Connected) {
                                if (_PeripheryBluetoothServiceCallBack != null)
                                    _PeripheryBluetoothServiceCallBack.OnDisConnected();
                            }
                            Close();
                            _ConnectionStatus = ConnectionStatus.DisConnected;
                            break;
                        /*case BluetoothProfile.STATE_CONNECTING:
                            Log.i("onConnectionStateChange", "Connecting ...");
                            _ConnectionStatus = ConnectionStatus.Connecting;
                            break;
                        case BluetoothProfile.STATE_DISCONNECTING:
                            Log.i("onConnectionStateChange", "Disconnecting ...");
                            _ConnectionStatus = ConnectionStatus.DisConnecting;
                            break;*/
                        default:
                            Log.e("onConnectionStateChange", "newState:" + newState);
                            break;
                    }
                }else {
                    Log.i("onConnectionStateChange", "GATT error");
                    Close();
                    if(_ConnectionStatus == ConnectionStatus.Connecting
                            && _Timeout - (new Date().getTime() - _ConnectStartTime.getTime()) > 0) {
                        Log.i("Connect", "Gatt Error! Try to reconnect ("+_ConnectIndex+")...");
                        _ConnectionStatus = ConnectionStatus.DisConnected;
                        Connect(); // Connect
                    }else {
                        _ConnectionStatus = ConnectionStatus.DisConnected;
                        if (_PeripheryBluetoothServiceCallBack != null)
                            _PeripheryBluetoothServiceCallBack.OnDisConnected();
                    }
                }
            }catch (Exception ex){
                Log.e("onConnectionStateChange", ex.toString());
            }
            super.onConnectionStateChange(gatt, status, newState);
        }
        /**
         *  Discover new services
         *  onServicesDiscovered
         * @param gatt
         * @param status
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i("onServicesDiscovered", "Obtaining device service information ......");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                try{
                    String messages = "-------------- （"+Periphery.getAddress()+"）Service Services information : --------------------\n";
                    List<BluetoothGattService> serviceList = gatt.getServices();
                    messages += "Total："+serviceList.size()+"\n";
                    for (int i = 0; i < serviceList.size(); i++) {
                        BluetoothGattService theService = serviceList.get(i);
                        BLEGattServiceList.add(new BLEGattService(theService));

                        String serviceName = theService.getUuid().toString();
                        String msg = "---- Service UUID："+serviceName+" -----\n";
                        // Each Service contains multiple features
                        List<BluetoothGattCharacteristic> characterList = theService.getCharacteristics();
                        msg += "Characteristic UUID List：\n";
                        for (int j = 0; j < characterList.size(); j++) {
                            BluetoothGattCharacteristic theCharacter = characterList.get(j);
                            msg += "("+(j+1)+")："+theCharacter.getUuid()+"\n";
                            //msg += " --> Value："+ StringConvertUtil.bytesToHexString(theCharacter.getValue())+"\n";
                        }
                        msg += "\n\n";
                        messages += msg;
                    }
                    messages += "-------------- （"+Periphery.getAddress()+"）--------------------\n";
                    Log.i("onServicesDiscovered", messages);
                }catch (Exception ex){
                    Log.e("onServicesDiscovered", "Exception:" + ex.toString());
                }
            }
            if (_PeripheryBluetoothServiceCallBack!=null){
                _PeripheryBluetoothServiceCallBack.OnServicesed(BLEGattServiceList);
            }
            super.onServicesDiscovered(gatt, status);
        }
        /**
         * Read the response
         * @param gatt
         * @param characteristic
         * @param status
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.i("onCharacteristicRead", "Read the response. uuid:"+characteristic.getUuid().toString() +" value:"+ StringConvertUtil.bytesToHexString(characteristic.getValue()));
            if (status == BluetoothGatt.GATT_SUCCESS) {
                try {
                    String uuid = characteristic.getUuid().toString();
                    byte[] val = characteristic.getValue();
                    for (int i = 0; i < BLEGattServiceList.size(); i++) {
                        BLEGattService theService = BLEGattServiceList.get(i);
                        for (int j = 0; j < theService.CharacterList.size(); j++) {
                            BLEGattService.BLEGattCharacteristic theCharacteristic = theService.CharacterList.get(j);
                            if(uuid.toLowerCase().equals(theCharacteristic.GattCharacteristic.getUuid().toString().toLowerCase())){
                                theCharacteristic.val = val;
                                break;
                            }
                        }
                    }
                    if (_PeripheryBluetoothServiceCallBack!=null){
                        _PeripheryBluetoothServiceCallBack.OnReadCallBack(characteristic.getUuid(), val);
                    }
                } catch (Exception ex) {
                    Log.e("onCharacteristicRead", ex.toString());
                }
            }
            super.onCharacteristicRead(gatt, characteristic, status);
        }
        /**
         * Write response
         * @param gatt
         * @param characteristic
         * @param status
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            Log.i("onCharacteristicWrite", "Write response.uuid:"+characteristic.getUuid().toString());
            try {
                if (_PeripheryBluetoothServiceCallBack != null){
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        _PeripheryBluetoothServiceCallBack.OnWriteCallBack(characteristic.getUuid(),true);
                    }else {
                        _PeripheryBluetoothServiceCallBack.OnWriteCallBack(characteristic.getUuid(),false);
                    }
                }
            }catch (Exception ex){
                Log.e("onCharacteristicWrite", ex.toString());
            }
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        /**
         * Receive the return data
         * @param gatt
         * @param characteristic
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            byte[] bytes = characteristic.getValue();
            Log.i("onCharacteristicChanged", "Receive the return data. uuid:" + characteristic.getUuid().toString() + " bytes:" + StringConvertUtil.bytesToHexString(bytes));
            if (_PeripheryBluetoothServiceCallBack != null){
                _PeripheryBluetoothServiceCallBack.OnReceiveCallBack(characteristic.getUuid(),bytes);
            }
            super.onCharacteristicChanged(gatt,characteristic);
        }

    };

    /**
     * Get Connect Status
     * @return
     */
    public ConnectionStatus GetConnectStatus(){
        return this._ConnectionStatus;
    }
}
