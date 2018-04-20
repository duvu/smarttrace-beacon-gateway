package au.com.smarttrace.beacon.net;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import au.com.smarttrace.beacon.GsonUtils;
import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.net.exception.ParsingException;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by beou on 3/21/18.
 */

public class Http {
    private OkHttpClient client = null;
    private static Http http = null;

    public static Http getIntance() {
        if (http == null) {
            http = new Http();
        }
        return http;
    }

    private Http() {
        client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
            .build();
    }

    //-- Sync
    public <T> T get(String url, Class<T> clazz) throws IOException, ParsingException {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        Response response = client.newCall(request).execute();
        ResponseBody body = response.body();
        if (body != null) {
            return GsonUtils.getInstance().fromJson(body.string(), clazz);
        } else {
            throw new ParsingException("Empty body");
        }
    }

    public <T> T post(String url, String data, Class<T> clazz) throws IOException {
        MediaType mediaType = getMediaType(data);
        RequestBody body = RequestBody.create(mediaType, data);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        ResponseBody body1 = response.body();
        if (body1 == null) {
            throw new ParsingException("Empty body");
        } else {
            return GsonUtils.getInstance().fromJson(body1.string(), clazz);
        }
    }

    public void post(String url, String data, Callback callback) {
        MediaType mediaType = getMediaType(data);
        RequestBody body = RequestBody.create(mediaType, data);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void get(String url, Callback callback) {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        client.newCall(request).enqueue(callback);
    }

    private MediaType getMediaType(String data) {
        if (isJson(data)) {
            return MediaType.parse("application/json; charset=utf-8");
        } else {
            return MediaType.parse("text/plain; charset=utf-8");
        }
    }

    private boolean isJson(String json) {
        try {
            GsonUtils.getInstance().fromJson(json, Object.class);
            return true;
        } catch (JsonSyntaxException ex) {
            return false;
        }
    }
}
