package au.com.smarttrace.beacon.service;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

public class BeaconJobCreator implements JobCreator {
    @Nullable
    @Override
    public Job create(@NonNull String tag) {
        switch (tag) {
            case BeaconSyncJob.JOBS_TAG:
                return new BeaconSyncJob();
            case BeaconSyncJob.JOBS_TAG_PERIODIC:
            default:
                return null;
        }
    }
}
