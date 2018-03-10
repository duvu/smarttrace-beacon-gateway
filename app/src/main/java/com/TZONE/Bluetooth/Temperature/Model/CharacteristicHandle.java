package com.TZONE.Bluetooth.Temperature.Model;

import android.util.Log;

import com.TZONE.Bluetooth.Utils.BinaryUtil;
import com.TZONE.Bluetooth.Utils.DateUtil;
import com.TZONE.Bluetooth.Utils.StringConvertUtil;
import com.TZONE.Bluetooth.Utils.StringUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Configuration
 * Created by Forrest on 2015/9/28.
 */
public class CharacteristicHandle {
    public List<Characteristic> CharacteristicList;

    public CharacteristicHandle(){
        if (this.CharacteristicList == null){
            this.CharacteristicList = new ArrayList<Characteristic>();
            this.CharacteristicList.add(new Characteristic("27763B11-999C-4D6A-9FC4-C7272BE10900", CharacteristicType.SN));
            this.CharacteristicList.add(new Characteristic("27763B12-999C-4D6A-9FC4-C7272BE10900", CharacteristicType.Interval));
            this.CharacteristicList.add(new Characteristic("27763B14-999C-4D6A-9FC4-C7272BE10900", CharacteristicType.TransmitPower));
            this.CharacteristicList.add(new Characteristic("27763B13-999C-4D6A-9FC4-C7272BE10900", CharacteristicType.Token));
            this.CharacteristicList.add(new Characteristic("27763B15-999C-4D6A-9FC4-C7272BE10900", CharacteristicType.SamplingInterval));
            // Add with storage
            this.CharacteristicList.add(new Characteristic("27763B16-999C-4D6A-9FC4-C7272BE10900", CharacteristicType.SaveInterval));
            this.CharacteristicList.add(new Characteristic("27763B17-999C-4D6A-9FC4-C7272BE10900", CharacteristicType.IsSaveOverwrite));
            this.CharacteristicList.add(new Characteristic("27763B18-999C-4D6A-9FC4-C7272BE10900", CharacteristicType.SavaCount));
            this.CharacteristicList.add(new Characteristic("27763B19-999C-4D6A-9FC4-C7272BE10900", CharacteristicType.Alarm));
            this.CharacteristicList.add(new Characteristic("27763B20-999C-4D6A-9FC4-C7272BE10900", CharacteristicType.UTC));
            this.CharacteristicList.add(new Characteristic("27763B21-999C-4D6A-9FC4-C7272BE10900", CharacteristicType.Sysn));
            this.CharacteristicList.add(new Characteristic("27763B22-999C-4D6A-9FC4-C7272BE10900", CharacteristicType.Trip));
            this.CharacteristicList.add(new Characteristic("27763B23-999C-4D6A-9FC4-C7272BE10900", CharacteristicType.ModelVersion));
            // 2016-09-28
            this.CharacteristicList.add(new Characteristic("27763B24-999C-4D6A-9FC4-C7272BE10900", CharacteristicType.LDOVoltage));
            this.CharacteristicList.add(new Characteristic("27763B25-999C-4D6A-9FC4-C7272BE10900", CharacteristicType.LDOTemp));
            this.CharacteristicList.add(new Characteristic("27763B26-999C-4D6A-9FC4-C7272BE10900", CharacteristicType.LDOPower));
            // 2016-11-15
            this.CharacteristicList.add(new Characteristic("27763B27-999C-4D6A-9FC4-C7272BE10900", CharacteristicType.OtherBytes1));
            this.CharacteristicList.add(new Characteristic("27763B28-999C-4D6A-9FC4-C7272BE10900", CharacteristicType.OtherBytes2));
            this.CharacteristicList.add(new Characteristic("27763B29-999C-4D6A-9FC4-C7272BE10900", CharacteristicType.OtherBytes3));
            this.CharacteristicList.add(new Characteristic("27763B2A-999C-4D6A-9FC4-C7272BE10900", CharacteristicType.OtherBytes4));
            this.CharacteristicList.add(new Characteristic("27763B2B-999C-4D6A-9FC4-C7272BE10900", CharacteristicType.OtherBytes5));
            this.CharacteristicList.add(new Characteristic("27763B40-999C-4D6A-9FC4-C7272BE10900", CharacteristicType.Name));
            //2017-05-05
            this.CharacteristicList.add(new Characteristic("27763B31-999C-4D6A-9FC4-C7272BE10900", CharacteristicType.SyncMode));
            //2017-08-21
            this.CharacteristicList.add(new Characteristic("27763B41-999C-4D6A-9FC4-C7272BE10900", CharacteristicType.Light));
        }
    }

