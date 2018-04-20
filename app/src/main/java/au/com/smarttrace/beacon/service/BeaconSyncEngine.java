package au.com.smarttrace.beacon.service;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.os.NetworkOnMainThreadException;
import android.os.SystemClock;
import android.support.annotation.WorkerThread;

public class BeaconSyncEngine {
    private Context mContext;
    public BeaconSyncEngine(Context context) {
        mContext = context;
    }

    @WorkerThread
    public boolean sync() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new NetworkOnMainThreadException();
        }

        SystemClock.sleep(1_000);
        boolean success = Math.random() > 0.1; // successful 90% of the time
        return  success;
    }

    @WorkerThread
    public boolean startService() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new NetworkOnMainThreadException();
        }
        Intent iService = new Intent(mContext, BeaconService.class);
        mContext.startService(iService);
        return true;
    }
}
