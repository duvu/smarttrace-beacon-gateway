package au.com.smarttrace.beacon.service;

import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;

import java.util.concurrent.TimeUnit;

import au.com.smarttrace.beacon.App;

public class DBSyncJob extends Job {
    public static final String TAG = App.PACKAGE + ".tag.db_sync_job";

    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        boolean success = (new DBSyncEngine(getContext())).sync();
        return success ? Result.SUCCESS : Result.FAILURE;
    }

    public static void scheduleNow() {
        new JobRequest.Builder(TAG)
                .startNow()
                .build()
                .schedule();
    }

    public static void schedule() {
        new JobRequest.Builder(TAG)
                .setPeriodic(TimeUnit.MINUTES.toMillis(15), TimeUnit.MINUTES.toMillis(5))
                .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                .build()
                .schedule();

    }
}
