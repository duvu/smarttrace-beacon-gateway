package au.com.smarttrace.beacon.net;

import android.text.TextUtils;

import java.io.IOException;

import au.com.smarttrace.beacon.AppConfig;
import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.SharedPref;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WebService {
    public static void getLocations(int page, int size, String sortColumn, String sortOrder, Callback callback) {
        if (!TextUtils.isEmpty(SharedPref.getToken())) {
            String urlSb = AppConfig.WEB_SERVICE_URL + "/getLocations/" + SharedPref.getToken() +
                    "?pageIndex=" + page +
                    "&pageSize=" + size +
                    "&sc=" + (TextUtils.isEmpty(sortColumn) ? "locationName" : sortColumn) +
                    "&so=" + (TextUtils.isEmpty(sortOrder) ? "asc" : sortOrder);
            Http.getIntance().get(urlSb, callback);
        }
    }

    public static void login(String userName, String password, Callback callback) {
        if (!TextUtils.isEmpty(userName) && !TextUtils.isEmpty(password)) {
            String urlSb = AppConfig.WEB_SERVICE_URL + "/login?email=" + userName + "&password="+password;
            Http.getIntance().get(urlSb, callback);
        }
    }

    public static void getUser(Callback callback) {
        String urlSb = AppConfig.WEB_SERVICE_URL + "/getUser/" + SharedPref.getToken();
        Http.getIntance().get(urlSb, callback);
    }

    public static void getDevice(String imei, Callback callback) {
        String urlSb = AppConfig.WEB_SERVICE_URL + "/getDevice/" + SharedPref.getToken() +
                "?imei=" + imei;

        Http.getIntance().get(urlSb, callback);
    }

    public static void createNewAutoSthipment(String deviceId, Callback callback) {
        if (!TextUtils.isEmpty(deviceId) && !TextUtils.isEmpty(SharedPref.getToken())) {
            //create shipment
            String urlSb = AppConfig.WEB_SERVICE_URL + "/createNewAutoSthipment/" + SharedPref.getToken() +
                    "?device=" + deviceId;
            Http.getIntance().get(urlSb, callback);
        }
    }
}
