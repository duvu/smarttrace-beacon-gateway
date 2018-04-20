package com.TZONE.Bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/**
 * Configuration operation service base class
 * Created by Forrest on 2016/6/1.
 */
public class ConfigServiceBase {
    /**
     * Bluetooth devices around the service
     */
    private PeripheryBluetoothService _PeripheryBluetoothService;
    /**
     * Is Connected
     */
    public boolean IsConnected = false;
    /**
     * Is Running
     */
    public boolean IsRunning = false;
    /**
     * Read Config log
     */
    private ConfigHandle _ReadConfigs;
    /**
     * Write Config log
     */
    private ConfigHandle _WriteConfigs;
    /**
     * Configure callback
     */
    private IConfigCallBack _ConfigCallBack;

    public ConfigServiceBase(BluetoothAdapter bluetoothAdapter, Context context, String macAddress, long timeout, IConfigCallBack configCallBack) throws Exception {
        IsRunning = true;
        _ConfigCallBack = configCallBack;
        _PeripheryBluetoothService = new PeripheryBluetoothService(bluetoothAdapter,context,macAddress,timeout,peripheryBluetoothCallBack);
        if(!_PeripheryBluetoothService.Connect())
            throw new Exception("Unable to connect"); //Unable to connect
    }

    public HashMap<String,byte[]> ReadItems = new HashMap<String,byte[]>();

