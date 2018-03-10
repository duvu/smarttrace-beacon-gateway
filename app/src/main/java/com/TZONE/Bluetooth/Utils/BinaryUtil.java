package com.TZONE.Bluetooth.Utils;

/**
 * Created by Forrest on 2016/5/17.
 */
public class BinaryUtil {
    /**
     * Take a byte [] the designated area
     * @param start
     * @param len
     * @return
     */
    public static byte[] CloneRange(byte[] bytes, int start,int len){
        if(bytes.length > start) {
            if(bytes.length - start < len)
                len = bytes.length - start;
            byte[] target = new byte[len];
            for (int i = 0; i < len; i++) {
                target[i] = bytes[start + i];
            }
            return target;
        }
        return null;
    }

    /**
     * Merge two bytes []
     * @param bytes1
     * @param bytes2
     * @return
     */
    public static byte[] Merge(byte[] bytes1,byte[] bytes2){
        byte[] bytes = new byte[bytes1.length + bytes2.length];
        System.arraycopy(bytes1, 0, bytes, 0, bytes1.length);
        System.arraycopy(bytes2, 0, bytes, bytes1.length, bytes2.length);
        return bytes;
    }

    /**
     *  Merge multiple bytes []
     * @param params
     * @return
     */
    public static byte[] MultipleMerge(byte[]... params){
        byte[] targets = null;
        for (byte[] item:params){
            if(targets == null)
                targets = item;
            else{
                targets = Merge(targets,item);
            }
        }
        return targets;
    }

    /**
     * The left is not enough
     * @param bytes
     * @param len
     * @return
     */
    public static byte[] PadRight(byte[] bytes,int len){
        byte[] target = new byte[len];
        int l = bytes.length;
        if(len < bytes.length)
            l = len;
        for (int i = 0; i < l; i++) {
            target[i] = bytes[i];
        }
        return target;
    }

    /**
     * Remove the right side of the empty byte
     * @param bytes
     * @return
     */
    public static byte[] TrimEnd(byte[] bytes)
    {
        int targetLen = bytes.length;
        while (targetLen > 0){
            if(bytes[targetLen - 1] != (byte)0)
                return BinaryUtil.CloneRange(bytes,0,targetLen);
            targetLen --;
        }
        return null;
    }
}
