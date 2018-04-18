package au.com.smarttrace.beacon.net;

import android.text.TextUtils;

import com.google.gson.Gson;

import java.io.IOException;

import au.com.smarttrace.beacon.AppConfig;
import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.SharedPref;
import au.com.smarttrace.beacon.net.model.PairedRequest;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WebService {
    private static final Gson gson = new Gson();
    public static void getLocations(int page, int size, String sortColumn, String sortOrder, String token, Callback callback) {
        if (token == null) {
            token = SharedPref.getToken();
        }

        if (!TextUtils.isEmpty(token)) {
            String urlSb = AppConfig.WEB_SERVICE_URL + "/getLocations/" + token +
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

    public static void getUser(String token, Callback callback) {
        if (token == null) {
            token = SharedPref.getToken();
        }

        String urlSb = AppConfig.WEB_SERVICE_URL + "/getUser/" + token;
        Http.getIntance().get(urlSb, callback);
    }

    public static void getDevice(String imei, String token, Callback callback) {
        String urlSb = AppConfig.WEB_SERVICE_URL + "/getDevice/" + token + "?imei=" + imei;
        Http.getIntance().get(urlSb, callback);
    }

    public static void createNewAutoSthipment(String deviceId, String token, Callback callback) {
        if (token == null) {
            token = SharedPref.getToken();
        }

        if (!TextUtils.isEmpty(deviceId) && !TextUtils.isEmpty(token)) {
            //create shipment
            String urlSb = AppConfig.WEB_SERVICE_URL + "/createNewAutoSthipment/" + token + "?device=" + deviceId;
            Http.getIntance().get(urlSb, callback);
        }
    }

    public static void getPairedBeacons(String imei, String token, Callback callback) {
        if (token == null) {
            token = SharedPref.getToken();
        }

        if (TextUtils.isEmpty(imei) || TextUtils.isEmpty(token)) {
            return;
        }
        String urlSb = AppConfig.WEB_SERVICE_URL + "/getPairedBeacons/" + token + "?phone="+imei;
        Http.getIntance().get(urlSb, callback);
    }

    public static void getPairedPhones(String token, Callback callback) {
        if (token == null) {
            token = SharedPref.getToken();
        }

        String urlSb = AppConfig.WEB_SERVICE_URL + "/getPairedPhones/" + token;
        Http.getIntance().get(urlSb, callback);
    }

    public static void savePairedPhone(String pImei, String bSn, String token, Callback callback) {

        if (token == null) {
            token = SharedPref.getToken();
        }

        if (TextUtils.isEmpty(pImei) || TextUtils.isEmpty(bSn) || TextUtils.isEmpty(token)) {
            return;
        }
        String urlSb = AppConfig.WEB_SERVICE_URL + "/savePairedPhone/" + token;
        PairedRequest request = new PairedRequest();
        request.setPairedPhoneIMEI(pImei);
        request.setPairedBeaconID(bSn);
        request.setCompany(SharedPref.getCompanyId());
        request.setActive(true);
        String postData = gson.toJson(request);
        Logger.d("savedPairedPhone-URL: " + urlSb);
        Logger.d("savedPairedPhone: " + postData);
        Http.getIntance().post(urlSb, postData, callback);

    }

    public static void sendEvent(String data) {
        sendEvent(data, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Logger.i("[Http] failed " + e.getMessage());
                // do nothing
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Logger.i("[Broadcast+] success " + response.toString());
                //do nothing
            }
        });
    }
    public static void sendEvent(String data, Callback callback) {
        if (TextUtils.isEmpty(data)) {
            Logger.d("[-] empty data");
            return;
        }
        String urlSb = AppConfig.BACKEND_URL_BT04_NEW;
        Http.getIntance().post(urlSb, data, callback);
    }
}
