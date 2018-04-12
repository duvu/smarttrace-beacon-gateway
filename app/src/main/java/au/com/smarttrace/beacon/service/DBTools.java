package au.com.smarttrace.beacon.service;

import com.orhanobut.hawk.Hawk;

import java.io.IOException;

import au.com.smarttrace.beacon.AppConfig;
import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.net.Http;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class DBTools {
    static long count = 0;
    public static synchronized void push(String strData) {
        Hawk.put(getKey(), strData);
        count++;
    }

    public static synchronized String pop() {
        String key = getKey();
        String ret = Hawk.get(key);
        Hawk.delete(key);
        count--;
        return ret;
    }

    private static synchronized String getKey() {
        return "_"+((count <= 0) ? Hawk.count() : count);
    }

    public static final long count() {
        return Hawk.count();
    }

    public static void uploadOldData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    String strdata = pop();
                    if (strdata != null) {
                        Http.getIntance().post(AppConfig.BACKEND_URL_BT04_NEW, strdata, new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                //
                                Logger.i("[-] Oldata upload failure");
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                Logger.i("[-] Oldata upload OKAY");
                            }
                        });
                    } else {
                        break;
                    }
                }
            }
        }).start();
    }
}
