package au.com.smarttrace.beacon.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import au.com.smarttrace.beacon.App;
import au.com.smarttrace.beacon.R;
import au.com.smarttrace.beacon.service.BeaconEngine;
import au.com.smarttrace.beacon.ui.MainActivity;

public class BeaconJobX extends Job {
    public static final String TAG = App.PACKAGE + ".tag.scan_and_upload.20";


    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        Location success = (new BeaconEngine(getContext())).scanAndUpload();
        createNotification();
        return Result.SUCCESS;
    }

    public static void schedule() {
        new JobRequest.Builder(TAG)
                .setPeriodic(TimeUnit.MINUTES.toMillis(20), TimeUnit.MINUTES.toMillis(5))
                .build()
                .schedule();
    }

    public static void scheduleNow() {
        new JobRequest.Builder(TAG).startNow().build().schedule();
    }

    private void createNotification() {
        PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 0, new Intent(getContext(), MainActivity.class), 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("beacon_engine_tag", "Job Demo", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Job demo job");
            getContext().getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(getContext(), "beacon_engine_tag")
                .setContentTitle("Uploaded data")
                .setContentText(" Background Jobs are running ")
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

        NotificationManagerCompat.from(getContext()).notify(new Random().nextInt(), notification);
    }
}
