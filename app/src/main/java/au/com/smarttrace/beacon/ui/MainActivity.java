package au.com.smarttrace.beacon.ui;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

//import com.TZONE.Bluetooth.Temperature.Model.BT04Package;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.R;
import au.com.smarttrace.beacon.SharedPref;
import au.com.smarttrace.beacon.model.BT04Package;
import au.com.smarttrace.beacon.model.BroadcastEvent;
import au.com.smarttrace.beacon.model.ExitEvent;
import au.com.smarttrace.beacon.model.UpdateEvent;
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

    private View mProgressView;
    private View mMainScreenView;
    private ImageButton mImageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        requestBluetoothPermission();

        Intent intent1 = new Intent(MainActivity.this, BeaconService.class);
        startService(intent1);

        TextView txt2Content = findViewById(R.id.txt_2_content);
        //- Press the POWER button for 6 secs util green light appears
        String textStr = getString(R.string.txt_statement_press_power_button);
        SpannableString text = new SpannableString(textStr);

        text.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.textColorWarn)), 0, textStr.length(), 0);
        text.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.colorPrimaryDark)), 39, 45, 0);
        text.setSpan(new RelativeSizeSpan(1.1f), 0, textStr.length(), 0);
        text.setSpan(new StyleSpan(Typeface.BOLD), 10, 15, 0);
        text.setSpan(new StyleSpan(Typeface.BOLD), 39, 45, 0);

        txt2Content.setText(text, TextView.BufferType.SPANNABLE);

        mProgressView = findViewById(R.id.login_progress);
        mMainScreenView = findViewById(R.id.main_screen);
        mImageButton = findViewById(R.id.btn_logout);
        mImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //TODO: clean saved user, password
                SharedPref.clear();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
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

        } else */
        if (id == R.id.nav_login) {
            startActivity(new Intent(this, LoginActivity.class));
        } else
        if (id == R.id.nav_manage) {
            startActivity(new Intent(this, SettingsActivity.class));
            //overridePendingTransition(R.anim.hold, R.anim.fade_in);

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

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onUpdateScan(BroadcastEvent data) {
        Logger.d("[MainActivity] onUpdateScan" + data.getBT04PackageList().size());
        List<BT04Package> BT04PackageList = data.getBT04PackageList();
        Location location = data.getLocation();
        for (int i = 0; i < BT04PackageList.size(); i++) {
            Logger.d("[MainActivity]" + (i+1) + "、SN:" + BT04PackageList.get(i).getSerialNumber() +" Temperature:" + BT04PackageList.get(i).getTemperature() +"℃  Humidity:" + BT04PackageList.get(i).getHumidity() + "% Battery:"+BT04PackageList.get(i).getBatteryLevel()+"%");
        }
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
}
