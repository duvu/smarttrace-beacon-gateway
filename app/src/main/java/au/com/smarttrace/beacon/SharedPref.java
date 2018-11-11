package au.com.smarttrace.beacon;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SharedPref {
    public static final String KEY_SMARTTRACE_IO_DATA           = "smarttrace_io_data";
    public static final String KEY_SMARRTRACE_IO_USERNAME       = "smarttrace_io_user_name";
    public static final String KEY_SMARRTRACE_IO_PASSWORD       = "smarttrace_io_password";
    public static final String KEY_SMARRTRACE_IO_TOKEN          = "smarttrace_io_token";
    public static final String KEY_SMARRTRACE_IO_TOKEN_EXPIRED  = "smarttrace_io_token_expired";
    public static final String KEY_SMARRTRACE_IO_TOKEN_EXPIRED_T  = "smarttrace_io_token_expired_t";
    public static final String KEY_SMARRTRACE_IO_TOKEN_INSTANCE = "smarttrace_io_token_instance";

    public static final String KEY_USER_TIMEZONE                = "user_timezone";
    public static final String KEY_COMPANY_ID                   = "company_id";
    public static final String KEY_ONBOOT                       = "application_on_boot";

    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;
    public static void init(Context ctx) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);

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
        getEditor().apply();
    }
    //2. get username
    public static String getUserName() {
        return sharedPreferences.getString(KEY_SMARRTRACE_IO_USERNAME, "");
    }

    //3. save password
    public static void savePassword(String password) {
        getEditor().putString(KEY_SMARRTRACE_IO_PASSWORD, password);
        getEditor().apply();
    }
    //4.
    public static String getPassword() {
        return sharedPreferences.getString(KEY_SMARRTRACE_IO_PASSWORD, "");
    }

    //5. save token
    public static void saveToken(String token) {
        getEditor().putString(KEY_SMARRTRACE_IO_TOKEN, token);
        getEditor().apply();
    }
    //6.
    public static String getToken() {
        return sharedPreferences.getString(KEY_SMARRTRACE_IO_TOKEN, "");
    }

    //7. save token
    public static void saveExpiredStr(String exp) {
        getEditor().putString(KEY_SMARRTRACE_IO_TOKEN_EXPIRED, exp);
        getEditor().apply();
    }

    public static void saveExpiredTimestamp(Long timestamp) {
        getEditor().putLong(KEY_SMARRTRACE_IO_TOKEN_EXPIRED_T, timestamp).apply();
    }

    public static Long getExpiredTimestamp() {
        return sharedPreferences.getLong(KEY_SMARRTRACE_IO_TOKEN_EXPIRED_T, 0);
    }

    //8.
    public static String getExpiredStr() {
        return sharedPreferences.getString(KEY_SMARRTRACE_IO_TOKEN_EXPIRED, "");
    }

    //9. save token
    public static void saveTokenInstance(String tokenInstance) {
        getEditor().putString(KEY_SMARRTRACE_IO_TOKEN_INSTANCE, tokenInstance);
        getEditor().apply();
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

    //13. save companyId
    public static void saveCompanyId(long id) {
        getEditor().putLong(KEY_COMPANY_ID, id);
        getEditor().apply();
    }

    public static long getCompanyId() {
        return sharedPreferences.getLong(KEY_COMPANY_ID, 0);
    }

    public static void saveOnBoot(boolean onBoot) {
        getEditor().putBoolean(KEY_ONBOOT, onBoot);
        getEditor().apply();
    }

    public static boolean isOnBoot() {
        return sharedPreferences.getBoolean(KEY_ONBOOT, false);
    }

    public static void clear() {
        getEditor().clear();
        getEditor().apply();
    }
}
