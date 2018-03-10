package com.TZONE.Bluetooth;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.ArrayList;
import java.util.List;

/**
 * BLE GATT
 * Created by Forrest on 2015/9/9.
 */
public class BLEGattService {
    public BluetoothGattService GattService;
    public List<BLEGattCharacteristic> CharacterList = new ArrayList<>();

    /**
     * @param bluetoothGattService
     */
    public BLEGattService(BluetoothGattService bluetoothGattService){
        this.GattService = bluetoothGattService;
        List<BluetoothGattCharacteristic> bluetoothGattCharacteristicList = bluetoothGattService.getCharacteristics();
        for (int i = 0; i < bluetoothGattCharacteristicList.size(); i++) {
            BLEGattCharacteristic bleGattCharacteristic = new BLEGattCharacteristic(bluetoothGattCharacteristicList.get(i));
            CharacterList.add(bleGattCharacteristic);
        }
    }

    /**
     * Characteristic
     */
    public class BLEGattCharacteristic{
        public BluetoothGattCharacteristic GattCharacteristic;
        public byte[] val;
        public BLEGattCharacteristic(BluetoothGattCharacteristic bluetoothGattCharacteristic){
            this.GattCharacteristic = bluetoothGattCharacteristic;
        }
    }
}
