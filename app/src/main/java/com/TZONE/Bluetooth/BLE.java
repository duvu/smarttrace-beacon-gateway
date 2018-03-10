package com.TZONE.Bluetooth;

import java.util.Date;

/**
 * BLE
 * Created by Forrest on 2015/9/8.
 * Note: all BLE equipment base class
 */
public class BLE {
    /**
     * Name
     */
    public String Name;
    /**
     * RSSI
     */
    public int RSSI;
    /**
     * Scan Data
     */
    public byte[] ScanData;
    /**
     * MacAddress
     */
    public String MacAddress;
    /**
     * Last ScanTime
     */
    public Date LastScanTime;

}