    /**
     * Read
     * @param uuids
     * @return
     */
    public boolean Read(final List<String> uuids){
        if(!IsRunning)
            return false;
        try{
            if(IsConnected && _PeripheryBluetoothService!=null){
                _ReadConfigs = new ConfigHandle();
                ReadItems.clear();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            int i = 0; String uuid = "";int c = 10;
                            while (i < uuids.size()){
                                // Check whether the previous feature was successful
                                if(i > 0 && !uuid.equals("") && !_ReadConfigs.IsRespond(uuid)) {
                                    if(c > 0){
                                        Thread.sleep(100);
                                        c --;
                                        continue;
                                    }else {
                                        if (_ConfigCallBack != null) {
                                            _ConfigCallBack.OnReadConfigCallBack(false, null);
                                        }
                                        break;
                                    }
                                }
                                uuid = uuids.get(i);
                                if(_PeripheryBluetoothService.IsExistCharacteristic(uuid)){
                                    Log.i("Read","Request to read the characteristic uuid : " + uuid);
                                    ReadItems.put(uuid, null);
                                    _ReadConfigs.ConfigRequest(uuid);
                                    _PeripheryBluetoothService.ReadCharacteristic(uuid);
                                    c = 10;
                                }else{
                                    uuid = "";
                                }
                                i++;
                            }
                            Log.i("Read","Read finished");
                            _ReadConfigs.ConfigRequestComplete();

                            int j = 0;
                            while (true){
                                // Check if the reading is complete
                                if(j < 3){
                                    if(_ReadConfigs.IsComplete()) {
                                        if (_ConfigCallBack != null) {
                                            _ConfigCallBack.OnReadConfigCallBack(true, ReadItems);
                                        }
                                        break;
                                    }
                                }else {
                                    if (_ConfigCallBack != null) {
                                        _ConfigCallBack.OnReadConfigCallBack(false, null);
                                    }
                                    break;
                                }
                                j ++;
                                Thread.sleep(1000);
                            }

                            _ReadConfigs = null;
                        } catch (Exception ex) {
                            Log.e("Read",ex.toString());
                        }
                    }
                }).start();

            }
        }catch (Exception ex){
            Log.e("Read",ex.toString());
        }
        return false;
    }

    /**
     * Write
     * @param uuids
     * @return
     */
    public boolean Write(final LinkedHashMap<String,byte[]> uuids){
        if(!IsRunning)
            return false;
        try{
            if(IsConnected && _PeripheryBluetoothService!=null){
                _WriteConfigs = new ConfigHandle();
               new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Iterator iter = uuids.entrySet().iterator();
                            while (iter.hasNext()){
                                java.util.Map.Entry entry =  (java.util.Map.Entry)iter.next();
                                String uuid = (String)entry.getKey();
                                byte[] bytes = (byte[])entry.getValue();
                                if(_PeripheryBluetoothService.IsExistCharacteristic(uuid) && bytes !=null) {
                                    _WriteConfigs.ConfigRequest(uuid);
                                    _PeripheryBluetoothService.WriteCharacteristic(uuid, bytes);
                                    Log.d("Write","uuid => " + uuid + " ok");
                                    int i = 0;
                                    while (!_WriteConfigs.IsRespond(uuid)){
                                        if(i > 30) {
                                            if(_ConfigCallBack!= null)
                                                _ConfigCallBack.OnWriteConfigCallBack(false);
                                            break;
                                        }
                                        i ++;
                                        Thread.sleep(100);
                                    }
                                }
                            }
                            Log.i("Write","Write finished");
                            _WriteConfigs.ConfigRequestComplete();

                            int j = 0;
                            while (true){
                                // Check if the reading is complete
                                if(j < 3){
                                    if( _WriteConfigs.IsComplete()) {
                                        if (_ConfigCallBack != null) {
                                            _ConfigCallBack.OnWriteConfigCallBack(true);
                                        }
                                        break;
                                    }
                                }else {
                                    if (_ConfigCallBack != null) {
                                        _ConfigCallBack.OnWriteConfigCallBack(false);
                                    }
                                    break;
                                }
                                j ++;
                                Thread.sleep(1000);
                            }

                            _WriteConfigs = null;

                        } catch (Exception ex) {
                            Log.e("Write",ex.toString());
                        }
                    }
                }).start();
            }
        }catch (Exception ex){
            Log.e("Write",ex.toString());
        }
        return false;
    }

    /**
     * Set when the specified characteristic value changes, issued a notice.
     * @param uuid
     * @param enabled
     * @return
     */
    public boolean EnableNotification(String uuid, boolean enabled){
        if(!IsRunning)
            return false;
        return  _PeripheryBluetoothService.EnableNotification(uuid,enabled);
    }

    public void Dispose(){
        try{
            IsRunning = false;
            _ReadConfigs = null;
            _WriteConfigs = null;
            if(_PeripheryBluetoothService!=null)
                _PeripheryBluetoothService.Close();
        }catch (Exception ex){
            Log.e("Dispose",ex.toString());
        }
    }

    /**
     * CallBack
     */
    public IPeripheryBluetoothCallBack peripheryBluetoothCallBack = new IPeripheryBluetoothCallBack() {
        @Override
        public void OnConnected() {
            IsConnected = true;
            if(!IsRunning)
                return;
            if(_ConfigCallBack!= null) {
                _ConfigCallBack.OnConnected();
            }
        }

        @Override
        public void OnDisConnected() {
            IsConnected = false;
            if(!IsRunning)
                return;
            if(_ConfigCallBack!= null) {
                _ConfigCallBack.OnDisConnected();
            }
        }

        @Override
        public void OnServicesed(List<BLEGattService> services) {
            if(!IsRunning)
                return;
            if(_ConfigCallBack!= null) {
                _ConfigCallBack.OnServicesed(services);
            }
        }

        @Override
        public void OnReadCallBack(UUID uuid, byte[] data) {
            if(!IsRunning)
                return;
            if(_ReadConfigs != null) {
                _ReadConfigs.ConfigRespond(uuid.toString());
                if(ReadItems.containsKey(uuid.toString())){
                    ReadItems.put(uuid.toString(),data);
                }
                if(_ConfigCallBack!= null){
                    _ConfigCallBack.OnReadCallBack(uuid,data);
                    /*if(_ReadConfigs.IsComplete()){
                        _ReadConfigs = null;
                        _ConfigCallBack.OnReadConfigCallBack(true,ReadItems);
                    }*/
                }
            }
        }

        @Override
        public void OnWriteCallBack(UUID uuid, boolean isSuccess) {
            if(!IsRunning)
                return;
            if (_WriteConfigs != null) {
                if(isSuccess)
                    _WriteConfigs.ConfigRespond(uuid.toString());
                if(_ConfigCallBack!= null){
                    _ConfigCallBack.OnWriteCallBack(uuid,isSuccess);
                    /*if(_WriteConfigs.IsComplete()) {
                        _WriteConfigs = null;
                        _ConfigCallBack.OnWriteConfigCallBack(true);
                    }*/
                }
            }
        }

        @Override
        public void OnReceiveCallBack(UUID uuid, byte[] data) {
            if(!IsRunning)
                return;
            if(_ConfigCallBack != null)
                _ConfigCallBack.OnReceiveCallBack(uuid, data);
        }
    };
}
