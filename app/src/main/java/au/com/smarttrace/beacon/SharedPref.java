package au.com.smarttrace.beacon;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SharedPref {
    private static final String KEY_SMARTTRACE_IO_DATA = "smarttrace_io_data";
    private static final String KEY_SMARRTRACE_IO_USERNAME = "smarttrace_io_user_name";
    private static final String KEY_SMARRTRACE_IO_PASSWORD = "smarttrace_io_password";
    private static final String KEY_SMARRTRACE_IO_TOKEN = "smarttrace_io_token";
    private static final String KEY_SMARRTRACE_IO_TOKEN_EXPIRED = "smarttrace_io_token_expired";
    private static final String KEY_SMARRTRACE_IO_TOKEN_INSTANCE = "smarttrace_io_token_instance";

    private static final String KEY_USER_TIMEZONE       = "user_timezone";

    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;
    public static void init(Context ctx) {
        //ctx.getSharedPreferences(KEY_SMARTTRACE_IO_DATA, Context.MODE_PRIVATE);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        editor = sharedPreferences.edit();
    }

    private static SharedPreferences.Editor getEditor() {
        if (editor == null) {
            editor = sharedPreferences.edit();
        }
        return editor;
    }

    //1. save username
    public static void saveUserName(String userName) {
        getEditor().putString(KEY_SMARRTRACE_IO_USERNAME, userName);
        getEditor().commit();
    }
    //2. get username
    public static String getUserName() {
        return sharedPreferences.getString(KEY_SMARRTRACE_IO_USERNAME, "");
    }

    //3. save password
    public static void savePassword(String password) {
        getEditor().putString(KEY_SMARRTRACE_IO_PASSWORD, password);
        getEditor().commit();
    }
    //4.
    public static String getPassword() {
        return sharedPreferences.getString(KEY_SMARRTRACE_IO_PASSWORD, "");
    }

    //5. save token
    public static void saveToken(String token) {
        getEditor().putString(KEY_SMARRTRACE_IO_TOKEN, token);
        getEditor().commit();
    }
    //6.
    public static String getToken() {
        return sharedPreferences.getString(KEY_SMARRTRACE_IO_TOKEN, "");
    }

    //7. save token
    public static void saveExpiredStr(String exp) {
        getEditor().putString(KEY_SMARRTRACE_IO_TOKEN_EXPIRED, exp);
        getEditor().commit();
    }
    //8.
    public static String getExpiredStr() {
        return sharedPreferences.getString(KEY_SMARRTRACE_IO_TOKEN_EXPIRED, "");
    }

    //9. save token
    public static void saveTokenInstance(String tokenInstance) {
        getEditor().putString(KEY_SMARRTRACE_IO_TOKEN_INSTANCE, tokenInstance);
        getEditor().commit();
    }
    //10.
    public static String getTokenInstance() {
        return sharedPreferences.getString(KEY_SMARRTRACE_IO_TOKEN_INSTANCE, "");
    }

    //11 save user_timezone
    public static void saveUserTimezone(String tz) {
        getEditor().putString(KEY_USER_TIMEZONE, tz);
        getEditor().apply();
    }

    public static String getUserTimezone() {
        return sharedPreferences.getString(KEY_USER_TIMEZONE, "GMT");
    }

    public static void clear() {
        getEditor().clear();
        getEditor().commit();
    }
}
