package com.TZONE.Bluetooth.Temperature;

import com.TZONE.Bluetooth.BLE;
import com.TZONE.Bluetooth.LocalBluetoothServer;
import com.TZONE.Bluetooth.Temperature.Model.Device;

/**
 * Broadcasting services
 * Created by Forrest on 2016/6/1.
 */
public class BroadcastService extends LocalBluetoothServer {
    /**
     * Ble to Device
     * @param ble
     * @return
     */
    public Device ConvertDevice(final BLE ble){
        Device device = new Device();
        if(device.fromScanData(ble))
            return device;
        else
            return null;
    }
}
