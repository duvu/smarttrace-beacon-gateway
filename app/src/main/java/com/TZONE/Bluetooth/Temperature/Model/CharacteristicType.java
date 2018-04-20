package com.TZONE.Bluetooth.Temperature.Model;

/**
 * Created by Forrest on 2015/9/28.
 */
public enum  CharacteristicType {
    /**
     * Unknown
     */
    Unknown,
    /**
     * Device Name
     */
    Name,
    /**
     * SN
     */
    SN,
    /**
     * Broadcast interval
     */
    Interval,
    /**
     * TransmitPower
     */
    TransmitPower,
    /**
     * Token
     */
    Token,
    /**
     * Sampling Interval
     */
    SamplingInterval,
    /**
     * Storage interval
     */
    SaveInterval,
    /**
     * Store data coverage
     */
    IsSaveOverwrite,
    /**
     * Data is stored article number
     */
    SavaCount,
    /**
     * Alarm parameters setting
     */
    Alarm,
    /**
     * UTC Time
     */
    UTC,
    /**
     * Synchronous data switch
     */
    Sysn,
    /**
     * Start or end
     */
    Trip,
    /**
     * Sync Mode
     */
    SyncMode,
    /**
     * Light
     */
    Light,
    /**
     * Model and version
     */
    ModelVersion,
    /**
     * LDO Voltage
     */
    LDOVoltage,
    /**
     * LDO
     */
    LDOTemp,
    /**
     * LDO Power
     */
    LDOPower,
    /**
     * Other
     */
    OtherBytes1,
    OtherBytes2,
    OtherBytes3,
    OtherBytes4,
    OtherBytes5
}
