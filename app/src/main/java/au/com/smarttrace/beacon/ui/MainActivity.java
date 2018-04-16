package au.com.smarttrace.beacon.ui;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

//import com.TZONE.Bluetooth.Temperature.Model.BeaconPackage;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import au.com.smarttrace.beacon.App;
import au.com.smarttrace.beacon.service.ServiceUtils;
import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.R;
import au.com.smarttrace.beacon.SharedPref;
import au.com.smarttrace.beacon.model.BeaconPackage;
import au.com.smarttrace.beacon.model.BroadcastEvent;
import au.com.smarttrace.beacon.model.ExitEvent;
import au.com.smarttrace.beacon.service.BeaconService;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String[] INITIAL_PERMS={
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
    };

    private MyReceiver myReceiver;
    private BeaconService mService = null;
    private boolean mBound = false;

    private View mProgressView;
    private View mMainScreenView;

    private final Handler mHandler = new Handler();

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        myReceiver = new MyReceiver();
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (!checkPermissions()) {
            requestPermissions();
        }

        Intent intent1 = new Intent(MainActivity.this, BeaconService.class);
        startService(intent1);

        TextView txt2Content = findViewById(R.id.txt_2_content);
        //- Press the POWER button for 6 secs util green light appears
        String textStr = getString(R.string.txt_statement_press_power_button);
        SpannableString text = new SpannableString(textStr);

        text.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.textColorWarn)), 0, textStr.length(), 0);
        text.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.colorPrimaryDark)), 39, 45, 0);
        //text.setSpan(new RelativeSizeSpan(1.1f), 0, textStr.length(), 0);
        text.setSpan(new StyleSpan(Typeface.BOLD), 10, 15, 0);
        text.setSpan(new StyleSpan(Typeface.BOLD), 39, 45, 0);
        txt2Content.setText(text, TextView.BufferType.SPANNABLE);

        mProgressView = findViewById(R.id.main_progress);
        mMainScreenView = findViewById(R.id.main_screen);
        registerEventBus();

        if (getIntent().getBooleanExtra(BeaconService.EXTRA_STARTED_FROM_BOOTSTRAP, false)) {
            //--
            Logger.i("[-A-] Started on-boot");
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //-- unbind service and finish this activity
                    unbindService(mConnection);
                    finish();
                }
            }, 1000);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, BeaconService.class), mConnection, BIND_AUTO_CREATE);
        mBound = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        App.activityStarted();
        LocalBroadcastManager.getInstance(this).registerReceiver(myReceiver, new IntentFilter(BeaconService.ACTION_BROADCAST));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver);
        App.activityPaused();
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (mBound) {
            mBound = false;
            unbindService(mConnection);
        }
        super.onStop();
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
        switch (id) {
            case R.id.action_logout:
                SharedPref.clear();
                mService.stopSelf();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
                return true;
            case R.id.action_beacons:
                Intent i = new Intent(MainActivity.this, BeaconListActivity.class);
                startActivity(i);
                return true;
        }

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_logout) {
//            SharedPref.clear();
//            mService.stopSelf();
//            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
//            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
//            startActivity(intent);
//            return true;
//        }
//
//        if (id == R.id.action_beacons) {
//            Intent i = new Intent(MainActivity.this, BeaconListActivity.class);
//            startActivity(i);
//            return true;
//        }

//        if (id == R.id.action_settings) {
//            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
//            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
//            startActivity(intent);
//            return true;
//        }
//
//        if (id == R.id.action_create_shipment) {
//            if (mBound) {
//                mService.wipeAllDataOut();
//            }
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_login) {
            startActivity(new Intent(this, LoginActivity.class));
        } else
        if (id == R.id.nav_manage) {
            startActivity(new Intent(this, SettingsActivity.class));
        }
        else if (id == R.id.nav_exit) {
            EventBus.getDefault().post(new ExitEvent());
            finish();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    /**
     * Returns the current state of the permissions needed.
     * Manifest.permission.ACCESS_FINE_LOCATION,
     Manifest.permission.ACCESS_COARSE_LOCATION,
     Manifest.permission.WRITE_EXTERNAL_STORAGE,
     Manifest.permission.READ_PHONE_STATE,
     Manifest.permission.BLUETOOTH,
     Manifest.permission.BLUETOOTH_ADMIN
     */
    private boolean checkPermissions() {
        return (
                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) &&
                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) &&
                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) &&
                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) &&
                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)

        );
    }
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(INITIAL_PERMS, 1);
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onUpdateScan(BroadcastEvent data) {
        //Logger.d("[MainActivity] onUpdateScan" + data.getBeaconPackageList().size());
        List<BeaconPackage> BeaconPackageList = data.getBeaconPackageList();
        Location location = data.getLocation();
//        for (int i = 0; i < BeaconPackageList.size(); i++) {
//            Logger.d("[MainActivity]" + (i+1) + "、SN:" + BeaconPackageList.get(i).getSerialNumber() +" Temperature:" + BeaconPackageList.get(i).getTemperature() +"℃  Humidity:" + BeaconPackageList.get(i).getHumidity() + "% Battery:"+BeaconPackageList.get(i).getBatteryLevel()+"%");
//        }
        showProgress(false);
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


    //--
    /**
     * Shows the progress UI and hides the login form.
     */

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mMainScreenView.setVisibility(show ? View.GONE : View.VISIBLE);
            mMainScreenView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mMainScreenView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mMainScreenView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Receiver for broadcasts sent by {@link BeaconService}.
     */
    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Location location = intent.getParcelableExtra(BeaconService.EXTRA_LOCATION);
            if (location != null) {
                Toast.makeText(MainActivity.this, ServiceUtils.getLocationText(location),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}
