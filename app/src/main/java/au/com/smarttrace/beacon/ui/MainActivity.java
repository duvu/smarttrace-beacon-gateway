package au.com.smarttrace.beacon.ui;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

//import com.TZONE.Bluetooth.Temperature.Model.BeaconPackage;

import com.evernote.android.job.JobManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.com.smarttrace.beacon.App;
import au.com.smarttrace.beacon.DeviceAdminReceiver;
import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.model.WakeUpEvent;
import au.com.smarttrace.beacon.service.jobs.BeaconJob00;
import au.com.smarttrace.beacon.service.jobs.BeaconJob05;
import au.com.smarttrace.beacon.service.jobs.BeaconJob10;
import au.com.smarttrace.beacon.service.jobs.BeaconJob15;
import au.com.smarttrace.beacon.service.jobs.BeaconJobX;
import au.com.smarttrace.beacon.service.DBSyncJob;
import au.com.smarttrace.beacon.service.ServiceUtils;
import au.com.smarttrace.beacon.R;
import au.com.smarttrace.beacon.SharedPref;
import au.com.smarttrace.beacon.model.BeaconPackage;
import au.com.smarttrace.beacon.model.BroadcastEvent;
import au.com.smarttrace.beacon.model.ExitEvent;
import au.com.smarttrace.beacon.service.BeaconService;
import au.com.smarttrace.beacon.service.jobs.firebase.Dispatcher;

import static au.com.smarttrace.beacon.service.BeaconService.EXTRA_STARTED_FROM_NOTIFICATION;

@TargetApi(Build.VERSION_CODES.M)
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String[] INITIAL_PERMS={
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
    };

    // Used in checking for runtime permissions.
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

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

    protected DevicePolicyManager mDPM;
    protected ComponentName mDeviceAdmin;
    protected boolean mAdminActive;
    private static final int REQUEST_CODE_ENABLE_ADMIN = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDPM = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        mDeviceAdmin = new ComponentName(this, DeviceAdminReceiver.class);


        myReceiver = new MyReceiver();
        setContentView(R.layout.activity_main);
        registerEventBus();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView txt2Content = findViewById(R.id.txt_2_content);
        String textStr = getString(R.string.txt_statement_press_power_button);
        SpannableString text = new SpannableString(textStr);

        text.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.textColorWarn)), 0, textStr.length(), 0);
        text.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.colorPrimaryDark)), 39, 45, 0);
        text.setSpan(new StyleSpan(Typeface.BOLD), 10, 15, 0);
        text.setSpan(new StyleSpan(Typeface.BOLD), 39, 45, 0);
        txt2Content.setText(text, TextView.BufferType.SPANNABLE);

        mProgressView = findViewById(R.id.main_progress);
        mMainScreenView = findViewById(R.id.main_screen);

        ignoreBattOpt();

        if (!checkPermissions()) {
            requestPermissions();
        } else {
            makeServiceRunning();
        }

//        Button btnAdm = findViewById(R.id.btn_admin);
//
//        btnAdm.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Logger.d("[>_] Made admin");
                admin();
//            }
//        });
    }

    private void admin() {
        //-- start admin
        Logger.d("Enabled Admin");
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mDeviceAdmin);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.app_name));
        startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);


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
    public void onDestroy() {
        Logger.d("[MainActivity] Destroying");
        unregisterEventBus();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            //super.onBackPressed();
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
            case R.id.action_remove:
                mDPM.removeActiveAdmin(mDeviceAdmin);
                break;
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
     * Manifest.permission.ACCESS_COARSE_LOCATION,
     * Manifest.permission.WRITE_EXTERNAL_STORAGE,
     * Manifest.permission.READ_PHONE_STATE,
     * Manifest.permission.BLUETOOTH,
     * Manifest.permission.BLUETOOTH_ADMIN
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
        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION);

        if (shouldProvideRationale) {
            Snackbar.make(
                    findViewById(R.id.drawer_layout),
                    R.string.permission_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MainActivity.this, INITIAL_PERMS, REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    })
                    .show();
        } else {
            Logger.i("Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this, INITIAL_PERMS, REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private void makeServiceRunning() {
        Intent i = getIntent();
        boolean startFromNotification = i.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false);
        Logger.d("[MainActivity] startFromNotification: " + startFromNotification);
        if (!App.isServiceRunning()) {
            Intent intent1 = new Intent(MainActivity.this, BeaconService.class);
            startService(intent1);

            JobManager.instance().cancelAllForTag(DBSyncJob.TAG);
            JobManager.instance().cancelAllForTag(BeaconJob00.TAG);
            JobManager.instance().cancelAllForTag(BeaconJob05.TAG);
            JobManager.instance().cancelAllForTag(BeaconJob10.TAG);
            JobManager.instance().cancelAllForTag(BeaconJob15.TAG);
            JobManager.instance().cancelAllForTag(BeaconJobX.TAG);

            DBSyncJob.scheduleNow();
            DBSyncJob.schedule(); // update & sync every 15 minutes
//            Dispatcher.getInstance(this).schedule();
            //BeaconJobX.schedule();
//            BeaconJob00.schedule();
//            BeaconJob05.schedule();
//            BeaconJob10.schedule();
//            BeaconJob15.schedule();
        }

    }

    private void setTest() {
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        KeyguardManager manager = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock lock = manager.newKeyguardLock("abc");
        lock.disableKeyguard();

    }

    @SuppressLint("BatteryLife")
    private void ignoreBattOpt() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                Intent intent = new Intent();
                intent.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS_REQUEST_CODE:
                Map<String, Integer> perms = new HashMap<>();
                for (int i = 0; i < permissions.length; i++) {
                    perms.put(permissions[i], grantResults[i]);
                }

                /**
                 * Returns the current state of the permissions needed.
                 * Manifest.permission.ACCESS_FINE_LOCATION,
                 * Manifest.permission.ACCESS_COARSE_LOCATION,
                 * Manifest.permission.WRITE_EXTERNAL_STORAGE,
                 * Manifest.permission.READ_PHONE_STATE,
                 * Manifest.permission.BLUETOOTH,
                 * Manifest.permission.BLUETOOTH_ADMIN
                 */
                if (perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        perms.get(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        perms.get(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                        perms.get(Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED) {

                    makeServiceRunning();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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

    @Subscribe
    public void onWakeupRequest(WakeUpEvent event) {
        Logger.d("[>_] WakeUpEvent ...");
        setTest();
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
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        if (mMainScreenView != null) {
            mMainScreenView.setVisibility(show ? View.GONE : View.VISIBLE);
            mMainScreenView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mMainScreenView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

        }
        if (mProgressView != null) {
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
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
