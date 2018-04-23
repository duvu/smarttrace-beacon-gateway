package au.com.smarttrace.beacon.firebase;

import android.content.Intent;

import com.evernote.android.job.JobManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import au.com.smarttrace.beacon.App;
import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.jobs.BeaconJob00;
import au.com.smarttrace.beacon.jobs.BeaconJob20;
import au.com.smarttrace.beacon.jobs.DBSyncJob;
import au.com.smarttrace.beacon.ui.SplashActivity;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        Logger.i("[MyFirebaseMessagingService >]: " + remoteMessage.getFrom());
        if (!App.isServiceRunning()) {
            Intent i = new Intent(this, SplashActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        } else {
            JobManager.instance().cancelAllForTag(DBSyncJob.TAG);
            DBSyncJob.schedule();
            JobManager.instance().cancelAllForTag(BeaconJob00.TAG);
            BeaconJob00.schedule();
        }

//        // ...
//
//        // TODO(developer): Handle FCM messages here.
//        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
//        Log.d(TAG, "From: " + remoteMessage.getFrom());
//
//        // Check if message contains a data payload.
//        if (remoteMessage.getData().size() > 0) {
//            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
//
//            if (/* Check if data needs to be processed by long running job */ true) {
//                // For long-running tasks (10 seconds or more) use Firebase Job Dispatcher.
//                schedule();
//            } else {
//                // Handle message within 10 seconds
//                handleNow();
//            }
//
//        }
//
//        // Check if message contains a notification payload.
//        if (remoteMessage.getNotification() != null) {
//            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
//        }
//
//        // Also if you intend on generating your own notifications as a result of a received FCM
//        // message, here is where that should be initiated. See sendNotification method below.
    }
}
