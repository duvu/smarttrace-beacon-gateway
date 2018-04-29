package au.com.smarttrace.beacon.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.AlarmClock;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import au.com.smarttrace.beacon.AppConfig;
import au.com.smarttrace.beacon.Logger;
import au.com.smarttrace.beacon.R;
import au.com.smarttrace.beacon.SharedPref;
import au.com.smarttrace.beacon.net.Http;
import au.com.smarttrace.beacon.net.model.LoginResponse;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class SplashActivity extends AppCompatActivity {

    private static final String[] INITIAL_PERMS={
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.SET_ALARM,
            Manifest.permission.DISABLE_KEYGUARD
    };

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    private UserLoginTask mAuthTask = null;

    private static final int UI_ANIMATION_DELAY = 100;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };

    private final Runnable mMoveToLogin = new Runnable() {
        @Override
        public void run() {
            moveToLogin();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);
        mContentView = findViewById(R.id.fullscreen_content);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);

        ignoreBattOpt();

        if (!checkPermissions()) {
            requestPermissions();
        } else {
            checkLogin();
        }
    }

//    private void setUpAlarmClock() {
//        Intent openNewAlarm = new Intent(AlarmClock.ACTION_SET_ALARM);
//        openNewAlarm.putExtra(AlarmClock.EXTRA_HOUR, 0);
//        openNewAlarm.putExtra(AlarmClock.EXTRA_MINUTES, 5);
//        startActivity(openNewAlarm);
//    }


    @Override
    protected void onStart() {
        super.onStart();
        //checkLogin();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    private void moveToMain() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    private void moveToLogin() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    private void checkLogin() {
        String username = SharedPref.getUserName();
        String password = SharedPref.getPassword();
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            mHideHandler.removeCallbacks(mMoveToLogin);
            mHideHandler.postDelayed(mMoveToLogin, 1000);
        } else {
            //try to login
            mAuthTask = new UserLoginTask(username, password);
            mAuthTask.execute((Void) null);
        }
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
                            ActivityCompat.requestPermissions(SplashActivity.this, INITIAL_PERMS, REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    })
                    .show();
        } else {
            Logger.i("Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(SplashActivity.this, INITIAL_PERMS, REQUEST_PERMISSIONS_REQUEST_CODE);
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

                    checkLogin();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    @SuppressLint("StaticFieldLeak")
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            String url = AppConfig.WEB_SERVICE_URL + "/login?email=" + mEmail + "&password="+mPassword;
            try {
                LoginResponse response = Http.getIntance().get(url, LoginResponse.class);
                if (response.getStatus().getCode() != 0) {
                    return false;
                }
                //-- start store login data
                // get more data and store
                return storeUserData(response);
            } catch (IOException e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;

            if (success) {
                moveToMain();
                finish();
            } else {
                moveToLogin();
                finish();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
        }

        private boolean storeUserData(LoginResponse data) {
            if (data != null) {
                if (data.getResponse() != null) {
                    SharedPref.saveToken(data.getResponse().getToken());
                    SharedPref.saveExpiredStr(data.getResponse().getExpired());
                    SharedPref.saveTokenInstance(data.getResponse().getInstance());
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }

        }
    }
}
