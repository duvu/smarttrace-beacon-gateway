package au.com.smarttrace.beacon.net;

import android.support.annotation.NonNull;

import java.io.IOException;

import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.model.BroadcastEvent;
import au.com.smarttrace.beacon.net.model.CommonResponse;
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

    public static void uploadData(BroadcastEvent broadcastEvent) throws IOException {
        String data  = DataUtil.formatData(broadcastEvent);
        Logger.d("[Net] - data: " + data);
        postAync(_URL, data);
    }
    //--Async
    public static void postAync(String url, String data) throws IOException {
        callPost(url, data, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Logger.d("Failed " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Logger.d("Success " + response.toString());
            }
        });
    }

    public static void getAsync(String url) throws IOException {
        callGetAsync(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Logger.d("Failed " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Logger.d("Success " + response.toString());
            }
        });
    }

    public static void getAsync(String url, Callback callback) throws IOException {
        callGetAsync(url, callback);
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

    private static void callGetAsync(String url, Callback callback) {
        Logger.d("[Net] - getAsync: " + url);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        Call call = client.newCall(request);
        call.enqueue(callback);
    }
}
