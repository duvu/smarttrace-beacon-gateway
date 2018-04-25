package au.com.smarttrace.beacon;

import android.app.Application;

import com.evernote.android.job.JobManager;
import com.google.firebase.FirebaseApp;

import au.com.smarttrace.beacon.db.MyObjectBox;
import au.com.smarttrace.beacon.service.BeaconJobCreator;
import au.com.smarttrace.beacon.service.NetworkUtils;
import io.objectbox.BoxStore;
import io.objectbox.android.AndroidObjectBrowser;

/**
 * Created by beou on 3/19/18.
 */

public class App extends Application {
    public static final String PACKAGE = "au.com.smarttrace.beacon";
    private BoxStore boxStore;
    private static boolean activityVisible = false;
    private static boolean serviceRunning = false;

    public static boolean isActivityVisible() {
        return activityVisible;
    }

    public static boolean isServiceRunning() {
        return serviceRunning;
    }

    public static void activityStarted() {
        activityVisible = true;
    }

    public static void activityPaused() {
        activityVisible = false;
    }

    public static void serviceStarted() {
        serviceRunning = true;
    }

    public static void serviceEnded() {
        serviceRunning = false;
    }


    public void onCreate() {
        super.onCreate();
        Logger.d("Application Started!");

        FirebaseApp.initializeApp(this);
        NetworkUtils.init(this);
        SharedPref.init(getApplicationContext());

        //start Android-job
        JobManager.create(this).addJobCreator(new BeaconJobCreator());

        AppConfig.populateSetting(App.this);
        boxStore = MyObjectBox.builder().androidContext(App.this).build();

        if (BuildConfig.DEBUG) {
            new AndroidObjectBrowser(boxStore).start(this);
        }
        Logger.d("Using ObjectBox " + BoxStore.getVersion() + " (" + BoxStore.getVersionNative() + ")");
    }

    public BoxStore getBoxStore() {
        return boxStore;
    }
}
