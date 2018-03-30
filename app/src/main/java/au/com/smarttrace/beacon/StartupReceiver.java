package au.com.smarttrace.beacon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import au.com.smarttrace.beacon.services.BeaconService;

public class StartupReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.i("[StartupReceiver] starting ...");
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent service1 = new Intent(context, BeaconService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(service1);
            } else {
                context.startService(service1);
            }
        }
    }
}
