package au.com.smarttrace.beacon;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class GsonUtils {
    private static Gson gson = null;
    public static Gson getInstance() {
        if (gson == null) {
            gson = new Gson();
        }
        return gson;
    }

    public <T> T fromJson(String json, Class<T> classOfT) {
        try {
            return gson.fromJson(json, classOfT);
        } catch (JsonSyntaxException jse) {
            return null;
        }
    }
}
