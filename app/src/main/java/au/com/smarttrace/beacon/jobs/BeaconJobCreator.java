package au.com.smarttrace.beacon.jobs;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

public class BeaconJobCreator implements JobCreator {
    @Nullable
    @Override
    public Job create(@NonNull String tag) {
        switch (tag) {
            case DBSyncJob.TAG:
                return new DBSyncJob();
            case BeaconJob00.TAG:
                return new BeaconJob00();
            case BeaconJob10.TAG:
                return new BeaconJob10();
            case BeaconJob20.TAG:
                return new BeaconJob20();
            default:
                return null;
        }
    }
}
