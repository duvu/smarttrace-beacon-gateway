package au.com.smarttrace.beacon.service.job;

import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;

import java.util.concurrent.TimeUnit;

public class BeaconDataJob extends Job {
    public static final String DATA_JOB_TAG = "beacon_data_job_tag";


    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        boolean success = (new EngineBeaconData(getContext())).uploadData();
        return success ? Result.SUCCESS : Result.FAILURE;
    }

    public static void scheduleJob() {
        new JobRequest.Builder(DATA_JOB_TAG)
                .setPeriodic(TimeUnit.MINUTES.toMillis(15), TimeUnit.MINUTES.toMillis(5))
                .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                .setUpdateCurrent(true)
                .build()
                .schedule();
    }
}
