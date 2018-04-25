package au.com.smarttrace.beacon.service;

import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;

import java.util.concurrent.TimeUnit;

import au.com.smarttrace.beacon.App;
import au.com.smarttrace.beacon.AppConfig;

public class BeaconJob05 extends Job {
    public static final String TAG = App.PACKAGE + ".tag.scan_and_upload.05";
    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        BeaconJobX.schedule();
        return Result.SUCCESS;
    }

    public static void schedule() {
        if (AppConfig.DEBUG_ENABLED) {
            new JobRequest.Builder(TAG)
                    .setExact(TimeUnit.MINUTES.toMillis(5))
                    .build()
                    .schedule();
        }
    }
}