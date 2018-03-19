package au.com.smarttrace.beacon.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.model.DataLogger;

public class BeaconService extends Service implements BeaconConsumer, BootstrapNotifier {
    private BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);
    private final IBinder binder = new DataBinder();
    private RegionBootstrap regionBootstrap;
    private Map<String, DataLogger> dataLoggerHashMap = new HashMap<>();

    public BeaconService() {
    }
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        Logger.d("[BeaconService] onCreated");
        // wake up the app when a beacon is seen
        Region region = new Region("backgroundRegion",null, null, null);
        regionBootstrap = new RegionBootstrap(this, region);

        beaconManager.bind(this);
    }

    @Override
    public void onDestroy() {
        Logger.i("Destroying by OS");
        beaconManager.unbind(this);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    for (Beacon beacon : beacons) {
                        dataLoggerHashMap.put(beacon.getBluetoothAddress(), new DataLogger(beacon));
                        Logger.d("[BeaconService] BluetoothAddress: " + beacon.getBluetoothAddress());
                        Logger.d("[BeaconService] BluetoothName(): " + beacon.getBluetoothName());
                        Logger.d("[BeaconService] Rssi(): " + beacon.getRssi());
                        Logger.d("[BeaconService] Distance: " + beacon.getDistance());
                        Logger.d("[BeaconService] BeaconTypeCode: " + beacon.getBeaconTypeCode());

                        List<Long> dataList = beacon.getDataFields();

                        for (Long d: dataList) {
                            Logger.d("[BeaconService] Date " + d);
                        }

                    }
                }
            }

        });
        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {   }

    }

    @Override
    public void didEnterRegion(Region region) {

    }

    @Override
    public void didExitRegion(Region region) {

    }

    @Override
    public void didDetermineStateForRegion(int i, Region region) {

    }

    //-- class DataBinder --
    public class DataBinder extends Binder {
        public BeaconService getService() {
            return BeaconService.this;
        }
    }
}