    /**
     * UUID is obtained according to the feature
     * @param characteristicType
     * @return
     */
    public UUID GetCharacteristicUUID(CharacteristicType characteristicType){
        for (Characteristic item:CharacteristicList){
            if(characteristicType.equals(item.Type))
                return item.UUID;
        }
        return null;
    }

    /**
     * According to UUID get feature type
     * @param uuid
     * @return
     */
    public CharacteristicType GetCharacteristicType(UUID uuid){
        for (Characteristic item:CharacteristicList){
            if(item.UUID.toString().equals(uuid.toString()))
                return item.Type;
        }
        return CharacteristicType.Unknown;
    }

    /**
     * Get the corresponding value
     * @param bt
     * @param type
     * @return byte[]
     */
    public byte[] GetItemValue(Device bt,CharacteristicType type){
        byte[] bytes = null;
        try {
            if (type.equals(CharacteristicType.SN)) {
                Log.i("GetItemValue", "SN:" + bt.SN);
                bytes = StringConvertUtil.hexStringToBytes(bt.SN);
            }else if (type.equals(CharacteristicType.Token) && bt.Token != null) {
                Log.i("GetItemValue","Token:"+bt.Token);
                String strToken = StringUtil.PadLeft(bt.Token, 6);
                if (strToken.length() == 6){
                    // New version of the firmware, 6-bit data password
                    byte[] b0 = StringConvertUtil.uint8ToByte(Integer.parseInt(strToken.substring(0,1)));
                    byte[] b1 = StringConvertUtil.uint8ToByte(Integer.parseInt(strToken.substring(1,2)));
                    byte[] b2 = StringConvertUtil.uint8ToByte(Integer.parseInt(strToken.substring(2,3)));
                    byte[] b3 = StringConvertUtil.uint8ToByte(Integer.parseInt(strToken.substring(3,4)));
                    byte[] b4 = StringConvertUtil.uint8ToByte(Integer.parseInt(strToken.substring(4,5)));
                    byte[] b5 = StringConvertUtil.uint8ToByte(Integer.parseInt(strToken.substring(5,6)));
                    bytes = StringConvertUtil.byteMergerMultiple(b0,b1,b2,b3,b4,b5);
                }
            }else if (type.equals(CharacteristicType.Interval)&& bt.Interval != -1) {
                Log.i("GetItemValue","Interval:"+bt.Interval);
                bytes = StringConvertUtil.uint16ToByte(bt.Interval);// StringConvertUtil.hexStringToBytes(StringConvertUtil.PadLeft(Integer.toHexString(bt.Interval),4));
            }else if (type.equals(CharacteristicType.TransmitPower)&& bt.TransmitPower != -1) {
                Log.i("GetItemValue","TransmitPower:"+bt.TransmitPower);
                bytes = StringConvertUtil.hexStringToBytes(StringUtil.PadLeft(GetTransmitPowerIndex(bt.TransmitPower) + "", 2));
            }else if (type.equals(CharacteristicType.SamplingInterval)&& bt.SamplingInterval != -1) {
                Log.i("GetItemValue","SamplingInterval:"+bt.SamplingInterval);
                bytes = StringConvertUtil.hexStringToBytes(StringConvertUtil.LittleEndian(StringUtil.PadLeft(Integer.toHexString(bt.SamplingInterval),8)));
            }else if (type.equals(CharacteristicType.SaveInterval)&& (bt.SaveInterval != -1 || bt.SaveInterval2 != -1)){
                byte[] b0 = StringConvertUtil.hexStringToBytes(StringConvertUtil.LittleEndian(StringUtil.PadLeft(Integer.toHexString(bt.SaveInterval),4)));
                byte[] b1 = StringConvertUtil.hexStringToBytes(StringConvertUtil.LittleEndian(StringUtil.PadLeft(Integer.toHexString(bt.SaveInterval2),4)));
                bytes = StringConvertUtil.byteMergerMultiple(b0,b1);
                Log.i("GetItemValue","SaveInterval:" + StringConvertUtil.bytesToHexString(bytes));
            }else if (type.equals(CharacteristicType.IsSaveOverwrite)){
                Log.i("GetItemValue","IsSaveOverwrite:"+bt.IsSaveOverwrite);
                if(bt.IsSaveOverwrite)
                    bytes = StringConvertUtil.uint8ToByte(0);
                else
                    bytes = StringConvertUtil.uint8ToByte(1);
            }else if (type.equals(CharacteristicType.SavaCount)){
                Log.i("GetItemValue","SavaCount:"+bt.SavaCount);
                bytes = StringConvertUtil.uint16ToByte(bt.SavaCount);
            }else if (type.equals(CharacteristicType.Alarm)){
                Log.i("GetItemValue","LT:"+bt.LT+" "+bt.HT);
                byte b0 = (byte)new Double(bt.LT).intValue(); //StringConvertUtil.uint8ToByte(Integer.parseInt(String.valueOf(bt.LT)));
                byte b1 = (byte)new Double(bt.HT).intValue(); //StringConvertUtil.uint8ToByte(Integer.parseInt(String.valueOf(bt.HT)));
                bytes = new byte[]{b0,b1};
            }else if (type.equals(CharacteristicType.UTC)){
                //Log.i("GetItemValue","UTC:" + DateUtil.ToString(DateUtil.GetUTCTime(), "yyMMddHHmmdd"));
                Date utc = DateUtil.GetUTCTime(); //bt.UTCTime;
                Calendar cal = Calendar.getInstance();
                cal.setTime(utc);
                byte[] b0 = StringConvertUtil.uint8ToByte(Integer.parseInt(String.valueOf(cal.get(Calendar.YEAR)).substring(1)));
                byte[] b1 = StringConvertUtil.uint8ToByte(cal.get(Calendar.MONTH)+1);
                byte[] b2 = StringConvertUtil.uint8ToByte(cal.get(Calendar.DAY_OF_MONTH));
                byte[] b3 = StringConvertUtil.uint8ToByte(cal.get(Calendar.HOUR_OF_DAY));
                byte[] b4 = StringConvertUtil.uint8ToByte(cal.get(Calendar.MINUTE));
                byte[] b5 = StringConvertUtil.uint8ToByte(cal.get(Calendar.SECOND));
                bytes = StringConvertUtil.byteMergerMultiple(b0,b1,b2,b3,b4,b5);
                Log.i("GetItemValue","UTC:" + StringConvertUtil.bytesToHexString(bytes));
            }else if (type.equals(CharacteristicType.Sysn)){
                //bytes = StringConvertUtil.uint16ToByte(1);
            }else if(type.equals(CharacteristicType.Trip)){
                bytes = StringConvertUtil.uint8ToByte(bt.TripStatus);
                Log.i("GetItemValue","TripStatus:" + StringConvertUtil.bytesToHexString(bytes));
            }else if(type.equals(CharacteristicType.LDOVoltage)){
                bytes = StringConvertUtil.uint16ToByte(new Double(bt.LDOVoltage).intValue());
                Log.i("GetItemValue","LDOVoltage:" + StringConvertUtil.bytesToHexString(bytes));
            }else if(type.equals(CharacteristicType.LDOTemp)){
                // Read only
            }else if(type.equals(CharacteristicType.LDOPower)){
                bytes = StringConvertUtil.uint8ToByte(bt.LDOPower);
                Log.i("GetItemValue","LDOPower:" + StringConvertUtil.bytesToHexString(bytes));
            }else if(type.equals(CharacteristicType.OtherBytes1)){
                bytes = bt.OtherBytes.get(0);
            }else if(type.equals(CharacteristicType.OtherBytes2)){
                bytes = bt.OtherBytes.get(1);
            }else if(type.equals(CharacteristicType.OtherBytes3)){
                bytes = bt.OtherBytes.get(2);
            }else if(type.equals(CharacteristicType.OtherBytes4)){
                bytes = bt.OtherBytes.get(3);
            }else if(type.equals(CharacteristicType.OtherBytes5)){
                bytes = bt.OtherBytes.get(4);
            }else if(type.equals(CharacteristicType.Name)){
                bytes = BinaryUtil.PadRight(BinaryUtil.MultipleMerge(new byte[]{ (byte)bt.Name.getBytes("UTF-8").length },bt.Name.getBytes("UTF-8")),8);
                Log.i("GetItemValue","Name:" + StringConvertUtil.bytesToHexString(bytes));
            }else if(type.equals(CharacteristicType.Light)){
                bytes = new byte[]{ 0x01 };
                Log.i("GetItemValue","Light:" + StringConvertUtil.bytesToHexString(bytes));
            }
        }catch (Exception ex){
            Log.e("GetItemValue","Exception："+ex.toString());
        }
        return bytes;
    }

