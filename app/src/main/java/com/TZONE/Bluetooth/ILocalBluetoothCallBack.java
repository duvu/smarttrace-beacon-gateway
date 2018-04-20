package com.TZONE.Bluetooth;

/**
 * Bluetooth radio callback interface
 * Created by Forrest on 2016/6/1.
 */
public interface ILocalBluetoothCallBack {
    /**
     * Entered event
     */
    public abstract void OnEntered(BLE ble);
    /**
     * Update event
     */
    public abstract void OnUpdate(BLE ble);
    /**
     * Exited event
     */
    public abstract void OnExited(BLE ble);
    /**
     * Scan Complete event
     */
    public abstract void OnScanComplete();
}
