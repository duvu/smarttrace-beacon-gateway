package au.com.smarttrace.beacon.service.jobs;

import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;

import au.com.smarttrace.beacon.App;

public class BeaconJob00 extends Job {
    public static final String TAG = App.PACKAGE + ".tag.scan_and_upload.00";
    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        BeaconJobX.schedule();
        return Result.SUCCESS;
    }

    public static void schedule() {
        new JobRequest.Builder(TAG)
                .startNow()
                .build()
                .schedule();
    }
}
