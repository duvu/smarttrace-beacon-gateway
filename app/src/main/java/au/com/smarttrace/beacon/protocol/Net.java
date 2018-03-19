package au.com.smarttrace.beacon.protocol;

import android.support.annotation.NonNull;

import java.io.IOException;

import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.model.AdvancedDevice;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by beou on 3/9/18.
 */

public class Net {
    private static final String _URL = "https://smarttrace.com.au/bt04";
    private static final MediaType TEXT = MediaType.parse("text/plain; charset=utf-8");

    private static OkHttpClient client = new OkHttpClient();

    public static void postBundle(AdvancedDevice advancedDevice) throws IOException {
        String data  = DataUtil.formatData(advancedDevice);
        Logger.d("[Net] - data: " + data);
        post(_URL, data);
    }

    private static void post(String url, String data) throws IOException {
        callPost(url, data, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Logger.d("Failed ");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Logger.d("Success " + response.toString());
            }
        });
    }

    private static void callPost(String url, String data, Callback callback) {
        RequestBody body = RequestBody.create(TEXT, data);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        Call call = client.newCall(request);
        call.enqueue(callback);
    }
}
