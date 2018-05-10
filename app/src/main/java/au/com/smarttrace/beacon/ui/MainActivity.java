package au.com.smarttrace.beacon.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
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
import android.widget.TextView;
import android.widget.Toast;

//import com.TZONE.Bluetooth.Temperature.Model.BeaconPackage;

import com.evernote.android.job.JobManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import au.com.smarttrace.beacon.App;
import au.com.smarttrace.beacon.DeviceAdminReceiver;
import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.model.WakeUpEvent;
import au.com.smarttrace.beacon.service.DBSyncJob;
import au.com.smarttrace.beacon.service.ServiceUtils;
import au.com.smarttrace.beacon.R;
import au.com.smarttrace.beacon.SharedPref;
import au.com.smarttrace.beacon.model.BeaconPackage;
import au.com.smarttrace.beacon.model.BroadcastEvent;
import au.com.smarttrace.beacon.model.ExitEvent;
import au.com.smarttrace.beacon.service.BeaconService;

import static au.com.smarttrace.beacon.service.BeaconService.EXTRA_STARTED_FROM_NOTIFICATION;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    // Used in checking for runtime permissions.
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    private MyReceiver myReceiver;
    private BeaconService mService = null;
    private boolean mBound = false;

    private View mProgressView;
    private View mMainScreenView;

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
        makeServiceRunning();
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
        super.onDestroy();
        Logger.d("[MainActivity] Destroying");

        unregisterEventBus();
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
        }
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

    private void makeServiceRunning() {
        Intent i = getIntent();
        boolean startFromNotification = i.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false);
        Logger.d("[MainActivity] startFromNotification: " + startFromNotification);
        if (!App.isServiceRunning()) {
            Intent intent1 = new Intent(MainActivity.this, BeaconService.class);
            startService(intent1);

            JobManager.instance().cancelAllForTag(DBSyncJob.TAG);

            DBSyncJob.scheduleNow();
            DBSyncJob.schedule(); // update & sync every 15 minutes
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
        //setTest();
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
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}
