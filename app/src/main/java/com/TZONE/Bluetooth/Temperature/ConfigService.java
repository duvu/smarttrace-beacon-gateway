package com.TZONE.Bluetooth.Temperature;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.util.Log;

import com.TZONE.Bluetooth.AppBase;
import com.TZONE.Bluetooth.ConfigServiceBase;
import com.TZONE.Bluetooth.IConfigCallBack;
import com.TZONE.Bluetooth.Temperature.Model.CharacteristicHandle;
import com.TZONE.Bluetooth.Temperature.Model.CharacteristicType;
import com.TZONE.Bluetooth.Temperature.Model.Device;
import com.TZONE.Bluetooth.Utils.BinaryUtil;
import com.TZONE.Bluetooth.Utils.StringConvertUtil;
import com.TZONE.Bluetooth.Utils.StringUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Config Service
 * Created by Forrest on 2016/6/1.
 */
public class ConfigService extends ConfigServiceBase{
    private CharacteristicHandle _CharacteristicHandle = null;
    private String _Token = "000000";
    /**
     *
     * @param bluetoothAdapter
     * @param context
     * @param macAddress
     * @param configCallBack
     * @throws Exception
     */
    public ConfigService(BluetoothAdapter bluetoothAdapter, Context context, String macAddress,long timeout,IConfigCallBack configCallBack) throws Exception {
        super(bluetoothAdapter, context, macAddress,timeout,configCallBack);
        _CharacteristicHandle = new CharacteristicHandle();
    }

