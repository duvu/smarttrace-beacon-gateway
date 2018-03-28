package io.smarttrace.beacon.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import io.smarttrace.beacon.R;
import io.smarttrace.beacon.ShutdownReceiver;


/**
 * Created by beou on 3/21/18.
 */

public class NotificationUtil {
    private static final String EXTRA_NOTIFICATION_ID = "io.smarttrace.beacon.EXTRA_NOTIFICATION_ID";
    private static final String GROUP_KEY_SMARTTRACE_IO = "io.smarttrace.beacon.GROUP_KEY_SMARTTRACE_IO";
    private static NotificationManager notificationManager;
    private static Context context;
    public static void init(Context context) {
        NotificationUtil.context = context;
        notificationManager = getNotificationManager(context);
    }

    public static void createNotificationChannel(String channelId) {
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

    public static Notification createNotification(String channelId) {
        createNotificationChannel(channelId);

        Intent shudownIntent = new Intent(context, ShutdownReceiver.class);
        shudownIntent.setAction("ACTION_SHUTDOWN");
        shudownIntent.putExtra(EXTRA_NOTIFICATION_ID, 0);
        PendingIntent shudownPendingIntent = PendingIntent.getBroadcast(context, 0, shudownIntent, 0);

        return new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.notification)  // the status icon
                .setTicker(context.getText(R.string.smarttrace_status_text))  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(context.getText(R.string.local_service_label))  // the label of the entry
                //.setContentText(context.getText(R.string.beacon_status_text))  // the contents of the entry
                //.setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                //.addAction(R.drawable.objectbox_notification, context.getString(R.string.shutdown), shudownPendingIntent)
                .setGroup(GROUP_KEY_SMARTTRACE_IO)
                .setOngoing(true)
                //.setSound(Uri.parse("android.resource://"+context.getPackageName()+"/"+R.raw.mute_sound))
                .build();
    }

    public static Notification createNotification(String channelId, String title, String text) {
        createNotificationChannel(channelId);

        Intent shudownIntent = new Intent(context, ShutdownReceiver.class);
        shudownIntent.setAction("ACTION_SHUTDOWN");
        shudownIntent.putExtra(EXTRA_NOTIFICATION_ID, 0);
        PendingIntent shudownPendingIntent = PendingIntent.getBroadcast(context, 0, shudownIntent, 0);

        return new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.notification)  // the status icon
                .setTicker(context.getText(R.string.smarttrace_status_text))  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(title)  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setColorized(true)
                .setColor(context.getResources().getColor(R.color.colorPrimaryDark))
                .setGroup(GROUP_KEY_SMARTTRACE_IO)
                .build();
    }

    public static void notify(int id, Notification notification) {
        if (notificationManager != null) {
            notificationManager.notify(id, notification);
        }
    }

    private static NotificationManager getNotificationManager(Context context) {
        if (notificationManager == null) {
            notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return notificationManager;
    }
}
