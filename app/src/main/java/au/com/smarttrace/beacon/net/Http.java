package au.com.smarttrace.beacon.net;

import java.io.IOException;

import au.com.smarttrace.beacon.Logger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
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

    public Http() {
        client = new OkHttpClient();
    }

    //-- Sync
    public <T> T get(String url, Class<T> clazz) throws IOException {
        Logger.d("[Net] - get-sync: " + url);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        Response response = client.newCall(request).execute();
        ResponseBody body = response.body();
        if (body != null) {
            Logger.d("Data#### " + body.toString());
            return MyGson.fromJson(body.string(), clazz);
        } else {
            throw new IOException("Empty body");
        }
    }
}
