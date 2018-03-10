package com.TZONE.Bluetooth.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * String conversion tool
 * Created by Forrest on 2015/4/13.
 */
public class StringConvertUtil {

    /**
     * byte to Ascii string
     * @param bytes
     * @return
     */
    public static String byteToAsciiString(byte[] bytes) {
        String result = "";
        char temp;

        int length = bytes.length;
        for (int i = 0; i < length; i++) {
            temp = (char) bytes[i];
            result += temp;
        }
        return result;
    }
    /**
     * Bytes to hex string
     * @param bytes
     * @return
     */
    public static String bytesToHexString(byte[] bytes){
        StringBuilder stringBuilder = new StringBuilder("");
        if (bytes == null || bytes.length <= 0) {
            return null;
        }
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    public static byte charToByte(char c) {
       return (byte) "0123456789ABCDEF".indexOf(c);
    }

    /**
     * Hex string to bytes
     * @param hexString
     * @return
     */
    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));

        }
        return d;
    }

    /**
     * int to byte[]
     * @param res
     * @return
     */
    public static byte[] intToByte(int res) {
        byte[] targets = new byte[4];
        for (int i = 0; i < targets.length; i++) {
            targets[i] = (byte) (res >> 8 * (3 - i) & 0xFF);
        }
        /*targets[0] = (byte) (res & 0xff);
        targets[1] = (byte) ((res >> 8) & 0xff);
        targets[2] = (byte) ((res >> 16) & 0xff);
        targets[3] = (byte) (res >>> 24);*/
        return targets;
    }

    /**
     * byte[] to int
     * @param res
     * @return
     */
    public static int byteToInt(byte[] res) {
        // A byte data left shift 24 into 0x ?? 000000, then the right shift 8 into 0x00 ?? 0000
        int targets = 0;
        for (int i = 0; i < res.length; i++) {
            targets += (res[i] & 0xFF) << (8 * (3 - i));
        }
        return targets;

    }


    /**
     * Merging two byte array
     * @param byte_1
     * @param byte_2
     * @return
     */
    public static byte[] byteMerger(byte[] byte_1, byte[] byte_2){
        byte[] byte_3 = new byte[byte_1.length+byte_2.length];
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
        return byte_3;
    }

    /**
     * Merge multiple byte array
     * @param params
     * @return
     */
    public static byte[] byteMergerMultiple(byte[]... params){
        byte[] targets = null;
        for (byte[] item:params){
            if(targets == null)
                targets = item;
            else{
                targets = byteMerger(targets,item);
            }
        }
        return targets;
    }

    /**
     * uint16 to byte[]
     * @param res
     * @return
     */
    public static byte[] uint16ToByte(int res){
        try {
            byte[] temp = hexStringToBytes(StringUtil.PadLeft(Integer.toHexString(res), 4));
            byte[] targets = new byte[2];
            targets[0] = temp[1];
            targets[1] = temp[0];
            return targets;
        }catch (Exception ex){
            return null;
        }
    }

    /**
     * uint8 to byte[]
     * @param res
     * @return
     */
    public static byte[] uint8ToByte(int res){
        try {
            byte[] targets = hexStringToBytes(StringUtil.PadLeft(Integer.toHexString(res), 2));
            return targets;
        }catch (Exception ex){
            return null;
        }

    }

    /**
     * byte[] to uint16
     * @param res
     * @return
     */
    public static int byteToUint16(byte[] res){
        try {
            byte[] targets = new byte[2];
            targets[0] = res[1];
            targets[1] = res[0];
            return Integer.parseInt(bytesToHexString(targets),16);
        }catch (Exception ex){
            return 0;
        }

    }


    /**
     * Turn hexadecimal form a binary form
     * @param bString
     * @return
     */
    public static String binaryString2hexString(String bString)
    {
        if (bString == null || bString.equals("") || bString.length() % 8 != 0)
            return null;
        StringBuffer tmp = new StringBuffer();
        int iTmp = 0;
        for (int i = 0; i < bString.length(); i += 4)
        {
            iTmp = 0;
            for (int j = 0; j < 4; j++)
            {
                iTmp += Integer.parseInt(bString.substring(i + j, i + j + 1)) << (4 - j - 1);
            }
            tmp.append(Integer.toHexString(iTmp));
        }
        return tmp.toString();
    }

    /**
     * Hexadecimal form binary form
     * @param hexString
     * @return
     */
    public static String hexString2binaryString(String hexString)
    {
        if (hexString == null || hexString.length() % 2 != 0)
            return null;
        String bString = "", tmp;
        for (int i = 0; i < hexString.length(); i++)
        {
            tmp = "0000"
                    + Integer.toBinaryString(Integer.parseInt(hexString
                    .substring(i, i + 1), 16));
            bString += tmp.substring(tmp.length() - 4);
        }
        return bString;
    }

    /**
     * The low byte in the front
     * @param hexString
     * @return
     */
    public static String LittleEndian(String hexString){
        try {
            if (hexString == null || hexString.length() % 2 != 0)
                return null;
            String output = "";
            for (int i = 0; i < hexString.length()/2; i++){
                output = hexString.substring(2*i,2*(i+1)) + output;
            }
            return output;
        }catch (Exception ex){
            return null;
        }
    }
}
