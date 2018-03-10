package com.TZONE.Bluetooth.Utils;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * Utility class broadcast packets
 * Created by Forrest on 2015/9/8.
 */
public class BroadcastPacketsUtil {
    /**
     * Analytical scanning to broadcast data
     * @param hexScanData
     * @return
     */
    public static Dictionary<String,String> GetScanDictionary(String hexScanData){
        Dictionary<String,String> dic = new Hashtable<String,String>();
        /*Log.i("BroadcastPacketsUtil", "Start parsing the data......");
        Log.i("BroadcastPacketsUtil","hexScanData:"+hexScanData+"");*/
        while (hexScanData.length()>0){
            //AD structure standard is: 1 byte length + AD type + AD Data
            int len = Integer.parseInt(hexScanData.substring(0,2),16);
            String type = "00";
            String value = "";
            if(len>=1){
                type = hexScanData.substring(2,4);
                value = hexScanData.substring(4,(len+1)*2);
                dic.put(type,value);
                //Log.i("BroadcastPacketsUtil","len:"+len+" type:"+type+" value:"+value+"");
            }
            hexScanData = hexScanData.substring((len+1)*2,hexScanData.length());
        }
        //Log.i("BroadcastPacketsUtil","Resolution is complete !");
        return dic;
    }

    /**
     * Get the broadcast data items
     * @param hexScanData
     * @param hexType ff:ibeacon data 16:Service Data 09:device name 03:eddstone data
     * @return
     */
    public static String GetScanParam(String hexScanData,String hexType){
        try {
            Dictionary<String,String> dic = GetScanDictionary(hexScanData);
            return dic.get(hexType).toString();
        }catch (Exception ex){
            return "";
        }
    }
}
