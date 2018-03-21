package au.com.smarttrace.beacon.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import au.com.smarttrace.beacon.R;

/**
 * Created by beou on 3/21/18.
 */

public class Util {
    private static NotificationManager notificationManager;

    public static void createNotificationChannel(Context context, String channelId) {
        notificationManager = getNotificationManager(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager.getNotificationChannel(channelId) == null) {
            // Create the NotificationChannel
            CharSequence name = context.getString(R.string.channel_name);
            String description = context.getString(R.string.channel_description);
            NotificationChannel mChannel = new NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_LOW);
            mChannel.setDescription(description);
            mChannel.setShowBadge(true);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            assert notificationManager != null;
            notificationManager.createNotificationChannel(mChannel);
        }
    }

    public static Notification createNotification(Context context, String channelId) {
        createNotificationChannel(context, channelId);
        CharSequence text = context.getText(R.string.local_service_started);
        return new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.notification)  // the status icon
                .setTicker(text)  // the status text
                //.setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(context.getText(R.string.local_service_label))  // the label of the entry
                .setContentText(text)  // the contents of the entry
                //.setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .setOngoing(true)
                .setSound(Uri.parse("android.resource://"+context.getPackageName()+"/"+R.raw.mute_sound))
                .build();
    }

    private static NotificationManager getNotificationManager(Context context) {
        if (notificationManager == null) {
            notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return notificationManager;
    }
}
