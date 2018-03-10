package com.TZONE.Bluetooth.Utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.Locale;

/**
 * App Util
 * Created by Forrest on 2015/9/29.
 */
public class AppUtil {
    /**
     * GetLanguage
     * @param context
     * @return
     */
    public static String GetLanguage(Context context){
        try{
            Locale locale = context.getResources().getConfiguration().locale;
            String language = locale.getLanguage();
            return language;
        }catch (Exception ex){
            return "";
        }
    }
    /**
     * Is Chinese
     * @return
     */
    public static boolean IsZh(Context context) {
        try{
            String language = GetLanguage(context);
            if (language.endsWith("zh"))
                return true;
            else
                return false;
        }catch (Exception ex){
            return true;
        }
    }

    /**
     * According to the resolution of the mobile phone from dp units become px (pixels)
     * @param context
     * @param dpValue
     * @return
     */
    public static int dip2px(Context context, float dpValue){
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * According to the resolution of the mobile phone from the px unit into dp (pixels)
     * @param context
     * @param pxValue
     * @return
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     * Get the version number
     * @return The version number of the current application
     */
    public static String getVersion(Context context) {
        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            String version = info.versionName;
            return version;
        } catch (Exception e) {
            return "1.0";
        }
    }
}
