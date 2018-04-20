package com.TZONE.Bluetooth.Utils;

/**
 * String instruments
 * Created by Forrest on 2015/6/26.
 */
public class StringUtil {
    /**
     * Zero padding is less than the size the front
     * @param res
     * @param size
     * @return
     */
    public static String PadLeft(String res, int size){
        String temp = res;
        int len = res.length();
        if(len<size){
            for (int i = 0; i < size - len; i++) {
                temp = "0"+temp;
            }
        }
        return temp;
    }

    /**
     * Verify for hexadecimal string
     * @param res
     * @return
     */
    public static boolean IsHexString(String res){
        try{
            for(int i=0;i<res.length();i++){
                char c=res.charAt(i);
                if(((c>='a')&&(c<='f'))||((c>='A')&&(c<='F'))||((c>='0')&&(c<='9')))
                    continue;
                else
                    return false;
            }
            return true;
        }catch (Exception ex){
            return false;
        }
    }

    /**
     * object to string
     * @param res
     * @return
     */
    public static String ToString(Object res){
        try {
            return res.toString();
        }catch (Exception ex){
            return "";
        }
    }

    public static String ToString(double res, int number){
        try {
            String temp = String.valueOf(res);
            String[] arr = temp.split("\\.");
            if(arr.length > 1){
                arr[1] = StringUtil.TrimEnd(arr[1],'0');
                if(StringUtil.IsNullOrEmpty(arr[1]))
                    return arr[0];
                if(arr[1].length() > number)
                    return arr[0] + "." + arr[1].substring(0, number);
                else
                    return arr[0] + "." + arr[1];
            }
            /*
            if(temp.indexOf(".") > 0){
                temp = temp.replaceAll("0+?$", "");
                temp = temp.replaceAll("[.]$", "");
            }
            */
            return temp;
        }catch (Exception ex){
            return "0";
        }
    }

    public static int GetInt(String res){
        try {
            return Integer.parseInt(res);
        }catch (Exception ex){
            return -1;
        }
    }

    /**
     * @param res
     * @return
     */
    public static boolean IsNullOrEmpty(String res){
        if(res == null)
            return true;
        res = res.trim();
        if(res.isEmpty())
            return true;
        return false;
    }

    /**
     * @param res
     * @param character
     * @return
     */
    public static String TrimEnd(String res, char character)
    {
        try {
            int resLen = res.length();
            while (resLen > 0){
                char temp = res.charAt(resLen - 1);
                if(temp != character)
                    return res.substring(0,resLen);
                resLen --;
            }
            return "";
        }catch (Exception ex){
            return  res;
        }
    }

}
