package au.com.smarttrace.beacon.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import au.com.smarttrace.beacon.R;
import au.com.smarttrace.beacon.ui.MainActivity;

public class CheckServiceRunningJob extends Job {
    public static final String JOBS_TAG_PERIODIC = "beacon_sync_job_periodic_tag";

    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        boolean success = (new BeaconSyncEngine(getContext())).startService();

        PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 0, new Intent(getContext(), MainActivity.class), 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(JOBS_TAG_PERIODIC, "Job Demo", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Job demo job");
            getContext().getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(getContext(), JOBS_TAG_PERIODIC)
                .setContentTitle(getContext().getString(R.string.app_name))
                .setContentText("Job ran, exact " + params.isExact() + " , periodic " + params.isPeriodic() + ", transient " + params.isTransient())
                .setTimeoutAfter(10*1000)
                .setChannelId(JOBS_TAG_PERIODIC)
                .setSound(null)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.notification)
                .setShowWhen(true)
                .setColor(Color.GREEN)
                .setLocalOnly(true)
                .build();

        NotificationManagerCompat.from(getContext()).notify(new Random().nextInt(), notification);

        return success ? Result.SUCCESS : Result.FAILURE;
    }
    public static void scheduleJobPeriodic() {
        new JobRequest.Builder(JOBS_TAG_PERIODIC)
                .setPeriodic(TimeUnit.MINUTES.toMillis(15), TimeUnit.MINUTES.toMillis(5))
                .setUpdateCurrent(true)
                .build()
                .schedule();
    }
}
