package au.com.smarttrace.beacon.service.jobs;

import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;

import au.com.smarttrace.beacon.App;
import au.com.smarttrace.beacon.AppConfig;
import au.com.smarttrace.beacon.service.BeaconService;

public class BroadcastJob extends Job {
    public static final String TAG = App.PACKAGE + ".broadcast.job.tag";
    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {

        return Result.SUCCESS;
    }

    public static void schedule() {
        new JobRequest.Builder(TAG).startNow().build().schedule();
    }
}
