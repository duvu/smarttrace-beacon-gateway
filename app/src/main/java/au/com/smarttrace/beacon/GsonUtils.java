package au.com.smarttrace.beacon;

import com.google.gson.Gson;

public class GsonUtils {
    private static Gson gson = null;
    public static Gson getInstance() {
        if (gson == null) {
            gson = new Gson();
        }
        return gson;
    }
}
