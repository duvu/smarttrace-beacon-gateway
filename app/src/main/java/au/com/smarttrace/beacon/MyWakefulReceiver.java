package au.com.smarttrace.beacon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

public class MyWakefulReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.d("[>_] Wakeful Receiver");
        Intent service = new Intent(context, MyWakefulIntentService.class);
        startWakefulService(context, service);
    }
}
