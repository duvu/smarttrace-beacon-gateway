package au.com.smarttrace.beacon;

import android.app.Application;
import android.content.Intent;

import com.evernote.android.job.JobManager;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import au.com.smarttrace.beacon.db.MyObjectBox;
import au.com.smarttrace.beacon.service.BeaconJobCreator;
import au.com.smarttrace.beacon.service.NetworkUtils;
import au.com.smarttrace.beacon.ui.SplashActivity;
import io.objectbox.BoxStore;
import io.objectbox.android.AndroidObjectBrowser;

/**
 * Created by beou on 3/19/18.
 */

public class App extends Application implements BootstrapNotifier {
    private final String PACKAGE = "au.com.smarttrace.beacon";
    private final String BOOTSTRAP_REGION = PACKAGE + ".bootstrap_region";
    private BoxStore boxStore;
    // private BackgroundPowerSaver backgroundPowerSaver;
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



        BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);
        // By default the AndroidBeaconLibrary will only find AltBeacons.  If you wish to make it
        // find a different type of beacon, you must specify the byte layout for that beacon's
        // advertisement with a line like below.  The example shows how to find a beacon with the
        // same byte layout as AltBeacon but with a beaconTypeCode of 0xaabb.  To find the proper
        // layout expression for other beacon types, do a web search for "setBeaconLayout"
        // including the quotes.
        //        ALTBEACON	m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25
        //        EDDYSTONE  TLM	x,s:0-1=feaa,m:2-2=20,d:3-3,d:4-5,d:6-7,d:8-11,d:12-15
        //        EDDYSTONE  UID	s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19
        //        EDDYSTONE  URL	s:0-1=feaa,m:2-2=10,p:3-3:-41,i:4-20v
        //        IBEACON	m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24
        beaconManager.getBeaconParsers().clear();
//        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.ALTBEACON_LAYOUT)); //"m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
//        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_TLM_LAYOUT)); //"x,s:0-1=feaa,m:2-2=20,d:3-3,d:4-5,d:6-7,d:8-11,d:12-15"));
//        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT)); //"s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19"));
//        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_URL_LAYOUT)); //"s:0-1=feaa,m:2-2=10,p:3-3:-41,i:4-20v"));
//        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.URI_BEACON_LAYOUT)); //"m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
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

    @Override
    public void didEnterRegion(Region region) {
        // check if service is running
        // if not, start it.
        Logger.i("Beacon enter region ... ");
        if (!isActivityVisible() && !isServiceRunning()) {
            //start activity
            Intent i = new Intent(this, SplashActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.startActivity(i);
        }
    }

    @Override
    public void didExitRegion(Region region) {
        Logger.d("Exit region ...");
    }

    @Override
    public void didDetermineStateForRegion(int i, Region region) {

    }

    public BoxStore getBoxStore() {
        return boxStore;
    }
}
