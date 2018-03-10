package com.TZONE.Bluetooth.Temperature;

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

import com.TZONE.Bluetooth.AppConfig;
import com.TZONE.Bluetooth.Temperature.Listener.IOTAService;
import com.TZONE.Bluetooth.Temperature.Model.Device;
import com.TZONE.Bluetooth.Temperature.Model.FileUpdate;
import com.TZONE.Bluetooth.Utils.StringConvertUtil;
import com.TZONE.Bluetooth.Utils.StringUtil;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Firmware upgrade service
 * Created by Forrest on 2017/4/17.
 */
public class OTAService {
    private final String _logTAG = "OTA";
    private Context _Context;
    // Device Mac address
    private String[] _MacAddress = new String[]{"",""};
    // Password
    private String _Password = "000000";
    // HardwareModel
    protected String HardwareModel = "3A01";
    // Firmware
    protected String Firmware = "";
    // BluetoothAdapter
    private BluetoothAdapter _BluetoothAdapter;
    // device
    private BluetoothDevice _Device;
    // device gatt
    private BluetoothGatt _BluetoothGatt;
    // Password characteristic
    private BluetoothGattCharacteristic _PasswordCharacteristic = null;
    // OTA OP characteristic
    private BluetoothGattCharacteristic _OTA_OP_Characteristic = null;
    private BluetoothGattDescriptor _OTA_OP_Descriptor = null;
    // OTA PACKET characteristic
    private BluetoothGattCharacteristic _OTA_Packet_Characteristic = null;
    // OTA version characteristic
    private BluetoothGattCharacteristic _OTA_Bootloader_Version_Characteristic = null;

    // file update
    private FileUpdate _FileUpdate = null;
    // Callback
    private IOTAService _IOTAService = null;

    // Whether it is running
    public boolean IsRunning = false;
    // Whether it has been connected
    public boolean IsConnected = false;
    // Step
    private final int _TotalStep = 13;
    private int _Index = 0;
    // Send data length once
    private final int _PackageLength = 100;
    private int _PackageIndex = 0;

    // Compatibility issues（Step = 4、7、10、11）
    private final boolean _IsCompatible = true;

    public OTAService(Context context,BluetoothAdapter bluetoothAdapter,String macAddress,String token,IOTAService iotaService){
        _Context = context;
        _BluetoothAdapter = bluetoothAdapter;
        _MacAddress[0] = macAddress;
        _MacAddress[1] = macAddress.split(":")[0] + ":"
                + macAddress.split(":")[1] + ":"
                + macAddress.split(":")[2] + ":"
                + macAddress.split(":")[3] + ":"
                + macAddress.split(":")[4] + ":"
                + StringUtil.PadLeft(Integer.toHexString(Integer.parseInt(macAddress.split(":")[5],16) + 1),2);
        _Password = token;
        _IOTAService = iotaService;
    }

    /**
     * Add upgrade file
     * @param fileUpdate
     */
    public void InFileUpdate(FileUpdate fileUpdate){
        _FileUpdate = fileUpdate;
    }

    /**
     * Start
     */
    public void Start(){
        try {
            IsRunning = true;
            if (_Index == 10){
                //During the upgrade process (send bin file) is broken
            }
            Step();
        }catch (Exception ex){
            Log.i(_logTAG, "Start => " + ex.toString());
        }
    }

    /**
     * Dispose
     */
    public void Dispose(){
        try {
            IsRunning = false;
            Close();
            IsConnected = false;
        }catch (Exception ex){
            Log.i(_logTAG, "Dispose => " + ex.toString());
        }
    }

