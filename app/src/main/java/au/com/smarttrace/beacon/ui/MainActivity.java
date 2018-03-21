package au.com.smarttrace.beacon.ui;

import android.Manifest;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

//import com.TZONE.Bluetooth.Temperature.Model.Device;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.R;
import au.com.smarttrace.beacon.model.AdvancedDevice;
import au.com.smarttrace.beacon.model.BroadcastEvent;
import au.com.smarttrace.beacon.model.Device;
import au.com.smarttrace.beacon.model.ExitEvent;
import au.com.smarttrace.beacon.services.BeaconService;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String[] INITIAL_PERMS={
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
    };

    AdvancedDevice advancedDevice;
    List<Device> deviceList;
    MyArrayAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        requestBluetoothPermission();

        Intent intent1 = new Intent(MainActivity.this, BeaconService.class);
        startService(intent1);


        ListView deviceListView = (ListView) findViewById(R.id.device_listview);
        //deviceList = advancedDevice != null ? advancedDevice.getDeviceList() : null;
        adapter = new MyArrayAdapter(this, R.layout.control_scan_device_list, deviceList);
        deviceListView.setAdapter(adapter);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        //--Toolbar
        final EditText edtSearch = toolbar.findViewById(R.id.edt_search_beacon);
        edtSearch.setVisibility(View.GONE);
        edtSearch.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    edtSearch.setVisibility(View.GONE);
                } else {
                    edtSearch.setVisibility(View.VISIBLE);
                }
            }
        });
        ImageButton btnSearch = (ImageButton) toolbar.findViewById(R.id.btn_search_beacon);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edtSearch.getVisibility() == View.GONE) {
                    edtSearch.setVisibility(View.VISIBLE);
                } else {
                    //do search/filter
                }
            }
        });
        registerEventBus();
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        /*if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else */
        if (id == R.id.nav_manage) {
            startActivity(new Intent(this, SettingsActivity.class));

        }
        /*else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }*/
        else if (id == R.id.nav_exit) {
            EventBus.getDefault().post(new ExitEvent());
            finish();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(INITIAL_PERMS, 1);
        }
    }

//    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
//    public void onUpdateScan(AdvancedDevice data) {
//        Logger.d("[MainActivity] onUpdateScan" + data.getDeviceList().size());
//        //update data
//        advancedDevice = data;
//        deviceList = data.getDeviceList();
//        adapter.setDeviceList(deviceList);
//        //adapter.notifyDataSetChanged();
//        Location location = data.getLocation();
//        for (int i = 0; i < deviceList.size(); i++) {
//            Logger.i("[MainActivity]" + (i+1) + "、SN:" + deviceList.get(i).SN +" Temperature:" + (deviceList.get(i).Temperature != - 1000 ? deviceList.get(i).Temperature : "--") +"℃  Humidity:" + (deviceList.get(i).Humidity != -1000 ? deviceList.get(i).Humidity : "--") + "% Battery:"+deviceList.get(i).Battery+"%");
//        }
//    }
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onUpdateScan(BroadcastEvent data) {
        Logger.d("[MainActivity] onUpdateScan" + data.getDeviceList().size());
        //update data
        //advancedDevice = data;
        deviceList = data.getDeviceList();
        adapter.setDeviceList(deviceList);
        Location location = data.getLocation();
        //for (int i = 0; i < deviceList.size(); i++) {
        //    Logger.i("[MainActivity]" + (i+1) + "、SN:" + deviceList.get(i).SN +" Temperature:" + (deviceList.get(i).Temperature != - 1000 ? deviceList.get(i).Temperature : "--") +"℃  Humidity:" + (deviceList.get(i).Humidity != -1000 ? deviceList.get(i).Humidity : "--") + "% Battery:"+deviceList.get(i).Battery+"%");
        //}
    }

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
