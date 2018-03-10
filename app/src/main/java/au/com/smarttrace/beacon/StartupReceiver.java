package au.com.smarttrace.beacon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import au.com.smarttrace.beacon.services.LoggingService;

public class StartupReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.i("[StartupReceiver] starting ...");
        context.startService(new Intent(context, LoggingService.class));
    }
}