    private BluetoothAdapter.LeScanCallback _LeScanCallback = new BluetoothAdapter.LeScanCallback(){
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord){
            try {
                Log.i(_logTAG, "onLeScan =>" + device.getAddress().toString() + " rssi:" + rssi);
                if(_Device != null)
                    return;
                if (device.getAddress().equals(_MacAddress[1]) || device.getAddress().equals(_MacAddress[0])) {
                    _Device = device;

                    if(_Index == 2)
                        _isStep2 = true;
                    else if(_Index == 13) {
                        final Device bt  = new Device();
                        if(bt.fromScanData(device.getName(),device.getAddress(),rssi,scanRecord)) {
                            HardwareModel = bt.HardwareModel;
                            Firmware = bt.Firmware;
                            _isStep13 = true;
                        }
                    }
                }
            }catch (Exception ex){
                Log.e(_logTAG,"onLeScan =>" + ex.toString());
            }
        }
    };

    // Number of connections
    private int _connectIndex = 0;
    // Whether to re-connect
    private boolean _isReConnect = false;
    private BluetoothGattCallback _BluetoothGattCallback = new BluetoothGattCallback() {
        // The connection status has changed
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i(_logTAG, "onConnectionStateChange => The connection status has changed.  status:" + status + " newState:" + newState + "");
            try {
                if(status == BluetoothGatt.GATT_SUCCESS){
                    switch (newState){
                        // Has been connected to the device
                        case BluetoothProfile.STATE_CONNECTED:
                            Log.i(_logTAG, "onConnectionStateChange => Has been connected to the device");
                            IsConnected = true;
                            try {
                                if (gatt.getDevice().getBondState() == BluetoothDevice.BOND_BONDED){
                                    //Url : https://github.com/NordicSemiconductor/Android-DFU-Library/blob/release/dfu/src/main/java/no/nordicsemi/android/dfu/DfuBaseService.java
                                    Log.i(_logTAG,"onConnectionStateChange => Waiting 1600 ms for a possible Service Changed indication...");
                                    // Connection successfully sleep 1600ms, the connection success rate will increase
                                    Thread.sleep(1600);
                                }
                            }catch (Exception ex){}
                            gatt.discoverServices(); // discoverServices
                            break;
                        // The connection has been disconnected
                        case BluetoothProfile.STATE_DISCONNECTED:
                            Log.i(_logTAG, "onConnectionStateChange => The connection has been disconnected");
                            Close();
                            IsConnected = false;
                            if(!_disconnecting){
                                // Not connected to the device
                                OnError(1);
                            }
                            break;
                    }
                }else {
                    Close();
                    if(_connectIndex <= 3) {
                        _isReConnect = true;
                        // Reconnect
                        Connect();
                    }else {
                        OnError(1);
                    }
                }
            }catch (Exception ex){
                Log.i(_logTAG, "onConnectionStateChange => " + ex.toString());
            }
            super.onConnectionStateChange(gatt, status, newState);
        }
        // Discovery of equipment services and characteristic
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status){
            Log.i(_logTAG, "onServicesDiscovered => Getting device service and feature information.....");
            try {
                List<BluetoothGattService> serviceList = gatt.getServices();
                for (int i = 0; i < serviceList.size(); i++) {
                    BluetoothGattService theService = serviceList.get(i);
                    List<BluetoothGattCharacteristic> characterList = theService.getCharacteristics();
                    // Find the password characteristic
                    if(theService.getUuid().toString().toLowerCase().equals("27763b10-999c-4d6a-9fc4-c7272be10900")){
                        for (int j = 0; j < characterList.size(); j++){
                            BluetoothGattCharacteristic theCharacter = characterList.get(j);
                            if(theCharacter.getUuid().toString().toLowerCase().equals("27763b13-999c-4d6a-9fc4-c7272be10900")){
                                _PasswordCharacteristic = theCharacter;
                            }
                        }
                    }
                    // Find OTA control characteristic
                    if(theService.getUuid().toString().toLowerCase().equals("00001530-1212-efde-1523-785feabcd123")){
                        for (int j = 0; j < characterList.size(); j++){
                            BluetoothGattCharacteristic theCharacter = characterList.get(j);
                            if(theCharacter.getUuid().toString().toLowerCase().equals("00001531-1212-efde-1523-785feabcd123")){
                                _OTA_OP_Characteristic = theCharacter;
                                try {
                                    _OTA_OP_Descriptor = theCharacter.getDescriptor(new UUID(0x0000290200001000L, 0x800000805f9b34fbL));
                                }catch (Exception ex){}

                            }
                            if(theCharacter.getUuid().toString().toLowerCase().equals("00001532-1212-efde-1523-785feabcd123")){
                                _OTA_Packet_Characteristic = theCharacter;
                            }
                            if(theCharacter.getUuid().toString().toLowerCase().equals("00001534-1212-efde-1523-785feabcd123")){
                                _OTA_Bootloader_Version_Characteristic = theCharacter;
                            }
                        }
                    }
                }

                if(_Index == 0) {
                    if (_PasswordCharacteristic == null) {
                        // Unable to get device information
                        OnError(7);
                        return;
                    } else {
                        // verify password
                        CheckToken(_Password);
                    }
                }

                if(_OTA_OP_Characteristic == null || _OTA_Packet_Characteristic == null || _OTA_OP_Descriptor == null){
                    if(_Index == 0) {
                        // The device does not support OTA functionality
                        OnError(2);
                    }
                    if(_Index == 2) {
                        // In upgrade mode, device information can not be obtained
                        OnError(5);
                    }
                    return;
                }else {
                    // Step 1: After the connection requires a password, enter the next step after verifying the password.
                    // Step 2, for the search device, go to the next step after searching the device
                    if(_Index > 1) {
                        // Enable CCCD
                        EnableCCCD();
                    }
                }
            }catch (Exception ex){
                Log.i(_logTAG, "onServicesDiscovered => " + ex.toString());
            }
            super.onServicesDiscovered(gatt, status);
        }
        // Read characteristic response
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status){
            Log.i(_logTAG, "onCharacteristicRead => Read characteristic response uuid:"+characteristic.getUuid().toString() +" value:"+ StringConvertUtil.bytesToHexString(characteristic.getValue()));
            if (status == BluetoothGatt.GATT_SUCCESS){

            }
            super.onCharacteristicRead(gatt, characteristic, status);
        }
        // Write characteristic response
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status){
            Log.i(_logTAG, "onCharacteristicWrite => Read characteristic response uuid:"+characteristic.getUuid().toString());
            try {
                if (status == BluetoothGatt.GATT_SUCCESS){
                    if(characteristic.getUuid().toString().toLowerCase().equals("27763b13-999c-4d6a-9fc4-c7272be10900")){
                        // Verify password is successful
                        if(_Index == 0) {
                            // Enable CCCD
                            EnableCCCD();
                        }
                    }

                    if(characteristic.getUuid().toString().toLowerCase().equals(_OTA_OP_Characteristic.getUuid().toString().toLowerCase())){
                        if(_Index == 1){
                            _isStep1 = true;
                        }else if(_Index == 3){
                            _isStep3 = true;
                        }else if(_Index == 5){
                            _isStep5 = true;
                        }else if(_Index == 8){
                            _isStep8 = true;
                        }else if(_Index == 9){
                            _isStep9 = true;
                        }else if(_Index == 12){
                            _isStep12 = true;
                        }
                    }
                    if(characteristic.getUuid().toString().toLowerCase().equals(_OTA_Packet_Characteristic.getUuid().toString().toLowerCase())){
                        if(_Index == 6){
                            _isStep6 = true;
                        }else if(_Index == 10){
                            _isStep10_Package_command_isok = true;
                        }
                    }
                }else {
                    if(characteristic.getUuid().toString().toLowerCase().equals("27763b13-999c-4d6a-9fc4-c7272be10900")){
                        // Password is error
                        OnError(8);
                    }
                }
            }catch (Exception ex){
                Log.e(_logTAG,"onCharacteristicWrite => "+ ex.toString());
            }
            super.onCharacteristicWrite(gatt, characteristic, status);
        }
        // Characteristic change response
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.i(_logTAG, "onCharacteristicChanged => Characteristic change response uuid:"+characteristic.getUuid().toString());
            try {
                byte[] bytes = characteristic.getValue();
                Log.i(_logTAG, "onCharacteristicChanged => value:"+ StringConvertUtil.bytesToHexString(bytes));
                if(characteristic.getUuid().toString().toLowerCase().equals(_OTA_OP_Characteristic.getUuid().toString().toLowerCase())){
                    if(_Index == 4) {
                        _isStep4_errcode = bytes[bytes.length - 1];
                        _isStep4 = true;
                    }else if(_Index == 7){
                        _isStep7_errcode = bytes[bytes.length - 1];
                        _isStep7 = true;
                    }else if(_Index == 10){
                        if(bytes[0] == 0x11) {
                            _isStep10_Package = true;
                        }else if(bytes[0] == 0x10){
                            if(_isStep10_errcode != 0x01)
                                _isStep10_errcode = bytes[bytes.length - 1];
                            _isStep10 = true;
                        }
                    }else if(_Index == 11){
                        _isStep11_errcode = bytes[bytes.length - 1];
                        _isStep11 = true;
                    }

                }
            }catch (Exception ex){
                Log.e(_logTAG,"onCharacteristicChanged => "+ ex.toString());
            }
            super.onCharacteristicChanged(gatt, characteristic);
        }

        // Read description response
        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.i(_logTAG, "onDescriptorRead => Read description response uuid:"+ descriptor.getUuid().toString());
            super.onDescriptorRead(gatt, descriptor, status);
        }

        // Write description response
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.i(_logTAG, "onDescriptorWrite => Write description response uuid:"+ descriptor.getUuid().toString());
            try {
                if (status == BluetoothGatt.GATT_SUCCESS){
                    if(descriptor.getUuid().toString().toLowerCase().equals(_OTA_OP_Descriptor.getUuid().toString().toLowerCase())){
                        if(_Index == 0){
                            _Index = 1;
                        }else if(_Index == 2){
                            _Index = 3;
                        }
                        Step();
                    }
                } else {
                    if(descriptor.getUuid().toString().toLowerCase().equals(_OTA_OP_Descriptor.getUuid().toString().toLowerCase())){
                        // Enable CCCD failure
                        OnError(34);
                    }
                }
            }catch (Exception ex){
                Log.e(_logTAG,"onDescriptorWrite => "+ ex.toString());
            }
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            Log.i(_logTAG, "onReliableWriteCompleted => status:" + status);
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.i(_logTAG, "onReadRemoteRssi => rssi:" + rssi);
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }
    };
    /**
     * Connect
     */
    private void Connect(){
        try {
            if(IsConnected)
                return;
            IsConnected = false;
            _connectIndex ++ ;
            if(_connectIndex == 1)
                _isReConnect = false;
            _OTA_OP_Characteristic = null;
            if(_Device == null)
                _Device = _BluetoothAdapter.getRemoteDevice(_MacAddress[0]);
            _BluetoothGatt = _Device.connectGatt(_Context,false,_BluetoothGattCallback);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                _BluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int i = 0;
                        while (IsRunning && !IsConnected){
                            if(i > AppConfig.ConnectTimeout){
                                Close();
                                OnError(1);
                                break;
                            }
                            int maxD = 15;
                            // sony phone connection takes 25 to 33 seconds to connect
                            if(AppConfig.ConnectTimeout > 50)
                                maxD = 40;
                            if(!_isReConnect && i > maxD){
                                Close();
                                Thread.sleep(3000);
                                _isReConnect = true;
                                Connect();
                                break;
                            }
                            i++;
                            Thread.sleep(1000);
                        }
                    }catch (Exception ex){}
                }
            }).start();

        }catch (Exception ex){
            Log.e(_logTAG,"Connect => " + ex.toString());
        }

    }
    /**
     * Check password
     * @param token
     */
    private void CheckToken(String token){
        try{
            if(!IsConnected)
                return;
            if(_PasswordCharacteristic == null)
                return;
            _Password = token;
            if(_Password == null || _Password.length() != 6) {
                // The password is not formatted error
                OnError(6);
                return;
            }

            byte[] b0 = StringConvertUtil.uint8ToByte(Integer.parseInt(_Password.substring(0,1)));
            byte[] b1 = StringConvertUtil.uint8ToByte(Integer.parseInt(_Password.substring(1,2)));
            byte[] b2 = StringConvertUtil.uint8ToByte(Integer.parseInt(_Password.substring(2,3)));
            byte[] b3 = StringConvertUtil.uint8ToByte(Integer.parseInt(_Password.substring(3,4)));
            byte[] b4 = StringConvertUtil.uint8ToByte(Integer.parseInt(_Password.substring(4,5)));
            byte[] b5 = StringConvertUtil.uint8ToByte(Integer.parseInt(_Password.substring(5,6)));
            byte[] bytes = StringConvertUtil.byteMergerMultiple(b0,b1,b2,b3,b4,b5);
            if(bytes == null)
                return;
            _PasswordCharacteristic.setValue(bytes);
            _BluetoothGatt.writeCharacteristic(_PasswordCharacteristic);
            Log.i(_logTAG,"CheckToken => Verifying password...");
        }catch (Exception ex){
            Log.e(_logTAG,"CheckToken => " + ex.toString());
        }
    }

    /**
     * Enable CCCD
     * 1 = NOTIFICATIONS
     * 2 = INDICATIONS
     */
    private void EnableCCCD(){
        try {
            if(!IsRunning)
                return;
            if(_OTA_OP_Characteristic == null)
                return;
            if(_OTA_OP_Descriptor == null)
                return;

            Thread.sleep(3000);

            boolean isEnableNotification = _BluetoothGatt.setCharacteristicNotification(_OTA_OP_Characteristic,true);
            if(isEnableNotification) {
                /*List<BluetoothGattDescriptor> descriptorList = _OTA_OP_Characteristic.getDescriptors();
                if (descriptorList != null && descriptorList.size() > 0) {
                    for (BluetoothGattDescriptor descriptor : descriptorList) {
                        _OTA_OP_Descriptor = descriptor;
                        Log.i(_logTAG, "EnableCCCD => BluetoothGattDescriptor : " + descriptor.toString());
                        *//**//*if ((_OTA_OP_Characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0){
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        }else if ((_OTA_OP_Characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0){
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                        }*//**//*
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        _BluetoothGatt.writeDescriptor(descriptor);
                    }
                }*/
                _OTA_OP_Descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                _BluetoothGatt.writeDescriptor(_OTA_OP_Descriptor);
            }

        }catch (Exception ex){
            Log.e(_logTAG,"EnableCCCD => " + ex.toString());
        }
    }

    // Is disconnecting, used to determine the abnormal situation
    boolean _disconnecting = false;
    /**
     * Close
     */
    private void Close(){
        try{
            if(_BluetoothGatt == null){
                return;
            }
            Log.i(_logTAG, "Close => Disconnecting...");
            _disconnecting = true;
            _BluetoothGatt.disconnect();
            if(_BluetoothGatt.getDevice().getBondState() == BluetoothDevice.BOND_NONE){
                // There is a refresh() method in BluetoothGatt class but for now it's hidden. We will call it using reflections.
                try {
                    final Method refresh = _BluetoothGatt.getClass().getMethod("refresh");
                    if (refresh != null) {
                        boolean success = (Boolean) refresh.invoke(_BluetoothGatt);
                        Log.i(_logTAG,"Close => gatt.refresh:" + success);
                    }
                } catch (Exception ex) {
                    Log.e(_logTAG,"Close => gatt.refresh:" + ex.toString());
                }
            }
            _BluetoothGatt.close();
            _BluetoothGatt = null;
        }catch (Exception ex){
            Log.e(_logTAG,"Close => " + ex.toString());
        }

    }

    /**
     * Step
     */
    private void Step(){
        int i = _Index;
        if(!(i == 2 || i == 13)){
            if(!IsConnected){
                _connectIndex = 0;
                Connect();
                return;
            }
        }
        switch (i){
            case 1: Step1(); break;
            case 2: Step2(); break;
            case 3: Step3(); break;
            case 4: Step4(); break;
            case 5: Step5(); break;
            case 6: Step6(); break;
            case 7: Step7(); break;
            case 8: Step8(); break;
            case 9: Step9(); break;
            case 10: Step10(); break;
            case 11: Step11(); break;
            case 12: Step12(); break;
            case 13: Step13(); break;
            default:break;
        }

        int progress  = (int)((i / ((double)_TotalStep * 1.00)) * 100);
        OnProgress(progress);
    }

    private boolean _isStep1 = false;
    /**
     * Enter the upgrade mode
     */
    private void Step1(){
        try {
            if(!IsRunning)
                return;
            if(_OTA_OP_Characteristic == null)
                return;
            _isStep1 = false;

            // Write characteristic
            _OTA_OP_Characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            _OTA_OP_Characteristic.setValue(new byte[]{0x01, 0x04});
            _BluetoothGatt.writeCharacteristic(_OTA_OP_Characteristic);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int i = 0;
                        while (IsRunning && !_isStep1){
                            if(i > 5)
                                break;
                            i++;
                            Thread.sleep(1000);
                        }
                        if(IsRunning) {
                            /*if (_isStep1) {
                                _Index = 2;
                                Step();
                            } else {
                                OnError(3);//无法进入升级模式
                            }*/
                            // The machine may restart directly and will not reply
                            _Index = 2;
                            Step();
                        }
                    }catch (Exception ex){}
                }
            }).start();
        }catch (Exception ex){
            Log.e(_logTAG,"Step1 => " + ex.toString());
            // Can not enter upgrade mode
            OnError(3);
        }
    }

    // Whether to find the device
    private boolean _isStep2 = false;
    /**
     * Receive directed broadcast
     */
    private void Step2_Scan(){
        try {
            if(!IsRunning)
                return;

            if(IsConnected){
                Close();
                Thread.sleep(3000);
            }
            _isStep2 = false;
            _Device = null;
            _BluetoothAdapter.startLeScan(_LeScanCallback);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        boolean isSkip = false;
                        int i = 0;
                        while (IsRunning && !_isStep2){
                            /*if(i > 15) {
                                OnError(4); // Unable to receive directed broadcasts
                                break;
                            }*/
                            // Some phones can not scan the broadcast when the device is directed to broadcast. But you can directly use mac to connect
                            if(i > 10){
                                isSkip = true;
                                break;
                            }
                            i++;
                            Thread.sleep(1000);
                        }
                        _BluetoothAdapter.stopLeScan(_LeScanCallback);
                        if(IsRunning) {
                            if (_isStep2 || isSkip) {
                                IsConnected = false;
                                _connectIndex = 0;
                                Connect();
                            }
                        }
                    }catch (Exception ex){
                        // Unable to receive directed broadcasts
                        OnError(4);
                    }
                }
            }).start();
        }catch (Exception ex){
            Log.e(_logTAG,"Step2 => " + ex.toString());
            // Unable to receive directed broadcasts
            OnError(4);
        }
    }

    /**
     * Direct connection
     */
    private void Step2(){
        try {
            if(!IsRunning)
                return;

            if(IsConnected){
                Close();
                Thread.sleep(1000);
            }
            _isStep2 = false;
            _Device = null;

            Thread.sleep(1000);

            _isStep2 = true;
            if(IsRunning) {
                if (_isStep2) {
                    IsConnected = false;
                    _connectIndex = 0;
                    Connect();
                }
            }
        }catch (Exception ex){
            Log.e(_logTAG,"Step2 => " + ex.toString());
            // Unable to receive directed broadcasts
            OnError(4);
        }
    }

    private boolean _isStep3 = false;
    /**
     * Upgrade mode initialization
     */
    private void Step3(){
        try {
            if(!IsRunning)
                return;
            if(_OTA_OP_Characteristic == null)
                return;
            _isStep3 = false;

            // Write Characteristic
            _OTA_OP_Characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            _OTA_OP_Characteristic.setValue(new byte[]{0x01, 0x04});
            _BluetoothGatt.writeCharacteristic(_OTA_OP_Characteristic);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int i = 0;
                        while (IsRunning && !_isStep3){
                            if(i > 5)
                                break;
                            i++;
                            Thread.sleep(1000);
                        }
                        if(IsRunning) {
                            if (_isStep3) {
                                _Index = 4;
                                Step();
                            } else {
                                // Upgrade mode initialization failed
                                OnError(9);
                            }
                        }
                    }catch (Exception ex){}
                }
            }).start();

        }catch (Exception ex){
            Log.e(_logTAG,"Step3 => " + ex.toString());
            // Upgrade mode initialization failed
            OnError(9);
        }
    }

    private boolean _isStep4 = false;
    private byte _isStep4_errcode = 0x00;
    /**
     * Send the file size to the device
     */
    private void Step4(){
        try {
            if(!IsRunning)
                return;
            if(_OTA_Packet_Characteristic == null)
                return;
            if(_FileUpdate == null){
                // The upgrade package is corrupted
                OnError(11);
                return;
            }

            _isStep4_errcode = 0x00;
            _isStep4 = false;

            int softDeviceImageSize = 0;
            int bootloaderImageSize = 0;
            int appImageSize = _FileUpdate.getFileSize();
            _OTA_Packet_Characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            _OTA_Packet_Characteristic.setValue(new byte[12]);
            _OTA_Packet_Characteristic.setValue(softDeviceImageSize, BluetoothGattCharacteristic.FORMAT_UINT32, 0);
            _OTA_Packet_Characteristic.setValue(bootloaderImageSize, BluetoothGattCharacteristic.FORMAT_UINT32, 4);
            _OTA_Packet_Characteristic.setValue(appImageSize, BluetoothGattCharacteristic.FORMAT_UINT32, 8);
            _BluetoothGatt.writeCharacteristic(_OTA_Packet_Characteristic);


            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int i = 0;
                        while (IsRunning && !_isStep4){
                            if(i > 5)
                                break;
                            i++;
                            Thread.sleep(1000);
                        }
                        if(IsRunning) {
                            if (_isStep4) {
                                if (_isStep4_errcode == 0x01) {
                                    _Index = 5;
                                    Step();
                                } else {
                                    // Write file size, rejected by device
                                    OnError(13);
                                }
                            } else {
                                if(_IsCompatible){
                                    _Index = 5;
                                    Step();
                                }else {
                                    // Write file size, device not responding
                                    OnError(12);
                                }
                            }
                        }
                    }catch (Exception ex){}
                }
            }).start();

        }catch (Exception ex){
            Log.e(_logTAG,"Step4 => " + ex.toString());
            // Write file size process exception
            OnError(10);
        }
    }

    private boolean _isStep5 = false;
    /**
     * Inform the device to receive the relevant parameters for initialization
     */
    private void Step5(){
        try {
            if(!IsRunning)
                return;
            if(_OTA_OP_Characteristic == null)
                return;
            _isStep5 = false;

            _OTA_OP_Characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            _OTA_OP_Characteristic.setValue(new byte[]{0x02, 0x00});
            _BluetoothGatt.writeCharacteristic(_OTA_OP_Characteristic);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int i = 0;
                        while (IsRunning && !_isStep5){
                            if(i > 5)
                                break;
                            i++;
                            Thread.sleep(1000);
                        }
                        if(IsRunning) {
                            if (_isStep5) {
                                _Index = 6;
                                Step();
                            } else {
                                // Inform the device to receive the initialization parameters, the device did not respond
                                OnError(15);
                            }
                        }
                    }catch (Exception ex){}
                }
            }).start();

        }catch (Exception ex){
            Log.e(_logTAG,"Step5 => " + ex.toString());
            // Inform the device to receive the initialization parameters related to the process of abnormal
            OnError(14);
        }
    }

    private boolean _isStep6 = false;
    /**
     * Write the initialization parameter
     */
    private void Step6(){
        try {
            if(!IsRunning)
                return;
            if(_OTA_Packet_Characteristic == null)
                return;
            if(_FileUpdate == null){
                // The upgrade package is corrupted
                OnError(11);
                return;
            }
            _isStep6 = false;

            _OTA_Packet_Characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            _OTA_Packet_Characteristic.setValue(new byte[14]);
            _OTA_Packet_Characteristic.setValue(Integer.parseInt(_FileUpdate.getType(),16), BluetoothGattCharacteristic.FORMAT_UINT16, 0);
            _OTA_Packet_Characteristic.setValue(Integer.parseInt(_FileUpdate.getVersion()), BluetoothGattCharacteristic.FORMAT_UINT16, 2);
            _OTA_Packet_Characteristic.setValue(0xffffffff, BluetoothGattCharacteristic.FORMAT_UINT32, 4);
            _OTA_Packet_Characteristic.setValue(1, BluetoothGattCharacteristic.FORMAT_UINT16, 8);
            _OTA_Packet_Characteristic.setValue(0x0064, BluetoothGattCharacteristic.FORMAT_UINT16, 10);
            _OTA_Packet_Characteristic.setValue(_FileUpdate.get_CRC16(), BluetoothGattCharacteristic.FORMAT_UINT16, 12);

            byte[] bytes = _OTA_Packet_Characteristic.getValue();
            String strBytes = StringConvertUtil.bytesToHexString(bytes);
            Log.i(_logTAG,"Step6 => Initialize parameters:" + strBytes);

            _BluetoothGatt.writeCharacteristic(_OTA_Packet_Characteristic);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int i = 0;
                        while (IsRunning && !_isStep6){
                            if(i > 5)
                                break;
                            i++;
                            Thread.sleep(1000);
                        }
                        if(IsRunning) {
                            if (_isStep6) {
                                _Index = 7;
                                Step();
                            } else {
                                // The write file is initialized and the device is not responding
                                OnError(17);
                            }
                        }
                    }catch (Exception ex){}
                }
            }).start();

        }catch (Exception ex){
            Log.e(_logTAG,"Step6 => " + ex.toString());
            // Write file initialization process exception
            OnError(16);
        }
    }

    private boolean _isStep7 = false;
    private byte _isStep7_errcode = 0x00;
    /**
     * Notify the device to initialize the relevant parameters sent to complete
     */
    private void Step7(){
        try {
            if(!IsRunning)
                return;
            if(_OTA_OP_Characteristic == null)
                return;
            _isStep7 = false;
            _isStep7_errcode = 0x00;

            _OTA_OP_Characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            _OTA_OP_Characteristic.setValue(new byte[]{0x02, 0x01});
            _BluetoothGatt.writeCharacteristic(_OTA_OP_Characteristic);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int i = 0;
                        while (IsRunning && !_isStep7){
                            if(i > 5)
                                break;
                            i++;
                            Thread.sleep(1000);
                        }
                        if(IsRunning) {
                            if (_isStep7) {
                                if (_isStep7_errcode == 0x01) {
                                    _Index = 8;
                                    Step();
                                } else {
                                    // Notify the device to write the file initialization parameters related to the completion of transmission, the device refused
                                    OnError(20);
                                }
                            } else {
                                if(_IsCompatible){
                                    _Index = 8;
                                    Step();
                                }else {
                                    // Notify the device to write the file initialization parameters related to the completion of sending, the device did not respond
                                    OnError(19);
                                }
                            }
                        }
                    }catch (Exception ex){}
                }
            }).start();
        }catch (Exception ex){
            Log.e(_logTAG,"Step7 => " + ex.toString());
            // Notify the device to write the file initialization parameters related to the completion of the sending process exception
            OnError(18);
        }
    }

    private boolean _isStep8 = false;
    /**
     * Set the device to receive N packets and wait for device response
     */
    private void Step8(){
        try {
            if(!IsRunning)
                return;
            if(_OTA_OP_Characteristic == null)
                return;
            if(_FileUpdate == null){
                // The upgrade package is corrupted
                OnError(11);
                return;
            }
            _isStep8 = false;

            _OTA_OP_Characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            _OTA_OP_Characteristic.setValue(new byte[3]);
            _OTA_OP_Characteristic.setValue(0x08, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            _OTA_OP_Characteristic.setValue(_PackageLength, BluetoothGattCharacteristic.FORMAT_UINT16, 1);

            String strLog = StringConvertUtil.bytesToHexString(_OTA_OP_Characteristic.getValue());
            Log.i(_logTAG, "Step8 => Set the device to receive N packets:" + strLog);

            _BluetoothGatt.writeCharacteristic(_OTA_OP_Characteristic);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int i = 0;
                        while (IsRunning && !_isStep8){
                            if(i > 5)
                                break;
                            i++;
                            Thread.sleep(1000);
                        }
                        if(IsRunning) {
                            if (_isStep8) {
                                _Index = 9;
                                Step();
                            } else {
                                // Set the device to receive N packets waiting for device response, the device is not responding
                                OnError(22);
                            }
                        }
                    }catch (Exception ex){}
                }
            }).start();

        }catch (Exception ex){
            Log.e(_logTAG,"Step8 => " + ex.toString());
            // Set the device to receive an N packet waiting for an exception during device response
            OnError(21);
        }
    }

    private boolean _isStep9 = false;
    /**
     * Notify the device to receive the file
     */
    private void Step9(){
        try {
            if(!IsRunning)
                return;
            if(_OTA_OP_Characteristic == null)
                return;
            _isStep9 = false;

            _OTA_OP_Characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            _OTA_OP_Characteristic.setValue(new byte[]{0x03});
            _BluetoothGatt.writeCharacteristic(_OTA_OP_Characteristic);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int i = 0;
                        while (IsRunning && !_isStep9){
                            if(i > 5)
                                break;
                            i++;
                            Thread.sleep(1000);
                        }
                        if(IsRunning) {
                            if (_isStep9) {
                                _Index = 10;
                                Step();
                            } else {
                                // Inform the device ready to receive the file, the device did not respond
                                OnError(23);
                            }
                        }
                    }catch (Exception ex){}
                }
            }).start();

        }catch (Exception ex){
            Log.e(_logTAG,"Step9 => " + ex.toString());
            // Notification device ready to receive file process exception
            OnError(23);
        }
    }

    private boolean _isStep10 = false;
    private byte  _isStep10_errcode = 0x00;
    private boolean _isStep10_isRunning = false;
    /**
     * Subcontract to send data
     */
    private void Step10(){
        try {
            if(!IsRunning)
                return;
            if(_OTA_Packet_Characteristic == null)
                return;
            if(_FileUpdate == null){
                // The upgrade package is corrupted
                OnError(11);
                return;
            }

            _isStep10 = false;
            _isStep10_isRunning = true;
            _isStep10_errcode = 0x00;
            _PackageIndex = 0;

            Step10_Package();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        OnWriteSpeed(_PackageLength,0);
                        int i = 0;
                        while (IsRunning && _isStep10_isRunning && !_isStep10){
                            // 5 minutes out
                            if(i > (5 * 60 * 100))
                                break;
                            if (_isStep10_Package_next) {
                                if (_PackageIndex * _PackageLength < _FileUpdate.getPackageNumber())
                                    Step10_Package();
                            }
                            i++;
                            Thread.sleep(10);
                        }
                        if(IsRunning) {
                            if (_isStep10) {
                                if(_isStep10_errcode == 0x01) {
                                    _Index = 11;
                                    Step();
                                }else {
                                    // Subcontracting data, data errors, or device unreachable
                                    OnError(27);
                                }
                            } else {
                                if(_IsCompatible){
                                    _Index = 11;
                                    Step();
                                }else {
                                    // Subcontract to send data, the device is not responding
                                    OnError(26);
                                }
                            }
                        }
                    }catch (Exception ex){
                        Log.e(_logTAG,"Step10 Thread => " + ex.toString());
                        // Subcontract send data process exception
                        OnError(25);
                    }
                }
            }).start();

        }catch (Exception ex){
            Log.e(_logTAG,"Step10 => " + ex.toString());
            // Subcontract send data process exception
            OnError(25);
        }
    }

    private boolean _isStep10_Package = false;
    private boolean _isStep10_Package_next = false;
    private boolean _isStep10_Package_command_isok = false;
    private void Step10_Package(){
        try {
            if(!IsRunning)
                return;
            _isStep10_Package = false;
            _isStep10_Package_next = false;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int i = 0;
                        boolean isEnd = false;
                        Date speedDate = new Date();
                        while (IsRunning && i < _PackageLength){
                            int j = (_PackageIndex * _PackageLength) + i;
                            if(j >= _FileUpdate.getPackageNumber()) {
                                isEnd = true;
                                break;
                            }
                            _isStep10_Package_command_isok = false;
                            byte[] bytes = _FileUpdate.getBytelist().get(j);
                            _OTA_Packet_Characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                            _OTA_Packet_Characteristic.setValue(bytes);
                            _BluetoothGatt.writeCharacteristic(_OTA_Packet_Characteristic);
                            Log.i(_logTAG,"write Package => " + j + "、" + StringConvertUtil.bytesToHexString(bytes));

                            /**************/
                            int k = 0;
                            while (!_isStep10_Package_command_isok){
                                if(k > 1000)
                                    break;
                                k++;
                                Thread.sleep(1);
                            }
                            if(!_isStep10_Package_command_isok){
                                break;
                            }
                            /**************/

                            i ++;
                        }
                        if(IsRunning) {
                            OnWriteSpeed(_PackageLength,new Date().getTime() - speedDate.getTime());// Calculate the speed
                            if (!isEnd) {
                                int j = 0;
                                while (IsRunning && _isStep10_Package_command_isok && !_isStep10_Package) {
                                    if (j > 500) {
                                        break;
                                    }
                                    j++;
                                    Thread.sleep(10);
                                }
                                if (_isStep10_Package) {
                                    _isStep10_Package_next = true;
                                    _PackageIndex++;
                                } else {
                                    _isStep10_isRunning = false;
                                    // Subcontract to send data, the device is not responding
                                    OnError(26);
                                }
                            }
                        }
                    }catch (Exception ex){
                        Log.e(_logTAG,"Step10_Package Thread => " + ex.toString());
                        _isStep10_isRunning = false;
                        // Subcontract send data process exception
                        OnError(25);
                    }
                }
            }).start();
        }catch (Exception ex){
            Log.e(_logTAG,"Step10_Package => " + ex.toString());
            _isStep10_isRunning = false;
            // Subcontract send data process exception
            OnError(25);
        }
    }

    private boolean _isStep11 = false;
    private byte _isStep11_errcode = 0x00;
    /**
     * Check that the verification of the received document is correct
     */
    private void Step11(){
        try {
            if(!IsRunning)
                return;
            if(_OTA_OP_Characteristic == null)
                return;
            _isStep11 = false;
            _isStep11_errcode = 0x00;

            _OTA_OP_Characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            _OTA_OP_Characteristic.setValue(new byte[]{0x04});
            _BluetoothGatt.writeCharacteristic(_OTA_OP_Characteristic);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int i = 0;
                        while (IsRunning && !_isStep11){
                            if(i > 5)
                                break;
                            i++;
                            Thread.sleep(1000);
                        }
                        if(IsRunning) {
                            if (_isStep11) {
                                if (_isStep11_errcode == 0x01) {
                                    _Index = 12;
                                    Step();
                                } else {
                                    // Check that the verification of the received file is correct and the error is ended
                                    OnError(30);
                                }
                            } else {
                                if(_IsCompatible){
                                    _Index = 12;
                                    Step();
                                }else {
                                    // Check that the file is checked for correctness and the device is not responding
                                    OnError(29);
                                }
                            }
                        }
                    }catch (Exception ex){}
                }
            }).start();

        }catch (Exception ex){
            Log.e(_logTAG,"Step11 => " + ex.toString());
            // Check if the check of the received file is correct
            OnError(28);
        }
    }

    private boolean _isStep12 = false;
    /**
     * Start new firmware
     */
    private void Step12(){
        try {
            if(!IsRunning)
                return;
            if(_OTA_OP_Characteristic == null)
                return;
            _isStep12 = false;

            _OTA_OP_Characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            _OTA_OP_Characteristic.setValue(new byte[]{0x05});
            _BluetoothGatt.writeCharacteristic(_OTA_OP_Characteristic);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int i = 0;
                        while (IsRunning && !_isStep12){
                            if(i > 5)
                                break;
                            i++;
                            Thread.sleep(1000);
                        }
                        if(IsRunning) {
                            /*if (_isStep12) {
                                _Index = 13;
                                Step();
                            } else {
                                OnError(32);//Start new firmware, device not responding
                            }*/

                            // The machine may restart directly and will not reply
                            _Index = 13;
                            Step();
                        }
                    }catch (Exception ex){}
                }
            }).start();

        }catch (Exception ex){
            Log.e(_logTAG,"Step12 => " + ex.toString());
            // Start a new firmware process exception
            OnError(31);
        }
    }

    private boolean _isStep13 = false;
    /**
     * Check for new firmware
     */
    public void Step13(){
        try {
            if(!IsRunning)
                return;
            if(IsConnected){
                Close();
                Thread.sleep(1000);
            }
            _isStep13 = false;
            _Device = null;
            _BluetoothAdapter.startLeScan(_LeScanCallback);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int i = 0;
                        while (IsRunning && !_isStep13){
                            if(i > 30) {
                                // Check for new firmware process exceptions
                                OnError(33);
                                break;
                            }
                            i++;
                            Thread.sleep(1000);
                        }
                        _BluetoothAdapter.stopLeScan(_LeScanCallback);
                        if(IsRunning) {
                            if (_isStep13) {
                                if (_FileUpdate.getType().equals(HardwareModel)
                                        && _FileUpdate.getVersion().equals(Firmware)) {
                                    if (_IOTAService != null)
                                        _IOTAService.OnComplete(true);
                                } else {
                                    if (_IOTAService != null)
                                        _IOTAService.OnComplete(false);
                                }
                                Dispose();
                            }
                        }
                    }catch (Exception ex){
                        // Check for new firmware process exceptions
                        OnError(33);
                    }
                }
            }).start();
        }catch (Exception ex){
            Log.e(_logTAG,"Step13 => " + ex.toString());
            // Check for new firmware process exceptions
            OnError(33);
        }
    }

    /**
     * To solve some of the phone response to read and write state error, in fact, is successful
     * Such as normal BluetoothGatt.GATT_SUCCESS (0x00), but some phones return 0x02
     * @return
     */
    protected boolean isSkipBluetoothGattCallbackStatus(){
        /*boolean isSkip = false;
        try {
            //Le X820
            if(android.os.Build.MANUFACTURER.contains("Le")){
                isSkip = true;
            }
        }catch (Exception ex){}
        return isSkip;*/
        return true;
    }

    /**
     * Progress
     * @param progress
     */
    public void OnProgress(int progress){
        if(_IOTAService != null){
            _IOTAService.OnProgress(progress,_Index);
        }
    }

    /**
     * Write speed
     * @param number Write the total number
     * @param time time (ms)
     */
    public void OnWriteSpeed(int number,long time){
        double speed = 0;
        if(time > 0 && number > 0) {
            speed = (number * 20 / 1000.00) / (time / 1000.00);
            speed = Double.parseDouble(String.format("%.2f",speed));
        }
        if(_IOTAService != null){
            _IOTAService.OnWriteSpeed(speed);
            if(_FileUpdate != null) {
                if(_FileUpdate.getPackageNumber() - (_PackageIndex * _PackageLength) < _PackageLength)
                    _IOTAService.OnWriteProgress(_FileUpdate.getPackageNumber(), _FileUpdate.getPackageNumber());
                else
                    _IOTAService.OnWriteProgress(_PackageIndex * _PackageLength, _FileUpdate.getPackageNumber());
            }
        }
    }

    /**
     * error code
     * @param code
     */
    public void OnError(int code){
        IsRunning = false;
        if(_IOTAService != null){
            _IOTAService.OnError(code);
        }
        Dispose();
    }
}