    /**
     * Set the corresponding value
     * @param bt
     * @param type
     * @param res
     * @return
     */
    public Device SetItemValue(Device bt,CharacteristicType type,byte[] res){
        try {
            String strlog = StringConvertUtil.bytesToHexString(res);
            Log.i("SetItemValue",strlog);
            if (type.equals(CharacteristicType.SN)) {
                bt.SN = StringConvertUtil.bytesToHexString(res);
            }else if (type.equals(CharacteristicType.Interval)){
                bt.Interval =  StringConvertUtil.byteToUint16(res);  // Integer.parseInt(StringConvertUtil.bytesToHexString(res),16);
            }else if (type.equals(CharacteristicType.TransmitPower)){
                bt.TransmitPower = GetTransmitPower(Integer.parseInt(StringConvertUtil.bytesToHexString(res), 16));
            }else if (type.equals(CharacteristicType.SamplingInterval)){
                bt.SamplingInterval = Integer.parseInt(StringConvertUtil.LittleEndian(StringConvertUtil.bytesToHexString(res)), 16);
            }else if (type.equals(CharacteristicType.SaveInterval)){
                bt.SaveInterval = Integer.parseInt(StringConvertUtil.bytesToHexString(new byte[]{res[0],res[1]}),16);
                bt.SaveInterval2 = Integer.parseInt(StringConvertUtil.bytesToHexString(new byte[]{res[2],res[3]}),16);
            }else if (type.equals(CharacteristicType.IsSaveOverwrite)){
                if(res[0] == (byte)0x01)
                    bt.IsSaveOverwrite = false;
                else
                    bt.IsSaveOverwrite = true;
            }else if (type.equals(CharacteristicType.SavaCount)){
                bt.SavaCount =  StringConvertUtil.byteToUint16(res);
            }else if (type.equals(CharacteristicType.Alarm)){
                bt.LT =  (int)res[0]; //StringConvertUtil.byteToInt(new byte[]{res[0]});
                bt.HT =  (int)res[1]; //StringConvertUtil.byteToInt(new byte[]{res[1]});
            }else if (type.equals(CharacteristicType.UTC)){
                bt.UTCTime = DateUtil.ToData("20" + (int)res[0]+"-"+(int)res[1]+"-"+(int)res[2]+" "+(int)res[3]+":"+(int)res[4]+":"+(int)res[5] ,"yyyy-MM-dd HH:mm:ss");
            }else if (type.equals(CharacteristicType.Sysn)){

            }else if (type.equals(CharacteristicType.Trip)){
                bt.TripStatus = StringConvertUtil.byteToInt(new byte[]{res[0]});
            }else if (type.equals(CharacteristicType.ModelVersion)){
                bt.HardwareModel = StringConvertUtil.bytesToHexString(new byte[]{res[0],res[1]});
                bt.Firmware = StringConvertUtil.bytesToHexString(new byte[]{res[2]});
            }else if(type.equals(CharacteristicType.LDOVoltage)){
                bt.LDOVoltage =  StringConvertUtil.byteToUint16(res);
            }else if(type.equals(CharacteristicType.LDOTemp)){
                bt.LDOTemp =  StringConvertUtil.byteToUint16(res);
            }else if(type.equals(CharacteristicType.LDOPower)){
                bt.LDOPower = StringConvertUtil.byteToInt(new byte[]{res[0]});
            }else if(type.equals(CharacteristicType.OtherBytes1)){
                bt.OtherBytes.set(0, BinaryUtil.CloneRange(res, 0, 20));
            }else if(type.equals(CharacteristicType.OtherBytes2)){
                bt.OtherBytes.set(1,BinaryUtil.CloneRange(res,0,20));
            }else if(type.equals(CharacteristicType.OtherBytes3)){
                bt.OtherBytes.set(2,BinaryUtil.CloneRange(res,0,20));
            }else if(type.equals(CharacteristicType.OtherBytes4)){
                bt.OtherBytes.set(3,BinaryUtil.CloneRange(res,0,20));
            }else if(type.equals(CharacteristicType.OtherBytes5)){
                bt.OtherBytes.set(4,BinaryUtil.CloneRange(res,0,16));
            }else if(type.equals(CharacteristicType.Name)){
                bt.Name = new String (BinaryUtil.CloneRange(res,1,res[0]),"UTF-8").trim();
            }
        }catch (Exception ex){
            Log.e("SetItemValue","Exception："+ex.toString());
        }
        return bt;
    }

