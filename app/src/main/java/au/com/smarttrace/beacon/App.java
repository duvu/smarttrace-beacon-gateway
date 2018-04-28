package au.com.smarttrace.beacon;

import android.app.Application;
import android.content.Intent;

import com.evernote.android.job.JobManager;
import com.google.firebase.FirebaseApp;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import au.com.smarttrace.beacon.db.MyObjectBox;
import au.com.smarttrace.beacon.service.jobs.BeaconJobCreator;
import au.com.smarttrace.beacon.service.NetworkUtils;
import au.com.smarttrace.beacon.ui.SplashActivity;
import io.objectbox.BoxStore;
import io.objectbox.android.AndroidObjectBrowser;

/**
 * Created by beou on 3/19/18.
 */

public class App extends Application implements BootstrapNotifier {
    public static final String PACKAGE = "au.com.smarttrace.beacon";
    private final String BOOTSTRAP_REGION = PACKAGE + ".bootstrap_region";

    private BoxStore boxStore;
    private RegionBootstrap regionBootstrap;

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
        try {
            JobManager.create(this).addJobCreator(new BeaconJobCreator());
        } catch (Exception e) {
            Logger.e("[>_] Failed to init JobManager", e);
        }

        AppConfig.populateSetting(App.this);
        boxStore = MyObjectBox.builder().androidContext(App.this).build();

        if (BuildConfig.DEBUG) {
            new AndroidObjectBrowser(boxStore).start(this);
        }
        Logger.d("Using ObjectBox " + BoxStore.getVersion() + " (" + BoxStore.getVersionNative() + ")");

        BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this.getApplicationContext());
        beaconManager.getBeaconParsers().clear();
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("s:0-1=cbff,m:2-2=11,i:3-4,i:5-5,i:6-9,p:10-10,d:11-11=04,d:12-13,d:14-15,d:16-18"));
        Region region3901 = new Region(BOOTSTRAP_REGION, Identifier.fromInt(0x3901), null, null);
        Region region3A01 = new Region(BOOTSTRAP_REGION, Identifier.fromInt(0x3A01), null, null);
        Region region3C01 = new Region(BOOTSTRAP_REGION, Identifier.fromInt(0x3C01), null, null);
        Region region3A04 = new Region(BOOTSTRAP_REGION, Identifier.fromInt(0x3A04), null, null);
        regionBootstrap = new RegionBootstrap(this, region3901);
        regionBootstrap.addRegion(region3A01);
        regionBootstrap.addRegion(region3C01);
        regionBootstrap.addRegion(region3A04);
    }

    public BoxStore getBoxStore() {
        return boxStore;
    }

    @Override
    public void didEnterRegion(Region region) {
        // check if service is running
        // if not, start it.
        Logger.d("[>_] Beacon enter region ... Service is running: " + isServiceRunning());
        if (!isActivityVisible() && !isServiceRunning()) {
            //start activity
            Intent i = new Intent(this, SplashActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.startActivity(i);
        }
    }

    @Override
    public void didExitRegion(Region region) {
        Logger.d("[*] Exit region ...");
    }

    @Override
    public void didDetermineStateForRegion(int i, Region region) {

    }
}
