package au.com.smarttrace.beacon.service;

import android.location.Location;
import android.os.AsyncTask;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.db.EventData;
import au.com.smarttrace.beacon.db.SensorData;
import au.com.smarttrace.beacon.model.BeaconPackage;
import au.com.smarttrace.beacon.model.BroadcastEvent;
import au.com.smarttrace.beacon.net.DataUtil;
import au.com.smarttrace.beacon.net.WebService;
import io.objectbox.Box;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class UploadDataAsync extends AsyncTask<Void, Void, Boolean> {
    private Box<EventData> eventDataBox;
    private BroadcastEvent event;
    private Location currentLocation;

    public UploadDataAsync() {
    }

    public UploadDataAsync(Box<EventData> eventDataBox) {
        this.eventDataBox = eventDataBox;
    }

    public UploadDataAsync(Box<EventData> eventDataBox, BroadcastEvent event) {
        this.eventDataBox = eventDataBox;
        this.event = event;
    }

    public UploadDataAsync(Box<EventData> eventDataBox, BroadcastEvent event, Location currentLocation) {
        this.eventDataBox = eventDataBox;
        this.event = event;
        this.currentLocation = currentLocation;
    }

    public Box<EventData> getEventDataBox() {
        return eventDataBox;
    }

    public void setEventDataBox(Box<EventData> eventDataBox) {
        this.eventDataBox = eventDataBox;
    }

    public BroadcastEvent getEvent() {
        return event;
    }

    public void setEvent(BroadcastEvent event) {
        this.event = event;
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(Location currentLocation) {
        this.currentLocation = currentLocation;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        if (NetworkUtils.isInternetAvailable()) {
            Logger.i("[Online] Network is online");
            //1. upload old data
            List<EventData> evdtList = eventDataBox.getAll();
            for (EventData evdt : evdtList) {
                final long evId = evdt.getId();
                Logger.i("[*] check: " + evdt.toString());
                WebService.sendEvent(evdt.toString(), new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        // remove data from db;
                        eventDataBox.remove(evId);
                    }
                });
            }

            //2. upload new data
            String dataForUpload = DataUtil.formatData(event);
            WebService.sendEvent(dataForUpload);
        } else {
            Logger.i("[Offline] Network is offline, going to store data");
            EventData evdt = new EventData();
            evdt.setPhoneImei(NetworkUtils.getGatewayId());
            Long timestamp = (new Date()).getTime();
            evdt.setTimestamp(timestamp);
            if (currentLocation != null) {
                evdt.setLatitude(currentLocation.getLatitude());
                evdt.setLongitude(currentLocation.getLongitude());
                evdt.setAltitude(currentLocation.getAltitude());
                evdt.setAccuracy(currentLocation.getAccuracy());
                evdt.setSpeedKPH(currentLocation.getSpeed());
            } else {
                evdt.setLatitude(0);
                evdt.setLongitude(0);
                evdt.setAltitude(0);
                evdt.setAccuracy(0);
                evdt.setSpeedKPH(0);
            }

            List<BeaconPackage> dataList = event.getBeaconPackageList();
            for (BeaconPackage data : dataList) {
                SensorData sd = new SensorData();
                sd.setSerialNumber(data.getSerialNumberString());
                sd.setName(data.getName());
                sd.setTemperature(data.getTemperature());
                sd.setHumidity(data.getHumidity());
                sd.setRssi(data.getRssi());
                sd.setDistance(data.getDistance());
                sd.setBattery(DataUtil.battPercentToVolt(data.getPhoneBatteryLevel()*100));
                sd.setLastScannedTime(data.getTimestamp());
                sd.setHardwareModel(data.getModel());

                evdt.getSensorDataList().add(sd);
            }
            long id = eventDataBox.put(evdt);
            Logger.i("[+] stored #" + id);
        }
        return true;
    }

    @Override
    protected void onPostExecute(final Boolean success) {
//        mAuthTask = null;
//        showProgress(false);
//
//        if (success) {
//            moveToMain();
//            finish();
//        } else {
//            mPasswordView.setError(getString(R.string.error_incorrect_password));
//            mPasswordView.requestFocus();
//        }
    }
}
