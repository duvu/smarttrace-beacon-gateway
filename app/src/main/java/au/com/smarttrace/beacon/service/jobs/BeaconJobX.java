package au.com.smarttrace.beacon.service.jobs;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import au.com.smarttrace.beacon.App;
import au.com.smarttrace.beacon.FireLogger;
import au.com.smarttrace.beacon.R;
import au.com.smarttrace.beacon.service.BeaconService;
import au.com.smarttrace.beacon.ui.MainActivity;

import static android.content.Context.BIND_AUTO_CREATE;

public class BeaconJobX extends Job {
    public static final String TAG = App.PACKAGE + ".tag.scan_and_upload.20";
    BeaconService mService;
    boolean mBound;
    PowerManager mPowerManager;
    PowerManager.WakeLock mWakeLokc;

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BeaconService.LocalBinder binder = (BeaconService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };

    public BeaconJobX() {
        Intent i = new Intent(getContext(), BeaconService.class);
        getContext().bindService(i, mConnection, BIND_AUTO_CREATE);
        mBound = true;
    }

    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        FireLogger.d("[>_] Run BeaconJobX ...");
        try {
            mWakeLokc.acquire(10*60*1000);
            mService.start();
        } finally {
            createNotification();
            mWakeLokc.release();
        }
        return Result.SUCCESS;
    }

    public static void schedule() {
        new JobRequest.Builder(TAG)
                .setPeriodic(TimeUnit.MINUTES.toMillis(15), TimeUnit.MINUTES.toMillis(5))
                .build()
                .schedule();
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
