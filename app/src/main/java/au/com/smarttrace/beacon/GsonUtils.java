package au.com.smarttrace.beacon;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class GsonUtils {
    private static GsonUtils instance = null;
    private Gson gson = null;
    public static GsonUtils getInstance() {
        if (instance == null) {
            instance = new GsonUtils();
        }
        return instance;
    }

    public GsonUtils() {
        gson = new Gson();
    }

    public <T> T fromJson(String json, Class<T> classOfT) {
        try {
            return gson.fromJson(json, classOfT);
        } catch (JsonSyntaxException jse) {
            return null;
        }
    }
}
