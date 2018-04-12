package au.com.smarttrace.beacon.ui;

import android.content.Intent;
import android.location.Location;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.R;
import au.com.smarttrace.beacon.model.BT04Package;
import au.com.smarttrace.beacon.model.BroadcastEvent;
import au.com.smarttrace.beacon.model.UpdateEvent;

public class BeaconListActivity extends AppCompatActivity {
    List<BT04Package> BT04PackageList;
    BeaconAdapter adapter;
    ListView deviceListView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon_list);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        deviceListView = (ListView) findViewById(R.id.list_beacons);
        adapter = new BeaconAdapter(this, R.layout.control_scan_device_list, BT04PackageList);
        deviceListView.setAdapter(adapter);

        Button btnScanNow = findViewById(R.id.btn_scan_now);
        btnScanNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EventBus.getDefault().post(new UpdateEvent());
            }
        });
        registerEventBus();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                Intent i = new Intent(BeaconListActivity.this, MainActivity.class);
                startActivity(i);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onUpdateScan(BroadcastEvent data) {
        Logger.d("[MainActivity] onUpdateScan" + data.getBT04PackageList().size());
        //update data
        //advancedDevice = data;
        BT04PackageList = data.getBT04PackageList();
        adapter.setDataPackageList(BT04PackageList);
        Location location = data.getLocation();
        for (int i = 0; i < BT04PackageList.size(); i++) {
            //Logger.i("[MainActivity]" + (i+1) + "、SN:" + BT04PackageList.get(i).getSerialNumber() +" Temperature:" + BT04PackageList.get(i).getTemperature() +"℃  Humidity:" + BT04PackageList.get(i).getHumidity() + "% Battery:"+BT04PackageList.get(i).getBatteryLevel()+"%");
        }
    }
    //--
    private void registerEventBus() {
        EventBus.getDefault().register(this);
    }

    private void unregisterEventBus(){
        try {
            EventBus.getDefault().unregister(this);
        } catch (Throwable t){
            //this may crash if registration did not go through. just be safe
        }
    }
}
