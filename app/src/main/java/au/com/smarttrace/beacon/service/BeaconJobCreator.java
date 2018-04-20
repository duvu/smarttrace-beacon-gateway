package au.com.smarttrace.beacon.service;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

import au.com.smarttrace.beacon.service.job.BeaconDataJob;
import au.com.smarttrace.beacon.service.job.BeaconSyncJob;

public class BeaconJobCreator implements JobCreator {
    @Nullable
    @Override
    public Job create(@NonNull String tag) {
        switch (tag) {
            case BeaconSyncJob.JOBS_TAG:
                return new BeaconSyncJob();
            case BeaconDataJob.DATA_JOB_TAG:
            case BeaconDataJob.DATA_ONCE_JOB_TAG:
                return new BeaconDataJob();
            default:
                return null;
        }
    }
}
