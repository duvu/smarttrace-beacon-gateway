package au.com.smarttrace.beacon.net;

import com.google.gson.Gson;

/**
 * Created by beou on 3/21/18.
 */

public class MyGson {
    private static Gson gson = null;
    public static Gson gson() {
        if (gson == null) {
            gson = new Gson();
        }
        return gson;
    }
    public static <T> T fromJson(String json, Class<T> clazz) {
        return gson().fromJson(json, clazz);
    }
}