    /**
     * Check Token
     * @param token
     */
    public void CheckToken(String token) {
        _Token = token;
        LinkedHashMap<String,byte[]> uuids = new LinkedHashMap<String,byte[]>();
        Device device = new Device();
        device.Token = token;
        uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Token).toString(), _CharacteristicHandle.GetItemValue(device, CharacteristicType.Token));
        super.Write(uuids);
    }

    /**
     * Read BT04 configuration
     *
     * @param version
     */
    public void ReadConfig_BT04(String version){
        int v = Integer.parseInt(version);
        List<String> uuid = new ArrayList<String>();
        if(v <= 5){
            uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.SN).toString());
            uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.TransmitPower).toString());
            uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Interval).toString());
            uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.SamplingInterval).toString());
        }else {
            uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.SN).toString());
            uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.TransmitPower).toString());
            uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Interval).toString());
            uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.SamplingInterval).toString());
            // New version of BT04 added
            uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.SaveInterval).toString());
            uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.IsSaveOverwrite).toString());
            uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.SavaCount).toString());
            uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Alarm).toString());
            uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Trip).toString());
            uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.ModelVersion).toString());
            //2016-09-28
            uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.LDOVoltage).toString());
            uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.LDOTemp).toString());
            //2016-11-15
            uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.OtherBytes1).toString());
            uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.OtherBytes2).toString());
            //uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.OtherBytes3).toString());
            //uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.OtherBytes4).toString());
            //uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.OtherBytes5).toString());
            uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Name).toString());
            //2017-03-06
            uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.UTC).toString());
        }
        super.Read(uuid);
    }

    /**
     * Read BT04B configuration
     * @param version
     */
    public void ReadConfig_BT04B(String version){
        //int v = Integer.parseInt(version);
        List<String> uuid = new ArrayList<String>();
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.SN).toString());
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.TransmitPower).toString());
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Interval).toString());
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.SamplingInterval).toString());
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.SaveInterval).toString());
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.IsSaveOverwrite).toString());
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.SavaCount).toString());
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Alarm).toString());
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Trip).toString());
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.ModelVersion).toString());
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.LDOVoltage).toString());
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.LDOTemp).toString());
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.OtherBytes1).toString());
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.OtherBytes2).toString());
        //uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.OtherBytes3).toString());
        //uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.OtherBytes4).toString());
        //uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.OtherBytes5).toString());
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Name).toString());
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.UTC).toString());
        super.Read(uuid);
    }

    /**
     * Read BT05 Config
     */
    public void ReadConfig_BT05(String version) {
        List<String> uuid = new ArrayList<String>();
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.SN).toString());
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.TransmitPower).toString());
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Interval).toString());
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.SamplingInterval).toString());
        //New version of BT04 added
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.SaveInterval).toString());
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.IsSaveOverwrite).toString());
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.SavaCount).toString());
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Alarm).toString());
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Trip).toString());
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.ModelVersion).toString());
        //2016-09-28
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.LDOVoltage).toString());
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.LDOTemp).toString());
        //2016-11-15
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.OtherBytes1).toString());
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.OtherBytes2).toString());
        //uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.OtherBytes3).toString());
        //uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.OtherBytes4).toString());
        //uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.OtherBytes5).toString());
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Name).toString());
        //2017-03-06
        uuid.add(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.UTC).toString());
        super.Read(uuid);
    }

    /**
     * Write BT05 Config
     * @param newConfig
     */
    public void WriteConfig_BT05(Device newConfig) {
        Device device = newConfig;

        /**************** 2016-11-15 *******************/
        device.OtherBytes.set(0,new byte[20]);
        device.OtherBytes.set(1,new byte[20]);
        device.OtherBytes.set(2,new byte[20]);
        device.OtherBytes.set(3,new byte[20]);
        device.OtherBytes.set(4,new byte[16]);
        byte[] notes = new byte[40];
        try {
            notes = device.Notes.getBytes("UTF-8");//new byte[]{ 0x01,0x02};
        }catch (Exception ex){}
        if(notes.length > 0)
            device.OtherBytes.set(0,BinaryUtil.PadRight(BinaryUtil.CloneRange(notes,0,20),20));
        /*if(notes.length > 20)
            device.OtherBytes.set(1,BinaryUtil.PadRight(BinaryUtil.CloneRange(notes, 20, 20), 20));*/

        byte[] description = new byte[56]; //new byte[]{ 0x01,0x02};
        try {
            description = device.Description.getBytes("UTF-8");
        } catch (Exception e){}
       /* if(description.length > 0)
            device.OtherBytes.set(2,BinaryUtil.PadRight(BinaryUtil.CloneRange(description, 0, 20), 20));
        if(description.length > 20)
            device.OtherBytes.set(3,BinaryUtil.PadRight(BinaryUtil.CloneRange(description, 20, 20), 20));
        if(description.length > 40)
            device.OtherBytes.set(4,BinaryUtil.PadRight(BinaryUtil.CloneRange(description, 40, 16), 16));*/
        if(description.length > 0)
            device.OtherBytes.set(1,BinaryUtil.PadRight(BinaryUtil.CloneRange(description, 0, 20), 20));
        /**************** 2016-11-15 *******************/


        LinkedHashMap<String,byte[]> uuids = new LinkedHashMap<String,byte[]>();
        uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.UTC).toString(),_CharacteristicHandle.GetItemValue(device,CharacteristicType.UTC));//先写时间防止延时
        if(!AppBase.IsSDK)
            uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.SN).toString(),_CharacteristicHandle.GetItemValue(device,CharacteristicType.SN));
        if(device.TransmitPower != -1000)
            uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.TransmitPower).toString(),_CharacteristicHandle.GetItemValue(device,CharacteristicType.TransmitPower));
        if(device.Interval > -1)
            uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Interval).toString(),_CharacteristicHandle.GetItemValue(device,CharacteristicType.Interval));
        // The acquisition interval is a minimum of 5
        if(device.SamplingInterval >= 5)
            uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.SamplingInterval).toString(),_CharacteristicHandle.GetItemValue(device,CharacteristicType.SamplingInterval));
        // New version of BT05 new characteristic added
        uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.SaveInterval).toString(),_CharacteristicHandle.GetItemValue(device,CharacteristicType.SaveInterval));
        //uuids.put(characteristicHandle.GetCharacteristicUUID(CharacteristicType.IsSaveOverwrite).toString(),characteristicHandle.GetItemValue(device,CharacteristicType.IsSaveOverwrite));
        uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Alarm).toString(),_CharacteristicHandle.GetItemValue(device,CharacteristicType.Alarm));
        if (device.TripStatus > 0) {
            uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Trip).toString(), _CharacteristicHandle.GetItemValue(device, CharacteristicType.Trip));
        }
        //2016-09-28
        if(device.LDOPower == 1){
            uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.LDOVoltage).toString(), _CharacteristicHandle.GetItemValue(device, CharacteristicType.LDOVoltage));
            uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.LDOPower).toString(), _CharacteristicHandle.GetItemValue(device, CharacteristicType.LDOPower));
        }
        uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.OtherBytes1).toString(), _CharacteristicHandle.GetItemValue(device, CharacteristicType.OtherBytes1));
        uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.OtherBytes2).toString(), _CharacteristicHandle.GetItemValue(device, CharacteristicType.OtherBytes2));
        //uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.OtherBytes3).toString(), _CharacteristicHandle.GetItemValue(device, CharacteristicType.OtherBytes3));
        //uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.OtherBytes4).toString(), _CharacteristicHandle.GetItemValue(device, CharacteristicType.OtherBytes4));
        //uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.OtherBytes5).toString(), _CharacteristicHandle.GetItemValue(device, CharacteristicType.OtherBytes5));
        if(device.Name != null && device.Name.length() > 0)
            uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Name).toString(), _CharacteristicHandle.GetItemValue(device, CharacteristicType.Name));

        if(!device.Token.equals(_Token))
            uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Token).toString(),_CharacteristicHandle.GetItemValue(device,CharacteristicType.Token));
        super.Write(uuids);
    }

    /**
     * Write BT04 Config
     * @param newConfig
     */
    public void WriteConfig_BT04(Device newConfig) {
        Device device = newConfig;
        int v = Integer.parseInt(device.Firmware);
        LinkedHashMap<String,byte[]> uuids = new LinkedHashMap<String,byte[]>();
        if(v <= 5){
            if(!AppBase.IsSDK)
                uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.SN).toString(),_CharacteristicHandle.GetItemValue(device,CharacteristicType.SN));
            if(device.TransmitPower != -1000)
                uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.TransmitPower).toString(),_CharacteristicHandle.GetItemValue(device,CharacteristicType.TransmitPower));
            if(device.Interval > -1)
                uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Interval).toString(),_CharacteristicHandle.GetItemValue(device,CharacteristicType.Interval));
            if(device.SamplingInterval > -1)
                uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.SamplingInterval).toString(),_CharacteristicHandle.GetItemValue(device,CharacteristicType.SamplingInterval));
            if(!device.Token.equals(_Token))
                uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Token).toString(),_CharacteristicHandle.GetItemValue(device,CharacteristicType.Token));
        }else {
            /**************** 2016-11-15 *******************/
            device.OtherBytes.set(0,new byte[20]);
            device.OtherBytes.set(1,new byte[20]);
            device.OtherBytes.set(2,new byte[20]);
            device.OtherBytes.set(3,new byte[20]);
            device.OtherBytes.set(4,new byte[16]);
            byte[] notes = new byte[40];
            try {
                notes = device.Notes.getBytes("UTF-8");//new byte[]{ 0x01,0x02};
            }catch (Exception ex){}
            if(notes.length > 0)
                device.OtherBytes.set(0,BinaryUtil.PadRight(BinaryUtil.CloneRange(notes,0,20),20));
            /*if(notes.length > 20)
            device.OtherBytes.set(1,BinaryUtil.PadRight(BinaryUtil.CloneRange(notes, 20, 20), 20));*/

            byte[] description = new byte[56]; //new byte[]{ 0x01,0x02};
            try {
                description = device.Description.getBytes("UTF-8");
            } catch (Exception e){}
           /* if(description.length > 0)
                device.OtherBytes.set(2,BinaryUtil.PadRight(BinaryUtil.CloneRange(description, 0, 20), 20));
            if(description.length > 20)
                device.OtherBytes.set(3,BinaryUtil.PadRight(BinaryUtil.CloneRange(description, 20, 20), 20));
            if(description.length > 40)
                device.OtherBytes.set(4,BinaryUtil.PadRight(BinaryUtil.CloneRange(description, 40, 16), 16));*/
            if(description.length > 0)
                device.OtherBytes.set(1,BinaryUtil.PadRight(BinaryUtil.CloneRange(description, 0, 20), 20));
            /**************** 2016-11-15 *******************/
            uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.UTC).toString(),_CharacteristicHandle.GetItemValue(device,CharacteristicType.UTC));//先写时间防止延时
            if(!AppBase.IsSDK)
                uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.SN).toString(),_CharacteristicHandle.GetItemValue(device,CharacteristicType.SN));
            if(device.TransmitPower != -1000)
                uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.TransmitPower).toString(),_CharacteristicHandle.GetItemValue(device,CharacteristicType.TransmitPower));
            if(device.Interval > -1)
                uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Interval).toString(),_CharacteristicHandle.GetItemValue(device,CharacteristicType.Interval));
            // The acquisition interval is a minimum of 5
            if(device.SamplingInterval >= 5)
                uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.SamplingInterval).toString(),_CharacteristicHandle.GetItemValue(device,CharacteristicType.SamplingInterval));
            // New version of BT04 added features
            uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.SaveInterval).toString(),_CharacteristicHandle.GetItemValue(device,CharacteristicType.SaveInterval));
            //uuids.put(characteristicHandle.GetCharacteristicUUID(CharacteristicType.IsSaveOverwrite).toString(),characteristicHandle.GetItemValue(device,CharacteristicType.IsSaveOverwrite));
            uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Alarm).toString(),_CharacteristicHandle.GetItemValue(device,CharacteristicType.Alarm));
            uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Trip).toString(), _CharacteristicHandle.GetItemValue(device, CharacteristicType.Trip));
            //2016-09-28
            if(device.LDOPower == 1){
                uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.LDOVoltage).toString(), _CharacteristicHandle.GetItemValue(device, CharacteristicType.LDOVoltage));
                uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.LDOPower).toString(), _CharacteristicHandle.GetItemValue(device, CharacteristicType.LDOPower));
            }
            uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.OtherBytes1).toString(), _CharacteristicHandle.GetItemValue(device, CharacteristicType.OtherBytes1));
            uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.OtherBytes2).toString(), _CharacteristicHandle.GetItemValue(device, CharacteristicType.OtherBytes2));
            //uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.OtherBytes3).toString(), _CharacteristicHandle.GetItemValue(device, CharacteristicType.OtherBytes3));
            //uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.OtherBytes4).toString(), _CharacteristicHandle.GetItemValue(device, CharacteristicType.OtherBytes4));
            //uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.OtherBytes5).toString(), _CharacteristicHandle.GetItemValue(device, CharacteristicType.OtherBytes5));
            if(device.Name != null && device.Name.length() > 0)
                uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Name).toString(), _CharacteristicHandle.GetItemValue(device, CharacteristicType.Name));

            if(!device.Token.equals(_Token))
                uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Token).toString(),_CharacteristicHandle.GetItemValue(device,CharacteristicType.Token));
        }
        super.Write(uuids);
    }

    /**
     * Write BT04 Config
     * @param newConfig
     */
    public void WriteConfig_BT04B(Device newConfig){
        Device device = newConfig;
        int v = Integer.parseInt(device.Firmware);
        LinkedHashMap<String,byte[]> uuids = new LinkedHashMap<String,byte[]>();
        device.OtherBytes.set(0,new byte[20]);
        device.OtherBytes.set(1,new byte[20]);
        device.OtherBytes.set(2,new byte[20]);
        device.OtherBytes.set(3,new byte[20]);
        device.OtherBytes.set(4,new byte[16]);
        byte[] notes = new byte[40];
        try {
            notes = device.Notes.getBytes("UTF-8");//new byte[]{ 0x01,0x02};
        }catch (Exception ex){}
        if(notes.length > 0)
            device.OtherBytes.set(0,BinaryUtil.PadRight(BinaryUtil.CloneRange(notes,0,20),20));
            /*if(notes.length > 20)
            device.OtherBytes.set(1,BinaryUtil.PadRight(BinaryUtil.CloneRange(notes, 20, 20), 20));*/

        byte[] description = new byte[56]; //new byte[]{ 0x01,0x02};
        try {
            description = device.Description.getBytes("UTF-8");
        } catch (Exception e){}
           /* if(description.length > 0)
                device.OtherBytes.set(2,BinaryUtil.PadRight(BinaryUtil.CloneRange(description, 0, 20), 20));
            if(description.length > 20)
                device.OtherBytes.set(3,BinaryUtil.PadRight(BinaryUtil.CloneRange(description, 20, 20), 20));
            if(description.length > 40)
                device.OtherBytes.set(4,BinaryUtil.PadRight(BinaryUtil.CloneRange(description, 40, 16), 16));*/
        if(description.length > 0)
            device.OtherBytes.set(1,BinaryUtil.PadRight(BinaryUtil.CloneRange(description, 0, 20), 20));
        /**************** 2016-11-15 *******************/
        uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.UTC).toString(),_CharacteristicHandle.GetItemValue(device,CharacteristicType.UTC));//先写时间防止延时
        if(!AppBase.IsSDK)
            uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.SN).toString(),_CharacteristicHandle.GetItemValue(device,CharacteristicType.SN));
        if(device.TransmitPower != -1000)
            uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.TransmitPower).toString(),_CharacteristicHandle.GetItemValue(device,CharacteristicType.TransmitPower));
        if(device.Interval > -1)
            uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Interval).toString(),_CharacteristicHandle.GetItemValue(device,CharacteristicType.Interval));
        // The acquisition interval is a minimum of 5
        if(device.SamplingInterval >= 5)
            uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.SamplingInterval).toString(),_CharacteristicHandle.GetItemValue(device,CharacteristicType.SamplingInterval));
        // New version of BT04 added features
        uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.SaveInterval).toString(),_CharacteristicHandle.GetItemValue(device,CharacteristicType.SaveInterval));
        // uuids.put(characteristicHandle.GetCharacteristicUUID(CharacteristicType.IsSaveOverwrite).toString(),characteristicHandle.GetItemValue(device,CharacteristicType.IsSaveOverwrite));
        uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Alarm).toString(),_CharacteristicHandle.GetItemValue(device,CharacteristicType.Alarm));
        uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Trip).toString(), _CharacteristicHandle.GetItemValue(device, CharacteristicType.Trip));
        // 2016-09-28
        if(device.LDOPower == 1){
            uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.LDOVoltage).toString(), _CharacteristicHandle.GetItemValue(device, CharacteristicType.LDOVoltage));
            uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.LDOPower).toString(), _CharacteristicHandle.GetItemValue(device, CharacteristicType.LDOPower));
        }
        uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.OtherBytes1).toString(), _CharacteristicHandle.GetItemValue(device, CharacteristicType.OtherBytes1));
        uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.OtherBytes2).toString(), _CharacteristicHandle.GetItemValue(device, CharacteristicType.OtherBytes2));
        //uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.OtherBytes3).toString(), _CharacteristicHandle.GetItemValue(device, CharacteristicType.OtherBytes3));
        //uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.OtherBytes4).toString(), _CharacteristicHandle.GetItemValue(device, CharacteristicType.OtherBytes4));
        //uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.OtherBytes5).toString(), _CharacteristicHandle.GetItemValue(device, CharacteristicType.OtherBytes5));
        if(device.Name != null && device.Name.length() > 0)
            uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Name).toString(), _CharacteristicHandle.GetItemValue(device, CharacteristicType.Name));

        if(!device.Token.equals(_Token))
            uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Token).toString(),_CharacteristicHandle.GetItemValue(device,CharacteristicType.Token));
        super.Write(uuids);
    }

    /**
     * Read Config
     */
    public void ReadConfig_BT05B(String version) {
        ReadConfig_BT05(version);
    }

    /**
     * Write BT05B Config
     * @param newConfig
     */
    public void WriteConfig_BT05B(Device newConfig) {
        WriteConfig_BT05(newConfig);
    }

    /**
     * Set the device time
     */
    public void SetDateTime(){
        LinkedHashMap<String,byte[]> uuids = new LinkedHashMap<String,byte[]>();
        Device device = new Device();
        uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.UTC).toString(),_CharacteristicHandle.GetItemValue(device,CharacteristicType.UTC));
        super.Write(uuids);
    }

    /**
     * Set the sync mode and time
     * @param mode 0 = Slow ; 1 = Fast
     * @param datetimeType 0 = All ; 1 = 1Day ; 2 = 3Day ; 3 = 7Day ; 4 = 30Day
     */
    public void SetSyncDateTimeMode(int mode,int datetimeType){
        LinkedHashMap<String,byte[]> uuids = new LinkedHashMap<String,byte[]>();
        byte[] bytes = new byte[9];
        bytes[0] = 0x00;
        bytes[1] = 0x00;
        bytes[2] = 0x00;
        bytes[3] = 0x00;
        long timestamp = 0;
        if(datetimeType == 1)
            timestamp =  (new Date().getTime() / 1000) - (24 * 60 * 60);
        else if(datetimeType == 2)
            timestamp =  (new Date().getTime() / 1000) - (3 * 24 * 60 * 60);
        else if(datetimeType == 3)
            timestamp =  (new Date().getTime() / 1000) - (7 * 24 * 60 * 60);
        else if(datetimeType == 4) {
            timestamp = (new Date().getTime() / 1000) - (30 * 24 * 60 * 60);
        }
        if(timestamp > 0){
            byte[] temp = StringConvertUtil.hexStringToBytes(StringUtil.PadLeft(Long.toHexString(timestamp),8));
            bytes[0] = temp[0];
            bytes[1] = temp[1];
            bytes[2] = temp[2];
            bytes[3] = temp[3];
        }
        bytes[4] = 0x00;
        bytes[5] = 0x00;
        bytes[6] = 0x00;
        bytes[7] = 0x00;
        bytes[8] = (byte)(mode == 1 ? 0x01:0x00);
        String logMsg = "SetSyncDateTimeMode:" + StringConvertUtil.bytesToHexString(bytes);
        Log.i("ConfigService",logMsg);
        uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.SyncMode).toString(),bytes);
        super.Write(uuids);
    }

    /**
     * Set the sync mode and time
     * @param mode
     * @param beginTime
     * @param endTime
     */
    public void SetSyncDateTime(int mode,Date beginTime,Date endTime){
        LinkedHashMap<String,byte[]> uuids = new LinkedHashMap<String,byte[]>();
        byte[] bytes = new byte[9];
        bytes[0] = 0x00;
        bytes[1] = 0x00;
        bytes[2] = 0x00;
        bytes[3] = 0x00;
        long timestamp = beginTime.getTime() / 1000;
        if(timestamp > 0){
            byte[] temp = StringConvertUtil.hexStringToBytes(StringUtil.PadLeft(Long.toHexString(timestamp),8));
            bytes[0] = temp[0];
            bytes[1] = temp[1];
            bytes[2] = temp[2];
            bytes[3] = temp[3];
        }
        bytes[4] = 0x00;
        bytes[5] = 0x00;
        bytes[6] = 0x00;
        bytes[7] = 0x00;
        timestamp = endTime.getTime() / 1000;
        if(timestamp > 0){
            byte[] temp = StringConvertUtil.hexStringToBytes(StringUtil.PadLeft(Long.toHexString(timestamp),8));
            bytes[4] = temp[0];
            bytes[5] = temp[1];
            bytes[6] = temp[2];
            bytes[7] = temp[3];
        }
        bytes[8] = (byte)(mode == 1 ? 0x01:0x00);
        String logMsg = "SetSyncDateTime:" + StringConvertUtil.bytesToHexString(bytes);
        Log.i("ConfigService",logMsg);
        uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.SyncMode).toString(),bytes);
        super.Write(uuids);
    }

    /**
     * Sync
     * @param enable
     */
    public void Sync(boolean enable){
        super.EnableNotification(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Sysn).toString(), enable);
    }

    /**
     * Turn on the lights
     */
    public void OpenLight(){
        LinkedHashMap<String,byte[]> uuids = new LinkedHashMap<String,byte[]>();
        Device device = new Device();
        uuids.put(_CharacteristicHandle.GetCharacteristicUUID(CharacteristicType.Light).toString(),_CharacteristicHandle.GetItemValue(device,CharacteristicType.Light));
        super.Write(uuids);
    }

    /**
     * Dispose
     */
    public void Dispose() {
        super.Dispose();
    }

    /**
     * Get reading configuration
     * @param uuids
     * @return
     */
    public Device GetCofing(final HashMap<String, byte[]> uuids){
        try {
            Device device = _CharacteristicHandle.Set(uuids);

            //byte[] notes = BinaryUtil.Merge(device.OtherBytes.get(0),device.OtherBytes.get(1));
            byte[] notes = BinaryUtil.TrimEnd(device.OtherBytes.get(0));
            if(notes != null)
                device.Notes = new String (notes,"UTF-8").trim();//StringConvertUtil.bytesToHexString(location); //location.toString();

            //byte[] description = BinaryUtil.MultipleMerge(device.OtherBytes.get(2),device.OtherBytes.get(3),device.OtherBytes.get(4));
            byte[] description =  BinaryUtil.TrimEnd(device.OtherBytes.get(1));
            if(description != null)
                device.Description = new String (description,"UTF-8").trim();//StringConvertUtil.bytesToHexString(description);

            return device;
        }catch (Exception ex){
            Log.e("ConfigService",ex.toString());
            return  null;
        }
    }

    /**
     * Get notification data
     * @param data
     * @param hardwareModel
     * @param firmware
     * @param syncMode 0 = Slow 1 = Fast
     * @return
     */
    public List<byte[]> GetDataBytes(byte[] data,String hardwareModel,String firmware,int syncMode){
        try {
            List<byte[]> ls = new ArrayList<>();
            if(syncMode == 1){
                int dataType = Integer.parseInt(StringUtil.PadLeft(Integer.toBinaryString(data[0]), 8).substring(0, 3),2);
                // Start the package, end the package
                if(dataType == 2 || dataType == 3)
                    return null;
                if(dataType == 1){
                    if(data.length >= 10 + 3){
                        ls.add(BinaryUtil.CloneRange(data, 10, 3));
                    }
                    if(data.length >= 10 + 3 * 2){
                        ls.add(BinaryUtil.CloneRange(data, 10 + 3, 3));
                    }
                    if(data.length >= 10 + 3 * 3){
                        ls.add(BinaryUtil.CloneRange(data, 10 + 3 * 2, 3));
                    }
                }else {
                    if(data.length >= 2 + 3){
                        ls.add(BinaryUtil.CloneRange(data, 2, 3));
                    }
                    if(data.length >= 2 + 3 * 2){
                        ls.add(BinaryUtil.CloneRange(data, 2 + 3, 3));
                    }
                    if(data.length >= 2 + 3 * 3){
                        ls.add(BinaryUtil.CloneRange(data, 2 + 3 * 2, 3));
                    }
                    if(data.length >= 2 + 3 * 4){
                        ls.add(BinaryUtil.CloneRange(data, 2 + 3 * 3, 3));
                    }
                    if(data.length >= 2 + 3 * 5){
                        ls.add(BinaryUtil.CloneRange(data, 2 + 3 * 4, 3));
                    }
                    if(data.length >= 2 + 3 * 6){
                        ls.add(BinaryUtil.CloneRange(data, 2 + 3 * 5, 3));
                    }
                }
            } else {
                if((hardwareModel.equals("3A01") && Integer.parseInt(firmware) >= 12)
                        || (hardwareModel.equals("3901") && Integer.parseInt(firmware) >= 25)
                        || (hardwareModel.equals("3C01") && Integer.parseInt(firmware) >= 26)
                        || (hardwareModel.equals("3A04") && Integer.parseInt(firmware) >= 13)){
                    // Start the package, end the package
                    if(data.length == 4 && (data[0] == 0x2A || data[0] == 0x24) && data[3] == 0x23)
                        return null;
                }
                String crc = StringConvertUtil.bytesToHexString(BinaryUtil.CloneRange(data, data.length - 1, 1));
                String checkcrc = CRC(BinaryUtil.CloneRange(data, 0, data.length - 1));
                if (!checkcrc.equals(crc))
                    return null;

                int len = 6;
                if((hardwareModel.equals("3901") && Integer.parseInt(firmware) < 20)
                        ||(hardwareModel.equals("3A01") && Integer.parseInt(firmware) < 7))
                    len = 6;
                else
                    len = 7;
                if(data.length >= len + 3) {
                    ls.add(BinaryUtil.CloneRange(data, 0, len));
                }
                if(data.length >= len * 2 + 3) {
                    ls.add(BinaryUtil.CloneRange(data, len, len));
                }
            }
            return ls;
        }catch (Exception ex){
            Log.e("GetData",ex.toString());
            return  null;
        }
    }

    /**
     * Accumulated checksum
     * @param data
     * @return
     */
    public String CRC(byte[] data)
    {
        int num = 0;
        for (int i = 0; i < data.length; i++)
            num = (num + data[i]) % 0xffff;
        String hex = Integer.toHexString(num);
        if(hex.length()%2 > 0)
            hex = "0"+hex;
        String crc = hex.substring(hex.length()- 2);
        return  crc;
    }
}
