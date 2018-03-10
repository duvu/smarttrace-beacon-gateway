package com.TZONE.Bluetooth.Temperature.Model;

import android.util.Log;

import com.TZONE.Bluetooth.AppBase;
import com.TZONE.Bluetooth.BLE;
import com.TZONE.Bluetooth.Utils.BroadcastPacketsUtil;
import com.TZONE.Bluetooth.Utils.DateUtil;
import com.TZONE.Bluetooth.Utils.StringConvertUtil;
import com.TZONE.Bluetooth.Utils.StringUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Device
 * Created by Forrest on 2016/6/1.
 */
public class Device extends BLE {
    /**
     * Measuring power 1 m of RSSI values
     */
    public int MeasuredPower;
    /**
     * battery
     */
    public int Battery;
    /**
     * SN
     */
    public String SN;
    /**
     * Connect the password
     */
    public String Token;
    /**
     * Broadcast interval
     */
    public int Interval;
    /**
     * Transmission power
     */
    public int TransmitPower;
    /**
     * Hardware model
     * 3901 BT04
     * 3A01 BT05
     * 3C01 BT04B(BT04 with buttons)
     * 3A04 BT05B(BT05 with external sensor)
     */
    public String HardwareModel;
    /**
     * Firmware version
     */
    public String Firmware;

    /**
     * Temperature
     */
    public double Temperature;
    /**
     * Humidity
     */
    public double Humidity;

    /**
     * Alarm Type
     */
    public String AlarmType;
    /**
     * Temperature and humidity sensor acquisition interval
     */
    public int SamplingInterval;
    /**
     * Storage interval (normal)
     */
    public int SaveInterval;
    /**
     * Storage interval (alarm)
     */
    public int SaveInterval2;
    /**
     * Store data coverage
     */
    public boolean IsSaveOverwrite;
    /**
     * Data is stored article number
     */
    public int SavaCount;
    /**
     * Low temperature
     */
    public double LT;
    /**
     * High temperature
     */
    public double HT;
    /**
     * UTC Time
     */
    public Date UTCTime;
    /**
     * Schedule status
     */
    public int TripStatus;
    /**
     * LDO
     */
    public double LDOVoltage;
    /**
     * LDO
     */
    public double LDOTemp;
    /**
     * LDO Power
     */
    public int LDOPower;

    /**
     * Other
     */
    public List<byte[]> OtherBytes;
    /**
     * Notes
     */
    public String Notes;
    /**
     * Description
     */
    public String Description;

    public Device() {
        this.TransmitPower = -1000;
        this.Interval = -1000;
        this.SamplingInterval = -1000;

        this.Temperature = -1000;
        this.Humidity = -1000;
        this.LT = -1000;
        this.HT = -1000;
        this.AlarmType = "00";

        this.LDOPower = 0;
        this.LDOVoltage = -1000;
        this.LDOTemp = -1000;

        this.Name = "";
        this.Notes = "";
        this.Description = "";
        this.OtherBytes = new ArrayList<byte[]>();
        this.OtherBytes.add(new byte[20]);
        this.OtherBytes.add(new byte[20]);
        this.OtherBytes.add(new byte[20]);
        this.OtherBytes.add(new byte[20]);
        this.OtherBytes.add(new byte[16]);
    }

    public boolean fromScanData(BLE ble) {
        return fromScanData(ble.Name, ble.MacAddress, ble.RSSI, ble.ScanData);
    }

