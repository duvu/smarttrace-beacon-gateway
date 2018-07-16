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
        DBSyncEngine dbSyncEngine = new DBSyncEngine(getContext());

        boolean success = dbSyncEngine.sync();
        dbSyncEngine.closeBox();

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
                .setPeriodic(TimeUnit.DAYS.toMillis(30), TimeUnit.DAYS.toMillis(15))
                .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
                .schedule();

    }
}
