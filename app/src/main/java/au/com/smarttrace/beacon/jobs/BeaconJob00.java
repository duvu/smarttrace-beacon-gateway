package au.com.smarttrace.beacon.jobs;

import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;

import java.util.concurrent.TimeUnit;

import au.com.smarttrace.beacon.App;

public class BeaconJob00 extends Job {
    public static final String TAG = App.PACKAGE + ".tag.scan_and_upload.00";
    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        BeaconJob20.schedule();
        boolean success = (new BeaconEngine(getContext())).scanAndUpload();
        return success ? Result.SUCCESS : Result.FAILURE;
    }

    public static void schedule() {
        new JobRequest.Builder(TAG)
                .startNow()
                .build()
                .scheduleAsync();
    }
}