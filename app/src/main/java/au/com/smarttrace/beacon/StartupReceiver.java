package au.com.smarttrace.beacon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import au.com.smarttrace.beacon.service.BeaconService;

public class StartupReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.i("[StartupReceiver] starting ...");
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent activity = new Intent(context, BeaconService.class);
                activity.putExtra(BeaconService.EXTRA_STARTED_FROM_BOOTSTRAP, true);
                context.startForegroundService(activity);
                //BootstrapingService.enqueueWork(context, activity);
            } else {
                Intent service1 = new Intent(context, BeaconService.class);
                service1.putExtra(BeaconService.EXTRA_STARTED_FROM_BOOTSTRAP, true);
                context.startService(service1);
            }
        }
    }
}
