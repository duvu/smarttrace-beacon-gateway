package au.com.smarttrace.beacon.firebase;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.PowerManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.greenrobot.eventbus.EventBus;

import au.com.smarttrace.beacon.App;
import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.model.UpdateEvent;
import au.com.smarttrace.beacon.service.BeaconService;
import au.com.smarttrace.beacon.service.DBSyncJob;
import au.com.smarttrace.beacon.service.NetworkUtils;
import au.com.smarttrace.beacon.ui.SplashActivity;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    BeaconService mService;
    boolean mBound;
    PowerManager mPowerManager;
    PowerManager.WakeLock mWakeLokc;

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BeaconService.LocalBinder binder = (BeaconService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };

    PendingIntent wakeupIntent, bleWakeupIntent;

    @Override
    public void onCreate() {
        super.onCreate();
        Intent iService = new Intent(this, BeaconService.class);
        bindService(iService, mConnection, BIND_AUTO_CREATE);
        mBound = true;

        wakeupIntent = PendingIntent.getBroadcast(getBaseContext(), 0, new Intent("com.android.internal.location.ALARM_WAKEUP"), 0);
        bleWakeupIntent = PendingIntent.getBroadcast(getBaseContext(), 0, new Intent("com.android.bluetooth.btservice.action.ALARM_WAKEUP"), 0);

        mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLokc = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "myWakeLockTag");
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        try {
            mWakeLokc.acquire(10*60*1000);
            Logger.i("[MyFirebaseMessagingService >]: " + remoteMessage.getFrom());
            if (!App.isServiceRunning()) {
                Intent i = new Intent(this, SplashActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
            } else {
//            JobManager.instance().cancelAllForTag(DBSyncJob.TAG);
                DBSyncJob.scheduleNow();
//            JobManager.instance().cancelAllForTag(BeaconJob00.TAG);
////            BeaconJob00.schedule();
////            JobManager.instance().cancelAllForTag(BeaconJob10.TAG);
////            BeaconJob10.schedule();
//            BeaconJobX.scheduleNow();
            }


            if (mService != null) {
                mService.start();
                //FirebaseDatabase.getInstance().getReference("logs").child(NetworkUtils.getGatewayId()).child(System.currentTimeMillis()+"").setValue("Start From FCM Message");
            } else {
                //FirebaseDatabase.getInstance().getReference("logs").child(NetworkUtils.getGatewayId()).child(System.currentTimeMillis()+"").setValue("Service Is Null");
            }
            EventBus.getDefault().post(new UpdateEvent());
        } finally {
            mWakeLokc.release();
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