    /**
     * Parsing the broadcast data
     *
     * @param name
     * @param macAddress
     * @param rssi
     * @param scanData
     */
    public boolean fromScanData(String name, String macAddress, int rssi, byte[] scanData) {
        try {
            this.Name = name;
            this.MacAddress = macAddress;
            this.RSSI = rssi;
            this.ScanData = scanData;
            this.LastScanTime = new Date();
            String strScanData = StringConvertUtil.bytesToHexString(scanData);
            if (AppBase.IsDebug) {
                Log.i("DeviceBase", "fromScanData:" + strScanData);
            }
            String serviceData = BroadcastPacketsUtil.GetScanParam(strScanData, "16");
            int len = Integer.parseInt(serviceData.substring(4, 6), 16);
            if (len >= 11) {
                this.HardwareModel = serviceData.substring(6, 10).toUpperCase();
                if (!(this.HardwareModel.equals("3901")
                        || this.HardwareModel.equals("3A01")
                        || this.HardwareModel.equals("3C01")
                        || this.HardwareModel.equals("3A04")
                )) {
                    if (!AppBase.IsDebug)
                        return false;
                }
                this.Firmware = serviceData.substring(10, 12);
                this.SN = serviceData.substring(12, 20);
                this.Battery = Integer.parseInt(serviceData.substring(20, 22), 16);
            }

            int n_0 = 22;
            int l_Temperature = Integer.parseInt(serviceData.substring(n_0, n_0 + 2), 16);
            this.Temperature = -1000;
            this.Humidity = -1000;
            if (l_Temperature == 4) {
                String s_Temperature = StringConvertUtil.hexString2binaryString(serviceData.substring(n_0 + 2, n_0 + 4));
                if (s_Temperature.substring(0, 1).equals("0")) {
                    int symbol = 1;
                    if (s_Temperature.substring(1, 2).equals("1")) {
                        symbol = -1;
                    }
                    this.Temperature = Integer.parseInt(StringConvertUtil.binaryString2hexString("00" + s_Temperature.substring(2, 8))
                            + serviceData.substring(n_0 + 4, n_0 + 6), 16) / 100.00;
                    this.Temperature = this.Temperature * symbol;
                }

                if (this.HardwareModel.equals("3901") || this.HardwareModel.equals("3C01")) {
                    String s_Humidity = StringConvertUtil.hexString2binaryString(serviceData.substring(n_0 + 6, n_0 + 8));
                    if (s_Temperature.substring(0, 1).equals("0")) {
                        this.Humidity = Integer.parseInt(StringConvertUtil.binaryString2hexString("00" + s_Humidity.substring(2, 8))
                                + serviceData.substring(n_0 + 8, n_0 + 10), 16) / 100.00;
                    }
                }
            }

            int n_3 = n_0 + 2 + l_Temperature * 2 + 2 * 2;
            this.AlarmType = "00";
            if (serviceData.length() >= 38)
                this.AlarmType = serviceData.substring(n_3, n_3 + 2);

            if (this.Temperature != -1000)
                this.Temperature = Double.parseDouble(StringUtil.ToString(this.Temperature, 1));
            if (this.Humidity != -1000)
                this.Humidity = Double.parseDouble(StringUtil.ToString(this.Humidity, 1));
            return true;
        } catch (Exception ex) {
            // Log.e("DeviceBase", "fromScanData" + StringConvertUtil.bytesToHexString(scanData) + " ex:" + ex.toString());
        }
        return false;
    }

