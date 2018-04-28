package au.com.smarttrace.beacon.service.jobs.firebase;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

import org.altbeacon.beacon.BeaconConsumer;

import java.util.Random;

import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.R;
import au.com.smarttrace.beacon.service.BeaconService;
import au.com.smarttrace.beacon.ui.MainActivity;

public class MyJobService extends JobService implements BeaconConsumer {

    @Override
    public boolean onStartJob(JobParameters job) {

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return false;
    }

    private void createNotification() {
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("beacon_engine_tag", "Job Demo", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Smarttrace Jobs Queue");
            this.getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, "beacon_engine_tag")
                .setContentTitle("Smarttrace Jobs Queue")
                .setContentText(" Based on firebasedispatcher ")
                .setAutoCancel(true)
                .setTimeoutAfter(20*60*1000)
                .setChannelId("beacon_engine_tag")
                .setSound(null)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.notification)
                .setShowWhen(true)
                .setColor(Color.GREEN)
                .setLocalOnly(true)
                .build();

        NotificationManagerCompat.from(this).notify(new Random().nextInt(), notification);
    }

    @Override
    public void onBeaconServiceConnect() {
        Logger.i("[+F+] onBeaconServiceConnect");
    }
}
