package au.com.smarttrace.beacon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.orhanobut.hawk.Hawk;

import au.com.smarttrace.beacon.service.BeaconService;
import au.com.smarttrace.beacon.ui.SplashActivity;

public class StartupReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.i("[StartupReceiver] starting ...");
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // store flag
            SharedPref.saveOnBoot(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent activity = new Intent(context, SplashActivity.class);
                activity.putExtra(BeaconService.EXTRA_STARTED_FROM_BOOTSTRAP, true);
                activity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(activity);
            } else {
//                Intent activity = new Intent(context, SplashActivity.class);
//                activity.putExtra(BeaconService.EXTRA_STARTED_FROM_BOOTSTRAP, true);
//                activity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                context.startActivity(activity);
                Intent i = new Intent(context, BeaconService.class);
                i.putExtra(BeaconService.EXTRA_STARTED_FROM_BOOTSTRAP, true);
                context.startService(i);

            }
        }
    }
}
