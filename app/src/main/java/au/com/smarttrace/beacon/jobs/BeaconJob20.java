package au.com.smarttrace.beacon.jobs;

import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;

import java.util.concurrent.TimeUnit;

import au.com.smarttrace.beacon.App;

public class BeaconJob20 extends Job {
    public static final String TAG = App.PACKAGE + ".tag.scan_and_upload.20";


    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        boolean success = (new BeaconEngine(getContext())).scanAndUpload();
        return success ? Result.SUCCESS : Result.FAILURE;
    }

    public static void schedule() {
        new JobRequest.Builder(TAG)
                .setPeriodic(TimeUnit.MINUTES.toMillis(20), TimeUnit.MINUTES.toMillis(5))
                .build()
                .scheduleAsync();
    }
}
