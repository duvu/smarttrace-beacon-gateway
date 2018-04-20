package com.TZONE.Bluetooth;

import java.util.HashMap;

/**
 * Config CallBack
 * Created by Forrest on 2016/6/1.
 */
public interface IConfigCallBack extends IPeripheryBluetoothCallBack {
    /**
     * Read Config CallBack
     * @param status
     * @param uuids
     */
    public abstract void OnReadConfigCallBack(boolean status, HashMap<String, byte[]> uuids);

    /**
     * Write Config CallBack
     * @param status
     */
    public abstract void OnWriteConfigCallBack(boolean status);
}
