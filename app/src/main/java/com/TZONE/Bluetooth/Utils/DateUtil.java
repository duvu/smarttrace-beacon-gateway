package com.TZONE.Bluetooth.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Date Util
 * Created by Forrest on 2016/5/16.
 */
public class DateUtil {
    /**
     * Get the UTC time
     * @return
     */
    public static Date GetUTCTime(){
        Date now = new Date();
        int min = GetTimeZone();
        Date utc = new Date(now.getTime() - min * 60 * 1000);
        return utc;
    }

    /**
     * Add hours
     * @param date
     * @param hour
     */
    public static Date DateAddHours(Date date, double hour){
        return new Date(date.getTime() + (int)(hour * 60 * 60 * 1000));
    }

    /**
     * Convert to a string display
     * Convert to string
     * @return
     */
    public static String ToString(Date date, String format){
        try {
            return (new SimpleDateFormat(format)).format(date);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Turn to time
     * @param strData
     * @param format
     * @return
     */
    public static Date ToData(String strData, String format){
        try {
            return (new SimpleDateFormat(format)).parse(strData);
        }catch (Exception ex){
            return null;
        }
    }

    /**
     * Calculating Time
     * @param data1
     * @param date2
     * @return
     */
    public static String TimeSpanString(Date data1, Date date2){
        try {
            int s = (int) (Math.abs(data1.getTime() - date2.getTime()) / 1000);
            int day = s / (24 * 60 * 60);
            int hour = (s - (day * 24 * 60 * 60)) / (60 * 60);
            int min = (s - (day * 24 * 60 * 60) - (hour * 60 * 60)) / 60;
            int se = s - (day * 24 * 60 * 60) - (hour * 60 * 60) - (min * 60);
            if(day > 0)
                return  day + ":" +  StringUtil.PadLeft(hour+"",2) + ":" + StringUtil.PadLeft(min+"",2) + ":" + StringUtil.PadLeft(se+"",2);
            else {
                return  StringUtil.PadLeft(hour+"",2) + ":" + StringUtil.PadLeft(min+"",2) + ":" + StringUtil.PadLeft(se+"",2);
            }
        }catch (Exception ex){
            return "";
        }
    }

    /**
     * Get the time zone
     * @return
     */
    public static int GetTimeZone(){
        try {
            //return (TimeZone.getDefault().getRawOffset() + TimeZone.getDefault().getDSTSavings()) / 60 / 1000;
            return (TimeZone.getDefault().getRawOffset()) / 60 / 1000;
        }catch (Exception ex){
            return 0;
        }
    }

}
