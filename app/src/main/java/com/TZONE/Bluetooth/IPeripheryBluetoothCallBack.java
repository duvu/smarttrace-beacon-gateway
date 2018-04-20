package com.TZONE.Bluetooth;

import java.util.List;
import java.util.UUID;

/**
 * 周边蓝牙设备操作回调接口
 * Bluetooth devices around callback interface to operate
 * Created by Forrest on 2015/9/9.
 */
public interface IPeripheryBluetoothCallBack {
    /**
     * e
     */
    public abstract void OnConnected();

    /**
     * DisConnected event
     */
    public abstract void OnDisConnected();

    /**
     * Servicesed event
     * @param services
     */
    public abstract void OnServicesed(List<BLEGattService> services);

    /**
     * Read CallBack event
     * @param uuid
     * @param data
     */
    public abstract void OnReadCallBack(UUID uuid, byte[] data);

    /**
     * Write Callback event
     * @param uuid
     */
    public abstract void OnWriteCallBack(UUID uuid, boolean isSuccess);

    /**
     * Receive Callback event
     * @param uuid
     * @param data
     */
    public abstract void OnReceiveCallBack(UUID uuid, byte[] data);
}