    /**
     *
     * @param uuids
     * @return
     */
    public Device Set(HashMap<String, byte[]> uuids){
        Device device = new Device();
        try {
            Iterator iter = uuids.entrySet().iterator();
            while (iter.hasNext()){
                java.util.Map.Entry entry =  (java.util.Map.Entry)iter.next();
                String uuid = (String)entry.getKey();
                byte[] bytes = (byte[])entry.getValue();
                CharacteristicType type = GetCharacteristicType(UUID.fromString(uuid));
                if(type!=CharacteristicType.Unknown){
                    SetItemValue(device,type,bytes);
                }
            }
        }catch (Exception ex){
            Log.e("SetItemValue", "Exception：" + ex.toString());
        }
        return device;
    }

    public static int GetTransmitPower(int index){
        try {
            String[] strings = new String[]{"4","0","-4","-8","-12","-16","-20","-30"};
            return Integer.parseInt(strings[index]);
        }catch (Exception ex){
            Log.e("GetTransmitPower",ex.toString());
            return -20;
        }
    }
    public static int GetTransmitPowerIndex(int value){
        try {
            String[] strings = new String[]{"4","0","-4","-8","-12","-16","-20","-30"};
            for (int i = 0; i < strings.length; i++) {
                if(value >= Integer.parseInt(strings[i])){
                    return i;
                }
            }
        }catch (Exception ex){
            Log.e("GetTransmitPowerIndex", ex.toString());
        }
        return 0;
    }

    /**
     * Characteristic
     */
    public class Characteristic{
        public UUID UUID;
        public CharacteristicType Type;

        public Characteristic(String uuid,CharacteristicType type){
            this.UUID = UUID.fromString(uuid);
            this.Type = type;
        }
    }
}
