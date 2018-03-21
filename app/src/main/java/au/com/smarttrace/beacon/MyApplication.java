package au.com.smarttrace.beacon;

import android.app.Application;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import au.com.smarttrace.beacon.model.MyObjectBox;
import io.objectbox.BoxStore;

/**
 * Created by beou on 3/19/18.
 */

public class MyApplication extends Application {

    // private BackgroundPowerSaver backgroundPowerSaver;
    private RegionBootstrap regionBootstrap;
    private BoxStore boxStore;

    public void onCreate() {
        super.onCreate();
        AppConfig.populateSetting(MyApplication.this);
        //-- init database
        boxStore = MyObjectBox.builder().androidContext(MyApplication.this).build();

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
    }
    public BoxStore getBoxStore() {
        return boxStore;
    }
}