    /**
     * Parsing notification data
     *
     * @param data
     */
    public boolean fromNotificationData(byte[] data) {
        String strLog = StringConvertUtil.bytesToHexString(data);
        try {
            Log.d("fromNotificationData", " data:" + strLog);
            if ((data.length == 6 && !StringConvertUtil.bytesToHexString(data).equals("000000000000"))
                    || (data.length == 7 && !StringConvertUtil.bytesToHexString(data).equals("00000000000000"))) {
                int firmware = 15;
                if (!this.Firmware.equals("")) {
                    firmware = Integer.parseInt(this.Firmware);
                }
                if (this.HardwareModel.equals("3901")) {
                    if (firmware != 15 && firmware < 18) {
                        String d = StringConvertUtil.hexString2binaryString(StringConvertUtil.bytesToHexString(data));
                        int month = Integer.parseInt(d.substring(0, 4), 2);
                        int day = Integer.parseInt("000" + d.substring(4, 9), 2);
                        int hour = Integer.parseInt("000" + d.substring(9, 14), 2);
                        int minute = Integer.parseInt("00" + d.substring(14, 20), 2);
                        int second = Integer.parseInt("00" + d.substring(20, 26), 2);
                        int battery = Integer.parseInt("0" + d.substring(26, 33), 2);
                        int humidity = Integer.parseInt("0" + d.substring(33, 40), 2);
                        int temperature = Integer.parseInt(d.substring(40, 48), 2);
                        Date rtc = new Date(DateUtil.GetUTCTime().getYear(), month - 1, day, hour, minute, second);
                        this.UTCTime = rtc;
                        this.Battery = battery;
                        if (temperature > 128)
                            this.Temperature = temperature - 256;
                        else
                            this.Temperature = temperature;
                        this.Humidity = humidity;
                        return true;
                    } else if (firmware < 20) {
                        String d = StringConvertUtil.hexString2binaryString(StringConvertUtil.bytesToHexString(data));
                        int month = Integer.parseInt(d.substring(0, 4), 2);
                        int day = Integer.parseInt("000" + d.substring(4, 9), 2);
                        int hour = Integer.parseInt("000" + d.substring(9, 14), 2);
                        int minute = Integer.parseInt("00" + d.substring(14, 20), 2);
                        int second = Integer.parseInt("00" + d.substring(20, 26), 2);
                        int humidity = Integer.parseInt("0" + d.substring(26, 33), 2);
                        double temperature = Integer.parseInt(d.substring(33, 44), 2);
                        if (humidity < 0 || humidity > 100)
                            humidity = -1000;
                        if (temperature < 1250)
                            temperature = temperature / 10.0;
                        else
                            temperature = (temperature - 2048) / 10.0;
                        Date rtc = new Date(DateUtil.GetUTCTime().getYear(), month - 1, day, hour, minute, second);
                        this.UTCTime = rtc;
                        if (temperature >= -200 && temperature <= 600)
                            this.Temperature = temperature;
                        else
                            this.Temperature = -1000;
                        this.Humidity = humidity;
                        return true;
                    } else if (firmware < 22) {
                        //20、21 bug
                        String d = StringConvertUtil.hexString2binaryString(StringConvertUtil.bytesToHexString(data));
                        Date rtc = new Date(Long.parseLong(d.substring(0, 32), 2) * 1000 - (DateUtil.GetTimeZone() * 60 * 1000));
                        int humidity = Integer.parseInt("0" + d.substring(32, 39), 2) * 2;
                        double temperature = Integer.parseInt(d.substring(39, 48) + d.substring(54, 56), 2);
                        if (humidity < 0 || humidity > 100)
                            humidity = -1000;
                        if (temperature < 1250)
                            temperature = temperature / 10.0;
                        else
                            temperature = (temperature - 2048) / 10.0;
                        this.UTCTime = rtc;
                        if (temperature >= -200 && temperature <= 600)
                            this.Temperature = temperature;
                        else
                            this.Temperature = -1000;
                        this.Humidity = humidity;
                        return true;
                    } else {
                        String d = StringConvertUtil.hexString2binaryString(StringConvertUtil.bytesToHexString(data));
                        Date rtc = new Date(Long.parseLong(d.substring(0, 32), 2) * 1000 - (DateUtil.GetTimeZone() * 60 * 1000));
                        int humidity = Integer.parseInt("0" + d.substring(32, 39), 2);
                        double temperature = Integer.parseInt(d.substring(39, 50), 2);
                        if (humidity < 0 || humidity > 100)
                            humidity = -1000;
                        if (temperature < 1250)
                            temperature = temperature / 10.0;
                        else
                            temperature = (temperature - 2048) / 10.0;
                        this.UTCTime = rtc;
                        if (temperature >= -200 && temperature <= 600)
                            this.Temperature = temperature;
                        else
                            this.Temperature = -1000;
                        this.Humidity = humidity;
                        return true;
                    }
                } else if (this.HardwareModel.equals("3A01")) {
                    if (firmware < 7) {
                        String d = StringConvertUtil.hexString2binaryString(StringConvertUtil.bytesToHexString(data));
                        int month = Integer.parseInt(d.substring(0, 4), 2);
                        int day = Integer.parseInt("000" + d.substring(4, 9), 2);
                        int hour = Integer.parseInt("000" + d.substring(9, 14), 2);
                        int minute = Integer.parseInt("00" + d.substring(14, 20), 2);
                        int second = Integer.parseInt("00" + d.substring(20, 26), 2);
                        int battery = Integer.parseInt("0" + d.substring(26, 33), 2);
                        int humidity = -1000;
                        double temperature = Integer.parseInt(d.substring(33, 44), 2);
                        if (temperature < 1250)
                            temperature = temperature / 10.0;
                        else
                            temperature = (temperature - 2048) / 10.0;
                        Date rtc = new Date(DateUtil.GetUTCTime().getYear(), month - 1, day, hour, minute, second);
                        this.UTCTime = rtc;
                        this.Battery = battery;
                        if (temperature >= -200 && temperature <= 600)
                            this.Temperature = temperature;
                        else
                            this.Temperature = -1000;
                        this.Humidity = humidity;
                        return true;
                    } else if (firmware < 9) {
                        //7、8 bug
                        String d = StringConvertUtil.hexString2binaryString(StringConvertUtil.bytesToHexString(data));
                        Date rtc = new Date(Long.parseLong(d.substring(0, 32), 2) * 1000 - (DateUtil.GetTimeZone() * 60 * 1000));
                        int battery = Integer.parseInt("0" + d.substring(32, 39), 2);
                        double temperature = Integer.parseInt(d.substring(39, 48) + d.substring(52, 54), 2);
                        if (temperature < 1250)
                            temperature = temperature / 10.0;
                        else
                            temperature = (temperature - 2048) / 10.0;
                        this.UTCTime = rtc;
                        this.Battery = battery;
                        if (temperature >= -200 && temperature <= 600)
                            this.Temperature = temperature;
                        else
                            this.Temperature = -1000;
                        this.Humidity = -1000;
                        return true;
                    } else {
                        String d = StringConvertUtil.hexString2binaryString(StringConvertUtil.bytesToHexString(data));
                        Date rtc = new Date(Long.parseLong(d.substring(0, 32), 2) * 1000 - (DateUtil.GetTimeZone() * 60 * 1000));
                        int battery = Integer.parseInt("0" + d.substring(32, 39), 2);
                        double temperature = Integer.parseInt(d.substring(39, 50), 2);
                        if (temperature < 1250)
                            temperature = temperature / 10.0;
                        else
                            temperature = (temperature - 2048) / 10.0;
                        this.UTCTime = rtc;
                        this.Battery = battery;
                        if (temperature >= -200 && temperature <= 600)
                            this.Temperature = temperature;
                        else
                            this.Temperature = -1000;
                        this.Humidity = -1000;
                        return true;
                    }
                } else if (this.HardwareModel.equals("3C01")) {
                    if (firmware >= 26) {
                        String d = StringConvertUtil.hexString2binaryString(StringConvertUtil.bytesToHexString(data));
                        Date rtc = new Date(Long.parseLong(d.substring(0, 32), 2) * 1000 - (DateUtil.GetTimeZone() * 60 * 1000));
                        int humidity = Integer.parseInt("0" + d.substring(32, 39), 2);
                        double temperature = Integer.parseInt(d.substring(39, 50), 2);
                        if (humidity < 0 || humidity > 100)
                            humidity = -1000;
                        if (temperature < 1250)
                            temperature = temperature / 10.0;
                        else
                            temperature = (temperature - 2048) / 10.0;
                        this.UTCTime = rtc;
                        if (temperature >= -200 && temperature <= 600)
                            this.Temperature = temperature;
                        else
                            this.Temperature = -1000;
                        this.Humidity = humidity;
                        return true;
                    }
                } else if (this.HardwareModel.equals("3A04")) {
                    if (firmware >= 13) {
                        String d = StringConvertUtil.hexString2binaryString(StringConvertUtil.bytesToHexString(data));
                        Date rtc = new Date(Long.parseLong(d.substring(0, 32), 2) * 1000 - (DateUtil.GetTimeZone() * 60 * 1000));
                        int battery = Integer.parseInt("0" + d.substring(32, 39), 2);
                        double temperature = Integer.parseInt(d.substring(39, 50), 2);
                        if (temperature < 1250)
                            temperature = temperature / 10.0;
                        else
                            temperature = (temperature - 2048) / 10.0;
                        this.UTCTime = rtc;
                        this.Battery = battery;
                        if (temperature >= -200 && temperature <= 600)
                            this.Temperature = temperature;
                        else
                            this.Temperature = -1000;
                        this.Humidity = -1000;
                        return true;
                    }
                }
            }
        } catch (Exception ex) {
            Log.e("Temperature", "fromNotificationData" + ex.toString() + " data:" + strLog);
        }
        return false;
    }

    /**
     * Numerical transformation for Rssi
     *
     * @param res
     * @return
     */
    public int ToRssi(int res) {
        if (res > 128) {
            return res - 256;
        }
        return res;
    }

    /**
     * Rssi is converted to a value
     *
     * @param res
     * @return
     */
    public int ConverRssi(int res) {
        if (res >= -128 && res < 0) {
            return res + 256;
        }
        return res;
    }

}
