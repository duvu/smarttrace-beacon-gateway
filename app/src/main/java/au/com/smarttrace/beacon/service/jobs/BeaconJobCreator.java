package au.com.smarttrace.beacon.service.jobs;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

import au.com.smarttrace.beacon.service.DBSyncJob;

public class BeaconJobCreator implements JobCreator {
    @Nullable
    @Override
    public Job create(@NonNull String tag) {
        switch (tag) {
            case DBSyncJob.TAG:
                return new DBSyncJob();
            case BeaconJobX.TAG:
                return new BeaconJobX();
            case BroadcastJob.TAG:
                return new BroadcastJob();
            default:
                return null;
        }
    }
}
