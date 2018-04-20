package au.com.smarttrace.beacon.service.job;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.os.NetworkOnMainThreadException;
import android.os.SystemClock;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import au.com.smarttrace.beacon.App;
import au.com.smarttrace.beacon.GsonUtils;
import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.SharedPref;
import au.com.smarttrace.beacon.db.EventData;
import au.com.smarttrace.beacon.db.Locations;
import au.com.smarttrace.beacon.db.Locations_;
import au.com.smarttrace.beacon.db.PhonePaired;
import au.com.smarttrace.beacon.db.PhonePaired_;
import au.com.smarttrace.beacon.net.WebService;
import au.com.smarttrace.beacon.net.model.LocationBody;
import au.com.smarttrace.beacon.net.model.LocationResponse;
import au.com.smarttrace.beacon.net.model.PairedBeaconResponse;
import au.com.smarttrace.beacon.net.model.UserResponse;
import au.com.smarttrace.beacon.service.NetworkUtils;
import io.objectbox.Box;
import io.objectbox.query.Query;
import io.objectbox.query.QueryBuilder;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class EngineBeaconSync {
    private Context mContext;
    public EngineBeaconSync(Context context) {
        mContext = context;
    }

    @WorkerThread
    public boolean sync() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new NetworkOnMainThreadException();
        }

        SystemClock.sleep(1_000);
        boolean success = Math.random() > 0.1; // successful 90% of the time
        return  success;
    }

    @WorkerThread
    public boolean syncPairedBeacons() {
        Logger.i("[>] Start Sync Paired-DB");
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new NetworkOnMainThreadException();
        }

        String gatewayId = NetworkUtils.getGatewayId();
        String token = SharedPref.getToken();
        Box<PhonePaired> pairedBox = ((App) mContext.getApplicationContext()).getBoxStore().boxFor(PhonePaired.class);
        Box<Locations> locationsBox = ((App) mContext.getApplicationContext()).getBoxStore().boxFor(Locations.class);

        PairedBeaconResponse pbr = WebService.getPairedBeacons(gatewayId, token);
        if (pbr != null && pbr.getStatus().getCode() == 0) {
            //save to db here
            Set<String> pb = pbr.getResponse();

            Query<PhonePaired> queryAll = pairedBox.query()
                    .equal(PhonePaired_.phoneImei, gatewayId)
                    .build();
            List<PhonePaired> lpp = queryAll.find();

            Logger.i("[>] Syncing: " + pb.size());
            for (PhonePaired pp: lpp) {
                if (!pb.contains(pp.getBeaconSerialNumber())) {
                    pairedBox.remove(pp);
                }
            }

            Query<PhonePaired> query = pairedBox.query().equal(PhonePaired_.phoneImei, gatewayId)
                    .equal(PhonePaired_.beaconSerialNumber, "").build();
            for (String b: pb) {
                query.setParameter(PhonePaired_.beaconSerialNumber, b);
                PhonePaired pp = query.findFirst();
                if (pp == null || pp.getId() == null) {
                    pp = new PhonePaired();
                    pp.setPhoneImei(gatewayId);
                    pp.setBeaconSerialNumber(b);
                    pairedBox.put(pp);
                }
            }
        } else {
            return false;
        }

        LocationResponse lr = WebService.getLocations(1, 1000, null, null, token);
        if (lr != null && lr.getStatus().getCode() == 0) {
            List<LocationBody> llb = lr.getResponse();
            Query<Locations> lqueryAll = locationsBox.query().build();
            List<Locations> lloc = lqueryAll.find();

            //removed
            Set<Long> ids = getIds(llb);
            for (Locations loc : lloc) {
                if (!ids.contains(loc.getLocationId())) {
                    locationsBox.remove(loc);
                }
            }

            Query<Locations> query = locationsBox.query().equal(Locations_.locationId, 0L).build();
            if (llb != null && llb.size() > 0) {
                for (LocationBody lb: llb) {
                    query.setParameter(Locations_.locationId, lb.getLocationId());
                    Locations locations = query.findFirst();
                    if (locations == null || locations.getId() == null) {
                        locations = new Locations();
                        locations.updateFromRaw(lb);

                    } else {
                        locations.updateFromRaw(lb);
                    }
                    locationsBox.put(locations);
                }
            }
        } else {
            return false;
        }

        //-- sync timezone
        UserResponse ur = WebService.getUser(token);
        if (ur != null && ur.getStatus().getCode() == 0) {
            String tz = ur.getResponse()!=null ? ur.getResponse().getTimeZone() : "GMT";
            long coId = ur.getResponse()!=null ? ur.getResponse().getInternalCompanyId():0;
            SharedPref.saveUserTimezone(tz);
            SharedPref.saveCompanyId(coId);
        } else {
            return false;
        }

        return true;
    }

    private Set<Long> getIds(List<LocationBody> llb) {
        Set<Long> ids = new HashSet<>();
        for (LocationBody lb: llb) {
            ids.add(lb.getLocationId());
        }
        return ids;
    }
}
